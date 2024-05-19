package org.storck.simplex.networking.api.message;

import java.util.Arrays;
import java.util.Objects;

/**
 * Base class for protocol message implementations. These are messages that are
 * sent between nodes during the course of the protocol iterations.
 */
public abstract class ProtocolMessage {

    /**
     * Message content in byte array form.
     */
    protected final byte[] content;

    /**
     * Create the instance with the message type and the content.
     *
     * @param content the message content
     */
    protected ProtocolMessage(final byte[] content) {
        Objects.requireNonNull(content);
        this.content = content.clone();
    }

    /**
     * The message type of the implementation.
     *
     * @return the message type
     */
    public abstract ProtocolMessageType getType();

    /**
     * Returns a copy of the content.
     *
     * @return a copy of the content
     */
    public byte[] getContent() {
        return content.clone();
    }

    /**
     * Compares this VoteProtocolMessage with the specified Object for equality.
     *
     * @param other the Object to compare to
     *
     * @return true if the specified Object is equal to this VoteProtocolMessage,
     *     false otherwise
     */
    @Override
    public boolean equals(final Object other) {
        boolean result = false;
        if (this == other) {
            result = true;
        } else if (other instanceof ProtocolMessage that) {
            result = getType().equals(that.getType()) && Arrays.equals(content, that.content);
        }
        return result;
    }

    /**
     * Calculates the hash code for the VoteProtocolMessage object. The hash code is
     * calculated based on the type of the protocol message and the content.
     *
     * @return the hash code value for the VoteProtocolMessage
     */
    @Override
    public int hashCode() {
        int result = Objects.hash(getType());
        result = 31 * result + Arrays.hashCode(content);
        return result;
    }

    /**
     * Returns a string representation of the VoteProtocolMessage object. The
     * returned string includes the type and content of the protocol message.
     *
     * @return a string representation of the VoteProtocolMessage object
     */
    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "{"
                + "type=" + getType()
                + ", content=" + Arrays.toString(content)
                + '}';
    }
}
