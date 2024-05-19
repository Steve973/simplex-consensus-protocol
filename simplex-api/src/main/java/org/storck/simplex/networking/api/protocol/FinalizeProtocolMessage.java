package org.storck.simplex.networking.api.protocol;

import org.storck.simplex.networking.api.message.ProtocolMessage;
import org.storck.simplex.networking.api.message.ProtocolMessageType;

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents a finalize protocol message.
 */
public record FinalizeProtocolMessage(byte[] content) implements ProtocolMessage {

    /**
     * Create an instance with the content.
     *
     * @param content vote content
     */
    public FinalizeProtocolMessage {
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
        return ProtocolMessageType.FINALIZE_MESSAGE;
    }

    /**
     * Returns a copy of the content.
     *
     * @return a copy of the content
     */
    public byte[] content() {
        return content.clone();
    }

    /**
     * Compares this FinalizeProtocolMessage with the specified Object for equality.
     *
     * @param other the Object to compare to
     *
     * @return true if the specified Object is equal to this
     *     FinalizeProtocolMessage,
     *     false otherwise
     */
    @Override
    public boolean equals(final Object other) {
        boolean result = false;
        if (this == other) {
            result = true;
        } else if (other instanceof FinalizeProtocolMessage that) {
            result = getType().equals(that.getType()) && Arrays.equals(content, that.content);
        }
        return result;
    }

    /**
     * Calculates the hash code for the FinalizeProtocolMessage object. The hash
     * code is
     * calculated based on the type of the protocol message and the content.
     *
     * @return the hash code value for the FinalizeProtocolMessage
     */
    @Override
    public int hashCode() {
        int result = Objects.hash(getType());
        result = 31 * result + Arrays.hashCode(content);
        return result;
    }

    /**
     * Returns a string representation of the FinalizeProtocolMessage object. The
     * returned string includes the type and content of the protocol message.
     *
     * @return a string representation of the FinalizeProtocolMessage object
     */
    @Override
    public String toString() {
        return "FinalizeProtocolMessage{"
                + "type=" + getType()
                + ", content=" + Arrays.toString(content)
                + '}';
    }
}
