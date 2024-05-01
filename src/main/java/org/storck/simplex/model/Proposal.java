package org.storck.simplex.model;

import java.util.Objects;

public record Proposal<T>(int iteration, String playerId, Block<T> newBlock, NotarizedBlockchain<T> parentChain) {

    public Proposal {
        Objects.requireNonNull(playerId);
        Objects.requireNonNull(newBlock);
        Objects.requireNonNull(parentChain);
    }
}
