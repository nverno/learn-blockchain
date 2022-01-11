import java.util.ArrayList;
import java.util.HashSet;

public class TxHandler {
  UTXOPool pool;                         // available outputs to claim by inputs
    
  /**
   * Creates a public ledger whose current UTXOPool (collection of unspent
   * transaction outputs) is {@code utxoPool}. This should make a copy of
   * utxoPool by using the UTXOPool(UTXOPool uPool) constructor.
   */
  public TxHandler(UTXOPool utxoPool) {
    pool = new UTXOPool(utxoPool);
  }

  /**
   * @return true if:
   * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
   * (2) the signatures on each input of {@code tx} are valid, 
   * (3) no UTXO is claimed multiple times by {@code tx},
   * (4) all of {@code tx}s output values are non-negative, and
   * (5) the sum of {@code tx}s input values is greater than or equal to 
   *     the sum of its output values; and false otherwise.
   */
  public boolean isValidTx(Transaction tx) {
    HashSet<UTXO> seen = new HashSet<>();
    double sumOut = 0, sumIn = 0;

    int i = 0;
    for (Transaction.Input in : tx.getInputs()) {
      UTXO coin = new UTXO(in.prevTxHash, in.outputIndex);

      // (1) coin is in available pool
      if (!pool.contains(coin)) return false;

      // (2) valid signatures: owner of coin has signed this transaction
      Transaction.Output txout = pool.getTxOutput(coin);
      byte[] msg = tx.getRawDataToSign(i);
      if (!Crypto.verifySignature(txout.address, msg, in.signature))
        return false;
            
      // (3) unique coins being used in this transaction
      if (seen.contains(coin)) return false;
      seen.add(coin);
      sumIn += txout.value;

      i += 1;
    }

    // (4) values >= 0
    for (Transaction.Output o : tx.getOutputs()) {
      if (o.value < 0) return false;
      sumOut += o.value;
    }
    // (5) input value must be at least output value
    return sumIn >= sumOut;
  }

  /**
   * Handles each epoch by receiving an unordered array of proposed
   * transactions, checking each transaction for correctness, returning a
   * mutually valid array of accepted transactions, and updating the current
   * UTXO pool as appropriate.
   */
  public Transaction[] handleTxs(Transaction[] possibleTxs) {
    ArrayList<Transaction> txs = new ArrayList<>();
    for (Transaction t : possibleTxs) {
      if (!isValidTx(t)) continue;
      txs.add(t);

      // remove used coins from available pool
      for (Transaction.Input in : t.getInputs())
        pool.removeUTXO(new UTXO(in.prevTxHash, in.outputIndex));

      // add new coins resulting from transction
      byte[] h = t.getHash();
      for (int i = 0; i < t.numOutputs(); i++)
        pool.addUTXO(new UTXO(h, i), t.getOutput(i));
    }
    Transaction[] res = new Transaction[txs.size()];
    return txs.toArray(res);
  }
}
