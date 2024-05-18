package org.storck.simplex.model;

import java.util.Arrays;
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

    /**
     * Compares this SignedProposal with the specified object for equality.
     *
     * @param other the object to compare to
     * 
     * @return true if the specified object is equal to this SignedProposal, false
     *     otherwise
     */
    @Override
    public boolean equals(final Object other) {
        boolean result = this == other;
        if (!result) {
            result = other != null && getClass() == other.getClass();
            if (result) {
                @SuppressWarnings("unchecked")
                SignedProposal<T> peerInfo = (SignedProposal<T>) other;
                result = proposal().equals(peerInfo.proposal()) && Arrays.equals(signature, peerInfo.signature);
            }
        }
        return result;
    }

    /**
     * Computes the hash code for the SignedProposal object.
     *
     * @return the hash code value for the SignedProposal object
     */
    @Override
    public int hashCode() {
        int result = Objects.hash(proposal());
        result = 31 * result + Arrays.hashCode(signature);
        return result;
    }

    /**
     * Returns a string representation of the SignedProposal object.
     * The string representation includes the proposal and the signature.
     *
     * @return a string representation of the SignedProposal object
     */
    @Override
    public String toString() {
        return "SignedProposal{"
                + "proposal=" + proposal()
                + ", signature=" + Arrays.toString(signature)
                + '}';
    }
}
