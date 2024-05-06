package org.storck.simplex.model;

import java.util.List;

/**
 * The Blockchain class represents a blockchain data structure.
 *
 * @param <T> the type of the transactions stored in the blocks of the
 *     blockchain
 * @param height the number of blocks in the blocks list
 * @param blocks the blocks of transactions
 */
public record Blockchain<T>(int height, List<Block<T>> blocks) {
}
