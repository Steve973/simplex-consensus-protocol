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
import org.storck.simplex.model.VoteSigned
import org.storck.simplex.model.Vote
import java.security.GeneralSecurityException

/**
 * Tests the Voting Service.
 */
@SuppressFBWarnings(
    value = ["NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", "SE_BAD_FIELD", "BC_BAD_CAST_TO_ABSTRACT_COLLECTION"],
    justification = "It is a test.")
class VotingServiceTest : BehaviorSpec({

    val iterationNumber = 1
    val playerId = "player1"
    val playerIds = listOf("player1", "player2", "player3")
    val blockHash = "ABCDEF123456"

    val digitalSignatureService = mockk<DigitalSignatureService>()
    val playerService = mockk<PlayerService>()

    beforeTest {
        every { digitalSignatureService.computeBlockHash(any<Block<String>>()) } returns blockHash
        every { digitalSignatureService.generateSignature(any<ByteArray>()) } returns byteArrayOf(1, 2, 3)
        every { digitalSignatureService.verifySignature(any(), any(), any()) } returns true
        every { playerService.playerIds } returns playerIds
        every { playerService.getPublicKey(any()) } returns mockk()
    }

    afterTest {
        clearAllMocks()
    }

    Given("initialization for iteration") {
        val votingService = VotingService<String>(digitalSignatureService, playerService)
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

        When("vote ID is not the same as the specified proposal ID") {
            every { vote.blockHash() } returns blockHash
            val result = VotingService.isVoteIdProposalId(vote, "something different")

            Then("vote id and proposal id are different") {
                result shouldBe false
            }
        }

        When("vote is from a known player") {
            val result = VotingService.isVoteFromKnownPlayer(playerId, playerService)

            Then("vote should be from known player") {
                result shouldBe true
            }
        }

        When("vote is not from a known player") {
            val result = VotingService.isVoteFromKnownPlayer("stranger danger", playerService)

            Then("we do not know that player") {
                result shouldBe false
            }
        }

        When("vote has a valid signature") {
            val signedVote = VoteSigned(Vote(playerId, iterationNumber, blockHash), byteArrayOf())
            val result = VotingService.isVoteSignatureValid(mockk(), signedVote, digitalSignatureService)

            Then("vote signature should be valid") {
                result shouldBe true
            }
        }

        When("vote does not have a valid signature") {
            every { digitalSignatureService.verifySignature(any(), any(), any()) } returns false
            val signedVote = VoteSigned(Vote(playerId, iterationNumber, blockHash), byteArrayOf())
            val result = VotingService.isVoteSignatureValid(mockk(), signedVote, digitalSignatureService)

            Then("it should not be valid") {
                result shouldBe false
            }
        }

        When("checking vote signature throws exception") {
            val signedVote = VoteSigned(
                Vote(playerId, iterationNumber, blockHash),
                byteArrayOf()
            )
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
        val votingService = VotingService<String>(digitalSignatureService, playerService)
        val proposal = mockk<Proposal<String>>()
        val block = mockk<Block<String>>()
        every { proposal.newBlock() } returns block
        votingService.initializeForIteration(iterationNumber, proposal)

        When("validating current iteration, proposal id, signed vote, player service and signature service") {
            val vote = Vote(playerId, iterationNumber, blockHash)
            val signedVote = VoteSigned(vote, byteArrayOf())
            val result = votingService.validateVote(iterationNumber, blockHash, signedVote, playerService, digitalSignatureService)

            Then("they should be valid") {
                result shouldBe true
            }
        }

        When("processing signedVote") {
            val vote = Vote(playerId, 3, blockHash)
            val signedVote = VoteSigned(vote, byteArrayOf())
            val result = votingService.processVote(signedVote)

            Then("they should be processed correctly and return true if a quorum of votes received, false otherwise") {
                result shouldBe false
            }
        }

        When("Creating a proposal vote") {
            val result = votingService.createProposalVote(playerId)

            Then("it should return a valid signed vote") {
                result shouldBe instanceOf<VoteSigned>()
            }
        }
    }

    Given("votes from less than 2/3 of players should not meet the quorum size") {
        val votingService = VotingService<String>(digitalSignatureService, playerService)
        val players = listOf("player1", "player2", "player3")
        val proposal = mockk<Proposal<String>>()
        val block = mockk<Block<String>>()
        every { proposal.newBlock() } returns block
        votingService.initializeForIteration(iterationNumber, proposal)
        val vote = Vote(players[0], iterationNumber, blockHash)
        val signedVoteBytes = byteArrayOf(1, 2, 3)
        val signedVote = VoteSigned(vote, signedVoteBytes)

        When("send a single vote to process and check quorum") {
            every { playerService.playerIds } returns players
            val result = votingService.processVote(signedVote)

            Then("quorum should NOT be reached") {
                result shouldBe false
            }
        }
    }

    Given("a vote is cast") {
        val votingService = VotingService<String>(digitalSignatureService, playerService)
        val proposal = mockk<Proposal<String>>()
        val block = mockk<Block<String>>()
        every { proposal.newBlock() } returns block
        votingService.initializeForIteration(iterationNumber, proposal)
        val vote1 = Vote(playerId, iterationNumber, blockHash)
        val signedVote1 = VoteSigned(vote1, byteArrayOf())
        val vote2 = Vote("player2", iterationNumber, blockHash)
        val signedVote2 = VoteSigned(vote2, byteArrayOf())
        votingService.processVote(signedVote1)

        When("another vote is processed") {
            val result = votingService.processVote(signedVote2)

            Then("verify that a quorum has been reached") {
                result shouldBe true
            }
        }
    }

    Given("unanimous vote") {
        val votingService = VotingService<String>(digitalSignatureService, playerService)
        val proposal = mockk<Proposal<String>>()
        val block = mockk<Block<String>>()
        every { proposal.newBlock() } returns block
        votingService.initializeForIteration(iterationNumber, proposal)
        val vote1 = Vote(playerId, iterationNumber, blockHash)
        val signedVote1 = VoteSigned(vote1, byteArrayOf())
        val vote2 = Vote("player2", iterationNumber, blockHash)
        val signedVote2 = VoteSigned(vote2, byteArrayOf())
        val vote3 = Vote("player3", iterationNumber, blockHash)
        val signedVote3 = VoteSigned(vote3, byteArrayOf())

        When("all three votes processed") {
            val result1 = votingService.processVote(signedVote1)
            val result2 = votingService.processVote(signedVote2)
            val result3 = votingService.processVote(signedVote3)

            Then("verify that a quorum has been reached") {
                result1 shouldBe false
                result2 shouldBe true
                result3 shouldBe true
            }
        }
    }

    Given("a vote from current iteration but not the valid proposal ID") {
        val votingService = VotingService<String>(digitalSignatureService, playerService)
        val voteOff = Vote(playerId, iterationNumber, "DifferentBlockHash")
        val signedVoteOff = VoteSigned(voteOff, byteArrayOf())

        When("validating the vote") {
            val result = votingService.validateVote(iterationNumber, blockHash, signedVoteOff, playerService, digitalSignatureService)

            Then("validation should fail") {
                result shouldBe false
            }
        }
    }

    Given("a vote from current iteration and valid proposal ID but from an unknown player") {
        val votingService = VotingService<String>(digitalSignatureService, playerService)
        val voteOff = Vote("unknownPlayerId", iterationNumber, blockHash)
        val signedVoteOff = VoteSigned(voteOff, byteArrayOf())

        When("validating the vote") {
            val result = votingService.validateVote(iterationNumber, blockHash, signedVoteOff, playerService, digitalSignatureService)

            Then("validation should fail") {
                result shouldBe false
            }
        }
    }

    Given("a vote from current iteration, valid proposal ID, known player but invalid signature") {
        val votingService = VotingService<String>(digitalSignatureService, playerService)
        val voteOff = Vote(playerId, iterationNumber, blockHash)
        val signedVoteOff = VoteSigned(voteOff, byteArrayOf(-1, -1, -1))

        When("validating the vote") {
            every { digitalSignatureService.verifySignature(any(), any(), any()) } returns false
            val result = votingService.validateVote(iterationNumber, blockHash, signedVoteOff, playerService, digitalSignatureService)

            Then("validation should fail") {
                result shouldBe false
            }
        }
    }

    Given("a vote not from current iteration yet valid in all other aspects") {
        val votingService = VotingService<String>(digitalSignatureService, playerService)
        val voteOff = Vote(playerId, 10, blockHash)
        val signedVoteOff = VoteSigned(voteOff, byteArrayOf())

        When("validating the vote") {
            val result = votingService.validateVote(iterationNumber, blockHash, signedVoteOff, playerService, digitalSignatureService)

            Then("validation should fail") {
                result shouldBe false
            }
        }
    }
})
