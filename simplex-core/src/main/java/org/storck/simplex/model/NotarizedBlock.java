package org.storck.simplex.model;

import java.util.List;

/**
 * The NotarizedBlock class represents a notarized block in a blockchain. It
 * consists of a Block object and a set of Votes.
 *
 * @param <T> the type of the transactions stored in the block
 * @param block the block of transactions
 * @param votes votes that have been received from players for this block
 */
public record NotarizedBlock<T>(Block<T> block, List<Vote> votes) {
}
