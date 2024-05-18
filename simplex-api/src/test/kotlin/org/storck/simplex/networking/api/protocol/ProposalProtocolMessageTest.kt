package org.storck.simplex.networking.api.protocol

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ProposalProtocolMessageTest : ShouldSpec({
    val content1 = byteArrayOf(1, 2, 3)
    val content2 = byteArrayOf(4, 5, 6)

    context("equals function") {
        should("return true for the same instances") {
            val message = ProposalProtocolMessage(content1)
            message shouldBe message
        }

        should("return false for different types of instances") {
            val message = ProposalProtocolMessage(content1)
            val other = "Not a ProposalProtocolMessage"
            (message.equals(other)) shouldBe false
        }

        should("return false for null") {
            val message = ProposalProtocolMessage(content1)
            (message.equals(null)) shouldBe false
        }

        should("return true for same type and content") {
            val message1 = ProposalProtocolMessage(content1)
            val message2 = ProposalProtocolMessage(content1)
            (message1 == message2) shouldBe true
        }

        should("return false for different types") {
            val message1 = ProposalProtocolMessage(content1)
            val message2 = FinalizeProtocolMessage(1)
            (message1.equals(message2)) shouldBe false
        }

        should("return false for different content") {
            val message1 = ProposalProtocolMessage(content1)
            val message2 = ProposalProtocolMessage(content2)
            (message1 == message2) shouldBe false
        }
    }

    context("hashCode function") {
        should("return the same hash code for the same instances") {
            val message = ProposalProtocolMessage(content1)
            message.hashCode() shouldBe message.hashCode()
        }

        should("return the same hash code for equal instances") {
            val message1 = ProposalProtocolMessage(content1)
            val message2 = ProposalProtocolMessage(content1)
            message1.hashCode() shouldBe message2.hashCode()
        }

        should("return different hash codes for different types") {
            val message1 = ProposalProtocolMessage(content1)
            val message2 = FinalizeProtocolMessage(1)
            message1.hashCode() shouldNotBe message2.hashCode()
        }

        should("return different hash codes for different content") {
            val message1 = ProposalProtocolMessage(content1)
            val message2 = ProposalProtocolMessage(content2)
            message1.hashCode() shouldNotBe message2.hashCode()
        }
    }

    context("toString function") {
        should("return the correct string representation") {
            val message = ProposalProtocolMessage(content1)
            message.toString() shouldBe "ProposalProtocolMessage{type=PROPOSAL_MESSAGE, content=[1, 2, 3]}"
        }
    }
})