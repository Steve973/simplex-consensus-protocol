package org.storck.simplex.model;

import java.util.Set;

/**
 * The VoteRegistryEntry class represents an entry in the vote registry.
 * It contains a block and a set of votes for that block.
 *
 * @param <T> the type of the transactions stored in the block
 * @param block the block to which votes pertain
 * @param votes the votes received for the block
 */
public record VoteRegistryEntry<T>(Block<T> block, Set<Vote> votes) {
}
