package org.storck.simplex.model;

import java.util.Objects;

/**
 * The SignedProposal class represents a signed proposal for a new block in a
 * blockchain.
 *
 * @param <T> the type of the transactions stored in the block
 * @param proposal the {@link Proposal}
 * @param signature the signature for proposal verification
 */
public record SignedProposal<T>(Proposal<T> proposal, byte[] signature) {

    /**
     * Create an instance with the proposal and signature.
     *
     * @param proposal the proposal
     * @param signature the signature
     */
    public SignedProposal {
        Objects.requireNonNull(proposal);
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
