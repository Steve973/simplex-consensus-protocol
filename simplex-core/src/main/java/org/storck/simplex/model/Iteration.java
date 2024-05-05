package org.storck.simplex.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import org.storck.simplex.networking.api.network.PeerNetworkClient;
import org.storck.simplex.networking.api.protocol.VoteProtocolMessage;
import org.storck.simplex.util.VoteValidator;
import org.storck.simplex.util.CryptoUtil;
import org.storck.simplex.util.SimplexConstants;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import static org.storck.simplex.util.SimplexConstants.DUMMY_BLOCK_HASH;
import static org.storck.simplex.util.SimplexConstants.electLeader;
import static org.storck.simplex.util.SimplexConstants.signedVoteToBytes;

@Getter
public class Iteration<T> {

    private static final String TIMER_NAME_PREFIX = "TimerForIteration_";

    private final int iterationNumber;

    private final String localPlayerId;

    private final String leaderId;

    /**
     * Map that stores the association between player IDs and their corresponding
     * public keys.
     */
    private final Map<String, PublicKey> playerIdsToPublicKeys;

    private final int networkDeltaSeconds;

    private final PeerNetworkClient peerNetworkClient;

    private final PrivateKey privateKey;

    private final Timer timer;

    private final CountDownLatch countDownLatch;

    private final List<Vote> votes;

    private Proposal<T> proposal;

    private String proposalId;

    public Iteration(int iterationNumber, String localPlayerId, Map<String, PublicKey> playerIdsToPublicKeys, int networkDeltaSeconds, PeerNetworkClient peerNetworkClient,
            PrivateKey privateKey) {
        this.iterationNumber = iterationNumber;
        this.localPlayerId = localPlayerId;
        this.playerIdsToPublicKeys = playerIdsToPublicKeys;
        this.leaderId = electLeader(iterationNumber, new ArrayList<>(playerIdsToPublicKeys.keySet()));
        ;
        this.networkDeltaSeconds = networkDeltaSeconds;
        this.timer = new Timer(TIMER_NAME_PREFIX + iterationNumber);
        this.peerNetworkClient = peerNetworkClient;
        this.privateKey = privateKey;
        this.countDownLatch = new CountDownLatch(1);
        this.votes = new ArrayList<>();
    }

    public void setProposal(Proposal<T> proposal, String proposalId) {
        this.proposal = proposal;
        this.proposalId = proposalId;
    }

    public void startIteration() {
        this.timer.schedule(new TimerTask() {

            @Override
            public void run() {
                try {
                    Vote vote = new Vote(localPlayerId, iterationNumber, DUMMY_BLOCK_HASH);
                    byte[] voteBytes = SimplexConstants.voteToBytes(vote);
                    byte[] voteSignature = CryptoUtil.generateSignature(privateKey, voteBytes);
                    SignedVote signedVote = new SignedVote(vote, voteSignature);
                    peerNetworkClient.broadcastVote(new VoteProtocolMessage(signedVoteToBytes(signedVote)));
                } catch (Exception e) {
                    throw new IllegalStateException("Error when creating/sending signed vote", e);
                } finally {
                    countDownLatch.countDown();
                }
            }
        }, 3L * networkDeltaSeconds);
    }

    /**
     * Checks if a proposal has enough votes to indicate successful consensus.
     *
     * @return true if the proposal has a quorum of votes, false otherwise
     */
    public boolean hasQuorum() {
        int quorumSize = (int) Math.ceil(playerIdsToPublicKeys.size() * 2 / 3.0);
        return votes.size() >= quorumSize;
    }

    /**
     * Processes a vote by validating it and adding it to the vote registry.
     * If the proposal has a quorum of votes, this method will return true.
     * Otherwise, false is returned.
     *
     * @param signedVote
     *            the signed vote to process
     * @return true if the vote is valid and the proposal has a quorum of votes,
     *         false otherwise
     * @throws GeneralSecurityException
     *             if there is a security exception during signature verification
     * @throws JsonProcessingException
     *             if an error occurs during JSON serialization
     */
    public boolean processVote(SignedVote signedVote) throws GeneralSecurityException, JsonProcessingException {
        if (!VoteValidator.validateVote(iterationNumber, proposalId, signedVote, playerIdsToPublicKeys)) {
            return false;
        }
        Vote vote = signedVote.vote();
        votes.add(vote);
        if (hasQuorum()) {
            timer.cancel();
            return true;
        }
        return false;
    }
}
