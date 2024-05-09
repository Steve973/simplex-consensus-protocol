package org.storck.simplex.service;

import com.google.common.base.Charsets;
import lombok.extern.slf4j.Slf4j;
import org.storck.simplex.model.NotarizedBlockchain;
import org.storck.simplex.model.PeerInfo;
import org.storck.simplex.model.SignedProposal;
import org.storck.simplex.model.SignedVote;
import org.storck.simplex.networking.api.message.NetworkMessage;
import org.storck.simplex.networking.api.message.NetworkMessageType;
import org.storck.simplex.networking.api.message.ProtocolMessage;
import org.storck.simplex.networking.api.message.ProtocolMessageType;
import org.storck.simplex.networking.api.network.NetworkEvent;
import org.storck.simplex.networking.api.network.NetworkEventMessage;
import org.storck.simplex.networking.api.network.PeerNetworkClient;
import org.storck.simplex.networking.api.protocol.ConsensusProtocolService;
import org.storck.simplex.networking.api.protocol.ProposalProtocolMessage;
import org.storck.simplex.networking.api.protocol.VoteProtocolMessage;
import org.storck.simplex.util.MessageUtils;

import java.security.PublicKey;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

/**
 * Orchestrates the overall execution of the Simplex Consensus Protocol,
 * managing iterations and finalization.
 *
 * @param <T> the transaction data type
 */
@Slf4j
public class ProtocolService<T> implements ConsensusProtocolService<T> {

    /** The ID of the local player (i.e., the ID for this local node). */
    private final String localPlayerId;

    /** The number of the current iteration. */
    private int iterationNumber;

    /**
     * Manages all peers/players that are participating.
     */
    private final PlayerService playerService;

    /**
     * Manages the creation, validation, and broadcasting of block proposals by the
     * leader.
     */
    private final ProposalService<T> proposalService;

    /**
     * Handles the voting process, including validation, tallying votes, and
     * notarizing blocks.
     */
    private final VotingService<T> votingService;

    /**
     * Handles communication/messaging between peers.
     */
    private final PeerNetworkClient peerNetworkClient;

    /**
     * Service to manage the blockchain.
     */
    private final BlockchainService<T> blockchainService;

    /**
     * Manages the iteration lifecycle.
     */
    private final IterationService iterationService;

    /** Flag that can be set to true to stop the service. */
    private boolean shutdown;

    /**
     * Creates the service that needs to be started via {@link #start()}.
     *
     * @param peerNetworkClient the client for network operations and information
     */
    public ProtocolService(final PeerNetworkClient peerNetworkClient) {
        this.localPlayerId = UUID.randomUUID().toString();
        this.iterationNumber = 0;
        this.playerService = new PlayerService();
        DigitalSignatureService signatureService = new DigitalSignatureService();
        this.proposalService = new ProposalService<>(localPlayerId, signatureService, peerNetworkClient);
        this.votingService = new VotingService<>(signatureService, playerService);
        this.blockchainService = new BlockchainService<>();
        this.peerNetworkClient = peerNetworkClient;
        this.iterationService = new IterationService(localPlayerId, playerService, signatureService, peerNetworkClient);
    }

    /**
     * Processes a network message.
     *
     * @param message the network message to process
     */
    @Override
    public void processNetworkMessage(final NetworkMessage message) {
        if (Objects.requireNonNull(message.getMessageType()) == NetworkMessageType.NETWORK_EVENT) {
            NetworkEventMessage eventMessage = (NetworkEventMessage) message;
            NetworkEvent networkEvent = eventMessage.event();
            switch (networkEvent) {
                case PEER_DISCONNECTED -> {
                    PeerInfo peerInfo = MessageUtils.peerInfoFromJson(eventMessage.details());
                    PublicKey removedPlayerKey = playerService.removePlayer(peerInfo.peerId());
                    if (removedPlayerKey == null) {
                        log.warn("Tried to remove unknown peer with peerId: '{}'", peerInfo.peerId());
                    } else {
                        log.info("Removed peer with peerId: '{}' and public key: '{}'", peerInfo.peerId(), new String(removedPlayerKey.getEncoded(), Charsets.UTF_8));
                    }
                }
                case PEER_CONNECTED -> {
                    PeerInfo peerInfo = MessageUtils.peerInfoFromJson(eventMessage.details());
                    playerService.addPlayer(peerInfo.peerId(), peerInfo.publicKey());
                }
                default -> log.warn("Received unknown network message type: '{}'", message.getMessageType());
            }
        }
    }

    /**
     * Processes a protocol message by extracting its type and performing the
     * corresponding actions.
     *
     * @param message the protocol message to process
     */
    @Override
    public void processProtocolMessage(final ProtocolMessage message) {
        ProtocolMessageType messageType = message.getType();
        switch (messageType) {
            case VOTE_MESSAGE -> {
                VoteProtocolMessage voteMessage = (VoteProtocolMessage) message;
                SignedVote signedVote = MessageUtils.fromBytes(voteMessage.content());
                if (votingService.processVote(signedVote)) {
                    // TODO: Quorum was reached, so handle this somehow
                    iterationService.stopIteration();
                    // Block<T> finalizeBlock =
                    // blockchainService.createFinalizeBlock.apply(iterationNumber);
                    // SignedFinalizeBlock signedFinalizeBlock = ???
                    // peerNetworkClient.broadcastFinalizeBlock(FinalizeProtocolMessage(MessageUtils.tyBytes(signedFinalizeBlock));
                }
            }
            case PROPOSAL_MESSAGE -> {
                ProposalProtocolMessage proposalMessage = (ProposalProtocolMessage) message;
                SignedProposal<T> signedProposal = MessageUtils.fromBytes(proposalMessage.content());
                synchronizeIterationNumber(signedProposal.proposal().parentChain());
                proposalService.processProposal(signedProposal, blockchainService.getBlockchain());
                votingService.initializeForIteration(iterationNumber, signedProposal.proposal());
                SignedVote signedVote = votingService.createProposalVote(localPlayerId);
                peerNetworkClient.broadcastVote(new VoteProtocolMessage(MessageUtils.toBytes(signedVote)));
            }
            case FINALIZE_MESSAGE -> {
                // TODO: handle finalization somehow
            }
            default -> log.warn("Message type unrecognized: {}", messageType);
        }
    }

    /**
     * Passes the given collection of transactions to the proposal service to be
     * proposed as a block the next time this node is the iteration leader.
     *
     * @param transactions the collection of transactions to receive
     */
    @Override
    public void processTransactions(final Collection<T> transactions) {
        proposalService.addTransactions(transactions);
    }

    /**
     * Synchronizes the iteration number based on the size of the notarized
     * blockchain that was received.
     *
     * @param notarizedBlockchain the notarized blockchain containing the number to
     *     synchronize the iteration number with
     */
    void synchronizeIterationNumber(final NotarizedBlockchain<T> notarizedBlockchain) {
        int chainLength = notarizedBlockchain.blocks().size() + 1;
        if (chainLength > iterationNumber) {
            iterationNumber = chainLength;
            iterationService.stopIteration();
        }
    }

    /**
     * Runs the consensus protocol until {@link #shutdown} flag is set to true.
     * Consists of several steps: 1. Enters a new iteration. 2. Create and broadcast
     * a proposal if the local player is the leader. 3. Listens for, validates, and
     * processes proposals and votes. 4. Outputs accepted transactions to the
     * client.
     */
    @Override
    public void start() {
        while (!shutdown) {
            this.iterationService.initializeForIteration(++iterationNumber);
            iterationService.startIteration();
            if (localPlayerId.equals(iterationService.getLeaderId())) {
                proposalService.proposeNewBlock(blockchainService.getBlockchain(), iterationNumber);
            }
            iterationService.awaitCompletion();
        }
    }

    /**
     * Sets the shutdown flag to stop this service.
     */
    @Override
    public void stop() {
        this.shutdown = true;
    }
}
