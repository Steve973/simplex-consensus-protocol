package org.storck.simplex.service;

import com.google.common.base.Charsets;
import lombok.Getter;
import org.storck.simplex.model.SignedVote;
import org.storck.simplex.model.Vote;
import org.storck.simplex.networking.api.network.PeerNetworkClient;
import org.storck.simplex.networking.api.protocol.VoteProtocolMessage;
import org.storck.simplex.util.MessageUtils;

import java.math.BigInteger;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

/**
 * Handles the lifecycle of an iteration, including backup steps, timers, leader
 * proposal, voting, and transitioning to the next iteration.
 */
public class IterationService {

    /**
     * Name prefix for an iteration timer. The iteration number should be appended
     * to this name.
     */
    private static final String TIMER_NAME_PREFIX = "TimerForIteration_";

    /**
     * The player ID for this (local) node.
     */
    private final String localPlayerId;

    /**
     * The latch to wait for the iteration to complete.
     */
    private final CountDownLatch countDownLatch;

    /**
     * The service that manages players participating in the protocol.
     */
    private final PlayerService playerService;

    /**
     * Service that handles the digital signing of objects/messages.
     */
    private final DigitalSignatureService digitalSignatureService;

    /**
     * Service that manages communication/messaging between players/peers.
     */
    private final PeerNetworkClient peerNetworkClient;

    /**
     * The iteration number to which this iteration instance pertains.
     */
    @Getter
    private int iterationNumber;

    /**
     * The ID of the player who is the leader for this iteration.
     */
    @Getter
    private String leaderId;

    /**
     * The timer to fire when the duration of this iteration expires.
     */
    private Timer timer;

    /**
     * Create a service instance that will need to be further initialized by calling
     * {@link #initializeForIteration(int)} with the iteration number.
     *
     * @param localPlayerId the player/peer ID of this (local) node
     * @param playerService the service that manages players/peers and their public
     *     keys
     * @param digitalSignatureService the service that performs digital signature
     *     operations
     * @param peerNetworkClient the service that provides network interoperability
     */
    public IterationService(final String localPlayerId, final PlayerService playerService, final DigitalSignatureService digitalSignatureService,
            final PeerNetworkClient peerNetworkClient) {
        this.iterationNumber = 0;
        this.localPlayerId = localPlayerId;
        this.playerService = playerService;
        this.digitalSignatureService = digitalSignatureService;
        this.peerNetworkClient = peerNetworkClient;
        this.countDownLatch = new CountDownLatch(1);
    }

    /**
     * Initializes the service for a new iteration by setting the iteration number,
     * electing an iteration leader, and creating a timer for the maximum iteration
     * duration.
     *
     * @param iterationNumber the iteration number
     */
    public void initializeForIteration(final int iterationNumber) {
        this.iterationNumber = iterationNumber;
        this.leaderId = electLeader(iterationNumber);
        this.timer = new Timer(TIMER_NAME_PREFIX + iterationNumber);
    }

    /**
     * For each iteration, its leader is chosen by hashing the iteration number h
     * using some public hash function. In other words, L = H * h % n, where H is a
     * predetermined hash function.
     *
     * @param iteration the iteration number used to calculate the leader
     */
    String electLeader(final long iteration) {
        List<String> participantIds = playerService.getPlayerIds();
        String hash = digitalSignatureService.computeBytesHash(Long.toString(iteration).getBytes(Charsets.UTF_8));
        BigInteger hashInt = new BigInteger(1, hash.getBytes(Charsets.UTF_8));
        return participantIds.get(hashInt.mod(BigInteger.valueOf(participantIds.size())).intValue());
    }

    /**
     * Start the iteration with a timer task scheduled to fire when the iteration
     * times out.
     */
    public void startIteration() {
        this.timer.schedule(new TimerTask() {

            @Override
            public void run() {
                try {
                    Vote vote = new Vote(localPlayerId, iterationNumber, "");
                    byte[] voteBytes = MessageUtils.toBytes(vote);
                    byte[] voteSignature = digitalSignatureService.generateSignature(voteBytes);
                    SignedVote signedVote = new SignedVote(vote, voteSignature);
                    peerNetworkClient.broadcastVote(new VoteProtocolMessage(MessageUtils.toBytes(signedVote)));
                } finally {
                    countDownLatch.countDown();
                }
            }
        }, 3L * peerNetworkClient.getNetworkDeltaSeconds());
    }

    /**
     * Stops the iteration and its timer, since certain events need to end the
     * iteration immediately.
     */
    public void stopIteration() {
        timer.cancel();
        this.countDownLatch.countDown();
    }

    /**
     * Await the completion of this iteration.
     */
    public void awaitCompletion() {
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new IllegalStateException("Unexpected error while waiting for iteration completion", e);
        }
    }
}
