package org.storck.simplex.networking.api.network;

/** Indicates the different network events that can occur. */
public enum NetworkEvent {

    /**
     * For notifications when a peer disconnects.
     */
    PEER_DISCONNECTED,

    /**
     * For notifications when a peer connects.
     */
    PEER_CONNECTED,

    /**
     * For notifications when the network is down or experiencing significant
     * trouble.
     */
    NETWORK_DOWN,

    /**
     * For notifications when the network is restored.
     */
    NETWORK_RESTORED,

    /**
     * For unspecified network events.
     */
    OTHER
}