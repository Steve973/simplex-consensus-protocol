package org.storck.simplex.message

/**
 * Message indicating that the iteration is finalized.
 *
 * @param playerId the ID of the player that sent the message
 * @param iterationNumber the iteration number to finalizeMsg
 */
case class Finalize(playerId: String, iterationNumber: Int) extends ProtocolIterationMessage

object Finalize {
  /**
   * Create the finalizeMsg message for the iteration.
   *
   * @param playerId the ID of the player that declares the iteration finalized
   * @param iterationNumber the iteration number
   */
  def apply(playerId: String, iterationNumber: Int): Finalize =
    new Finalize(playerId, iterationNumber)
}