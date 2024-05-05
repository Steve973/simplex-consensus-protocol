package org.storck.simplex.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Setter;
import org.storck.simplex.model.Block;
import org.storck.simplex.model.Iteration;
import org.storck.simplex.model.NotarizedBlock;
import org.storck.simplex.model.NotarizedBlockchain;
import org.storck.simplex.model.PeerInfo;
import org.storck.simplex.model.Proposal;
import org.storck.simplex.model.SignedProposal;
import org.storck.simplex.model.SignedVote;
import org.storck.simplex.model.Vote;
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
import org.storck.simplex.util.CryptoUtil;
import org.storck.simplex.util.ProposalValidator;
import org.storck.simplex.util.SimplexConstants;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.LinkedTransferQueue;

import static org.storck.simplex.util.SimplexConstants.createNotarizedBlock;
import static org.storck.simplex.util.SimplexConstants.finalizeIteration;
import static org.storck.simplex.util.SimplexConstants.fromBytes;
import static org.storck.simplex.util.SimplexConstants.genesisBlock;
import static org.storck.simplex.util.SimplexConstants.peerInfoFromJson;
import static org.storck.simplex.util.SimplexConstants.signedProposalToBytes;
import static org.storck.simplex.util.SimplexConstants.signedVoteToBytes;

/**
 * This service implements the steps of the Simplex consensus protocol --
 * backup, leader election, proposal submission,
 * validating and responding to proposals and votes, block notarization, and the
 * output of accepted transactions to the
 * client.
 *
 * @param <T>
 *            the type of the transactions stored in the blocks
 */
public class SimplexConsensusProtocolService<T> implements ConsensusProtocolService<T> {

    /**
     * The ID of the local player (i.e., the ID for this local node).
     */
    private final String localPlayerId;

    /**
     * The network client that handles peer discovery, as well as sending and
     * receiving messages between peers.
     */
    private final PeerNetworkClient peerNetworkClient;

    /**
     * The key pair used for cryptographic operations, including signing and
     * verifying proposals and votes.
     */
    private final KeyPair keyPair;

    /**
     * Map that stores the association between player IDs and their corresponding
     * public keys.
     */
    private final Map<String, PublicKey> playerIdsToPublicKeys;

    /**
     * Under good network conditions, every message is delivered within <delta>
     * seconds. This is that value, and it is
     * provided by the peer network client.
     */
    private final int deltaSeconds;

    /**
     * Contains all notarized (accepted) blocks.
     */
    private final NotarizedBlockchain<T> notarizedBlockchain;

    /**
     * Keeps track of proposals that have been processed before to avoid processing
     * them again.
     */
    private final Set<String> processedProposalIds;

    /**
     * Stores transactions until this player is the leader, and can propose them as
     * a block.
     */
    private final LinkedTransferQueue<T> transactionQueue;

    /**
     * The number of the current iteration.
     */
    private int iterationNumber;

    /**
     * The iteration instance that manages the iteration lifecycle.
     */
    private Iteration<T> currentIteration;

    /**
     * Flag indicating if the peer-to-peer network is currently down as reported by
     * the {@link PeerNetworkClient} in a {@link NetworkMessage}.
     */
    private boolean networkDown;

    /**
     * Flag that can be set to true to stop the service.
     */
    @Setter
    private boolean shutdown = false;

    /**
     * Create the instance of the Simplex consensus protocol service.
     */
    public SimplexConsensusProtocolService(PeerNetworkClient peerNetworkClient, KeyPair keyPair, Map<String, PublicKey> playerIdsToPublicKeys, int deltaSeconds) {
        this.peerNetworkClient = peerNetworkClient;
        this.keyPair = keyPair;
        this.localPlayerId = UUID.randomUUID().toString();
        this.playerIdsToPublicKeys = playerIdsToPublicKeys;
        this.deltaSeconds = deltaSeconds;
        this.notarizedBlockchain = new NotarizedBlockchain<>(new ArrayList<>());
        this.iterationNumber = 0;
        this.transactionQueue = new LinkedTransferQueue<>();
        this.processedProposalIds = new HashSet<>();
        notarizedBlockchain.blocks().add(new NotarizedBlock<>(genesisBlock(), List.of()));
    }

    /**
     * Synchronizes the iteration number based on the size of the notarized
     * blockchain that was received.
     *
     * @param notarizedBlockchain
     *            the notarized blockchain to synchronize the iteration number with
     */
    void synchronizeIterationNumber(NotarizedBlockchain<T> notarizedBlockchain) {
        int chainLength = notarizedBlockchain.blocks().size();
        if (chainLength > iterationNumber) {
            iterationNumber = chainLength + 1;
        }
    }

    /**
     * Updates the current iteration number and elects the leader for the iteration.
     */
    void enterIteration() {
        this.iterationNumber++;
        this.currentIteration = new Iteration<>(iterationNumber, localPlayerId, playerIdsToPublicKeys, deltaSeconds, peerNetworkClient, keyPair.getPrivate());
        this.currentIteration.startIteration();
    }

    /**
     * If the local player is the leader, it retrieves pending transactions, creates
     * a proposal, and broadcasts it to peers.
     *
     * @throws JsonProcessingException
     *             if an error occurs during JSON serialization
     * @throws GeneralSecurityException
     *             if there is a security exception during cryptographic operations
     */
    void proposeNewBlock() throws JsonProcessingException, GeneralSecurityException {
        if (localPlayerId.equals(currentIteration.getLeaderId())) {
            Collection<T> transactions = new ArrayList<>();
            transactionQueue.drainTo(transactions);
            int currentBlockchainSize = notarizedBlockchain.blocks().size();
            String parentHash = Optional.of(notarizedBlockchain.blocks())
                    .map(blocks -> blocks.get(currentBlockchainSize - 1))
                    .map(NotarizedBlock::block)
                    .map(CryptoUtil::computeHash)
                    .orElseThrow(() -> new IllegalArgumentException("Could not get parent block hash for proposal"));
            Proposal<T> proposal = new Proposal<>(iterationNumber, localPlayerId, new Block<>(currentBlockchainSize + 1, parentHash, transactions), notarizedBlockchain);
            byte[] proposalBytes = SimplexConstants.proposalToBytes(proposal);
            byte[] proposalSignature = CryptoUtil.generateSignature(keyPair.getPrivate(), proposalBytes);
            SignedProposal<T> signedProposal = new SignedProposal<>(proposal, proposalSignature);
            if (!networkDown) {
                peerNetworkClient.broadcastProposal(new ProposalProtocolMessage(signedProposalToBytes(signedProposal)));
            }
        }
    }

    private void removePeer(String peerId) {
        playerIdsToPublicKeys.remove(peerId);
    }

    private void addPeer(String peerId, PublicKey publicKey) {
        playerIdsToPublicKeys.put(peerId, publicKey);
    }

    @Override
    public void processNetworkMessage(NetworkMessage message) throws JsonProcessingException {
        if (Objects.requireNonNull(message.getMessageType()) == NetworkMessageType.NETWORK_EVENT) {
            NetworkEventMessage eventMessage = (NetworkEventMessage) message;
            NetworkEvent networkEvent = eventMessage.event();
            switch (networkEvent) {
                case PEER_DISCONNECTED -> {
                    PeerInfo peerInfo = peerInfoFromJson(eventMessage.details());
                    removePeer(peerInfo.peerId());
                }
                case PEER_CONNECTED -> {
                    PeerInfo peerInfo = peerInfoFromJson(eventMessage.details());
                    addPeer(peerInfo.peerId(), peerInfo.publicKey());
                }
                case NETWORK_DOWN -> this.networkDown = true;
                case NETWORK_RESTORED -> this.networkDown = false;
            }
        }
    }

    /**
     * Processes a proposal by validating it and broadcasting the resulting vote.
     *
     * @param signedProposal
     *            the proposal message to process
     * @throws GeneralSecurityException
     *             if there is a security exception during signature verification
     * @throws JsonProcessingException
     *             if an error occurs during JSON serialization
     */
    public void processProposal(SignedProposal<T> signedProposal) throws IOException, GeneralSecurityException {
        SignedVote signedVote = null;
        Proposal<T> proposal = signedProposal.proposal();
        String proposalId = CryptoUtil.computeHash(proposal.newBlock());
        if (!processedProposalIds.contains(proposalId) && ProposalValidator.isValidProposal(signedProposal, notarizedBlockchain)) {
            processedProposalIds.add(proposalId);
            currentIteration.setProposal(proposal, proposalId);
            Vote vote = new Vote(localPlayerId, iterationNumber, proposalId);
            byte[] voteBytes = SimplexConstants.voteToBytes(vote);
            byte[] voteSignature = CryptoUtil.generateSignature(keyPair.getPrivate(), voteBytes);
            signedVote = new SignedVote(vote, voteSignature);
        }
        if (!networkDown && signedVote != null) {
            synchronizeIterationNumber(signedProposal.proposal().parentChain());
            peerNetworkClient.broadcastVote(new VoteProtocolMessage(signedVoteToBytes(signedVote)));
        }
    }

    /**
     * Processes a vote through the {@link #currentIteration}. If a quorum is
     * reached after processing this vote, it finalizes the iteration, and outputs
     * the transactions to the client.
     *
     * @param signedVote
     *            the vote message to process
     * @throws GeneralSecurityException
     *             if there is a security exception during signature verification
     * @throws JsonProcessingException
     *             if an error occurs during JSON serialization
     */
    public void processVote(SignedVote signedVote) throws GeneralSecurityException, IOException {
        // Process vote and check if a quorum has been reached for this proposal
        if (currentIteration.processVote(signedVote)) {
            // Create a notarized block with the proposal and the quorum votes
            Block<T> newBlock = currentIteration.getProposal().newBlock();
            List<Vote> votes = currentIteration.getVotes();
            NotarizedBlock<T> notarizedBlock = createNotarizedBlock(newBlock, votes);

            // Finalize the iteration
            finalizeIteration(notarizedBlock, notarizedBlockchain);

            // Output transactions to the client
            outputTransactions(notarizedBlock);
        }
    }

    @Override
    public void processTransactions(Collection<T> transactions) {
        transactionQueue.addAll(transactions);
    }

    /**
     * Processes a protocol message by extracting its type and performing the
     * corresponding actions.
     *
     * @param message
     *            the protocol message to process
     * @throws IOException
     *             if an I/O error occurs
     * @throws GeneralSecurityException
     *             if a security exception occurs
     */
    @Override
    public void processProtocolMessage(ProtocolMessage message) throws IOException, GeneralSecurityException {
        ProtocolMessageType messageType = message.getType();
        switch (messageType) {
            case VOTE_MESSAGE -> {
                VoteProtocolMessage voteMessage = (VoteProtocolMessage) message;
                SignedVote signedVote = fromBytes(voteMessage.content());
                processVote(signedVote);
            }
            case PROPOSAL_MESSAGE -> {
                ProposalProtocolMessage proposalMessage = (ProposalProtocolMessage) message;
                SignedProposal<T> signedProposal = fromBytes(proposalMessage.content());
                processProposal(signedProposal);
            }
        }
    }

    /**
     * Outputs transactions from a notarized and finalized block to the client.
     *
     * @param notarizedBlock
     *            a block that has been notarized and finalized that is ready to
     *            send to the client
     */
    private void outputTransactions(NotarizedBlock<T> notarizedBlock) {
        // Output the transactions in the notarized block to the client
        for (T transaction : notarizedBlock.block().transactions()) {
            // Output or process the transaction
            System.out.println(transaction);
        }
    }

    /**
     * Runs the consensus protocol until {@link #shutdown} flag is set to true.
     * Consists of several steps:
     * 1. Enters a new iteration.
     * 2. Create and broadcast a proposal if the local player is the leader.
     * 3. Listens for, validates, and processes proposals and votes.
     * 4. Outputs accepted transactions to the client.
     *
     * @throws InterruptedException
     *             if the current thread is interrupted while waiting for the
     *             iteration countdown latch
     * @throws GeneralSecurityException
     *             if a security exception occurs during cryptographic operations
     * @throws JsonProcessingException
     *             if a JSON processing exception occurs when serializing objects
     */
    public void runProtocol() throws InterruptedException, GeneralSecurityException, JsonProcessingException {
        while (!shutdown) {
            enterIteration();
            proposeNewBlock();
            currentIteration.getCountDownLatch().await();
        }
    }
}
