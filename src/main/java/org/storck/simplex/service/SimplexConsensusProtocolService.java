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
import java.util.function.Function;

import static org.storck.simplex.util.SimplexConsensusProtocolConstants.DUMMY_BLOCK_HASH;
import static org.storck.simplex.util.SimplexConsensusProtocolConstants.electLeader;
import static org.storck.simplex.util.SimplexConsensusProtocolConstants.genesisBlock;

public class SimplexConsensusProtocolService<T> {

    private final String localPlayerId;

    private final PeerNetworkClient peerNetworkClient;

    private final KeyPair keyPair;

    private final Map<String, PublicKey> playerIdsToPublicKeys;

    private final int deltaSeconds;

    private final NotarizedBlockchain<T> notarizedBlockchain;

    private final ProposalValidator<T> proposalValidator;

    private final Timer iterationTimer;

    private final Map<String, VoteRegistryEntry<T>> voteRegistry;

    private int currentIteration;

    private String leaderId;

    private CountDownLatch iterationCountdownLatch;

    @Setter
    private boolean shutdown = false;

    public SimplexConsensusProtocolService(PeerNetworkClient peerNetworkClient, KeyPair keyPair, Map<String, PublicKey> playerIdsToPublicKeys, int deltaSeconds) {
        this.peerNetworkClient = peerNetworkClient;
        this.keyPair = keyPair;
        this.localPlayerId = UUID.randomUUID().toString();
        this.playerIdsToPublicKeys = playerIdsToPublicKeys;
        this.deltaSeconds = deltaSeconds;
        this.notarizedBlockchain = new NotarizedBlockchain<>(new ArrayList<>());
        this.currentIteration = 0;
        this.proposalValidator = new ProposalValidator<>(localPlayerId, keyPair, playerIdsToPublicKeys);
        notarizedBlockchain.blocks().add(new NotarizedBlock<>(genesisBlock(), Set.of()));
        this.iterationTimer = new Timer();
        this.voteRegistry = new HashMap<>();
    }

    void synchronizeIterationNumber(NotarizedBlockchain<T> notarizedBlockchain) {
        int chainLength = notarizedBlockchain.blocks().size();
        if (chainLength > currentIteration) {
            currentIteration = chainLength + 1;
        }
    }

    void enterIterationStep() {
        this.currentIteration++;
        this.leaderId = electLeader(currentIteration, new ArrayList<>(playerIdsToPublicKeys.keySet()));
    }

    void backupStep() {
        this.iterationTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                try {
                    Vote vote = new Vote(localPlayerId, currentIteration, DUMMY_BLOCK_HASH);
                    byte[] voteBytes = CryptoUtil.voteToBytes(vote);
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
            byte[] proposalBytes = CryptoUtil.proposalToBytes(proposal);
            byte[] proposalSignature = CryptoUtil.generateSignature(keyPair.getPrivate(), proposalBytes);
            SignedProposal<T> signedProposal = new SignedProposal<>(proposal, proposalSignature);
            peerNetworkClient.broadcastProposal(signedProposal);
        }
    }

    public void processProposal(SignedProposal<T> signedProposal) throws JsonProcessingException, GeneralSecurityException {
        synchronizeIterationNumber(signedProposal.proposal().parentChain());
        SignedVote signedVote = proposalValidator.validateProposal(currentIteration, notarizedBlockchain, signedProposal, voteRegistry);
        peerNetworkClient.broadcastVote(signedVote);
    }

    public void processVote(SignedVote signedVote) throws GeneralSecurityException, JsonProcessingException {
        if (!validateVote(signedVote)) {
            return;
        }

        Vote vote = signedVote.vote();
        String proposalIdentifier = getProposalIdentifier.apply(vote);

        // Keep track of the votes for this proposal
        VoteRegistryEntry<T> voteRegistryEntry = voteRegistry.get(proposalIdentifier);
        voteRegistryEntry.votes().add(vote);

        // Check if a quorum has been reached for this proposal
        if (hasQuorum(proposalIdentifier)) {
            // Create a notarized block with the proposal and the quorum votes
            NotarizedBlock<T> notarizedBlock = createNotarizedBlock(voteRegistryEntry.block(), voteRegistryEntry.votes());

            // Cancel the iteration timer (if running)
            iterationTimer.cancel();

            // Finalize the iteration
            finalizeIteration(notarizedBlock);

            // Output transactions to the client
            outputTransactions(notarizedBlock);

            // Reset the CountDownLatch to signal the end of the current iteration
            iterationCountdownLatch.countDown();
        }
    }

    private boolean validateVote(SignedVote signedVote) throws JsonProcessingException, GeneralSecurityException {
        Vote vote = signedVote.vote();
        String playerId = vote.playerId();
        if (!playerIdsToPublicKeys.containsKey(playerId)) {
            // Vote is from an unknown player
            return false;
        }

        if (vote.iteration() != currentIteration) {
            // Vote is for a different iteration
            return false;
        }

        if (!voteRegistry.containsKey(getProposalIdentifier.apply(vote))) {
            // Vote is for an unknown proposal
            return false;
        }

        PublicKey playerPublicKey = playerIdsToPublicKeys.get(playerId);
        byte[] input = CryptoUtil.voteToBytes(vote);
        return CryptoUtil.verifySignature(playerPublicKey, input, signedVote.signature());
    }

    private static final Function<Vote, String> getProposalIdentifier = (vote) -> vote.iteration() + ":" + vote.blockHash();

    private boolean hasQuorum(String proposalIdentifier) {
        Set<Vote> votes = voteRegistry.get(proposalIdentifier).votes();
        if (votes == null) {
            return false;
        }

        int numPlayers = playerIdsToPublicKeys.size();
        int quorumSize = (int) Math.ceil(numPlayers * 2 / 3.0);
        return votes.size() >= quorumSize;
    }

    private NotarizedBlock<T> createNotarizedBlock(Block<T> block, Set<Vote> quorumVotes) {
        // Create a NotarizedBlock object with the block and the quorum votes
        return new NotarizedBlock<>(block, quorumVotes);
    }

    private void finalizeIteration(NotarizedBlock<T> notarizedBlock) {
        // Add the notarized block to the blockchain
        notarizedBlockchain.blocks().add(notarizedBlock);
    }

    private void outputTransactions(NotarizedBlock<T> notarizedBlock) {
        // Output the transactions in the notarized block to the client
        for (T transaction : notarizedBlock.block().transactions()) {
            // Output or process the transaction
            System.out.println(transaction);
        }
    }

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
