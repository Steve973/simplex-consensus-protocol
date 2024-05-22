package org.storck.simplex.model

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests the customized methods in VoteSigned.
 */
@SuppressFBWarnings(
    value = ["NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", "SE_BAD_FIELD", "SE_BAD_FIELD_STORE", "NP_NONNULL_PARAM_VIOLATION",
        "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", "EC_NULL_ARG"],
    justification = "I cannot find anything wrong with the test.")
class VoteSignedTest : ShouldSpec({
    val vote1 = Vote("player1", 1, "block1hash")
    val vote2 = Vote("player2", 2, "block2hash")
    val signature1 = byteArrayOf(1, 2, 3)
    val signature2 = byteArrayOf(4, 5, 6)

    context("VoteSigned constructor") {
        should("create a copy of the signature") {
            val voteSigned = VoteSigned(vote1, signature1)
            voteSigned.signature() shouldBe signature1
        }
    }

    context("signature()") {
        should("return a copy of the signature") {
            val voteSigned = VoteSigned(vote1, signature1)
            val signatureCopy = voteSigned.signature()
            signatureCopy shouldBe signature1
        }
    }

    context("equals()") {
        should("return true for same object") {
            val voteSigned1 = VoteSigned(vote1, signature1)
            voteSigned1.equals(voteSigned1) shouldBe true
        }

        should("return true for equal objects") {
            val voteSigned1 = VoteSigned(vote1, signature1)
            val voteSigned2 = VoteSigned(vote1, signature1)
            voteSigned1.equals(voteSigned2) shouldBe true
        }

        should("return false for different objects") {
            val voteSigned1 = VoteSigned(vote1, signature1)
            val voteSigned2 = VoteSigned(vote2, signature2)
            voteSigned1.equals(voteSigned2) shouldBe false
        }

        should("return false for different vote") {
            val voteSigned1 = VoteSigned(vote1, signature1)
            val voteSigned2 = VoteSigned(vote2, signature1)
            voteSigned1.equals(voteSigned2) shouldBe false
        }

        should("return false for different signature") {
            val voteSigned1 = VoteSigned(vote1, signature1)
            val voteSigned2 = VoteSigned(vote1, signature2)
            voteSigned1.equals(voteSigned2) shouldBe false
        }

        should("return false for null") {
            val voteSigned1 = VoteSigned(vote1, signature1)
            voteSigned1.equals(null) shouldBe false
        }
    }

    context("hashCode()") {
        should("return same hash code for equal objects") {
            val voteSigned1 = VoteSigned(vote1, signature1)
            val voteSigned2 = VoteSigned(vote1, signature1)
            val hashCode = voteSigned1.hashCode()
            hashCode shouldBe voteSigned2.hashCode()
            hashCode shouldBe 1994606177
        }

        should("return different hash code for different objects") {
            val voteSigned1 = VoteSigned(vote1, signature1)
            val voteSigned2 = VoteSigned(vote2, signature2)
            val hashCode = voteSigned1.hashCode()
            hashCode shouldNotBe voteSigned2.hashCode()
            hashCode shouldBe 1994606177
        }
    }

    context("toString()") {
        should("return correct string representation") {
            val voteSigned = VoteSigned(vote1, signature1)
            val expected = "VoteSigned{vote=$vote1, signature=${signature1.contentToString()}}"
            voteSigned.toString() shouldBe expected
        }
    }
})