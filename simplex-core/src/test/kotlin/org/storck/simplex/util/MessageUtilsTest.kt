package org.storck.simplex.util

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.json.JsonMapper
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.throwable.shouldHaveMessage
import io.kotest.matchers.types.shouldBeTypeOf
import org.storck.simplex.model.PeerInfo
import java.security.KeyPairGenerator

/**
 * Class used for testing the MessageUtils class.
 */
@SuppressFBWarnings(
    value = ["NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", "SE_BAD_FIELD"],
    justification = "I cannot find anything wrong with the test.")
class MessageUtilsTest : BehaviorSpec({

    val jsonMapper = JsonMapper(JsonFactory())

    given("peerInfoFromJson should correctly convert from a valid json string") {
        val keyPair = KeyPairGenerator.getInstance("RSA").genKeyPair()
        val publicKey = keyPair.public
        val peerInfo = PeerInfo("1", publicKey.encoded)
        val json = jsonMapper.writeValueAsString(peerInfo)

        `when`("convert to json") {
            val result = MessageUtils.peerInfoFromJson(json)

            then("The result should be a PeerInfo instance") {
                result.shouldBeTypeOf<PeerInfo>()
            }
        }
    }

    given("peerInfoFromJson should throw IllegalStateException when the JSON string is invalid") {
        val json = """{"invalid_argument": "Test"}"""

        `when`("Try to convert from JSON to a PeerInfo instance, but encounter an exception") {
            val exception = shouldThrow<IllegalStateException> {
                MessageUtils.peerInfoFromJson(json)
            }

            then("The error message should indicate that there was an error during deserialization") {
                exception shouldHaveMessage "Unexpected error when getting peer information from JSON string"
            }
        }
    }

    given("fromBytes should correctly convert from valid byte array") {
        val map = mapOf("key" to "val")
        val byteArray = MessageUtils.toBytes(map)
        val typeRef = object : TypeReference<Map<String, String>>() {}
        `when`("convert from bytes to a map") {
            val result = MessageUtils.fromBytes(byteArray, typeRef)

            then("The result should be the expected map") {
                result shouldBe map
            }
        }
    }

    given("fromBytes should throw IllegalStateException when byte array is invalid") {
        val byteArray = "not a json string".toByteArray()
        val typeRef = object : TypeReference<String>() {}

        `when`("Try to convert the bytes of the invalid json string, and encounter an exception") {
            val exception = shouldThrow<IllegalStateException> {
                MessageUtils.fromBytes(byteArray, typeRef)
            }

            then("The error message should indicate that there was an error during deserialization") {
                exception shouldHaveMessage "Unexpected error when converting from a byte array to 'java.lang.String'"
            }
        }
    }

    given("toBytes should correctly convert a string to a byte array") {
        val map = mapOf("testKey" to "testValue")
        val mapBytes = jsonMapper.writeValueAsBytes(map)

        `when`("Convert the map to a byte array") {
            val result = MessageUtils.toBytes(map)

            then("The result should be the same as the expected bytes") {
                result shouldBe mapBytes
            }
        }
    }

    given("toBytes should throw IllegalStateException when object is not serializable") {
        `when`("Try to convert the non-serializable object and encounter an exception") {
            val unserializable = CircularReferenceObject("test error")

            val exception = shouldThrow<IllegalStateException> {
                MessageUtils.toBytes(unserializable)
            }

            then("The error message should indicate the error in serialization") {
                exception shouldHaveMessage "Unexpected error when converting object to byte array"
            }
        }
    }
})

/**
 * Class for an instance that should case an error with jackson serialization.
 */
@Suppress("UNNECESSARY_LATEINIT")
@SuppressFBWarnings(
    value = ["NP_NONNULL_RETURN_VIOLATION"],
    justification = "This is supposed to be incorrect to make the test fail.")
class CircularReferenceObject(val name: String) {

    /**
     * Circular references should cause an error during serialization.
     */
    lateinit var selfReference: CircularReferenceObject
        private set

    init {
        selfReference = this
    }
}
