package org.storck.simplex.model;

import java.util.Objects;

/**
 * The Vote class represents a vote cast by a player for a specific block in a blockchain.
 *
 * @param playerId the ID of the player that cast this vote
 * @param iteration the iteration number when this vote was cast
 * @param blockHash the hash of the block to which this vote pertains (to identify the block)
 */
public record Vote(String playerId, int iteration, String blockHash) {

    public Vote {
        Objects.requireNonNull(playerId);
        Objects.requireNonNull(blockHash);
    }
}
