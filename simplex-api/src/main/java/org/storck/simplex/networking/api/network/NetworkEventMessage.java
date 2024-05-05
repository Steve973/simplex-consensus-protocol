package org.storck.simplex.networking.api.network;

import org.storck.simplex.networking.api.message.NetworkMessage;
import org.storck.simplex.networking.api.message.NetworkMessageType;

public record NetworkEventMessage(NetworkEvent event, String details) implements NetworkMessage {

    public NetworkMessageType getMessageType() {
        return NetworkMessageType.NETWORK_EVENT;
    }
}