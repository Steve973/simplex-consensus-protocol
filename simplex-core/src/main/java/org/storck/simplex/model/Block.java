package org.storck.simplex.model;

import java.util.Collection;
import java.util.List;

/**
 * The Block class represents a block in a blockchain.
 *
 * @param <T> the type of the transactions stored in the block
 */
public record Block<T>(int height, String parentHash, Collection<T> transactions) {

    /**
     * Create a Block.
     *
     * @param height the height of the block in the chain
     * @param parentHash the hash of the parent block in the chain
     * @param transactions the transactions for this block
     */
    public Block {
        transactions = List.copyOf(transactions);
    }
}