package org.storck.simplex.model;

import java.util.Objects;

public record SignedVote(Vote vote, byte[] signature) {

    public SignedVote {
        Objects.requireNonNull(vote);
        Objects.requireNonNull(signature);
    }
}
