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

    /**
     * Create an instance with the vote and signature.
     *
     * @param vote the vote
     * @param signature the signature
     */
    public SignedVote {
        Objects.requireNonNull(vote);
        Objects.requireNonNull(signature);
        signature = signature.clone();
    }

    /**
     * Returns a copy of the signature.
     *
     * @return a copy of the signature
     */
    public byte[] signature() {
        return signature.clone();
    }
}
