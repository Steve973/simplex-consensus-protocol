package org.storck.simplex.util

import org.storck.simplex.message.{Block, BlockNotarized, Vote}

object BlockchainUtil {

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
}