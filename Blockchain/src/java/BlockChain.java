import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory as it
// would cause a memory overflow.

public class BlockChain {
  public static final int CUT_OFF_AGE = 10;
  private TransactionPool txPool;       // transactions to build next block
  private BlockNode hd;                 // head of chain
  private Map<byte[],BlockNode> chain;  // represent blockchain

  // store Block on the chain
  private class BlockNode {
    int height;
    Block block;
    UTXOPool pool;
    public BlockNode(int h, Block b, UTXOPool p) {
      height = h;
      block = b;
      pool = p;
    }
  }

  /** Get the maximum height block */
  public Block getMaxHeightBlock() {
    return hd.block;
  }

  /** Get the UTXOPool for mining a new block on top of max height block */
  public UTXOPool getMaxHeightUTXOPool() {
    return hd.pool;
  }

  /** Get the transaction pool to mine a new block */
  public TransactionPool getTransactionPool() {
    return txPool;
  }

  /** Add a transaction to the transaction pool */
  public void addTransaction(Transaction tx) {
    txPool.addTransaction(tx);
  }

  // add block's coinbase UTXO to pool
  private UTXOPool nextPool(Block b, UTXOPool p) {
    Transaction ctx = b.getCoinbase();
    p.addUTXO(new UTXO(ctx.getHash(), /*null*/0), ctx.getOutput(0));
    return p;
  }

  /**
   * Create an empty block chain with just a genesis block. Assume
   * {@code genesisBlock} is a valid block
   * 
   */
  public BlockChain(Block genesisBlock) {
    txPool = new TransactionPool();
    chain = new HashMap<>();
    // initial pool is just output from coinbase transaction
    UTXOPool pool = nextPool(genesisBlock, new UTXOPool());
    hd = new BlockNode(1, genesisBlock, pool);
    chain.put(genesisBlock.getHash(), hd);
  }

  /**
   * Add {@code block} to the block chain if it is valid. For validity, all
   * transactions should be valid and block should be at
   * {@code height > (maxHeight - CUT_OFF_AGE)}.
   * 
   * <p> For example, you can try creating a new block over the genesis block
   * (block height 2) if the block chain height is
   * {@code <= CUT_OFF_AGE + 1}. As soon as
   * {@code height > CUT_OFF_AGE + 1}, you cannot create a new block at
   * height 2.
   * 
   * @return true if block is successfully added
   */
  public boolean addBlock(Block block) {
    byte[] phash = block.getPrevBlockHash();
    if (phash == null || block.getHash() == null || !chain.containsKey(phash))
      return false;

    BlockNode prev = chain.get(phash);
    // can't be too old
    if (hd.height - CUT_OFF_AGE > prev.height)
      return false;
    
    // validate transactions agains available coins in parent block
    ArrayList<Transaction> tlist = block.getTransactions();
    TxHandler handler            = new TxHandler(prev.pool);
    Transaction[] txs            = tlist.toArray(new Transaction[tlist.size()]),
             validTxs            = handler.handleTxs(txs);

    // check all proposed transactions were accepted
    if (txs.length != validTxs.length)
      return false;

    // remove accepted transactions
    for (Transaction t : tlist)
      txPool.removeTransaction(t.getHash());

    // create next pool of coins from coinbase Tx and results of validTxs
    UTXOPool pool = nextPool(block, handler.getUTXOPool());

    // add new block to chain
    BlockNode next = new BlockNode(prev.height+1, block, pool);
    chain.put(block.getHash(), next);

    // update hd
    if (next.height > hd.height) hd = next;
    trimChain();
    return true;
  }

  // Only store head of chain in memory
  private void trimChain() {
    int mn = hd.height - CUT_OFF_AGE - 1;
    ArrayList<byte[]> old = new ArrayList<>();
    for (Map.Entry<byte[],BlockNode> e : chain.entrySet())
      if (e.getValue().height < mn)
        old.add(e.getKey());

    for (byte[] b : old) chain.remove(b);
  }
}
