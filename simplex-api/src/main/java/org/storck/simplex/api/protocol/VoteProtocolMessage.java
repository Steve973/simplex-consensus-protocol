package org.storck.simplex.api.protocol;

import org.storck.simplex.api.message.ProtocolMessage;
import org.storck.simplex.api.message.ProtocolMessageType;

/**
 * Represents a protocol message used for voting.
 */
public class VoteProtocolMessage extends ProtocolMessage {

    /**
     * Create an instance with the content.
     *
     * @param content vote content
     */
    public VoteProtocolMessage(final byte[] content) {
        super(content);
    }

    /**
     * Returns the message type.
     *
     * @return the message type
     */
    @Override
    public ProtocolMessageType getType() {
        return ProtocolMessageType.VOTE_MESSAGE;
    }
}
