package org.storck.simplex.model;

import java.util.Objects;

public record Vote(String playerId, int iteration, String blockHash) {

    public Vote {
        Objects.requireNonNull(playerId);
        Objects.requireNonNull(blockHash);
    }
}
