package org.storck.simplex.model;

import java.util.Arrays;
import java.util.Objects;

/**
 * Peer information for messages.
 *
 * @param peerId the player peer ID
 * @param publicKeyBytes the player public key encoded in bytes
 */
public record PeerInfo(String peerId, byte[] publicKeyBytes) {

    /**
     * Create the peer info.
     *
     * @param peerId the id of the peer
     * @param publicKeyBytes the public key of the peer encoded in bytes
     */
    public PeerInfo {
        Objects.requireNonNull(peerId);
        Objects.requireNonNull(publicKeyBytes);
        publicKeyBytes = publicKeyBytes.clone();
    }

    /**
     * Return a copy of the encoded public key.
     *
     * @return a copy of the encoded public key
     */
    public byte[] publicKeyBytes() {
        return publicKeyBytes.clone();
    }

    /**
     * Compares this PeerInfo object to the specified object for equality.
     *
     * @param other the object to compare to
     * 
     * @return true if the specified object is equal to this PeerInfo object, false
     *     otherwise
     */
    @Override
    public boolean equals(final Object other) {
        boolean result = this == other;
        if (!result && other instanceof PeerInfo peerInfo) {
            result = peerId().equals(peerInfo.peerId()) && Arrays.equals(publicKeyBytes, peerInfo.publicKeyBytes);
        }
        return result;
    }

    /**
     * Computes the hash code for the PeerInfo object. The hash code is calculated
     * based on the peerId and publicKeyBytes
     * fields of the PeerInfo object.
     *
     * @return the hash code value for the PeerInfo object
     */
    @Override
    public int hashCode() {
        int result = Objects.hash(peerId());
        result = 31 * result + Arrays.hashCode(publicKeyBytes);
        return result;
    }

    /**
     * Returns a string representation of the PeerInfo object.
     *
     * @return a string representation of the PeerInfo object
     */
    @Override
    public String toString() {
        return "PeerInfo{"
                + "peerId=" + peerId()
                + ", publicKeyBytes=" + Arrays.toString(publicKeyBytes)
                + '}';
    }
}
