package org.storck.simplex.message

/**
 * The NotarizedBlock class represents a notarized block in a blockchain. It
 * consists of a Block object and a set of Votes.
 *
 * @param block the block of transactions
 * @param votes votes that have been received from players for this block
 */
case class BlockNotarized(block: Block, votes: Seq[Vote])

object BlockNotarized {
  /**
   * Create the notarized block.
   *
   * @param block the block to notarize
   * @param votes the votes from players/peers
   */
  def apply(block: Block, votes: Iterable[Vote]): BlockNotarized =
    new BlockNotarized(block, votes.toSeq)
}