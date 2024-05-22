package org.storck.simplex.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import org.storck.simplex.api.network.PeerNetworkClient;
import org.storck.simplex.api.protocol.VoteProtocolMessage;
import org.storck.simplex.model.Vote;
import org.storck.simplex.model.VoteSigned;
import org.storck.simplex.util.MessageUtils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Handles the lifecycle of an iteration, including backup steps, timers, leader
 * proposal, voting, and transitioning to the next iteration.
 */
@SuppressWarnings("PMD.DoNotUseThreads") // Need a timer to handle the iteration timeout
public class IterationService {

    /**
     * The player ID for this (local) node.
     */
    private final String localPlayerId;

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
     * Keeps track of the IDs of players that have sent finalizeMsg messages.
     */
    private final Set<String> finalizeReceipts = new HashSet<>();

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
     * The latch to wait for the iteration to complete.
     */
    private CountDownLatch countDownLatch;

    /**
     * The timer to fire when the duration of this iteration expires.
     */
    private ScheduledExecutorService scheduledExecutorService;

    /**
     * The timer task that is responsible for ending the iteration when the allotted
     * duration elapses.
     */
    private TimerTask timerTask;

    /**
     * Create a service instance that will need to be further initialized by calling
     * {@link #initializeForIteration(int, CountDownLatch)} with the iteration
     * number.
     *
     * @param localPlayerId the player/peer ID of this (local) node
     * @param playerService the service that manages players/peers and their public
     *     keys
     * @param digitalSignatureService the service that performs digital signature
     *     operations
     * @param peerNetworkClient the service that provides network interoperability
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The player service state is meant to change as players arrive and depart.")
    public IterationService(final String localPlayerId, final PlayerService playerService, final DigitalSignatureService digitalSignatureService,
            final PeerNetworkClient peerNetworkClient) {
        this.iterationNumber = 0;
        this.localPlayerId = localPlayerId;
        this.playerService = playerService;
        this.digitalSignatureService = digitalSignatureService;
        this.peerNetworkClient = peerNetworkClient;
    }

    /**
     * Initializes the service for a new iteration by setting the iteration number,
     * electing an iteration leader, and creating a timer for the maximum iteration
     * duration.
     *
     * @param iterationNumber the iteration number
     * @param countDownLatch the latch to wait for the completion of the iteration
     */
    public void initializeForIteration(final int iterationNumber, final CountDownLatch countDownLatch) {
        if (iterationNumber <= this.iterationNumber) {
            throw new IllegalArgumentException("Iteration number must only increase");
        }
        this.iterationNumber = iterationNumber;
        this.leaderId = electLeader(iterationNumber);
        this.countDownLatch = countDownLatch;
        this.scheduledExecutorService = Executors.newScheduledThreadPool(1);
        this.timerTask = new TimerTask() {

            @Override
            public void run() {
                try {
                    Vote vote = new Vote(localPlayerId, iterationNumber, "");
                    byte[] voteBytes = MessageUtils.toBytes(vote);
                    byte[] voteSignature = digitalSignatureService.generateSignature(voteBytes);
                    VoteSigned signedVote = new VoteSigned(vote, voteSignature);
                    peerNetworkClient.broadcastVote(new VoteProtocolMessage(MessageUtils.toBytes(signedVote)));
                } finally {
                    countDownLatch.countDown();
                }
            }
        };
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
        String hash = digitalSignatureService.computeBytesHash(Long.toString(iteration).getBytes(StandardCharsets.UTF_8));
        BigInteger hashInt = new BigInteger(1, hash.getBytes(StandardCharsets.UTF_8));
        return participantIds.get(hashInt.mod(BigInteger.valueOf(participantIds.size())).intValue());
    }

    /**
     * Start the iteration with a timer task scheduled to fire when the iteration
     * times out.
     */
    public void startIteration() {
        this.scheduledExecutorService.schedule(this.timerTask, 3L * peerNetworkClient.getNetworkDeltaSeconds(), TimeUnit.SECONDS);
    }

    /**
     * If a player sends a finalizeMsg message for the iteration, we capture the
     * player ID as a record of receiving that
     * message.
     *
     * @param playerId the ID of the player that sent a finalizeMsg message
     */
    public void logFinalizeReceipt(final String playerId) {
        finalizeReceipts.add(playerId);
    }

    /**
     * Get the IDs of the players that have sent finalizeMsg messages for this
     * iteration.
     *
     * @return IDs of players that have sent finalizeMsg messages
     */
    public Collection<String> getFinalizeReceipts() {
        return List.copyOf(finalizeReceipts);
    }

    /**
     * Stops the iteration and its timer, since certain events need to end the
     * iteration immediately.
     */
    public void stopIteration() {
        scheduledExecutorService.shutdownNow();
        this.countDownLatch.countDown();
    }

    /**
     * Await the completion of this iteration.
     *
     * @throws InterruptedException if interrupted while waiting for completion
     */
    public void awaitCompletion() throws InterruptedException {
        this.countDownLatch.await();
    }

    /**
     * If the timer is shut down, this returns true, or false otherwise.
     *
     * @return true if the timer is shut down, or false otherwise
     */
    public boolean isShutdown() {
        return scheduledExecutorService.isShutdown();
    }
}
