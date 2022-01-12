import java.util.Arrays;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

public class CompliantNode implements Node {
  protected double p_graph,            // prob. an edge exists b/w nodes in graph
    p_malicious,                       // prob. a node in graph is malicious
    p_txDistribution;                  // prob. initial tx assigned to node
  protected int numRounds,             // total rounds
    prevRound, curRound;               // previous/current round number
  protected boolean[] followees, mal, seen;  // track malicious nodes
  protected Set<Transaction> sent;   // already sent
  protected Set<Transaction> pending, consensus;

  public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
    this.p_graph = p_graph;
    this.p_malicious = p_malicious;
    this.p_txDistribution = p_txDistribution;
    this.numRounds = numRounds;
    sent = new HashSet<>();
    curRound = prevRound = 0;
  }

  public void setFollowees(boolean[] followees) {
    mal = new boolean[followees.length];
    seen = new boolean[followees.length];
    Arrays.fill(mal, false);
    this.followees = followees;
  }

  // Assumed valid
  public void setPendingTransaction(Set<Transaction> pendingTransactions) {
    consensus = pendingTransactions;
    pending = pendingTransactions;
  }

  public Set<Transaction> sendToFollowers() {
    if (curRound >= numRounds) return consensus;

    HashSet<Transaction> res = new HashSet<>();
    for (Transaction t : pending) {
      if (!sent.contains(t)) {
        res.add(t);
        sent.add(t);
      }
    }
    prevRound = curRound;
    pending.clear();
    return res;
  }

  public void receiveFromFollowees(Set<Candidate> candidates) {
    Arrays.fill(seen, false);
    if (++curRound > prevRound)
      pending.clear();
    
    for (Candidate c : candidates) {
      seen[c.sender] = true;
      if (!mal[c.sender]) {
        pending.add(c.tx);
        consensus.add(c.tx);
      }
    }

    // track followees that aren't broadcasting
    for (int i = 0; i < followees.length; i++)
      if (!seen[i]) mal[i] = true;
  }
}
