package org.storck.simplex.model;

import java.util.List;

/**
 * The NotarizedBlockchain class represents a blockchain that consists of notarized blocks.
 *
 * @param <T> the type of the transactions stored in the blocks
 * @param blocks the list of notarized blocks in the blockchain
 */
public record NotarizedBlockchain<T>(List<NotarizedBlock<T>> blocks) {
}
