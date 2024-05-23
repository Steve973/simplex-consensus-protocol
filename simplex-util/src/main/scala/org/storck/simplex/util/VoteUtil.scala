package org.storck.simplex.util

import org.storck.simplex.message.Vote

import java.security.PublicKey
import scala.util.Try

/**
 * Utility class for Vote operations
 */
object VoteUtil {

  /**
   * Checks if the given vote pertains to the current iteration.
   *
   * @param vote the vote to check
   * @param currentIteration the current iteration number
   * @return true if the vote pertains to the current iteration, false otherwise
   */
  def isVoteIterationCurrentIteration(vote: Vote, currentIteration: Int): Boolean =
    vote.iterationNumber == currentIteration

  /**
   * Checks if the given vote pertains to the specified proposal ID.
   *
   * @param vote the vote to check
   * @param proposalId the ID of the proposal compare against
   * @return true if the vote pertains to the proposal ID, false otherwise
   */
  def isVoteIdProposalId(vote: Vote, proposalId: String): Boolean =
    proposalId == vote.blockHash

  /**
   * Checks if the given vote is from a known player.
   *
   * @param playerId the ID of the player that cast the vote
   * @param playerIds collection of all known player IDs
   * @return true if the vote is from a known player, false otherwise
   */
  def isVoteFromKnownPlayer(playerId: String, playerIds: Seq[String]): Boolean =
    playerIds.contains(playerId)

  /**
   * Validates a vote. Checks that it comes from a known player, that it pertains
   * to the current iteration, that the proposal is known, and that the signature
   * is valid, indicating that it really came from the player that the player ID
   * indicates.
   *
   * @param currentIteration the current iteration number
   * @param proposalId the ID of the proposal for this iteration
   * @param vote the signed vote to validate
   * @param voterPublicKey the public key of the voter to verify its signature
   * @return true if the vote is valid, false otherwise
   */
  def validateVote(currentIteration: Int, proposalId: String, playerIds: Seq[String], vote: Vote, voterPublicKey: PublicKey): Try[Boolean] = {
    val voteChecks = List(
      (v: Vote) => isVoteIterationCurrentIteration(v, currentIteration),
      (v: Vote) => isVoteIdProposalId(v, proposalId),
      (v: Vote) => isVoteFromKnownPlayer(v.playerId, playerIds))
    Try(voteChecks.forall(check => check(vote)))
  }
}