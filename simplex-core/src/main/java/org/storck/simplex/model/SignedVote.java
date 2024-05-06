package org.storck.simplex.model;

import java.util.Objects;

/**
 * The SignedVote class represents a signed vote cast by a player for a specific
 * block in a blockchain. It contains the vote and its corresponding signature.
 *
 * @param vote the {@link Vote}
 * @param signature the vote signature for verification purposes
 */
public record SignedVote(Vote vote, byte[] signature) {

    public SignedVote {
        Objects.requireNonNull(vote);
        Objects.requireNonNull(signature);
    }
}
