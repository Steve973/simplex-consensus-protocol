package org.storck.simplex.model;

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
}
