package org.storck.simplex.message

/**
 * The Proposal class represents a proposal for a new block in a blockchain.
 *
 * @param playerId the ID of the player proposing the block
 * @param iterationNumber the iteration number of the block
 * @param newBlock the new block containing the transactions
 * @param parentChain the parent blockchain on which the new block is built
 */
case class Proposal(playerId: String, iterationNumber: Int, newBlock: Block, parentChain: BlockchainNotarized) extends ProtocolIterationMessage

object Proposal {
  /**
   * Requires non-null for various fields.
   *
   * @param playerId the ID of the player/peer
   * @param iterationNumber the iteration number
   * @param newBlock the proposed block
   * @param parentChain the current chain that the block is proposed to be added
   */
  def apply(playerId: String, iterationNumber: Int, newBlock: Block, parentChain: BlockchainNotarized): Proposal = {
    require(playerId != null, "playerId cannot be null")
    require(iterationNumber >= 0, "iterationNumber must be non-negative")
    require(newBlock != null, "newBlock cannot be null")
    require(parentChain != null, "parentChain cannot be null")
    new Proposal(playerId, iterationNumber, newBlock, parentChain)
  }
}