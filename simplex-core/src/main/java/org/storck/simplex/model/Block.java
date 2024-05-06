package org.storck.simplex.model;

import java.util.Collection;
import java.util.List;

/**
 * The Block class represents a block in a blockchain.
 *
 * @param <T> the type of the transactions stored in the block
 */
public record Block<T>(int height, String parentHash, Collection<T> transactions) {

    public Block {
        transactions = List.copyOf(transactions);
    }
}