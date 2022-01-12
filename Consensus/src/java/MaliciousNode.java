import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

// Malicious nodes
// type 1: never update transactions after initial
public class MaliciousNode extends CompliantNode {
  int type;
  public MaliciousNode(double p_graph, double p_malicious, double p_txDistribution,
                       int numRounds, int type) {
    super(p_graph, p_malicious, p_txDistribution, numRounds);
    this.type = type;
  }

  @Override
  public Set<Transaction> sendToFollowers() {
    // if (type != 1)
    //   txs.clear();
    // return res;
    return pending;
  }

  @Override
  public void receiveFromFollowees(Set<Candidate> candidates) {
    return;
  }
}
