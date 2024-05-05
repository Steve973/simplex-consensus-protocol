package org.storck.simplex.networking.api.protocol;

import org.storck.simplex.networking.api.message.ProtocolMessageType;
import org.storck.simplex.networking.api.message.ProtocolMessage;

public record VoteProtocolMessage(byte[] content) implements ProtocolMessage {

    public ProtocolMessageType getType() {
        return ProtocolMessageType.VOTE_MESSAGE;
    }
}
