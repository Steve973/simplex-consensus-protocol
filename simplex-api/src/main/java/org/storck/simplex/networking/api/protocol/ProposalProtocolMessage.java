package org.storck.simplex.networking.api.protocol;

import org.storck.simplex.networking.api.message.ProtocolMessage;
import org.storck.simplex.networking.api.message.ProtocolMessageType;

import java.util.Objects;

/**
 * Represents a protocol message for a block proposal.
 */
public record ProposalProtocolMessage(byte[] content) implements ProtocolMessage {

    /**
     * Create an instance with the content.
     *
     * @param content proposal content
     */
    public ProposalProtocolMessage {
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
        return ProtocolMessageType.PROPOSAL_MESSAGE;
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
