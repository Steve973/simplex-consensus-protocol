package org.storck.simplex.model;

import org.storck.simplex.networking.api.network.NetworkEventMessage;

import java.security.PublicKey;
import java.util.Objects;

/**
 * Details bean for a {@link NetworkEventMessage}.
 *
 * @param peerId the player peer ID
 * @param publicKey the player public key
 */
public record PeerInfo(String peerId, PublicKey publicKey) {

    /**
     * Create the peer info.
     *
     * @param peerId the id of the peer
     * @param publicKey the public key of the peer
     */
    public PeerInfo {
        Objects.requireNonNull(peerId);
    }
}
