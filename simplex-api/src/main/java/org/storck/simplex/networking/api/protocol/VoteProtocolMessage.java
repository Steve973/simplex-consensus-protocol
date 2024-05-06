package org.storck.simplex.networking.api.protocol;

import org.storck.simplex.networking.api.message.ProtocolMessage;
import org.storck.simplex.networking.api.message.ProtocolMessageType;

import java.util.Objects;

/**
 * Represents a protocol message used for voting.
 */
public record VoteProtocolMessage(byte[] content) implements ProtocolMessage {

    /**
     * Create an instance with the content.
     *
     * @param content vote content
     */
    public VoteProtocolMessage {
        Objects.requireNonNull(content);
        content = content.clone();
    }

    /**
     * Returns the type of the protocol message.
     *
     * @return the type of the protocol message
     */
    @Override
    public ProtocolMessageType getType() {
        return ProtocolMessageType.VOTE_MESSAGE;
    }

    /**
     * Returns a copy of the content.
     *
     * @return a copy of the content
     */
    public byte[] content() {
        return content.clone();
    }
}
