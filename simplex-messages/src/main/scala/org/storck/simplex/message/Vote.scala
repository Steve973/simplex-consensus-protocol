package org.storck.simplex.message

import org.storck.simplex.message.ProtocolIterationMessage

/**
 * The Vote class represents a vote cast by a player for a specific block in a
 * blockchain.
 *
 * @param playerId the ID of the player that cast this vote
 * @param iterationNumber the iteration number when this vote was cast
 * @param blockHash the hash of the block to which this vote pertains (to
 *     identify the block)
 */
case class Vote(playerId: String, iterationNumber: Int, blockHash: String) extends ProtocolIterationMessage

object Vote {
  /**
   * Create a vote.
   *
   * @param playerId the ID of the player casting the vote
   * @param iterationNumber the protocol iteration
   * @param blockHash the hash/ID of the block that this vote is for
   */
  def apply(playerId: String, iterationNumber: Int, blockHash: String): Vote = {
    require(playerId != null, "playerId cannot be null")
    require(iterationNumber >= 0, "iterationNumber must be non-negative")
    require(blockHash != null, "blockHash cannot be null")
    new Vote(playerId, iterationNumber, blockHash)
  }
}