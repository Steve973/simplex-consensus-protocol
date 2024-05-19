package org.storck.simplex.model;

import java.util.Objects;

/**
 * The Proposal class represents a proposal for a new block in a blockchain.
 *
 * @param <T> the type of the transactions stored in the block
 * @param iteration the iteration number of the block
 * @param playerId the ID of the player proposing the block
 * @param newBlock the new block containing the transactions
 * @param parentChain the parent blockchain on which the new block is built
 */
public record Proposal<T>(int iteration, String playerId, Block<T> newBlock, BlockchainNotarized<T> parentChain) {

    /**
     * Requires non-null for various fields.
     *
     * @param iteration the iteration number
     * @param playerId the ID of the player/peer
     * @param newBlock the proposed block
     * @param parentChain the current chain that the block is proposed to be added
     */
    public Proposal {
        Objects.requireNonNull(playerId);
        Objects.requireNonNull(newBlock);
        Objects.requireNonNull(parentChain);
    }
}
