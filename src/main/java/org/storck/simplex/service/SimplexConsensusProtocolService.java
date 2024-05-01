package org.storck.simplex.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Setter;
import org.storck.simplex.api.PeerNetworkClient;
import org.storck.simplex.model.Block;
import org.storck.simplex.model.NotarizedBlock;
import org.storck.simplex.model.NotarizedBlockchain;
import org.storck.simplex.model.Proposal;
import org.storck.simplex.model.SignedProposal;
import org.storck.simplex.model.SignedVote;
import org.storck.simplex.model.Vote;
import org.storck.simplex.model.VoteRegistryEntry;
import org.storck.simplex.util.CryptoUtil;
import org.storck.simplex.util.SimplexConstants;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static org.storck.simplex.util.SimplexConstants.DUMMY_BLOCK_HASH;
import static org.storck.simplex.util.SimplexConstants.createNotarizedBlock;
import static org.storck.simplex.util.SimplexConstants.electLeader;
import static org.storck.simplex.util.SimplexConstants.finalizeIteration;
import static org.storck.simplex.util.SimplexConstants.genesisBlock;

/**
 * This service implements the steps of the Simplex consensus protocol -- backup, leader election, proposal submission,
 * validating and responding to proposals and votes, block notarization, and the output of accepted transactions to the
 * client.
 *
 * @param <T> the type of the transactions stored in the blocks
 */
public class SimplexConsensusProtocolService<T> {

    /**
     * The ID of the local player (i.e., the ID for this local node).
     */
    private final String localPlayerId;

    /**
     * The network client that handles peer discovery, as well as sending and receiving messages between peers.
     */
    private final PeerNetworkClient peerNetworkClient;

    /**
     * The key pair used for cryptographic operations, including signing and verifying proposals and votes.
     */
    private final KeyPair keyPair;

    /**
     * Map that stores the association between player IDs and their corresponding public keys.
     */
    private final Map<String, PublicKey> playerIdsToPublicKeys;

    /**
     * Represents the time duration in seconds for network operations like sending messages and responding.
     */
    private final int deltaSeconds;

    /**
     * Contains all notarized (accepted) blocks.
     */
    private final NotarizedBlockchain<T> notarizedBlockchain;

    /**
     * Service that handles proposal validation and processing.
     */
    private final ProposalValidator<T> proposalValidator;

    /**
     * Service that handles vote validation and processing.
     */
    private final VoteService<T> voteValidator;

    /**
     * Timer that limits the duration of an iteration.
     */
    private final Timer iterationTimer;

    /**
     * Registry that keeps track of proposal/block votes.
     */
    private final Map<String, VoteRegistryEntry<T>> voteRegistry;

    /**
     * The number of the current iteration.
     */
    private int currentIteration;

    /**
     * The ID of the current iteration leader.
     */
    private String leaderId;

    /**
     * Latch to wait until the end of the iteration so that proposals and votes can be received and processed.
     */
    private CountDownLatch iterationCountdownLatch;

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
        this.currentIteration = 0;
        this.voteRegistry = new HashMap<>();
        this.proposalValidator = new ProposalValidator<>(localPlayerId, keyPair, playerIdsToPublicKeys);
        this.voteValidator = new VoteService<>(playerIdsToPublicKeys, voteRegistry);
        notarizedBlockchain.blocks().add(new NotarizedBlock<>(genesisBlock(), Set.of()));
        this.iterationTimer = new Timer();
    }

    /**
     * Synchronizes the iteration number based on the size of the notarized blockchain that was received.
     *
     * @param notarizedBlockchain the notarized blockchain to synchronize the iteration number with
     */
    void synchronizeIterationNumber(NotarizedBlockchain<T> notarizedBlockchain) {
        int chainLength = notarizedBlockchain.blocks().size();
        if (chainLength > currentIteration) {
            currentIteration = chainLength + 1;
        }
    }

    /**
     * Updates the current iteration number and elects the leader for the iteration.
     */
    void enterIterationStep() {
        this.currentIteration++;
        this.leaderId = electLeader(currentIteration, new ArrayList<>(playerIdsToPublicKeys.keySet()));
    }

    /**
     * Performs the backup step in the Simplex consensus protocol.
     * This step creates an iteration timer to limit the duration of the iteration. If the iteration is not finalized before
     * the timer delay completes, the player broadcasts a vote for the dummy block, and releases the
     * {@link #iterationCountdownLatch} so that the next iteration can begin.
     */
    void backupStep() {
        this.iterationTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                try {
                    Vote vote = new Vote(localPlayerId, currentIteration, DUMMY_BLOCK_HASH);
                    byte[] voteBytes = SimplexConstants.voteToBytes(vote);
                    byte[] voteSignature = CryptoUtil.generateSignature(keyPair.getPrivate(), voteBytes);
                    SignedVote signedVote = new SignedVote(vote, voteSignature);
                    peerNetworkClient.broadcastVote(signedVote);
                } catch (Exception e) {
                    throw new IllegalStateException("Error when creating/sending signed vote", e);
                } finally {
                    iterationCountdownLatch.countDown();
                }
            }
        }, 3000L * deltaSeconds);
    }

    /**
     * If the local player is the leader, it retrieves pending transactions, creates a proposal, and broadcasts it to peers.
     *
     * @throws JsonProcessingException   if an error occurs during JSON serialization
     * @throws GeneralSecurityException if there is a security exception during cryptographic operations
     */
    void leaderProposalStep() throws JsonProcessingException, GeneralSecurityException {
        if (localPlayerId.equals(leaderId)) {
            Collection<T> transactions = peerNetworkClient.getPendingTransactions();
            int currentBlockchainSize = notarizedBlockchain.blocks().size();
            String parentHash = Optional.of(notarizedBlockchain.blocks())
                    .map(blocks -> blocks.get(currentBlockchainSize - 1))
                    .map(NotarizedBlock::block)
                    .map(CryptoUtil::computeHash)
                    .orElseThrow(() -> new IllegalArgumentException("Could not get parent block hash for proposal"));
            Proposal<T> proposal = new Proposal<>(currentIteration, localPlayerId, new Block<>(currentBlockchainSize + 1, parentHash, new ArrayList<>(transactions)),
                    notarizedBlockchain);
            byte[] proposalBytes = SimplexConstants.proposalToBytes(proposal);
            byte[] proposalSignature = CryptoUtil.generateSignature(keyPair.getPrivate(), proposalBytes);
            SignedProposal<T> signedProposal = new SignedProposal<>(proposal, proposalSignature);
            peerNetworkClient.broadcastProposal(signedProposal);
        }
    }

    /**
     * Processes a proposal by validating it and broadcasting the resulting vote.
     * This method is called by the {@link #peerNetworkClient} after receiving a signed proposal from another player.
     *
     * @param signedProposal the signed proposal to process
     * @throws GeneralSecurityException if there is a security exception during signature verification
     * @throws JsonProcessingException if an error occurs during JSON serialization
     */
    public void processProposal(SignedProposal<T> signedProposal) throws JsonProcessingException, GeneralSecurityException {
        synchronizeIterationNumber(signedProposal.proposal().parentChain());
        Optional.ofNullable(proposalValidator.validateProposal(currentIteration, notarizedBlockchain, signedProposal, voteRegistry))
                .ifPresent(peerNetworkClient::broadcastVote);
    }

    /**
     * Processes a vote through the {@link #voteValidator}. If a quorum is reached after processing this vote, it cancels
     * the iteration timer, finalizes the iteration, and outputs the transactions to the client.  The countdown latch that
     * prevents the start of the next iteration is released, so the next iteration will start immediately after this method.
     * <p>
     * This method is called by the {@link #peerNetworkClient} after it receives the vote from another player.
     *
     * @param signedVote the signed vote to process
     * @throws GeneralSecurityException if there is a security exception during signature verification
     * @throws JsonProcessingException if an error occurs during JSON serialization
     */
    public void processVote(SignedVote signedVote) throws GeneralSecurityException, JsonProcessingException {
        // Process vote and check if a quorum has been reached for this proposal
        if (voteValidator.processVote(currentIteration, signedVote)) {
            // Create a notarized block with the proposal and the quorum votes
            VoteRegistryEntry<T> voteRegistryEntry = voteRegistry.get(VoteService.getProposalIdentifier.apply(signedVote.vote()));
            NotarizedBlock<T> notarizedBlock = createNotarizedBlock(voteRegistryEntry.block(), voteRegistryEntry.votes());

            // Cancel the iteration timer (if running)
            iterationTimer.cancel();

            // Finalize the iteration
            finalizeIteration(notarizedBlock, notarizedBlockchain);

            // Output transactions to the client
            outputTransactions(notarizedBlock);

            // Reset the CountDownLatch to signal the end of the current iteration
            iterationCountdownLatch.countDown();
        }
    }

    /**
     * Outputs transactions from a notarized and finalized block to the client.
     *
     * @param notarizedBlock a block that has been notarized and finalized that is ready to send to the client
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
     * 1. Updates the current iteration and elects the leader for the iteration.
     * 2. Executes the backup step, which involves creating a vote and broadcasting it to peers.
     * 3. Executes the leader proposal step, which involves creating a proposal (if the local player is the leader) and broadcasting it to peers.
     * 4. Listens for, validates, and processes proposals (if another player is the leader) and votes from other players.
     * 5. Outputs accepted transactions to the client.
     *
     * @throws InterruptedException       if the current thread is interrupted while waiting for the iteration countdown latch
     * @throws GeneralSecurityException  if a security exception occurs during cryptographic operations
     * @throws JsonProcessingException   if a JSON processing exception occurs when serializing objects
     */
    public void runConsensusProtocol() throws InterruptedException, GeneralSecurityException, JsonProcessingException {
        while (!shutdown) {
            iterationCountdownLatch = new CountDownLatch(1);
            enterIterationStep();
            backupStep();
            leaderProposalStep();
            iterationCountdownLatch.await();
        }
    }
}
