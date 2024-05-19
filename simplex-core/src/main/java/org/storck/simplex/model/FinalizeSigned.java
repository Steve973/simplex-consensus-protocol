package org.storck.simplex.model;

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents a signed {@link Finalize} message.
 *
 * @param finalizeMsg the finalizeMsg message
 * @param signature the digital signature of the player sending the message
 */
public record FinalizeSigned(Finalize finalizeMsg, byte[] signature) {

    /**
     * Create an instance with the finalize message and signature.
     *
     * @param finalizeMsg the proposal
     * @param signature the signature
     */
    public FinalizeSigned {
        Objects.requireNonNull(finalizeMsg);
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
     * Compares this SignedFinalize with the specified object for equality.
     *
     * @param other the object to compare to
     *
     * @return true if the specified object is equal to this SignedFinalize, false
     *     otherwise
     */
    @Override
    public boolean equals(final Object other) {
        boolean result = this == other;
        if (!result && other instanceof FinalizeSigned finalizeSigned) {
            result = finalizeMsg().equals(finalizeSigned.finalizeMsg()) && Arrays.equals(signature, finalizeSigned.signature);
        }
        return result;
    }

    /**
     * Computes the hash code for the SignedFinalize object.
     *
     * @return the hash code value for the SignedFinalize object
     */
    @Override
    public int hashCode() {
        int result = Objects.hash(finalizeMsg());
        result = 31 * result + Arrays.hashCode(signature);
        return result;
    }

    /**
     * Returns a string representation of the SignedFinalize object.
     * The string representation includes the finalizeMsg and the signature.
     *
     * @return a string representation of the SignedFinalize object
     */
    @Override
    public String toString() {
        return "FinalizeSigned{"
                + "finalizeMsg=" + finalizeMsg()
                + ", signature=" + Arrays.toString(signature)
                + '}';
    }
}
