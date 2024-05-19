package org.storck.simplex.model

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests the customized methods in SignedVote.
 */
@SuppressFBWarnings(
    value = ["NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", "SE_BAD_FIELD", "SE_BAD_FIELD_STORE", "NP_NONNULL_PARAM_VIOLATION",
        "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", "EC_NULL_ARG"],
    justification = "I cannot find anything wrong with the test.")
class SignedVoteTest : ShouldSpec({
    val vote1 = Vote("player1", 1, "block1hash")
    val vote2 = Vote("player2", 2, "block2hash")
    val signature1 = byteArrayOf(1, 2, 3)
    val signature2 = byteArrayOf(4, 5, 6)

    context("SignedVote constructor") {
        should("create a copy of the signature") {
            val signedVote = SignedVote(vote1, signature1)
            signedVote.signature() shouldBe signature1
        }
    }

    context("signature()") {
        should("return a copy of the signature") {
            val signedVote = SignedVote(vote1, signature1)
            val signatureCopy = signedVote.signature()
            signatureCopy shouldBe signature1
        }
    }

    context("equals()") {
        should("return true for same object") {
            val signedVote1 = SignedVote(vote1, signature1)
            signedVote1.equals(signedVote1) shouldBe true
        }

        should("return true for equal objects") {
            val signedVote1 = SignedVote(vote1, signature1)
            val signedVote2 = SignedVote(vote1, signature1)
            signedVote1.equals(signedVote2) shouldBe true
        }

        should("return false for different objects") {
            val signedVote1 = SignedVote(vote1, signature1)
            val signedVote2 = SignedVote(vote2, signature2)
            signedVote1.equals(signedVote2) shouldBe false
        }

        should("return false for null") {
            val signedVote1 = SignedVote(vote1, signature1)
            signedVote1.equals(null) shouldBe false
        }
    }

    context("hashCode()") {
        should("return same hash code for equal objects") {
            val signedVote1 = SignedVote(vote1, signature1)
            val signedVote2 = SignedVote(vote1, signature1)
            signedVote1.hashCode() shouldBe signedVote2.hashCode()
        }

        should("return different hash code for different objects") {
            val signedVote1 = SignedVote(vote1, signature1)
            val signedVote2 = SignedVote(vote2, signature2)
            signedVote1.hashCode() shouldNotBe signedVote2.hashCode()
        }
    }

    context("toString()") {
        should("return correct string representation") {
            val signedVote = SignedVote(vote1, signature1)
            val expected = "SignedVote{vote=$vote1, signature=${signature1.contentToString()}}"
            signedVote.toString() shouldBe expected
        }
    }
})