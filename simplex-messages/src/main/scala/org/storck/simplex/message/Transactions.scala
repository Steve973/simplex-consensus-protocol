package org.storck.simplex.message

/**
 * Represents a [[Seq]] of transactions of [[?]] type.  For example, this class allows
 * the client to add transactions that will be part of the new proposed block the next
 * time the player is the iteration leader.
 *
 * @param transactions the transactions
 */
case class Transactions(transactions: Seq[?]) {

  /**
   * Add a collection of one or more transactions to the list of transactions.
   *
   * @param transactions the transactions to add
   * @return a new Transactions object with the transactions added
   */
  def apply(transactions: Iterable[?]): Transactions = Transactions(transactions.toSeq)
}
