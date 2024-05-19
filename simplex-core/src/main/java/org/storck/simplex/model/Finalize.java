package org.storck.simplex.model;

import java.util.Objects;

/**
 * Message indicating that the iteration is finalized.
 *
 * @param playerId the ID of the player that sent the message
 * @param iteration the iteration number to finalizeMsg
 */
public record Finalize(String playerId, int iteration) {

    /**
     * Create the finalizeMsg message for the iteration.
     *
     * @param playerId the ID of the player that declares the iteration finalized
     * @param iteration the iteration number
     */
    public Finalize {
        Objects.requireNonNull(playerId);
    }
}
