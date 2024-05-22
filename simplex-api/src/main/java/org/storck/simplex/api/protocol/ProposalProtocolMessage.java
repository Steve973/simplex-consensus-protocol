package org.storck.simplex.api.protocol;

import org.storck.simplex.api.message.ProtocolMessage;
import org.storck.simplex.api.message.ProtocolMessageType;

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
