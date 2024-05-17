package org.storck.simplex.networking.api.protocol;

import org.storck.simplex.networking.api.message.ProtocolMessage;
import org.storck.simplex.networking.api.message.ProtocolMessageType;

/**
 * Represents a finalize protocol message.
 */
public record FinalizeProtocolMessage(int iterationNumber) implements ProtocolMessage {

    /**
     * Returns the type of the protocol message.
     *
     * @return the type of the protocol message
     */
    @Override
    public ProtocolMessageType getType() {
        return ProtocolMessageType.FINALIZE_MESSAGE;
    }
}
