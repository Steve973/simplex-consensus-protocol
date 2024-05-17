package org.storck.simplex.service

import com.fasterxml.jackson.core.type.TypeReference
import edu.umd.cs.findbugs.annotations.CheckReturnValue
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.storck.simplex.model.*
import org.storck.simplex.networking.api.message.*
import org.storck.simplex.networking.api.network.NetworkEvent
import org.storck.simplex.networking.api.network.NetworkEventMessage
import org.storck.simplex.networking.api.network.PeerNetworkClient
import org.storck.simplex.networking.api.protocol.FinalizeProtocolMessage
import org.storck.simplex.networking.api.protocol.ProposalProtocolMessage
import org.storck.simplex.networking.api.protocol.VoteProtocolMessage
import org.storck.simplex.util.MessageUtils
import java.security.PublicKey

/**
 * Test the Blockchain Service.
 */
@OptIn(DelicateCoroutinesApi::class)
@SuppressFBWarnings(
    value = ["NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", "SE_BAD_FIELD", "NP_NULL_ON_SOME_PATH", "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT"],
    justification = "I cannot find anything wrong with the test, and mock objects used in a test do not need to be serializable.")
class ProtocolServiceTest : BehaviorSpec({
    val localPlayerId = "testPlayer"
    val iterationNumber = 0
    val playerService = mockk<PlayerService>()
    val signatureService = mockk<DigitalSignatureService>()
    val proposalService = mockk<ProposalService<String>>()
    val votingService = mockk<VotingService<String>>()
    val blockchainService = mockk<BlockchainService<String>>()
    val peerNetworkClient = mockk<PeerNetworkClient>()
    val iterationService = mockk<IterationService>()
    mockkStatic(MessageUtils::class)
    mockkStatic(DigitalSignatureService::class)

    afterTest {
        clearAllMocks()
    }

    Given("A peer network client") {

        When("an instance is created with the single-arg constructor accepting a peer network client") {
            val svc = ProtocolService<String>(peerNetworkClient)

            Then("Service created successfully") {
                svc shouldNotBe null
            }
        }
    }

    Given("a peer connected message") {
        val protocolService = ProtocolService(localPlayerId, iterationNumber, playerService, signatureService, proposalService,
            votingService, blockchainService, peerNetworkClient, iterationService)
        val peerId = "peerId"
        val publicKeyBytes = byteArrayOf(0x01, 0x02, 0x03)
        val details = "details"
        val mockPeerInfo = mockk<PeerInfo>()
        val mockPublicKey = mockk<PublicKey>()
        val message = NetworkEventMessage(NetworkEvent.PEER_CONNECTED, details)

        When("NetworkMessageType is NETWORK_EVENT and NetworkEvent is PEER_CONNECTED") {
            every { MessageUtils.peerInfoFromJson(details) } returns mockPeerInfo
            every { mockPeerInfo.publicKeyBytes() } returns publicKeyBytes
            every { signatureService.publicKeyFromBytes(any<ByteArray>()) } returns mockPublicKey
            every { mockPeerInfo.peerId } returns peerId
            every { playerService.addPlayer(any<String>(), any<PublicKey>()) } just Runs

            protocolService.processNetworkMessage(message)

            Then("New peer is added") {
                verify { playerService.addPlayer(any<String>(), any<PublicKey>()) }
            }
        }

    }

    Given("a peer disconnected message") {
        val protocolService = ProtocolService(localPlayerId, iterationNumber, playerService, signatureService, proposalService,
            votingService, blockchainService, peerNetworkClient, iterationService)
        val peerId = "peerId"
        val publicKeyEncoded = "publicKeyEncoded"
        val publicKeyBytes = publicKeyEncoded.toByteArray()
        val details = "details"
        val mockPeerInfo = mockk<PeerInfo>()
        val mockPublicKey = mockk<PublicKey>()
        val message = NetworkEventMessage(NetworkEvent.PEER_DISCONNECTED, details)

        When("NetworkMessageType is NETWORK_EVENT and NetworkEvent is PEER_DISCONNECTED") {
            every { mockPublicKey.encoded } returns publicKeyEncoded.toByteArray()
            every { mockPeerInfo.peerId } returns peerId
            every { mockPeerInfo.publicKeyBytes() } returns publicKeyBytes
            every { MessageUtils.peerInfoFromJson(details) } returns mockPeerInfo
            every { playerService.removePlayer(peerId) } returns mockPublicKey

            protocolService.processNetworkMessage(message)

            Then("Peer is disconnected") {
                verify(exactly = 1) { playerService.removePlayer(peerId) }
            }
        }

        When("NetworkMessageType is NETWORK_EVENT and NetworkEvent is PEER_DISCONNECTED, but peerId is unknown") {
            every { mockPublicKey.encoded } returns publicKeyEncoded.toByteArray()
            every { mockPeerInfo.peerId } returns peerId
            every { mockPeerInfo.publicKeyBytes() } returns publicKeyBytes
            every { MessageUtils.peerInfoFromJson(details) } returns mockPeerInfo
            every { playerService.removePlayer(peerId) } returns null

            protocolService.processNetworkMessage(message)

            Then("Peer is disconnected") {
                verify(exactly = 1) { playerService.removePlayer(peerId) }
            }
        }
    }

    Given("an unexpected message type") {
        val protocolService = ProtocolService(localPlayerId, iterationNumber, playerService, signatureService, proposalService,
            votingService, blockchainService, peerNetworkClient, iterationService)
        val message = NetworkEventMessage(NetworkEvent.OTHER, "unexpected message")

        When("a message with a null message type is processed") {

            protocolService.processNetworkMessage(message)

            Then("Nothing should happen") {
                verify(exactly = 0) { playerService.addPlayer(any(), any()) }
                verify(exactly = 0) { playerService.removePlayer(any()) }
            }
        }
    }

    Given("a null message type") {
        val protocolService = ProtocolService(localPlayerId, iterationNumber, playerService, signatureService, proposalService,
            votingService, blockchainService, peerNetworkClient, iterationService)
        val message = mockk<NetworkEventMessage>()

        When("a message with a null message type is processed") {
            every { message.messageType } returns null

            protocolService.processNetworkMessage(message)

            Then("Nothing should happen") {
                verify(exactly = 0) { message.event }
            }
        }
    }

    Given("a vote message") {
        val protocolService = ProtocolService(localPlayerId, iterationNumber, playerService, signatureService, proposalService,
            votingService, blockchainService, peerNetworkClient, iterationService)
        val voteMessage = mockk<VoteProtocolMessage>()
        val voteContentBytes = byteArrayOf(0x01, 0x02, 0x03)
        val signedVote = mockk<SignedVote>()

        When("a Vote message is processed without quorum reached") {
            every { voteMessage.type } returns ProtocolMessageType.VOTE_MESSAGE
            every { voteMessage.content } returns voteContentBytes
            every { MessageUtils.fromBytes(any<ByteArray>(), any<TypeReference<Any>>()) } returns signedVote
            every { votingService.processVote(any<SignedVote>()) } returns false

            protocolService.processProtocolMessage(voteMessage)

            Then("the service attempts to process and broadcast the vote") {
                verify(exactly = 1) { votingService.processVote(any()) }
                verify(exactly = 0) { iterationService.stopIteration() }
            }
        }

        When("a Vote message is processed with quorum reached") {
            every { voteMessage.type } returns ProtocolMessageType.VOTE_MESSAGE
            every { voteMessage.content } returns voteContentBytes
            every { MessageUtils.fromBytes(any<ByteArray>(), any<TypeReference<Any>>()) } returns signedVote
            every { votingService.processVote(any<SignedVote>()) } returns true
            every { iterationService.stopIteration() } just Runs

            protocolService.processProtocolMessage(voteMessage)

            Then("the service attempts to process and broadcast the vote") {
                verify(exactly = 1) { votingService.processVote(any()) }
                verify(exactly = 1) { iterationService.stopIteration() }
            }
        }
    }

    Given("a proposal message for processing") {
        val protocolService = ProtocolService(localPlayerId, iterationNumber, playerService, signatureService, proposalService,
            votingService, blockchainService, peerNetworkClient, iterationService)
        val proposalMessage = mockk<ProposalProtocolMessage>()
        val proposalContentBytes = byteArrayOf(0x01, 0x02, 0x03)
        val signedProposal = mockk<SignedProposal<String>>()
        val proposal = mockk<Proposal<String>>()
        val block1 = mockk<NotarizedBlock<String>>()
        val block2 = mockk<NotarizedBlock<String>>()
        val notarizedBlockchain = mockk<NotarizedBlockchain<String>>()
        val signedVote = mockk<SignedVote>()

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
                    any<SignedProposal<String>>(),
                    any<List<NotarizedBlock<String>>>()
                )
            } returns true
            every { votingService.initializeForIteration(any<Int>(), any<Proposal<String>>()) } just Runs
            every { votingService.createProposalVote(any<String>()) } returns signedVote
            every { MessageUtils.toBytes(any<Any>()) } returns proposalContentBytes
            every { peerNetworkClient.broadcastVote(any<VoteProtocolMessage>()) } just Runs

            protocolService.processProtocolMessage(proposalMessage)

            Then("the service attempts to process the proposal and broadcasts a vote") {
                verify(exactly = 1) { proposalService.processProposal(any(), any()) }
                verify(exactly = 1) { votingService.initializeForIteration(any(), any()) }
                verify(exactly = 1) { votingService.createProposalVote(localPlayerId) }
                verify(exactly = 1) { peerNetworkClient.broadcastVote(any()) }
            }
        }

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
                    any<SignedProposal<String>>(),
                    any<List<NotarizedBlock<String>>>()
                )
            } returns false

            protocolService.processProtocolMessage(proposalMessage)

            Then("the service attempts to process the proposal and broadcasts a vote") {
                verify(exactly = 1) { proposalService.processProposal(any(), any()) }
            }
        }
    }

    Given("a finalize message for processing") {
        val protocolService = ProtocolService(localPlayerId, iterationNumber, playerService, signatureService, proposalService,
            votingService, blockchainService, peerNetworkClient, iterationService)
        val finalizeMessage = FinalizeProtocolMessage(iterationNumber)

        When("a Finalize message is processed") {
            // TODO: The behavior for FINALIZE_MESSAGE case is not implemented yet, so we need to update this test once it is.
            protocolService.processProtocolMessage(finalizeMessage)

            Then("Something evaluated here") {
            }
        }
    }

    Given("an unexpected message for processing") {
        val protocolService = ProtocolService(localPlayerId, iterationNumber, playerService, signatureService, proposalService,
            votingService, blockchainService, peerNetworkClient, iterationService)
        val message = ProtocolMessage { ProtocolMessageType.OTHER }

        When("an unexpected message is processed") {
            protocolService.processProtocolMessage(message)

            Then("The default case should be invoked, so no service calls should be made") {
                verify(exactly = 0) { votingService.processVote(any()) }
                verify(exactly = 0) { proposalService.processProposal(any(), any()) }
            }
        }
    }

    Given("transactions to process") {
        val protocolService = ProtocolService(localPlayerId, iterationNumber, playerService, signatureService, proposalService,
            votingService, blockchainService, peerNetworkClient, iterationService)

        // Test for processTransactions method
        When("processTransactions is called") {
            val transaction = "Test transaction"
            val transactions = listOf(transaction)

            every { proposalService.addTransactions(any<Collection<String>>()) } just Runs

            protocolService.processTransactions(transactions)

            Then("it passes the transactions to the proposal service") {
                verify(exactly = 1) { proposalService.addTransactions(transactions) }
            }
        }
    }

    Given("a blockchain from a future iteration to sync with") {
        val protocolService = ProtocolService(localPlayerId, iterationNumber, playerService, signatureService, proposalService,
            votingService, blockchainService, peerNetworkClient, iterationService)
        val block1 = mockk<NotarizedBlock<String>>()
        val block2 = mockk<NotarizedBlock<String>>()
        val notarizedBlockchain = mockk<NotarizedBlockchain<String>>()

        When("synchronizeIterationNumber is called with blockchain") {
            every { notarizedBlockchain.blocks() } answers { listOf(block1, block2) }
            every { iterationService.stopIteration() } just Runs

            protocolService.synchronizeIterationNumber(notarizedBlockchain)

            Then("it synchronizes iteration number based on the size of the notarized blockchain") {
                verify { iterationService.stopIteration() }
            }
        }
    }

    Given("a check of block size to make sure we are in the correct iteration number") {
        val protocolService = ProtocolService(localPlayerId, 3, playerService, signatureService, proposalService,
            votingService, blockchainService, peerNetworkClient, iterationService)
        val block1 = mockk<NotarizedBlock<String>>()
        val block2 = mockk<NotarizedBlock<String>>()
        val notarizedBlockchain = mockk<NotarizedBlockchain<String>>()

        When("synchronizeIterationNumber is called with blockchain for iteration, but not to exceed iteration") {
            every { notarizedBlockchain.blocks() } answers { listOf(block1, block2) }
            every { iterationService.stopIteration() } just Runs

            protocolService.synchronizeIterationNumber(notarizedBlockchain)

            Then("nothing happens because the number of blocks is not greater than the iteration number") {
                verify(exactly = 0) { iterationService.stopIteration() }
            }
        }
    }

    Given("a service to start and stop") {
        val protocolService = ProtocolService(localPlayerId, iterationNumber, playerService, signatureService, proposalService,
            votingService, blockchainService, peerNetworkClient, iterationService)

        When("start method is called and then stopped") {
            every { iterationService.leaderId } returns "leaderId"
            every { iterationService.initializeForIteration(any<Int>()) } just Runs
            every { iterationService.startIteration() } just Runs
            every { iterationService.awaitCompletion() } just Runs
            every { proposalService.proposeNewBlock(any<List<NotarizedBlock<String>>>(), any<Int>()) } just Runs

            // starting protocol service in a separate coroutine because it has a blocking loop
            val job = GlobalScope.launch {
                protocolService.start()
            }
            delay(100)
            protocolService.stop()
            job.join()

            Then("the protocol runs its iterations until shut down") {
                verify(atLeast = 1) { iterationService.initializeForIteration(any()) }
                verify(atLeast = 1) { iterationService.startIteration() }
                verify(atLeast = 1) { iterationService.awaitCompletion() }
            }
        }
    }

    Given("an iteration to start where the player is the leader") {
        val protocolService = ProtocolService(localPlayerId, iterationNumber, playerService, signatureService, proposalService,
            votingService, blockchainService, peerNetworkClient, iterationService)
        val block1 = mockk<NotarizedBlock<String>>()
        val block2 = mockk<NotarizedBlock<String>>()

        When("start method is called when local player is the iteration leader") {
            every { blockchainService.blockchain } returns listOf(block1, block2)
            every { iterationService.leaderId } returns localPlayerId
            every { iterationService.initializeForIteration(any<Int>()) } just Runs
            every { iterationService.startIteration() } just Runs
            every { iterationService.awaitCompletion() } just Runs
            every { proposalService.proposeNewBlock(any<List<NotarizedBlock<String>>>(), any<Int>()) } just Runs

            // starting protocol service in a separate coroutine because it has a blocking loop
            val job = GlobalScope.launch {
                protocolService.start()
            }
            delay(10)
            protocolService.stop()
            job.join()

            Then("as the leader, the proposal service should be called to propose a new block") {
                verify(atLeast = 1) { proposalService.proposeNewBlock(any<List<NotarizedBlock<String>>>(), any<Int>()) }
            }
        }
    }

    Given("a running service to stop") {
        val protocolService = ProtocolService(localPlayerId, iterationNumber, playerService, signatureService, proposalService,
            votingService, blockchainService, peerNetworkClient, iterationService)

        When("stop method is called") {
            protocolService.stop()

            Then("it should set shutdown flag to true") {
                protocolService.isShutdown shouldBe true
            }
        }
    }
})