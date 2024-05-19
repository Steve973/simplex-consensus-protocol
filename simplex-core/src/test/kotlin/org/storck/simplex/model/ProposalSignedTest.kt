package org.storck.simplex.model

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.*

/**
 * Tests the ProposalSigned customization methods.
 */
@SuppressFBWarnings(
    value = ["NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", "SE_BAD_FIELD", "SE_BAD_FIELD_STORE", "NP_NONNULL_PARAM_VIOLATION", "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE"],
    justification = "I cannot find anything wrong with the test.")
class ProposalSignedTest : ShouldSpec({

    should("initialize ProposalSigned") {
        val block = Block<String>(1, "parentHash", listOf("Transaction1", "Transaction2"))
        val vote = Vote("playerId", 1, "blockHash")
        val notarizedBlock = BlockNotarized<String>(block, listOf(vote))
        val parentChain = BlockchainNotarized(listOf(notarizedBlock))
        val proposal = Proposal(1, "playerId", block, parentChain)
        val signature = byteArrayOf(1, 2, 3)
        val proposalSigned = ProposalSigned(proposal, signature)

        // Verification
        proposalSigned shouldNotBe null
        proposalSigned.proposal shouldBe proposal
        proposalSigned.signature shouldBe signature
    }

    should("test equals method of ProposalSigned") {
        val block = Block<String>(1, "parentHash", listOf("Transaction1", "Transaction2"))
        val vote = Vote("playerId", 1, "blockHash")
        val notarizedBlock = BlockNotarized<String>(block, listOf(vote))
        val parentChain = BlockchainNotarized(listOf(notarizedBlock))
        val proposal = Proposal(1, "playerId", block, parentChain)
        val signature = byteArrayOf(1, 2, 3)
        val proposalSigned1 = ProposalSigned(proposal, signature)
        val proposalSigned2 = ProposalSigned(proposal, signature)

        // Verification
        (proposalSigned1 == proposalSigned2) shouldBe true
    }

    should("test equals method of ProposalSigned with different proposal") {
        val block = Block<String>(1, "parentHash", listOf("Transaction1", "Transaction2"))
        val vote = Vote("playerId", 1, "blockHash")
        val notarizedBlock = BlockNotarized<String>(block, listOf(vote))
        val parentChain = BlockchainNotarized(listOf(notarizedBlock))
        val proposal1 = Proposal(1, "playerId1", block, parentChain)
        val proposal2 = Proposal(1, "playerId2", block, parentChain)
        val signature = byteArrayOf(1, 2, 3)
        val proposalSigned1 = ProposalSigned(proposal1, signature)
        val proposalSigned2 = ProposalSigned(proposal2, signature)

        // Verification
        (proposalSigned1 == proposalSigned2) shouldBe false
    }

    should("test equals method of ProposalSigned with different signature") {
        val block = Block<String>(1, "parentHash", listOf("Transaction1", "Transaction2"))
        val vote = Vote("playerId", 1, "blockHash")
        val notarizedBlock = BlockNotarized<String>(block, listOf(vote))
        val parentChain = BlockchainNotarized(listOf(notarizedBlock))
        val proposal = Proposal(1, "playerId", block, parentChain)
        val signature1 = byteArrayOf(1, 2, 3)
        val signature2 = byteArrayOf(2, 3, 4)
        val proposalSigned1 = ProposalSigned(proposal, signature1)
        val proposalSigned2 = ProposalSigned(proposal, signature2)

        // Verification
        (proposalSigned1 == proposalSigned2) shouldBe false
    }

    should("test equals method of ProposalSigned with same instance compared") {
        val block = Block<String>(1, "parentHash", listOf("Transaction1", "Transaction2"))
        val vote = Vote("playerId", 1, "blockHash")
        val notarizedBlock = BlockNotarized<String>(block, listOf(vote))
        val parentChain = BlockchainNotarized(listOf(notarizedBlock))
        val proposal = Proposal(1, "playerId", block, parentChain)
        val signature = byteArrayOf(1, 2, 3)
        val proposalSigned = ProposalSigned(proposal, signature)

        // Verification
        (proposalSigned == proposalSigned) shouldBe true
    }

    should("test hashCode method of ProposalSigned") {
        val block = Block<String>(1, "parentHash", listOf("Transaction1", "Transaction2"))
        val vote = Vote("playerId", 1, "blockHash")
        val notarizedBlock = BlockNotarized<String>(block, listOf(vote))
        val parentChain = BlockchainNotarized(listOf(notarizedBlock))
        val proposal = Proposal(1, "playerId", block, parentChain)
        val signature = byteArrayOf(1, 2, 3)
        val proposalSigned = ProposalSigned(proposal, signature)

        // Verification
        proposalSigned.hashCode() shouldBe Objects.hash(proposalSigned.proposal) * 31 + Arrays.hashCode(signature)
    }

    should("test toString method of ProposalSigned") {
        val block = Block<String>(1, "parentHash", listOf("Transaction1", "Transaction2"))
        val vote = Vote("playerId", 1, "blockHash")
        val notarizedBlock = BlockNotarized<String>(block, listOf(vote))
        val parentChain = BlockchainNotarized(listOf(notarizedBlock))
        val proposal = Proposal(1, "playerId", block, parentChain)
        val signature = byteArrayOf(1, 2, 3)
        val proposalSigned = ProposalSigned(proposal, signature)

        // Verification
        proposalSigned.toString() shouldBe "ProposalSigned{proposal=$proposal, signature=[1, 2, 3]}"
    }

    should("throw NullPointerException when initialize ProposalSigned with null proposal") {
        shouldThrow<NullPointerException> {
            ProposalSigned(null as Proposal<String>, byteArrayOf(1, 2, 3))
        }
    }

    should("throw NullPointerException when initialize ProposalSigned with null signature") {
        val block = Block<String>(1, "parentHash", listOf("Transaction1", "Transaction2"))
        val vote = Vote("playerId", 1, "blockHash")
        val notarizedBlock = BlockNotarized<String>(block, listOf(vote))
        val parentChain = BlockchainNotarized(listOf(notarizedBlock))
        val proposal = Proposal(1, "playerId", block, parentChain)

        shouldThrow<NullPointerException> {
            ProposalSigned(proposal, null)
        }
    }
})