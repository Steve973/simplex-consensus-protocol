package org.storck.simplex.api.message;

/**
 * Defines the base functionality for any network message; this is mostly for
 * specifying the message type.
 */
public interface NetworkMessage {

    /**
     * Indicates the message type for a message implementation that extends this
     * interface.
     *
     * @return the message type
     */
    NetworkMessageType getMessageType();
}
