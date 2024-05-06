package org.storck.simplex.networking.api.message;

/**
 * Defines basic behavior of any protocol message implementation.
 */
public interface ProtocolMessage {

    /**
     * Specifies the type of protocol message.
     * 
     * @return the type of protocol message
     */
    ProtocolMessageType getType();
}
