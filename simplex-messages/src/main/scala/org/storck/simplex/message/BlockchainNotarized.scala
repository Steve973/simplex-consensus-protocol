package org.storck.simplex.message

/**
 * The NotarizedBlockchain class represents a blockchain that consists of
 * notarized blocks.
 *
 * @param blocks the list of notarized blocks in the blockchain
 */
case class BlockchainNotarized(blocks: Seq[BlockNotarized]) {

  /**
   * Adds a notarized block to the blockchain.
   *
   * @param newBlock the notarized block to be added
   * @return a new BlockchainNotarized instance with the added block
   */
  def addBlock(newBlock: BlockNotarized): BlockchainNotarized = {
    BlockchainNotarized(blocks :+ newBlock)
  }

  /**
   * Returns the number of blocks in the blockchain as a convenience.
   *
   * @return the number of blocks in the blockchain
   */
  def height: Int = blocks.size
}

object BlockchainNotarized {
  /**
   * Create the notarized blockchain.
   *
   * @param blocks the blocks that constitute the chain
   */
  def apply(blocks: Iterable[BlockNotarized]): BlockchainNotarized = {
    new BlockchainNotarized(blocks.toSeq)
  }
}