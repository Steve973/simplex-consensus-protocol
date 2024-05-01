package org.storck.simplex.model;

import java.util.Objects;

/**
 * The SignedProposal class represents a signed proposal for a new block in a blockchain.
 *
 * @param <T> the type of the transactions stored in the block
 * @param proposal the {@link Proposal}
 * @param signature the signature for proposal verification
 */
public record SignedProposal<T>(Proposal<T> proposal, byte[] signature) {

    public SignedProposal {
        Objects.requireNonNull(proposal);
        Objects.requireNonNull(signature);
    }
}
