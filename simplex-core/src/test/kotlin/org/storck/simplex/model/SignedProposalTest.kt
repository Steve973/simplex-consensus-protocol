package org.storck.simplex.model

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.*

/**
 * Tests the SignedProposal customization methods.
 */
@SuppressFBWarnings(
    value = ["NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", "SE_BAD_FIELD", "SE_BAD_FIELD_STORE", "NP_NONNULL_PARAM_VIOLATION", "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE"],
    justification = "I cannot find anything wrong with the test.")
class SignedProposalTest : ShouldSpec({

    should("initialize SignedProposal") {
        val block = Block<String>(1, "parentHash", listOf("Transaction1", "Transaction2"))
        val vote = Vote("playerId", 1, "blockHash")
        val notarizedBlock = NotarizedBlock<String>(block, listOf(vote))
        val parentChain = NotarizedBlockchain(listOf(notarizedBlock))
        val proposal = Proposal(1, "playerId", block, parentChain)
        val signature = byteArrayOf(1, 2, 3)
        val signedProposal = SignedProposal(proposal, signature)

        // Verification
        signedProposal shouldNotBe null
        signedProposal.proposal shouldBe proposal
        signedProposal.signature shouldBe signature
    }

    should("test equals method of SignedProposal") {
        val block = Block<String>(1, "parentHash", listOf("Transaction1", "Transaction2"))
        val vote = Vote("playerId", 1, "blockHash")
        val notarizedBlock = NotarizedBlock<String>(block, listOf(vote))
        val parentChain = NotarizedBlockchain(listOf(notarizedBlock))
        val proposal = Proposal(1, "playerId", block, parentChain)
        val signature = byteArrayOf(1, 2, 3)
        val signedProposal1 = SignedProposal(proposal, signature)
        val signedProposal2 = SignedProposal(proposal, signature)

        // Verification
        (signedProposal1 == signedProposal2) shouldBe true
    }

    should("test hashCode method of SignedProposal") {
        val block = Block<String>(1, "parentHash", listOf("Transaction1", "Transaction2"))
        val vote = Vote("playerId", 1, "blockHash")
        val notarizedBlock = NotarizedBlock<String>(block, listOf(vote))
        val parentChain = NotarizedBlockchain(listOf(notarizedBlock))
        val proposal = Proposal(1, "playerId", block, parentChain)
        val signature = byteArrayOf(1, 2, 3)
        val signedProposal = SignedProposal(proposal, signature)

        // Verification
        signedProposal.hashCode() shouldBe Objects.hash(signedProposal.proposal) * 31 + Arrays.hashCode(signature)
    }

    should("test toString method of SignedProposal") {
        val block = Block<String>(1, "parentHash", listOf("Transaction1", "Transaction2"))
        val vote = Vote("playerId", 1, "blockHash")
        val notarizedBlock = NotarizedBlock<String>(block, listOf(vote))
        val parentChain = NotarizedBlockchain(listOf(notarizedBlock))
        val proposal = Proposal(1, "playerId", block, parentChain)
        val signature = byteArrayOf(1, 2, 3)
        val signedProposal = SignedProposal(proposal, signature)

        // Verification
        signedProposal.toString() shouldBe "SignedProposal{proposal=$proposal, signature=[1, 2, 3]}"
    }

    should("throw NullPointerException when initialize SignedProposal with null proposal") {
        shouldThrow<NullPointerException> {
            SignedProposal( null as Proposal<String>, byteArrayOf(1, 2, 3))
        }
    }

    should("throw NullPointerException when initialize SignedProposal with null signature") {
        val block = Block<String>(1, "parentHash", listOf("Transaction1", "Transaction2"))
        val vote = Vote("playerId", 1, "blockHash")
        val notarizedBlock = NotarizedBlock<String>(block, listOf(vote))
        val parentChain = NotarizedBlockchain(listOf(notarizedBlock))
        val proposal = Proposal(1, "playerId", block, parentChain)

        shouldThrow<NullPointerException> {
            SignedProposal(proposal, null)
        }
    }
})