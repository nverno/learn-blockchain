import java.util.Arrays;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.PriorityQueue;

public class CompliantNode implements Node {
  protected double p_graph,                  // prob. an edge exists b/w nodes in graph
    p_malicious,                             // prob. a node in graph is malicious
    p_txDistribution;                        // prob. initial tx assigned to node
  protected int numRounds, N, cur;           // total rounds, followees(non-mal), round#
  protected boolean[] followees, mal, seen;  // track malicious nodes
  protected Set<Transaction> pending, consensus;

  public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
    this.p_graph = p_graph;
    this.p_malicious = p_malicious;
    this.p_txDistribution = p_txDistribution;
    this.numRounds = numRounds;
    N = cur = 0;
  }

  public void setFollowees(boolean[] followees) {
    mal = new boolean[followees.length];
    seen = new boolean[followees.length];
    Arrays.fill(mal, false);
    this.followees = followees;
    for (int i = 0; i < followees.length; i++)
      if (followees[i]) N++;
    N -= N * p_malicious;
  }

  // Assumed valid
  public void setPendingTransaction(Set<Transaction> pendingTransactions) {
    consensus = new HashSet<Transaction>(pendingTransactions);
    pending = pendingTransactions;
  }

  // after numRounds behaviour changes to only sending consesus
  public Set<Transaction> sendToFollowers() {
    return (++cur >= numRounds) ? consensus : pending;
  }

  public void receiveFromFollowees(Set<Candidate> candidates) {
    pending.clear();
    Arrays.fill(seen, false);

    HashMap<Transaction,Integer> cnt = new HashMap<>();
    for (Candidate c : candidates) {
      seen[c.sender] = true;
      if (!mal[c.sender]) {
        cnt.merge(c.tx, 1, Integer::sum);
      }
    }

    // Tests are stict on message sizes: so only send
    // those with higher confidence
    final int maxsz = 200;
    PriorityQueue<Transaction> pq = new PriorityQueue<>
      ((a, b) -> -1 * Integer.compare(cnt.get(a), cnt.get(b)));

    for (Map.Entry<Transaction,Integer> e : cnt.entrySet()) {
      pq.add(e.getKey());
      if (e.getValue() > N * (p_graph * p_txDistribution))
        consensus.add(e.getKey());
      else consensus.remove(e.getKey());
    }

    for (Transaction t : pq) {
      if (pending.size() > maxsz) break;
      pending.add(t);
    }

    // track followees that aren't broadcasting
    for (int i = 0; i < followees.length; i++)
      if (!seen[i]) mal[i] = true;
  }
}
