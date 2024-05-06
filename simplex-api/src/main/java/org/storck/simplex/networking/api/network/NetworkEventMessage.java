package org.storck.simplex.networking.api.network;

import org.storck.simplex.networking.api.message.NetworkMessage;
import org.storck.simplex.networking.api.message.NetworkMessageType;

/**
 * Network message implementation for notifications of network events.
 *
 * @param event the event that this is notifying players about
 * @param details the details of the event (i.e., a json string)
 */
public record NetworkEventMessage(NetworkEvent event, String details) implements NetworkMessage {

    /**
     * Specifies that this is a network event message.
     *
     * @return the value of {@link NetworkMessageType#NETWORK_EVENT}
     */
    @Override
    public NetworkMessageType getMessageType() {
        return NetworkMessageType.NETWORK_EVENT;
    }
}