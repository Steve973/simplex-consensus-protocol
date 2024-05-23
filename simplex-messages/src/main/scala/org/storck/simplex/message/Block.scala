package org.storck.simplex.message

/**
 * The Block class represents a block in a blockchain.
 */
case class Block(height: Int, parentHash: String, transactions: Seq[?])

/**
 * Creates a dummy block with the specified iteration number.
 */
def createDummyBlock(i: Int): Block = Block(i, "", Seq.empty)

/**
 * Creates the genesis block of a blockchain.
 */
def createGenesisBlock(): Block = createDummyBlock(0)

/**
 * Creates the genesis block of a blockchain.
 */
def createFinalizeBlock(i: Int): Block = Block(i, "FINALIZE", Seq.empty)

/**
 * Creates a notarized block using the given block and set of quorum votes.
 */
def createNotarizedBlock(block: Block, votes: Seq[Vote]): BlockNotarized = BlockNotarized(block, votes)

object Block {

  /**
   * Create a Block.
   *
   * @param height       the height of the block in the chain
   * @param parentHash   the hash of the parent block in the chain
   * @param transactions the transactions for this block
   */
  def apply(height: Int, parentHash: String, transactions: Iterable[?]): Block =
    new Block(height, parentHash, transactions.toSeq)
}