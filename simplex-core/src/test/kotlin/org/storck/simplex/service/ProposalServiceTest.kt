package org.storck.simplex.service

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*
import org.storck.simplex.model.*
import org.storck.simplex.networking.api.network.PeerNetworkClient
import org.storck.simplex.networking.api.protocol.ProposalProtocolMessage
import java.util.concurrent.TransferQueue

/**
 * Test the Proposal Service.
 */
@SuppressFBWarnings(
    value = ["NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", "SE_BAD_FIELD"],
    justification = "I cannot find anything wrong with the test, and mock objects used in a test do not need to be serializable.")
class ProposalServiceTest : BehaviorSpec({

    val localPlayerId = "localPlayerId"
    val blockHash = "ABCDEF123456"
    val transactionQueue = mockk<TransferQueue<String>>()
    val signatureService = mockk<DigitalSignatureService>()
    val peerNetworkClient = mockk<PeerNetworkClient>()
    val proposal = mockk<Proposal<String>>()
    val notarizedBlock = mockk<NotarizedBlock<String>>()
    val notarizedBlocks = listOf(notarizedBlock)

    val proposalService = ProposalService<String>(localPlayerId, signatureService, peerNetworkClient)

    given("checks if proposal is for current iteration") {

        every { proposal.iteration } returns 2

        `when`("Check to see if the proposal is for the current iteration") {
            val result = ProposalService.isForCurrentIteration(proposal, notarizedBlocks)

            then("Proposal should be for the current iteration") {
                result shouldBe true
            }
        }
    }

    given("checks if proposal is for the wrong iteration") {

        every { proposal.iteration } returns 3

        `when`("Check to see if the proposal is for the current iteration") {
            val result = ProposalService.isForCurrentIteration(proposal, notarizedBlocks)

            then("Proposal should not be for the current iteration") {
                result shouldBe false
            }
        }
    }

    given("checks if the parent chain of a proposal is the current notarized chain") {

        every { proposal.parentChain.blocks } returns notarizedBlocks

        `when`("Check to see if the parent chain of a proposal is the current notarized chain") {
            val result = ProposalService.isParentChainCurrentChain(proposal, notarizedBlocks)

            then("Parent chain is the current chain") {
                result shouldBe true
            }
        }
    }

    given("checks if the height of a new block is valid based on the parent block's height") {
        val parentBlock = mockk<NotarizedBlock<String>>()
        val newBlock = mockk<Block<String>>()

        every { newBlock.height } returns 2
        every { parentBlock.block.height } returns 1

        `when`("Check to see if the parent block's height is valid") {
            val result = ProposalService.isHeightValid(parentBlock, newBlock)

            then("Should be true, indicating that the block height was as expected") {
                result shouldBe true
            }
        }
    }

    given("checks if the height of a new block is invalid") {
        val parentBlock = mockk<NotarizedBlock<String>>()
        val newBlock = mockk<Block<String>>()

        every { newBlock.height } returns 3
        every { parentBlock.block.height } returns 1

        `when`("Check to see if the parent block's height is valid") {
            val result = ProposalService.isHeightValid(parentBlock, newBlock)

            then("Should be false, indicating that the block height was invalid") {
                result shouldBe false
            }
        }
    }

    given("checks if the parent hash of a new block is valid based on the expected parent hash computed from the parent block") {
        val parentBlock = mockk<NotarizedBlock<String>>()
        val newBlock = mockk<Block<String>>()

        every { parentBlock.block } returns newBlock
        every { signatureService.computeBlockHash(any<Block<String>>()) } returns blockHash
        every { newBlock.parentHash } returns blockHash

        `when`("Check to see if the parent hash of a new block is valid") {
            val result = proposalService.isParentHashValid(parentBlock, newBlock)

            then("Should be true, indicating that the block hash was as expected") {
                result shouldBe true
            }
        }
    }

    given("checks if the parent hash of a new block is invalid") {
        val parentBlock = mockk<NotarizedBlock<String>>()
        val newBlock = mockk<Block<String>>()

        every { parentBlock.block } returns newBlock
        every { signatureService.computeBlockHash(any<Block<String>>()) } returns blockHash
        every { newBlock.parentHash } returns "invalidHash"

        `when`("Check to see if the parent hash of a new block is valid") {
            val result = proposalService.isParentHashValid(parentBlock, newBlock)

            then("Should be false, indicating that the block hash was invalid") {
                result shouldBe false
            }
        }
    }

    given("checks if the given proposal is a proper extension of the current notarized blockchain") {
        val newBlock = mockk<Block<String>>()

        every { proposal.newBlock } returns newBlock
        every { notarizedBlock.block } returns newBlock
        every { signatureService.computeBlockHash(any<Block<String>>()) } returns blockHash
        every { newBlock.parentHash } returns blockHash
        every { newBlock.height } returns 2
        every { notarizedBlock.block.height } returns 1

        `when`("Check to see if the proposed block is a proper extension of the blockchain") {
            val result = proposalService.isProperBlockchainExtension(proposal, notarizedBlocks)

            then("Should be true, indicating that the proposed block is a proper extension") {
                result shouldBe true
            }
        }
    }

    given("checks if the given proposal is an improper extension of the current notarized blockchain") {
        val parentBlock = mockk<NotarizedBlock<String>>()
        val newBlock = mockk<Block<String>>()

        every { proposal.newBlock } returns newBlock
        every { parentBlock.block } returns newBlock

        `when`("Check to see if the proposed block is an improper extension with an incorrect block height") {
            every { newBlock.height } returns 3
            every { parentBlock.block.height } returns 1

            val result = proposalService.isProperBlockchainExtension(proposal, notarizedBlocks)

            then("Should be false, indicating that the proposed block is an improper extension") {
                result shouldBe false
            }
        }

        `when`("Check to see if the proposed block is an improper extension with an invalid parent hash") {
            every { newBlock.height } returns 2
            every { parentBlock.block.height } returns 1
            every { signatureService.computeBlockHash(any<Block<String>>()) } returns blockHash
            every { newBlock.parentHash } returns "invalidHash"

            val result = proposalService.isProperBlockchainExtension(proposal, notarizedBlocks)

            then("Should be false, indicating that the proposed block is an improper extension") {
                result shouldBe false
            }
        }
    }

    given("Test adding transactions") {
        val transactions = listOf("transaction1", "transaction2")

        `when`("Add transactions") {
            proposalService.addTransactions(transactions)

            then("expected transactions have been added") {
                proposalService.transactions shouldBe transactions
            }
        }
    }

    given("Try to propose a new block successfully") {
        val iterationNumber = 1
        val playerId = "testPlayer"
        val parentBlock = mockk<Block<String>>()
        val transaction = "test transaction"
        val transactions = listOf(transaction)
        val vote = mockk<Vote>()
        val votes = listOf(vote)
        val signature = byteArrayOf()

        every { transactionQueue.drainTo(any<ArrayList<String>>()) } returns 1
        every { notarizedBlock.block } returns parentBlock
        every { signatureService.computeBlockHash(any<Block<String>>()) } returns blockHash
        every { notarizedBlock.block.height } returns 1
        every { notarizedBlock.block.parentHash } returns blockHash
        every { notarizedBlock.block.transactions } returns transactions
        every { notarizedBlock.votes } returns votes
        every { vote.playerId } returns playerId
        every { vote.iteration } returns iterationNumber
        every { vote.blockHash } returns blockHash
        every { signatureService.generateSignature(any(ByteArray::class)) } returns signature
        every { peerNetworkClient.broadcastProposal(any(ProposalProtocolMessage::class)) } just Runs

        `when`("Propose a new block") {
            proposalService.proposeNewBlock(notarizedBlocks, iterationNumber)

            then("The network client should broadcast the block, meaning that all checks passed and the block is proposed successfully") {
                verify(exactly = 1) { peerNetworkClient.broadcastProposal(any(ProposalProtocolMessage::class)) }
            }
        }
    }

    given("Try to propose a new block with absent parent block hash") {
        val iterationNumber = 1
        val parentBlock = mockk<Block<String>>()

        every { transactionQueue.drainTo(any<ArrayList<String>>()) } returns 1
        every { notarizedBlock.block } returns parentBlock
        every { signatureService.computeBlockHash(any<Block<String>>()) } returns null

        `when`("Propose a new block") {
            val exception = shouldThrow<IllegalArgumentException> {
                proposalService.proposeNewBlock(notarizedBlocks, iterationNumber)
            }

            then("The exception message should indicate that the parent hash was unavailable") {
                exception.message shouldBe "Could not get parent block hash for proposal"
            }
        }
    }

    given("Process a valid proposal") {
        val iterationNumber = 2
        val signedProposal = mockk<SignedProposal<String>>()
        val newBlock = mockk<Block<String>>()

        every { proposal.iteration } returns iterationNumber
        every { proposal.parentChain.blocks } returns notarizedBlocks
        every { proposal.newBlock } returns newBlock
        every { newBlock.height } returns 2
        every { notarizedBlock.block.height } returns 1
        every { newBlock.parentHash } returns blockHash
        every { signedProposal.proposal } returns proposal
        every { signatureService.computeBlockHash(any<Block<String>>()) } returns blockHash

        `when`("Process the proposal") {
            val result = proposalService.processProposal(signedProposal, notarizedBlocks)

            then("Should be true, indicating that the valid proposal passed validation checks") {
                result shouldBe true
                proposalService.processedProposalIds shouldContain blockHash
            }
        }
    }

    given("Process an invalid proposal") {
        val iterationNumber = 2
        val signedProposal = mockk<SignedProposal<String>>()
        val parentBlock = mockk<NotarizedBlock<String>>()
        val newBlock = mockk<Block<String>>()

        every { proposal.iteration } returns iterationNumber
        every { proposal.parentChain.blocks } returns notarizedBlocks
        every { proposal.newBlock } returns newBlock
        every { parentBlock.block.height } returns 1
        every { newBlock.parentHash } returns blockHash
        every { signedProposal.proposal } returns proposal
        every { signatureService.computeBlockHash(any<Block<String>>()) } returns blockHash

        // Process a proposal so its ID is known
        proposalService.processProposal(signedProposal, notarizedBlocks)

        `when`("Process the proposal when proposal ID is already known") {
            every { newBlock.height } returns 2

            val result = proposalService.processProposal(signedProposal, notarizedBlocks)

            then("Should be false, indicating that the proposal is not valid") {
                result shouldBe false
            }
        }

        `when`("Process the proposal when proposal is invalid") {
            every { newBlock.height } returns 3
            every { signatureService.computeBlockHash(any<Block<String>>()) } returns "newHash"

            val result = proposalService.processProposal(signedProposal, notarizedBlocks)

            then("Should be false, indicating that the proposal is not valid") {
                result shouldBe false
            }
        }
    }
})