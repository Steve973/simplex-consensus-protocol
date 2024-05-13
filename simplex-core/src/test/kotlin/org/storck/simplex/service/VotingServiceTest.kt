package org.storck.simplex.service

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.storck.simplex.model.Block
import org.storck.simplex.model.Proposal
import org.storck.simplex.model.SignedVote
import org.storck.simplex.model.Vote

/**
 * Tests the Voting Service.
 */
@SuppressFBWarnings(
    value = ["NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", "SE_BAD_FIELD"],
    justification = "I cannot find anything wrong with the test, and mock objects used in a test do not need to be serializable.")
class VotingServiceTest : FunSpec({

    val iterationNumber = 1
    val playerId = "player1"
    val proposalId = "testProposalId"
    val playerIds = listOf("player1", "player2", "player3")
    val blockHash = "ABCDEF123456"

    val digitalSignatureService = mockk<DigitalSignatureService>()
    val playerService = mockk<PlayerService>()

    every { digitalSignatureService.computeBlockHash(any<Block<String>>()) } returns blockHash
    every { digitalSignatureService.generateSignature(any<ByteArray>()) } returns byteArrayOf()
    every { digitalSignatureService.verifySignature(any(), any(), any()) } returns true
    every { playerService.playerIds } returns playerIds
    every { playerService.getPublicKey(any()) } returns mockk()

    val votingService = VotingService<String>(digitalSignatureService, playerService)

    test("initializes for iteration correctly") {
        val proposal = mockk<Proposal<String>>()
        val block = mockk<Block<String>>()
        every { proposal.newBlock() } returns block

        votingService.initializeForIteration(iterationNumber, proposal)
    }

    test("determines if vote pertains to the current iteration") {
        val vote = mockk<Vote>()
        every { vote.iteration() } returns 1

        val result = VotingService.isVoteIterationCurrentIteration(vote, iterationNumber)
        result shouldBe true
    }

    test("determines if vote pertains to the specified proposal ID") {
        val vote = mockk<Vote>()
        every { vote.blockHash() } returns proposalId

        val result = VotingService.isVoteIdProposalId(vote, proposalId)
        result shouldBe true
    }

    test("determines if vote is from a known player") {
        val result = VotingService.isVoteFromKnownPlayer(playerId, playerService)
        result shouldBe true
    }

    test("determines if vote has a valid signature") {
        val signedVote = SignedVote(Vote(playerId, iterationNumber, proposalId), byteArrayOf())
        val result = VotingService.isVoteSignatureValid(mockk(), signedVote, digitalSignatureService)
        result shouldBe true
    }
})