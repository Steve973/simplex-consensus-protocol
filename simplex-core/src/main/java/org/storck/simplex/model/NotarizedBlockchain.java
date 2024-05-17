package org.storck.simplex.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The NotarizedBlockchain class represents a blockchain that consists of
 * notarized blocks.
 *
 * @param <T> the type of the transactions stored in the blocks
 * @param blocks the list of notarized blocks in the blockchain
 */
public record NotarizedBlockchain<T>(List<NotarizedBlock<T>> blocks) {

    /**
     * Create the notarized blockchain.
     *
     * @param blocks the blocks that constitute the chain
     */
    public NotarizedBlockchain {
        blocks = new ArrayList<>(blocks);
    }

    /**
     * Returns an immutable copy of the list of notarized blocks in the blockchain.
     *
     * @return an immutable copy of the list of notarized blocks in the blockchain
     */
    public List<NotarizedBlock<T>> blocks() {
        return List.copyOf(blocks);
    }

    /**
     * Adds a notarized block to the blockchain.
     *
     * @param newBlock the notarized block to be added
     */
    public void addBlock(final NotarizedBlock<T> newBlock) {
        blocks.add(newBlock);
    }
}
