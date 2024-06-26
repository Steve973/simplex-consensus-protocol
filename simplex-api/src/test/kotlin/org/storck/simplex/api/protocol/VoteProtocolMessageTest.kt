package org.storck.simplex.api.protocol

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.*

/**
 * This class contains the test cases for the [VoteProtocolMessage] class.
 */
@SuppressFBWarnings(
    value = ["NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", "SE_BAD_FIELD", "SE_BAD_FIELD_STORE", "EC_UNRELATED_TYPES", "EC_NULL_ARG"],
    justification = "It is a test.")
class VoteProtocolMessageTest : ShouldSpec({

    context("content function") {
        should("return a clone of the content") {
            val content = byteArrayOf(1, 2, 3)
            val message = VoteProtocolMessage(content)
            message.getContent() shouldBe content
        }
    }

    context("equals function") {
        should("return true for the same instances") {
            val message = VoteProtocolMessage(byteArrayOf(1, 2, 3))
            message shouldBe message
        }

        should("return false for different types of instances") {
            val message = VoteProtocolMessage(byteArrayOf(1, 2, 3))
            val other = "Not a VoteProtocolMessage"
            (message.equals(other)) shouldBe false
        }

        should("return false for null") {
            val message = VoteProtocolMessage(byteArrayOf(1, 2, 3))
            (message.equals(null)) shouldBe false
        }

        should("return true for same type and content") {
            val message1 = VoteProtocolMessage(byteArrayOf(1, 2, 3))
            val message2 = VoteProtocolMessage(byteArrayOf(1, 2, 3))
            (message1 == message2) shouldBe true
        }

        should("return false for different content") {
            val message1 = VoteProtocolMessage(byteArrayOf(1, 2, 3))
            val message2 = VoteProtocolMessage(byteArrayOf(4, 5, 6))
            (message1 == message2) shouldBe false
        }
    }

    context("hashCode function") {
        should("return the same hash code for the same instances") {
            val message = VoteProtocolMessage(byteArrayOf(1, 2, 3))
            val expectedHash = 31 * Objects.hash(message.type) + message.content.contentHashCode()
            message.hashCode() shouldBe expectedHash
            message.hashCode() shouldBe message.hashCode()
        }

        should("return the same hash code for equal instances") {
            val message1 = VoteProtocolMessage(byteArrayOf(1, 2, 3))
            val message2 = VoteProtocolMessage(byteArrayOf(1, 2, 3))
            val expectedHash = 31 * Objects.hash(message1.type) + message1.content.contentHashCode()
            message1.hashCode() shouldBe expectedHash
            message1.hashCode() shouldBe message2.hashCode()
        }

        should("return different hash codes for different content") {
            val message1 = VoteProtocolMessage(byteArrayOf(1, 2, 3))
            val message2 = VoteProtocolMessage(byteArrayOf(4, 5, 6))
            val expectedHash = 31 * Objects.hash(message1.type) + message1.content.contentHashCode()
            message1.hashCode() shouldBe expectedHash
            message1.hashCode() shouldNotBe message2.hashCode()
        }
    }

    context("toString function") {
        should("return the correct string representation") {
            val message = VoteProtocolMessage(byteArrayOf(1, 2, 3))
            message.toString() shouldBe "VoteProtocolMessage{type=VOTE_MESSAGE, content=[1, 2, 3]}"
        }
    }
})