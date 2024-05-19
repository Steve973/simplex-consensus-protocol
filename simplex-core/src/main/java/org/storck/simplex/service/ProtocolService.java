package org.storck.simplex.service;

import com.fasterxml.jackson.core.type.TypeReference;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.storck.simplex.model.BlockchainNotarized;
import org.storck.simplex.model.Finalize;
import org.storck.simplex.model.FinalizeSigned;
import org.storck.simplex.model.PeerInfo;
import org.storck.simplex.model.ProposalSigned;
import org.storck.simplex.model.VoteSigned;
import org.storck.simplex.networking.api.message.NetworkMessage;
import org.storck.simplex.networking.api.message.NetworkMessageType;
import org.storck.simplex.networking.api.message.ProtocolMessage;
import org.storck.simplex.networking.api.message.ProtocolMessageType;
import org.storck.simplex.networking.api.network.NetworkEvent;
import org.storck.simplex.networking.api.network.NetworkEventMessage;
import org.storck.simplex.networking.api.network.PeerNetworkClient;
import org.storck.simplex.networking.api.protocol.ConsensusProtocolService;
import org.storck.simplex.networking.api.protocol.FinalizeProtocolMessage;
import org.storck.simplex.networking.api.protocol.ProposalProtocolMessage;
import org.storck.simplex.networking.api.protocol.VoteProtocolMessage;
import org.storck.simplex.util.MessageUtils;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * Orchestrates the overall execution of the Simplex Consensus Protocol,
 * managing iterations and finalization.
 *
 * @param <T> the transaction data type
 */
@Slf4j
@SuppressWarnings("PMD.CouplingBetweenObjects") // This main service class needs these objects
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
     * Manages the player's keypair, and digital signature functions.
     */
    private final DigitalSignatureService signatureService;

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

    /**
     * Flag that can be set to true to stop the service.
     */
    @Getter
    private boolean shutdown;

    /**
     * Creates the service that needs to be started via {@link #start()}.
     *
     * @param peerNetworkClient the client for network operations and information
     */
    public ProtocolService(final PeerNetworkClient peerNetworkClient) {
        this(UUID.randomUUID().toString(), 0, new PlayerService(), new DigitalSignatureService(), new BlockchainService<>(), peerNetworkClient);
    }

    /**
     * Constructs a new instance of the ProtocolService.
     *
     * @param localPlayerId the ID of the local player
     * @param iterationNumber the iteration number
     * @param playerService the player service instance
     * @param signatureService the digital signature service instance
     * @param blockchainService the blockchain service instance
     * @param peerNetworkClient the peer network client instance
     */
    @SuppressFBWarnings(value = { "EI_EXPOSE_REP2" }, justification = "You have to set final variables from a constructor.")
    public ProtocolService(final String localPlayerId, final int iterationNumber, final PlayerService playerService, final DigitalSignatureService signatureService,
            final BlockchainService<T> blockchainService, final PeerNetworkClient peerNetworkClient) {
        this(localPlayerId, iterationNumber, playerService, signatureService, new ProposalService<>(localPlayerId, signatureService, peerNetworkClient),
                new VotingService<>(signatureService, playerService), blockchainService, peerNetworkClient,
                new IterationService(localPlayerId, playerService, signatureService, peerNetworkClient));
    }

    /**
     * Constructs a new instance of the ProtocolService.
     *
     * @param localPlayerId the ID of the local player
     * @param iterationNumber the iteration number
     * @param playerService the player service instance
     * @param signatureService the digital signature service instance
     * @param proposalService the proposal service instance
     * @param votingService the voting service instance
     * @param blockchainService the blockchain service instance
     * @param peerNetworkClient the peer network client instance
     * @param iterationService the iteration service instance
     */
    @SuppressFBWarnings(value = { "EI_EXPOSE_REP2" }, justification = "You have to set final variables from a constructor.")
    public ProtocolService(final String localPlayerId, final int iterationNumber, final PlayerService playerService, final DigitalSignatureService signatureService,
            final ProposalService<T> proposalService, final VotingService<T> votingService, final BlockchainService<T> blockchainService, final PeerNetworkClient peerNetworkClient,
            final IterationService iterationService) {
        this.localPlayerId = localPlayerId;
        this.iterationNumber = iterationNumber;
        this.playerService = playerService;
        this.signatureService = signatureService;
        this.proposalService = proposalService;
        this.votingService = votingService;
        this.blockchainService = blockchainService;
        this.peerNetworkClient = peerNetworkClient;
        this.iterationService = iterationService;
    }

    /**
     * Processes a network message.
     *
     * @param message the network message to process
     */
    @Override
    public void processNetworkMessage(final NetworkMessage message) {
        NetworkMessageType messageType = Optional.ofNullable(message)
                .map(NetworkMessage::getMessageType)
                .orElse(NetworkMessageType.OTHER);
        if (NetworkMessageType.NETWORK_EVENT == messageType) {
            NetworkEventMessage eventMessage = (NetworkEventMessage) message;
            NetworkEvent networkEvent = eventMessage.event();
            switch (networkEvent) {
                case PEER_DISCONNECTED -> {
                    PeerInfo peerInfo = MessageUtils.peerInfoFromJson(eventMessage.details());
                    PublicKey removedPlayerKey = playerService.removePlayer(peerInfo.peerId());
                    if (removedPlayerKey == null) {
                        log.warn("Tried to remove unknown peer with peerId: '{}'", peerInfo.peerId());
                    } else {
                        log.info("Removed peer with peerId: '{}' and public key: '{}'", peerInfo.peerId(),
                                new String(removedPlayerKey.getEncoded(), StandardCharsets.UTF_8));
                    }
                }
                case PEER_CONNECTED -> {
                    PeerInfo peerInfo = MessageUtils.peerInfoFromJson(eventMessage.details());
                    byte[] publicKeyBytes = peerInfo.publicKeyBytes();
                    PublicKey publicKey = signatureService.publicKeyFromBytes(publicKeyBytes);
                    playerService.addPlayer(peerInfo.peerId(), publicKey);
                }
                default -> log.warn("Received unknown network message type: '{}'", networkEvent.name());
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
                VoteSigned signedVote = MessageUtils.fromBytes(voteMessage.getContent(), new TypeReference<>() {
                });
                if (votingService.processVote(signedVote)) {
                    // TODO: Quorum was reached, so blockchain is considered notarized -- handle
                    // this:
                    // 2/3 player vote means block is notarized, so stop the iteration and process
                    // the blockchain
                    iterationService.stopIteration();
                    // processNotarizedBlockchain(?);
                }
            }
            case PROPOSAL_MESSAGE -> {
                ProposalProtocolMessage proposalMessage = (ProposalProtocolMessage) message;
                ProposalSigned<T> signedProposal = MessageUtils.fromBytes(proposalMessage.getContent(), new TypeReference<>() {
                });
                processNotarizedBlockchain(signedProposal.proposal().parentChain());
                if (proposalService.processProposal(signedProposal, blockchainService.getBlockchain())) {
                    votingService.initializeForIteration(iterationNumber, signedProposal.proposal());
                    VoteSigned signedVote = votingService.createProposalVote(localPlayerId);
                    peerNetworkClient.broadcastVote(new VoteProtocolMessage(MessageUtils.toBytes(signedVote)));
                } else {
                    log.warn("Received invalid proposal");
                }
            }
            case FINALIZE_MESSAGE -> {
                FinalizeProtocolMessage finalizeMessage = (FinalizeProtocolMessage) message;
                FinalizeSigned finalizeSigned = MessageUtils.fromBytes(finalizeMessage.getContent(), new TypeReference<>() {
                });
                Finalize finalizeMsg = finalizeSigned.finalizeMsg();
                String playerId = finalizeSigned.finalizeMsg().playerId();
                PublicKey playerPublicKey = playerService.getPublicKey(playerId);
                boolean finalizeValid;
                try {
                    finalizeValid = signatureService.verifySignature(finalizeMessage.getContent(), finalizeSigned.signature(), playerPublicKey);
                } catch (GeneralSecurityException e) {
                    throw new IllegalArgumentException("Received an invalid finalize message", e);
                }
                if (finalizeValid) {
                    log.info("Received finalizeMsg message for iteration number: {}", finalizeMsg.iteration());
                    // TODO: handle finalization:
                    // evaluate if the specified iteration number has finalize messages from 2/3
                    // players
                    // if so, iteration is finalized, so transactions can be output to the client
                }
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
     * blockchain that was received. If the notarized blockchain is of the same
     * length as the current iteration number, then we send a finalize message.
     *
     * @param notarizedBlockchain the notarized blockchain containing the number to
     *     synchronize the iteration number with
     */
    void processNotarizedBlockchain(final BlockchainNotarized<T> notarizedBlockchain) {
        int chainLength = notarizedBlockchain.blocks().size() + 1;
        if (chainLength == iterationNumber) {
            // TODO: Move on to next iteration and send finalize message
            Finalize finalizeMessage = new Finalize(localPlayerId, iterationNumber);
            FinalizeSigned finalizeSigned = new FinalizeSigned(finalizeMessage, signatureService.generateSignature(MessageUtils.toBytes(finalizeMessage)));
            FinalizeProtocolMessage finalizeProtocolMessage = new FinalizeProtocolMessage(MessageUtils.toBytes(finalizeSigned));
            peerNetworkClient.broadcastFinalize(finalizeProtocolMessage);
        } else if (chainLength > iterationNumber) {
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
     *
     * @throws InterruptedException if interrupted while awaiting completion from
     *     the iteration service
     */
    @Override
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public void start() throws InterruptedException {
        while (!shutdown) {
            iterationService.initializeForIteration(++iterationNumber, new CountDownLatch(1));
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
