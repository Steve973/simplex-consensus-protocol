package org.storck.simplex.networking.api.protocol;

import org.storck.simplex.networking.api.message.ProtocolMessage;
import org.storck.simplex.networking.api.message.ProtocolMessageType;

/**
 * Represents a protocol message for a block proposal.
 */
public class ProposalProtocolMessage extends ProtocolMessage {

    /**
     * Create an instance with the content.
     *
     * @param content proposal content
     */
    public ProposalProtocolMessage(final byte[] content) {
        super(content);
    }

    /**
     * Returns the message type.
     *
     * @return the message type
     */
    @Override
    public ProtocolMessageType getType() {
        return ProtocolMessageType.PROPOSAL_MESSAGE;
    }
}
