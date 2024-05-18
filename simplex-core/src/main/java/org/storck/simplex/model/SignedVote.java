package org.storck.simplex.model;

import java.util.Arrays;
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

    /**
     * Checks if this SignedVote object is equal to the specified object. Two
     * SignedVote objects are considered equal if their vote and signature are
     * equal.
     *
     * @param other the object to compare to this SignedVote object
     * 
     * @return {@code true} if the specified object is equal to this SignedVote
     *     object, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object other) {
        boolean result = this == other;
        if (!result && other instanceof SignedVote signedVote) {
            result = vote().equals(signedVote.vote()) && Arrays.equals(signature, signedVote.signature);
        }
        return result;
    }

    /**
     * Computes the hash code for the SignedVote object.
     *
     * @return the hash code value for the SignedVote object
     */
    @Override
    public int hashCode() {
        int result = Objects.hash(vote());
        result = 31 * result + Arrays.hashCode(signature);
        return result;
    }

    /**
     * Returns a string representation of the SignedVote object.
     * The string representation includes the proposal and the signature.
     *
     * @return a string representation of the SignedVote object
     */
    @Override
    public String toString() {
        return "SignedVote{"
                + "vote=" + vote()
                + ", signature=" + Arrays.toString(signature)
                + '}';
    }
}
