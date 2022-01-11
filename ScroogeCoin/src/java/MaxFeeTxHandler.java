import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

public class MaxFeeTxHandler {
  UTXOPool pool;    // available outputs to claim by inputs from previous epochs

  // Temporary variables used during handleTx
  UTXOPool pending;  // temporary pool
  // idx of prereq transactions that created coins during this epoch that
  // are claimed by transaction
  HashMap<Integer,ArrayList<Integer>> depends;
  HashMap<UTXO,Integer> createdBy;  // tx_i that created this output
  ArrayList<HashSet<UTXO>> coins;   // coins claimed by tx_i
  ArrayList<Double> fees;           // tx fee for tx_i
  
  /**
   * Creates a transaction handler that maximizes transaction fees.
   */
  public MaxFeeTxHandler(UTXOPool utxoPool) {
    pool = new UTXOPool(utxoPool);
  }

  /**
   * @return fee for transaction, if negative it is invalid:
   * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
   * (2) the signatures on each input of {@code tx} are valid, 
   * (3) no UTXO is claimed multiple times by {@code tx},
   * (4) all of {@code tx}s output values are non-negative, and
   * (5) the sum of {@code tx}s input values is greater than or equal to 
   *     the sum of its output values; and false otherwise.
   */
  public double txFee(Transaction tx, int idx) {
    Set<UTXO> seen = new HashSet<UTXO>();
    double sumIn = 0, sumOut = 0;
    ArrayList<Integer> deps = new ArrayList<>();

    int i = 0;
    for (Transaction.Input in : tx.getInputs()) {
      UTXO coin = new UTXO(in.prevTxHash, in.outputIndex);
      // (1) coin is available
      if (!pending.contains(coin)) return -1;
      // add prereq tx if coin created in this epoch
      if (!pool.contains(coin))
        deps.add(createdBy.get(coin));

      // (2) valid signatures: owner of coin has signed this transaction
      Transaction.Output txout = pending.getTxOutput(coin);
      byte[] msg = tx.getRawDataToSign(i);
      if (!Crypto.verifySignature(txout.address, msg, in.signature))
        return -1;
            
      // (3) unique coins being used in this transaction
      if (seen.contains(coin)) return -1;
      seen.add(coin);
      sumIn += txout.value;

      i += 1;
    }
    // (4) values >= 0
    for (Transaction.Output o : tx.getOutputs()) {
      if (o.value < 0) return -1;
      sumOut += o.value;
    }
    // (5) sum(inputs) >= sum(outputs)
    if (sumIn - sumOut >= 0) depends.put(idx, deps);
    return sumIn - sumOut;
  }

  /**
   * Handles each epoch by receiving an unordered array of proposed
   * transactions, checking each transaction for correctness, returning a
   * mutually valid array of accepted transactions, and updating the current
   * UTXO pool as appropriate.
   * 
   * Finds set of transactions with maximum total transaction fees - i.e.
   * maximizes the sum over all transactions in the set:
   * (sum of input values - sum of output values). 
   * 
   * Runtime O(2^n) from exhaustively checking potential subsets.
   * 
   * Note: greedy selection isn't maximal, and outputs created in the current
   * epoch can be consumed by other transactions from the same epoch.
   */
  public Transaction[] handleTxs(Transaction[] possibleTxs) {
    // Topographically sorted potential transactions
    ArrayList<Transaction> txs = validTxs(possibleTxs);
    // backtrack to compute best set of transactions to maximize transaction fees
    bestIdx = new ArrayList<>();
    best = 0;
    bt(0, 0, new ArrayList<>(), new HashSet<>(), txs);

    ArrayList<Transaction> rs = new ArrayList<>();
    for (int i : bestIdx) {
      Transaction t = txs.get(i);
      rs.add(t);

      // removed used coins from pool
      for (UTXO utxo : coins.get(i))
        pool.removeUTXO(utxo);

      // add new coins
      byte[] h = t.getHash();
      for (int j = 0; j < t.numOutputs(); j++)
        pool.addUTXO(new UTXO(h, j), t.getOutput(j));
    }

    Transaction[] res = new Transaction[rs.size()];
    return rs.toArray(res);
  }

  // Return list of valid transactions
  // NOTE: the result is topographically sorted, all prereqs (ie.
  // transactions that create a necessary input) will be before a transaction
  // that consumes them in the list
  private ArrayList<Transaction> validTxs(Transaction[] possibleTxs) {
    pending = new UTXOPool(pool);
    createdBy = new HashMap<>();
    depends = new HashMap<>();
    coins = new ArrayList<>();
    fees = new ArrayList<>();
    ArrayList<Transaction> txs = new ArrayList<>();

    // While any new transactions are added
    // (worst case O(n^2) when each transaction relies on previous)
    boolean done = false;
    while (!done) {
      done = true;
      for (Transaction t : possibleTxs) {
        if (txs.contains(t)) continue;
        double fee = txFee(t, /*idx=*/txs.size());
        if (fee < 0) continue;
        done = false;

        // get required coins for transaction
        HashSet<UTXO> cs = new HashSet<>();
        for (Transaction.Input in : t.getInputs()) {
          UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
          cs.add(utxo);
        }

        // add outputs to pending
        byte[] h = t.getHash();
        for (int j = 0; j < t.numOutputs(); ++j) {
          UTXO utxo = new UTXO(h, j);
          createdBy.put(utxo, txs.size());
          pending.addUTXO(new UTXO(h, j), t.getOutput(j));
        }
        txs.add(t);
        coins.add(cs);
        fees.add(fee);
      }
    }
    return txs;
  }

  /// Backtracking: find maximal fee subset of proposed transactions
  double best;
  ArrayList<Integer> bestIdx;

  // Compute best set of transactions to maximize the transaction fee,
  // ie. fee = sumInputs - sumOutputs
  // The result set can't consume the same coin more than once
  private void bt(int i, double txFee, ArrayList<Integer> cur,
                  Set<UTXO> used, ArrayList<Transaction> txs) {
    if (i == txs.size()) {
      if (txFee > best) {
        best = txFee;
        bestIdx = new ArrayList<Integer>(cur);
      }
      return;
    }
    // check all prereqs are in current subset
    boolean hasDeps = true;
    for (Integer j : depends.get(i)) {
      if (cur.contains(j)) continue;
      hasDeps = false;
      break;
    }
    if (hasDeps) {
      // try to use current transaction
      int origSz = used.size(), sz = coins.get(i).size();
      Set<UTXO> s = new HashSet<UTXO>(used);
      s.addAll(coins.get(i));
      // check if all were unique
      if (s.size() - origSz == sz) {
        cur.add(i);
        bt(i+1, txFee + fees.get(i), cur, s, txs);
        cur.remove(cur.size()-1);
      }
    }
    // try without current transaction
    bt(i+1, txFee, cur, used, txs);
  }
}
