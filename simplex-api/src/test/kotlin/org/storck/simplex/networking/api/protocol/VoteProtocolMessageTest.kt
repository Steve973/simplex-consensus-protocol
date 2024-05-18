package org.storck.simplex.networking.api.protocol

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class VoteProtocolMessageTest : ShouldSpec({

    context("content function") {
        should("return a clone of the content") {
            val content = byteArrayOf(1, 2, 3)
            val message = VoteProtocolMessage(content)
            message.content() shouldBe content
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
            message.hashCode() shouldBe message.hashCode()
        }

        should("return the same hash code for equal instances") {
            val message1 = VoteProtocolMessage(byteArrayOf(1, 2, 3))
            val message2 = VoteProtocolMessage(byteArrayOf(1, 2, 3))
            message1.hashCode() shouldBe message2.hashCode()
        }

        should("return different hash codes for different content") {
            val message1 = VoteProtocolMessage(byteArrayOf(1, 2, 3))
            val message2 = VoteProtocolMessage(byteArrayOf(4, 5, 6))
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