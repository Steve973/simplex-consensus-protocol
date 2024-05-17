package org.storck.simplex.service

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.instanceOf
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.storck.simplex.model.Block
import org.storck.simplex.model.Proposal
import org.storck.simplex.model.SignedVote
import org.storck.simplex.model.Vote
import java.security.GeneralSecurityException

/**
 * Tests the Voting Service.
 */
@SuppressFBWarnings(
    value = ["NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", "SE_BAD_FIELD"],
    justification = "I cannot find anything wrong with the test, and mock objects used in a test do not need to be serializable.")
class VotingServiceTest : BehaviorSpec({

    val iterationNumber = 1
    val playerId = "player1"
    val playerIds = listOf("player1", "player2", "player3")
    val blockHash = "ABCDEF123456"

    val digitalSignatureService = mockk<DigitalSignatureService>()
    val playerService = mockk<PlayerService>()

    val votingService = VotingService<String>(digitalSignatureService, playerService)

    beforeTest {
        clearAllMocks()
        every { digitalSignatureService.computeBlockHash(any<Block<String>>()) } returns blockHash
        every { digitalSignatureService.generateSignature(any<ByteArray>()) } returns byteArrayOf()
        every { digitalSignatureService.verifySignature(any(), any(), any()) } returns true
        every { playerService.playerIds } returns playerIds
        every { playerService.getPublicKey(any()) } returns mockk()
    }

    Given("initialization for iteration") {
        val proposal = mockk<Proposal<String>>()
        val block = mockk<Block<String>>()

        When("starting new iteration") {
            every { proposal.newBlock() } returns block
            votingService.initializeForIteration(iterationNumber, proposal)

            Then("no exception is thrown and fields are set as expected") {
                votingService.iterationNumber shouldBe iterationNumber
                votingService.proposalId shouldBe blockHash
            }
        }
    }

    Given("a vote") {
        val vote = mockk<Vote>()

        When("vote iteration is current iteration") {
            every { vote.iteration() } returns 1
            val result = VotingService.isVoteIterationCurrentIteration(vote, iterationNumber)

            Then("it should pertain to the current iteration") {
                result shouldBe true
            }
        }

        When("vote iteration is not current iteration") {
            every { vote.iteration() } returns 3
            val result = VotingService.isVoteIterationCurrentIteration(vote, iterationNumber)

            Then("it should not pertain to the current iteration") {
                result shouldBe false
            }
        }

        When("vote pertains to the specified proposal ID") {
            every { vote.blockHash() } returns blockHash
            val result = VotingService.isVoteIdProposalId(vote, blockHash)

            Then("vote id and proposal id should be the same") {
                result shouldBe true
            }
        }

        When("vote is from a known player") {
            val result = VotingService.isVoteFromKnownPlayer(playerId, playerService)

            Then("vote should be from known player") {
                result shouldBe true
            }
        }

        When("vote has a valid signature") {
            val signedVote = SignedVote(Vote(playerId, iterationNumber, blockHash), byteArrayOf())
            val result = VotingService.isVoteSignatureValid(mockk(), signedVote, digitalSignatureService)

            Then("vote signature should be valid") {
                result shouldBe true
            }
        }

        When("vote does not have a valid signature") {
            every { digitalSignatureService.verifySignature(any(), any(), any()) } returns false
            val signedVote = SignedVote(Vote(playerId, iterationNumber, blockHash), byteArrayOf())
            val result = VotingService.isVoteSignatureValid(mockk(), signedVote, digitalSignatureService)

            Then("it should not be valid") {
                result shouldBe false
            }
        }

        When("checking vote signature throws exception") {
            val signedVote = SignedVote(Vote(playerId, iterationNumber, blockHash), byteArrayOf())
            every { digitalSignatureService.verifySignature(any(), any(), any()) } throws GeneralSecurityException()
            val exception = shouldThrow<GeneralSecurityException> {
                VotingService.isVoteSignatureValid(mockk(), signedVote, digitalSignatureService)
            }

            Then("exception should be thrown") {
                exception.message shouldBe null
            }
        }
    }

    Given("a vote requirement") {

        When("validating current iteration, proposal id, signed vote, player service and signature service") {
            val vote = Vote(playerId, iterationNumber, blockHash)
            val signedVote = SignedVote(vote, byteArrayOf())
            val result = votingService.validateVote(iterationNumber, blockHash, signedVote, playerService, digitalSignatureService)

            Then("they should be valid") {
                result shouldBe true
            }
        }

        When("processing signedVote") {
            val vote = Vote(playerId, 3, blockHash)
            val signedVote = SignedVote(vote, byteArrayOf())
            val result = votingService.processVote(signedVote)

            Then("they should be processed correctly and return true if a quorum of votes received, false otherwise") {
                result shouldBe false
            }
        }

        When("Creating a proposal vote") {
            val result = votingService.createProposalVote(playerId)

            Then("it should return a valid signed vote") {
                result shouldBe instanceOf<SignedVote>()
            }
        }
    }

    Given("a vote is cast") {
        val vote1 = Vote(playerId, iterationNumber, blockHash)
        val signedVote1 = SignedVote(vote1, byteArrayOf())
        val vote2 = Vote("player2", iterationNumber, blockHash)
        val signedVote2 = SignedVote(vote2, byteArrayOf())
        votingService.processVote(signedVote1)

        When("another vote is processed") {
            val result = votingService.processVote(signedVote2)

            Then("verify that a quorum has been reached") {
                result shouldBe true
            }
        }
    }
})
