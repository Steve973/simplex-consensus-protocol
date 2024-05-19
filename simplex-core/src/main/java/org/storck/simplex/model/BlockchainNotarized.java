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
public record BlockchainNotarized<T>(List<BlockNotarized<T>> blocks) {

    /**
     * Create the notarized blockchain.
     *
     * @param blocks the blocks that constitute the chain
     */
    public BlockchainNotarized {
        blocks = new ArrayList<>(blocks);
    }

    /**
     * Returns an immutable copy of the list of notarized blocks in the blockchain.
     *
     * @return an immutable copy of the list of notarized blocks in the blockchain
     */
    public List<BlockNotarized<T>> blocks() {
        return List.copyOf(blocks);
    }

    /**
     * Adds a notarized block to the blockchain.
     *
     * @param newBlock the notarized block to be added
     */
    public void addBlock(final BlockNotarized<T> newBlock) {
        blocks.add(newBlock);
    }
}
