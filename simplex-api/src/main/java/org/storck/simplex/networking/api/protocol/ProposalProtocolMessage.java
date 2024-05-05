package org.storck.simplex.networking.api.protocol;

import org.storck.simplex.networking.api.message.ProtocolMessageType;
import org.storck.simplex.networking.api.message.ProtocolMessage;

public record ProposalProtocolMessage(byte[] content) implements ProtocolMessage {

    public ProtocolMessageType getType() {
        return ProtocolMessageType.PROPOSAL_MESSAGE;
    }
}
