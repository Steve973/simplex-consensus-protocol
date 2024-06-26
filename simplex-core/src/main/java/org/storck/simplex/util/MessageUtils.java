package org.storck.simplex.util;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.experimental.UtilityClass;
import org.storck.simplex.api.network.NetworkEventMessage;
import org.storck.simplex.model.PeerInfo;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Optional;

/**
 * Utility class that provides methods to convert messages between formats.
 */
@UtilityClass
public class MessageUtils {

    /**
     * Instance of the JsonMapper class, which is used to serialize and deserialize
     * JSON data.
     */
    public static final JsonMapper JSON_MAPPER = new JsonMapper(new JsonFactory());

    /**
     * Convert the message details from a {@link NetworkEventMessage} for messages
     * indicating that a peer has connected or disconnected.
     *
     * @param json the message details to convert
     *
     * @return the {@link PeerInfo}
     */
    public static PeerInfo peerInfoFromJson(final String json) {
        try {
            return JSON_MAPPER.readValue(json, PeerInfo.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unexpected error when getting peer information from JSON string", e);
        }
    }

    /**
     * Method to convert to the specified type from a byte array.
     *
     * @param bytes the byte array to be converted
     * @param typeRef the type reference of the destination type
     * @param <T> the type of the object to be converted to
     *
     * @return an object of type T converted from the byte array
     */
    public static <T> T fromBytes(final byte[] bytes, final TypeReference<T> typeRef) {
        try {
            return JSON_MAPPER.readValue(bytes, typeRef);
        } catch (IOException e) {
            String typeName = Optional.ofNullable(typeRef)
                    .map(TypeReference::getType)
                    .map(Type::getTypeName)
                    .orElse("Unknown");
            throw new IllegalStateException("Unexpected error when converting from a byte array to '" + typeName + "'", e);
        }
    }

    /**
     * Converts an object of type T to a byte array.
     *
     * @param <T> the type of object to convert
     * @param object the object to convert to bytes
     * 
     * @return a byte array representation of the given object
     */
    public static <T> byte[] toBytes(final T object) {
        try {
            return JSON_MAPPER.writeValueAsBytes(object);
        } catch (IOException e) {
            throw new IllegalStateException("Unexpected error when converting object to byte array", e);
        }
    }
}
