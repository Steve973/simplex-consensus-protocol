package org.storck.simplex.util

import org.storck.simplex.message.{Block, BlockNotarized, Proposal}

import scala.util.Try

/**
 * Utility class for Proposal operations.
 */
object ProposalUtil {
  /**
   * Determines if the given proposal is for the current iteration of the
   * notarized chain.
   *
   * @param proposal the proposal for a new block in a blockchain
   * @param notarizedBlocks the current notarized chain
   * @return true if the proposal is for the current iteration, false otherwise
   */
  def isForCurrentIteration(proposal: Proposal, notarizedBlocks: Seq[BlockNotarized]): Boolean =
    proposal.iterationNumber == notarizedBlocks.size + 1

  /**
   * Determines if the parent chain of a proposal is the current notarized chain.
   *
   * @param proposal the proposal for a new block in a blockchain
   * @param notarizedBlocks the current notarized chain
   * @return true if the parent chain of the proposal is the current notarized
   *         chain, false otherwise
   */
  def isParentChainCurrentChain(proposal: Proposal, notarizedBlocks: Seq[BlockNotarized]): Boolean =
    proposal.parentChain.blocks.equals(notarizedBlocks)

  /**
   * Determines if the height of a new block is valid based on the parent block's
   * height.
   *
   * @param parentBlock the parent block in the blockchain
   * @param newBlock the new block for which the height needs to be validated
   * @return true if the height of the new block is valid, false otherwise
   */
  def isHeightValid(parentBlock: BlockNotarized, newBlock: Block): Boolean =
    newBlock.height == parentBlock.block.height + 1

  /**
   * Checks if the parent hash of a new block is valid based on the expected
   * parent hash computed from the parent block.
   *
   * @param parentBlock the parent block in the blockchain
   * @param newBlock the new block for which the parent hash needs to be validated
   * @return true if the parent hash of the new block is valid, false otherwise
   */
  def isParentHashValid(parentBlock: BlockNotarized, newBlock: Block, messageDigestAlgorithm: String): Boolean = {
    val expectedParentHash = CryptoUtil.computeBlockHash(parentBlock.block, messageDigestAlgorithm)
    newBlock.parentHash.equals(expectedParentHash)
  }

  /**
   * Determines if the given proposal is a proper extension of the current
   * notarized blockchain.
   *
   * @param proposal the proposal for a new block in the blockchain
   * @param notarizedBlocks the current notarized chain
   * @return true if the proposal is a proper extension, false otherwise
   */
  def isProperBlockchainExtension(proposal: Proposal, notarizedBlocks: Seq[BlockNotarized], messageDigestAlgorithm: String): Boolean = {
    val parentBlock = notarizedBlocks.last
    val newBlock = proposal.newBlock
    isHeightValid(parentBlock, newBlock) && isParentHashValid(parentBlock, newBlock, messageDigestAlgorithm)
  }

  /**
   * Checks if the given vote is from a known player.
   *
   * @param playerId the ID of the player that cast the vote
   * @param playerIds collection of all known player IDs
   * @return true if the vote is from a known player, false otherwise
   */
  def isProposalFromKnownPlayer(playerId: String, playerIds: Seq[String]): Boolean =
    playerIds.contains(playerId)

  /**
   * Determines if the given proposal is valid.
   *
   * @param proposal the signed proposal for a new block in a blockchain
   * @param notarizedBlocks the current notarized chain
   * @return true if the proposal is valid, false otherwise
   */
  def isValidProposal(proposal: Proposal, notarizedBlocks: Seq[BlockNotarized], playerIds: Seq[String], messageDigestAlgorithm: String): Try[Boolean] = {
    val checks = List(
      (p: Proposal) => isProposalFromKnownPlayer(p.playerId, playerIds),
      (p: Proposal) => isForCurrentIteration(p, notarizedBlocks),
      (p: Proposal) => isParentChainCurrentChain(p, notarizedBlocks),
      (p: Proposal) => isProperBlockchainExtension(p, notarizedBlocks, messageDigestAlgorithm)
    )
    Try(checks.forall(check => check(proposal)))
  }
}