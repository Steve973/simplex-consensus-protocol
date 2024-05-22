package org.storck.simplex.api.protocol;

import org.storck.simplex.api.message.ProtocolMessage;
import org.storck.simplex.api.message.ProtocolMessageType;

/**
 * Represents a finalize protocol message.
 */
public class FinalizeProtocolMessage extends ProtocolMessage {

    /**
     * Create an instance with the content.
     *
     * @param content vote content
     */
    public FinalizeProtocolMessage(final byte[] content) {
        super(content);
    }

    /**
     * Returns the message type.
     *
     * @return the message type
     */
    @Override
    public ProtocolMessageType getType() {
        return ProtocolMessageType.FINALIZE_MESSAGE;
    }
}
