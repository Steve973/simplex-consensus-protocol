package org.storck.simplex.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.SneakyThrows;
import org.storck.simplex.model.Proposal;
import org.storck.simplex.model.SignedVote;
import org.storck.simplex.model.Vote;
import org.storck.simplex.util.MessageUtils;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Handles the voting process, including validation, tallying votes, and
 * notarizing blocks.
 */
public class VotingService<T> {

    /**
     * Holds any votes that are received for the proposed block during this
     * iteration.
     */
    private final List<Vote> votes;

    /**
     * Service that handles digital signature operations.
     */
    private final DigitalSignatureService signatureService;

    /**
     * Service that manages players and their public keys.
     */
    private final PlayerService playerService;

    /**
     * The current iteration number for "this" round of voting.
     */
    private int iterationNumber;

    /**
     * The ID (hash) of the current proposal for "this" round of voting.
     */
    private String proposalId;

    /**
     * Create the service.
     *
     * @param signatureService the service that handles digital signature operations
     * @param playerService the service that manages players and their public keys
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The player service state is meant to change as players arrive and depart.")
    public VotingService(final DigitalSignatureService signatureService, final PlayerService playerService) {
        this.votes = new ArrayList<>();
        this.signatureService = signatureService;
        this.playerService = playerService;
    }

    /**
     * Initializes this service for processing votes in a new iteration.
     *
     * @param iterationNumber the iteration number
     * @param proposal the proposal for this iteration
     */
    public void initializeForIteration(final int iterationNumber, final Proposal<T> proposal) {
        this.iterationNumber = iterationNumber;
        this.proposalId = signatureService.computeBlockHash(proposal.newBlock());
    }

    /**
     * Checks if the given vote pertains to the current iteration.
     *
     * @param vote the vote to check
     * @param currentIteration the current iteration number
     *
     * @return true if the vote pertains to the current iteration, false otherwise
     */
    static boolean isVoteIterationCurrentIteration(final Vote vote, final int currentIteration) {
        return vote.iteration() == currentIteration;
    }

    /**
     * Checks if the given vote pertains to the specified proposal ID.
     *
     * @param vote the vote to check
     * @param proposalId the ID of the proposal to compare against
     *
     * @return true if the vote pertains to the proposal ID, false otherwise
     */
    static boolean isVoteIdProposalId(final Vote vote, final String proposalId) {
        return proposalId.equals(vote.blockHash());
    }

    /**
     * Checks if the given vote is from a known player.
     *
     * @param playerId the ID of the player that cast the vote
     * @param playerService service that manages players and their public keys
     *
     * @return true if the vote is from a known player, false otherwise
     */
    static boolean isVoteFromKnownPlayer(final String playerId, final PlayerService playerService) {
        return playerService.getPlayerIds().contains(playerId);
    }

    /**
     * Checks if the given vote has a valid signature.
     *
     * @param playerPublicKey the public key of the player
     * @param signedVote the signed vote to validate
     *
     * @return true if the signature is valid, false otherwise
     */
    @SneakyThrows
    static boolean isVoteSignatureValid(final PublicKey playerPublicKey, final SignedVote signedVote, final DigitalSignatureService signatureService) {
        return signatureService.verifySignature(MessageUtils.toBytes(signedVote.vote()), signedVote.signature(), playerPublicKey);
    }

    /**
     * Validates a vote. Checks that it comes from a known player, that it pertains
     * to the current iteration, that the proposal is known, and that the signature
     * is valid, indicating that it really came from the player that the player ID
     * indicates.
     *
     * @param currentIteration the current iteration number
     * @param proposalId the id of the proposal for this iteration
     * @param signedVote the signed vote to validate
     * @param playerService service that manages players and their public keys
     *
     * @return true if the vote is valid, false otherwise
     */
    public boolean validateVote(final int currentIteration, final String proposalId, final SignedVote signedVote, final PlayerService playerService,
            final DigitalSignatureService signatureService) {
        Vote vote = signedVote.vote();
        List<Predicate<Vote>> voteChecks = List.of(
                v -> isVoteIterationCurrentIteration(v, currentIteration),
                v -> isVoteIdProposalId(v, proposalId),
                v -> isVoteFromKnownPlayer(v.playerId(), playerService),
                v -> isVoteSignatureValid(playerService.getPublicKey(vote.playerId()), signedVote, signatureService));
        return voteChecks.stream().allMatch(check -> check.test(vote));
    }

    /**
     * Processes a vote by validating it and adding it to the vote registry. If the
     * proposal has a quorum of votes, this method will return true. Otherwise,
     * false is returned.
     *
     * @param signedVote the signed vote to process
     *
     * @return true if the vote is valid and the proposal has a quorum of votes,
     *     false otherwise
     */
    public boolean processVote(final SignedVote signedVote) {
        boolean result = false;
        if (validateVote(iterationNumber, proposalId, signedVote, playerService, signatureService)) {
            Vote vote = signedVote.vote();
            votes.add(vote);
            int quorumSize = (int) Math.ceil(playerService.getPlayerIds().size() * 2 / 3.0);
            if (votes.size() >= quorumSize) {
                result = true;
            }
        }
        return result;
    }

    /**
     * Create a vote for the proposal of this iteration.
     *
     * @return a signed vote for the proposal
     */
    public SignedVote createProposalVote(final String localPlayerId) {
        Vote vote = new Vote(localPlayerId, iterationNumber, proposalId);
        byte[] voteBytes = MessageUtils.toBytes(vote);
        byte[] voteSignature = signatureService.generateSignature(voteBytes);
        return new SignedVote(vote, voteSignature);
    }
}
