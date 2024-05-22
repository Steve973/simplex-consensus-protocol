package org.storck.simplex.service

import com.fasterxml.jackson.core.type.TypeReference
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.storck.simplex.api.message.ProtocolMessage
import org.storck.simplex.api.message.ProtocolMessageType
import org.storck.simplex.model.*
import org.storck.simplex.api.network.NetworkEvent
import org.storck.simplex.api.network.NetworkEventMessage
import org.storck.simplex.api.network.PeerNetworkClient
import org.storck.simplex.api.protocol.FinalizeProtocolMessage
import org.storck.simplex.api.protocol.ProposalProtocolMessage
import org.storck.simplex.api.protocol.VoteProtocolMessage
import org.storck.simplex.util.MessageUtils
import java.security.GeneralSecurityException
import java.security.PublicKey

/**
 * Test the Blockchain Service.
 */
@SuppressFBWarnings(
    value = ["NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", "SE_BAD_FIELD", "NP_NULL_ON_SOME_PATH", "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT"],
    justification = "It is a test.")
class ProtocolServiceTest : BehaviorSpec({
    val details = "details"
    val peerId = "peerId"
    val localPlayerId = "testPlayer"
    val iterationNumber = 0
    val playerService = mockk<PlayerService>()
    val signatureService = mockk<DigitalSignatureService>()
    val proposalService = mockk<ProposalService<String>>()
    val votingService = mockk<VotingService<String>>()
    val blockchainService = mockk<BlockchainService<String>>()
    val peerNetworkClient = mockk<PeerNetworkClient>()
    val iterationService = mockk<IterationService>()
    val block1 = mockk<BlockNotarized<String>>()
    val block2 = mockk<BlockNotarized<String>>()
    val notarizedBlockchain = mockk<BlockchainNotarized<String>>()
    val mockPeerInfo = mockk<PeerInfo>()
    val mockPublicKey = mockk<PublicKey>()
    val signedVote = mockk<VoteSigned>()
    mockkStatic(MessageUtils::class)
    mockkStatic(DigitalSignatureService::class)

    lateinit var protocolService: ProtocolService<String>

    beforeEach {
        protocolService = ProtocolService(
            localPlayerId, iterationNumber, playerService, signatureService, proposalService,
            votingService, blockchainService, peerNetworkClient, iterationService)
    }

    afterEach {
        clearAllMocks()
    }

    Given("A peer network client") {

        When("an instance is created with the single-arg constructor accepting a peer network client") {
            val svc = ProtocolService<String>(peerNetworkClient)

            Then("Service created successfully") {
                svc.isShutdown shouldBe false
                svc shouldNotBe null
            }
        }
    }

    Given("a peer connected message") {
        val publicKeyBytes = byteArrayOf(0x01, 0x02, 0x03)
        val message = NetworkEventMessage(NetworkEvent.PEER_CONNECTED, details)

        When("NetworkMessageType is NETWORK_EVENT and NetworkEvent is PEER_CONNECTED") {
            every { MessageUtils.peerInfoFromJson(details) } returns mockPeerInfo
            every { mockPeerInfo.publicKeyBytes() } returns publicKeyBytes
            every { signatureService.publicKeyFromBytes(any<ByteArray>()) } returns mockPublicKey
            every { mockPeerInfo.peerId } returns peerId
            every { playerService.addPlayer(any<String>(), any<PublicKey>()) } just Runs

            protocolService.processNetworkMessage(message)

            Then("New peer is added") {
                protocolService.isShutdown shouldBe false
                verify { playerService.addPlayer(any<String>(), any<PublicKey>()) }
            }
        }

    }

    Given("a peer disconnected message") {
        val publicKeyEncoded = "publicKeyEncoded"
        val publicKeyBytes = publicKeyEncoded.toByteArray()
        val message = NetworkEventMessage(NetworkEvent.PEER_DISCONNECTED, details)

        When("NetworkMessageType is NETWORK_EVENT and NetworkEvent is PEER_DISCONNECTED") {
            every { mockPublicKey.encoded } returns publicKeyEncoded.toByteArray()
            every { mockPeerInfo.peerId } returns peerId
            every { mockPeerInfo.publicKeyBytes() } returns publicKeyBytes
            every { MessageUtils.peerInfoFromJson(details) } returns mockPeerInfo
            every { playerService.removePlayer(peerId) } returns mockPublicKey

            protocolService.processNetworkMessage(message)

            Then("Peer is disconnected") {
                protocolService.isShutdown shouldBe false
                verify(exactly = 1) { playerService.removePlayer(peerId) }
            }
        }
    }

    Given("a message from an unknown peer") {
        val publicKeyEncoded = "publicKeyEncoded"
        val publicKeyBytes = publicKeyEncoded.toByteArray()
        val message = NetworkEventMessage(NetworkEvent.PEER_DISCONNECTED, details)

        When("NetworkMessageType is NETWORK_EVENT and NetworkEvent is PEER_DISCONNECTED, but peerId is unknown") {
            every { mockPublicKey.encoded } returns publicKeyEncoded.toByteArray()
            every { mockPeerInfo.peerId } returns peerId
            every { mockPeerInfo.publicKeyBytes() } returns publicKeyBytes
            every { MessageUtils.peerInfoFromJson(details) } returns mockPeerInfo
            every { playerService.removePlayer(peerId) } returns null

            protocolService.processNetworkMessage(message)

            Then("Peer is disconnected") {
                protocolService.isShutdown shouldBe false
                verify(exactly = 1) { playerService.removePlayer(peerId) }
            }
        }
    }

    Given("an unexpected message type") {
        val message = NetworkEventMessage(NetworkEvent.OTHER, "unexpected message")

        When("a message with a null message type is processed") {

            protocolService.processNetworkMessage(message)

            Then("Nothing should happen") {
                protocolService.isShutdown shouldBe false
                verify(exactly = 0) { playerService.addPlayer(any(), any()) }
                verify(exactly = 0) { playerService.removePlayer(any()) }
            }
        }
    }

    Given("a null message type") {
        val message = mockk<NetworkEventMessage>()

        When("a message with a null message type is processed") {
            every { message.messageType } returns null

            protocolService.processNetworkMessage(message)

            Then("Nothing should happen") {
                protocolService.isShutdown shouldBe false
                verify(exactly = 0) { message.event }
            }
        }
    }

    Given("a vote message") {
        val voteMessage = mockk<VoteProtocolMessage>()
        val voteContentBytes = byteArrayOf(0x01, 0x02, 0x03)

        When("a Vote message is processed without quorum reached") {
            every { voteMessage.type } returns ProtocolMessageType.VOTE_MESSAGE
            every { voteMessage.content } returns voteContentBytes
            every { MessageUtils.fromBytes(any<ByteArray>(), any<TypeReference<Any>>()) } returns signedVote
            every { votingService.processVote(any<VoteSigned>()) } returns false

            protocolService.processProtocolMessage(voteMessage)

            Then("the service attempts to process and broadcast the vote") {
                protocolService.isShutdown shouldBe false
                verify(exactly = 1) { votingService.processVote(any()) }
                verify(exactly = 0) { iterationService.stopIteration() }
            }
        }
    }

    Given("a vote that has a quorum reached") {
        val voteMessage = mockk<VoteProtocolMessage>()
        val voteContentBytes = byteArrayOf(0x01, 0x02, 0x03)

        When("a Vote message is processed with quorum reached") {
            every { voteMessage.type } returns ProtocolMessageType.VOTE_MESSAGE
            every { voteMessage.content } returns voteContentBytes
            every { MessageUtils.fromBytes(any<ByteArray>(), any<TypeReference<Any>>()) } returns signedVote
            every { votingService.processVote(any<VoteSigned>()) } returns true
            every { iterationService.stopIteration() } just Runs

            protocolService.processProtocolMessage(voteMessage)

            Then("the service attempts to process and broadcast the vote") {
                protocolService.isShutdown shouldBe false
                verify(exactly = 1) { votingService.processVote(any()) }
                verify(exactly = 1) { iterationService.stopIteration() }
            }
        }
    }

    Given("a proposal message for processing") {
        val proposalMessage = mockk<ProposalProtocolMessage>()
        val proposalContentBytes = byteArrayOf(0x01, 0x02, 0x03)
        val signedProposal = mockk<ProposalSigned<String>>()
        val proposal = mockk<Proposal<String>>()

        When("a Proposal message is processed") {
            every { proposalMessage.type } returns ProtocolMessageType.PROPOSAL_MESSAGE
            every { proposalMessage.content } returns proposalContentBytes
            every { MessageUtils.fromBytes(any<ByteArray>(), any<TypeReference<Any>>()) } returns signedProposal
            every { signedProposal.proposal } returns proposal
            every { proposal.parentChain } returns notarizedBlockchain
            every { notarizedBlockchain.blocks() } returns listOf(block1, block2)
            every { iterationService.stopIteration() } just Runs
            every { blockchainService.blockchain } returns listOf(block1, block2)
            every {
                proposalService.processProposal(
                    any<ProposalSigned<String>>(),
                    any<List<BlockNotarized<String>>>()
                )
            } returns true
            every { votingService.initializeForIteration(any<Int>(), any<Proposal<String>>()) } just Runs
            every { votingService.createProposalVote(any<String>()) } returns signedVote
            every { MessageUtils.toBytes(any<Any>()) } returns proposalContentBytes
            every { peerNetworkClient.broadcastVote(any<VoteProtocolMessage>()) } just Runs

            protocolService.processProtocolMessage(proposalMessage)

            Then("the service attempts to process the proposal and broadcasts a vote") {
                protocolService.isShutdown shouldBe false
                verify(exactly = 2) { signedProposal.proposal }
                verify(exactly = 1) { proposal.parentChain }
                verify(exactly = 1) { proposalService.processProposal(any(), any()) }
                verify(exactly = 1) { votingService.initializeForIteration(any(), any()) }
                verify(exactly = 1) { votingService.createProposalVote(localPlayerId) }
                verify(exactly = 1) { peerNetworkClient.broadcastVote(any()) }
            }
        }
    }

    Given("an invalid proposal message for processing") {
        val proposalMessage = mockk<ProposalProtocolMessage>()
        val proposalContentBytes = byteArrayOf(0x01, 0x02, 0x03)
        val signedProposal = mockk<ProposalSigned<String>>()
        val proposal = mockk<Proposal<String>>()

        When("an invalid Proposal message is processed") {
            every { proposalMessage.type } returns ProtocolMessageType.PROPOSAL_MESSAGE
            every { proposalMessage.content } returns proposalContentBytes
            every { MessageUtils.fromBytes(any<ByteArray>(), any<TypeReference<Any>>()) } returns signedProposal
            every { signedProposal.proposal } returns proposal
            every { proposal.parentChain } returns notarizedBlockchain
            every { notarizedBlockchain.blocks() } returns listOf(block1)
            every { blockchainService.blockchain } returns listOf(block1, block2)
            every {
                proposalService.processProposal(
                    any<ProposalSigned<String>>(),
                    any<List<BlockNotarized<String>>>()
                )
            } returns false
            every { iterationService.stopIteration() } just Runs

            protocolService.processProtocolMessage(proposalMessage)

            Then("the service attempts to process the proposal and broadcasts a vote") {
                protocolService.isShutdown shouldBe false
                verify(exactly = 1) { proposalService.processProposal(any(), any()) }
            }
        }
    }

    Given("a finalizeMsg message for processing") {
        val finalizeContentBytes = byteArrayOf(0x01, 0x02, 0x03)
        val finalizeMessage = mockk<FinalizeProtocolMessage>()
        val signedFinalize = mockk<FinalizeSigned>()
        val finalize = mockk<Finalize>()
        val playerPubKey = mockk<PublicKey>()

        When("a Finalize message is processed") {
            every { finalizeMessage.type } returns ProtocolMessageType.FINALIZE_MESSAGE
            every { finalizeMessage.content } returns finalizeContentBytes
            every { MessageUtils.fromBytes(any<ByteArray>(), any<TypeReference<Any>>()) } returns signedFinalize
            every { signedFinalize.finalizeMsg } returns finalize
            every { signedFinalize.signature } returns finalizeContentBytes
            every { playerService.getPublicKey(any()) } returns playerPubKey
            every { signatureService.verifySignature(any(), any(), any()) } returns true
            every { finalize.playerId } returns "otherPlayerId"
            every { finalize.iteration } returns iterationNumber
            every { iterationService.logFinalizeReceipt(any()) } just Runs

            protocolService.processProtocolMessage(finalizeMessage)

            Then("Something evaluated here") {
                protocolService.isShutdown shouldBe false
                // TODO: The behavior for FINALIZE_MESSAGE case is not finished yet, so update this later!
                verify(exactly = 1) { iterationService.logFinalizeReceipt(any()) }
            }
        }

        When("signature service throws an exception") {
            every { finalize.playerId } returns "otherPlayerId"
            every { finalizeMessage.type } returns ProtocolMessageType.FINALIZE_MESSAGE
            every { finalizeMessage.content } returns finalizeContentBytes
            every { MessageUtils.fromBytes(any<ByteArray>(), any<TypeReference<Any>>()) } returns signedFinalize
            every { signedFinalize.finalizeMsg } returns finalize
            every { signedFinalize.signature } returns finalizeContentBytes
            every { playerService.getPublicKey(any()) } returns playerPubKey
            every { signatureService.verifySignature(any(), any(), any()) } throws GeneralSecurityException("test")

            val exception = shouldThrow<IllegalArgumentException> { protocolService.processProtocolMessage(finalizeMessage) }

            Then("exception should indicate that the finalize message was invalid") {
                exception.message shouldBe "Received an invalid finalize message"
            }
        }
    }

    Given("an unexpected message for processing") {
        val message = object : ProtocolMessage(byteArrayOf(1, 2, 3)) {
            override fun getType(): ProtocolMessageType {
                return ProtocolMessageType.OTHER
            }
        }

        When("an unexpected message is processed") {
            protocolService.processProtocolMessage(message)

            Then("The default case should be invoked, so no service calls should be made") {
                protocolService.isShutdown shouldBe false
                verify(exactly = 0) { votingService.processVote(any()) }
                verify(exactly = 0) { proposalService.processProposal(any(), any()) }
            }
        }
    }

    Given("transactions to process") {

        // Test for processTransactions method
        When("processTransactions is called") {
            val transaction = "Test transaction"
            val transactions = listOf(transaction)

            every { proposalService.addTransactions(any<Collection<String>>()) } just Runs

            protocolService.processTransactions(transactions)

            Then("it passes the transactions to the proposal service") {
                protocolService.isShutdown shouldBe false
                verify(exactly = 1) { proposalService.addTransactions(transactions) }
            }
        }
    }

    Given("a blockchain from a future iteration to sync with") {

        When("synchronizeIterationNumber is called with blockchain") {
            every { notarizedBlockchain.blocks() } answers { listOf(block1, block2) }
            every { iterationService.stopIteration() } just Runs

            protocolService.processNotarizedBlockchain(notarizedBlockchain)

            Then("it synchronizes iteration number based on the size of the notarized blockchain") {
                protocolService.isShutdown shouldBe false
                verify { iterationService.stopIteration() }
            }
        }
    }

    Given("a check of block size to make sure we are in the correct iteration number") {
        val svc = ProtocolService("test", 5, playerService, signatureService, proposalService,
            votingService, blockchainService, peerNetworkClient, iterationService)

        When("synchronizeIterationNumber is called with blockchain for iteration, but not to exceed iteration") {
            every { notarizedBlockchain.blocks() } answers { listOf(block1, block2) }

            svc.processNotarizedBlockchain(notarizedBlockchain)

            Then("nothing happens because the number of blocks is not greater than the iteration number") {
                protocolService.isShutdown shouldBe false
                verify(exactly = 0) { iterationService.stopIteration() }
            }
        }
    }

    Given("blockchain is notarized") {
        val svc = ProtocolService("test", 2, playerService, signatureService, proposalService,
            votingService, blockchainService, peerNetworkClient, iterationService)

        When("synchronizeIterationNumber is called with blockchain for iteration, and has height same as iteration") {
            every { notarizedBlockchain.blocks() } answers { listOf(block1) }
            every { iterationService.stopIteration() } just Runs
            every { signatureService.generateSignature(any()) } returns byteArrayOf(0x01, 0x02, 0x03)
            every { peerNetworkClient.broadcastFinalize(any()) } just Runs

            svc.processNotarizedBlockchain(notarizedBlockchain)

            Then("iteration should be stopped and finalize should be broadcast") {
                protocolService.isShutdown shouldBe false
                verify { peerNetworkClient.broadcastFinalize(any()) }
                verify(exactly = 1) { iterationService.stopIteration() }
            }
        }
    }

    Given("a service to start and stop") {
        val coroutineScope = CoroutineScope(Dispatchers.Default)

        When("start method is called and then stopped") {
            every { iterationService.leaderId } returns "leaderId"
            every { iterationService.initializeForIteration(any<Int>(), any()) } just Runs
            every { iterationService.startIteration() } just Runs
            every { iterationService.awaitCompletion() } just Runs
            every { proposalService.proposeNewBlock(any<List<BlockNotarized<String>>>(), any<Int>()) } just Runs

            // starting protocol service in a separate coroutine because it has a blocking loop
            val job = coroutineScope.launch {
                protocolService.start()
            }
            delay(100)
            protocolService.stop()
            job.join()

            Then("the protocol runs its iterations until shut down") {
                verify(atLeast = 1) { iterationService.initializeForIteration(iterationNumber + 1, any()) }
                verify(atLeast = 1) { iterationService.startIteration() }
                verify(atLeast = 1) { iterationService.awaitCompletion() }
            }
        }
    }

    Given("an iteration to start where the player is the leader") {
        val coroutineScope = CoroutineScope(Dispatchers.Default)

        When("start method is called when local player is the iteration leader") {
            every { blockchainService.blockchain } returns listOf(block1, block2)
            every { iterationService.leaderId } returns localPlayerId
            every { iterationService.initializeForIteration(any<Int>(), any()) } just Runs
            every { iterationService.startIteration() } just Runs
            every { iterationService.awaitCompletion() } just Runs
            every { proposalService.proposeNewBlock(any<List<BlockNotarized<String>>>(), any<Int>()) } just Runs

            // starting protocol service in a separate coroutine because it has a blocking loop
            val job = coroutineScope.launch {
                protocolService.start()
            }
            delay(100)
            protocolService.stop()
            job.join()

            Then("as the leader, the proposal service should be called to propose a new block") {
//                protocolService.isShutdown shouldBe true
                verify(atLeast = 1) { proposalService.proposeNewBlock(any<List<BlockNotarized<String>>>(), any<Int>()) }
            }
        }
    }

    Given("a running service to stop") {
        val svc = ProtocolService(
            "someUser", 5, playerService, signatureService, proposalService,
            votingService, blockchainService, peerNetworkClient, iterationService)

        When("stop method is called") {
            svc.stop()

            Then("it should set shutdown flag to true") {
                svc.isShutdown shouldBe true
            }
        }
    }
})
