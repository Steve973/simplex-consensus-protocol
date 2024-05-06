package org.storck.simplex.networking.api.protocol;

import org.storck.simplex.networking.api.message.ProtocolMessage;
import org.storck.simplex.networking.api.message.ProtocolMessageType;

/**
 * Represents a protocol message for a block proposal.
 */
public record ProposalProtocolMessage(byte[] content) implements ProtocolMessage {

    /**
     * Returns the type of the protocol message.
     *
     * @return the type of the protocol message
     */
    @Override
    public ProtocolMessageType getType() {
        return ProtocolMessageType.PROPOSAL_MESSAGE;
    }
}
