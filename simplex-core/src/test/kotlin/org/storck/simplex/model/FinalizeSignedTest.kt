package org.storck.simplex.model

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests the customized methods in the {@link FinalizedSigned} class.
 */
@SuppressFBWarnings(
    value = ["NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", "SE_BAD_FIELD", "SE_BAD_FIELD_STORE",
        "NP_NONNULL_PARAM_VIOLATION", "EC_NULL_ARG"],
    justification = "It is a test.")
class FinalizeSignedTest : ShouldSpec({
    val finalize1 = Finalize("player1", 1)
    val finalize2 = Finalize("player2", 2)
    val signature1 = byteArrayOf(1, 2, 3)
    val signature2 = byteArrayOf(4, 5, 6)

    context("FinalizeSigned constructor") {
        should("create a copy of the signature") {
            val finalizeSigned = FinalizeSigned(finalize1, signature1)
            finalizeSigned.signature() shouldBe signature1
        }
    }

    context("signature()") {
        should("return a copy of the signature") {
            val finalizeSigned = FinalizeSigned(finalize1, signature1)
            val signatureCopy = finalizeSigned.signature()
            signatureCopy shouldBe signature1
        }
    }

    context("equals()") {
        should("return true for same object") {
            val finalizeSigned1 = FinalizeSigned(finalize1, signature1)
            finalizeSigned1.equals(finalizeSigned1) shouldBe true
        }

        should("return true for equal objects") {
            val finalizeSigned1 = FinalizeSigned(finalize1, signature1)
            val finalizeSigned2 = FinalizeSigned(finalize1, signature1)
            finalizeSigned1.equals(finalizeSigned2) shouldBe true
        }

        should("return false for different objects") {
            val finalizeSigned1 = FinalizeSigned(finalize1, signature1)
            val finalizeSigned2 = FinalizeSigned(finalize2, signature2)
            finalizeSigned1.equals(finalizeSigned2) shouldBe false
        }

        should("return false for different finalize message") {
            val finalizeSigned1 = FinalizeSigned(finalize1, signature1)
            val finalizeSigned2 = FinalizeSigned(finalize2, signature1)
            finalizeSigned1.equals(finalizeSigned2) shouldBe false
        }

        should("return false for different signature") {
            val finalizeSigned1 = FinalizeSigned(finalize1, signature1)
            val finalizeSigned2 = FinalizeSigned(finalize1, signature2)
            finalizeSigned1.equals(finalizeSigned2) shouldBe false
        }

        should("return false for null") {
            val finalizeSigned1 = FinalizeSigned(finalize1, signature1)
            finalizeSigned1.equals(null) shouldBe false
        }
    }

    context("hashCode()") {
        should("return same hash code for equal objects") {
            val finalizeSigned1 = FinalizeSigned(finalize1, signature1)
            val finalizeSigned2 = FinalizeSigned(finalize1, signature1)
            val hashCode = finalizeSigned1.hashCode()
            hashCode shouldBe finalizeSigned2.hashCode()
            hashCode shouldBe -1872059983
        }

        should("return different hash code for different objects") {
            val finalizeSigned1 = FinalizeSigned(finalize1, signature1)
            val finalizeSigned2 = FinalizeSigned(finalize2, signature2)
            val hashCode = finalizeSigned1.hashCode()
            hashCode shouldNotBe finalizeSigned2.hashCode()
            hashCode shouldBe -1872059983
        }
    }

    context("toString()") {
        should("return correct string representation") {
            val finalizeSigned = FinalizeSigned(finalize1, signature1)
            val expected = "FinalizeSigned{finalizeMsg=$finalize1, signature=${signature1.contentToString()}}"
            finalizeSigned.toString() shouldBe expected
        }
    }
})