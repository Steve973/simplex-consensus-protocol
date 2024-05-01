package org.storck.simplex.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.storck.simplex.model.SignedVote;
import org.storck.simplex.model.Vote;
import org.storck.simplex.model.VoteRegistryEntry;
import org.storck.simplex.util.CryptoUtil;
import org.storck.simplex.util.SimplexConstants;

import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Provides methods for validating and processing votes.
 *
 * @param <T> the type of the transactions stored in the block
 */
public class VoteService<T> {

    /**
     * Returns the proposal identifier for a given vote.
     * Accepts the vote for which to get the proposal identifier, and returns it.
     */
    public static final Function<Vote, String> getProposalIdentifier = (vote) -> vote.iteration() + ":" + vote.blockHash();

    /**
     * For all players that participate in the Simplex Consensus protocol, an entry consists of the player ID to its public key.
     */
    private final Map<String, PublicKey> playerIdsToPublicKeys;

    /**
     * The vote registry consists of entries where the key is an ID provided by {@link #getProposalIdentifier} and the value is a
     * {@link VoteRegistryEntry}.
     */
    private final Map<String, VoteRegistryEntry<T>> voteRegistry;

    /**
     * Create an instance for validating and processing votes.
     *
     * @param playerIdsToPublicKeys a {@link Map} containing player IDs as keys and corresponding public keys as values
     * @param voteRegistry a {@link Map} containing vote registry entries, where the key is an ID provided by {@link #getProposalIdentifier} and the value is a {@link VoteRegistry
     * Entry}
     */
    public VoteService(Map<String, PublicKey> playerIdsToPublicKeys, Map<String, VoteRegistryEntry<T>> voteRegistry) {
        this.playerIdsToPublicKeys = playerIdsToPublicKeys;
        this.voteRegistry = voteRegistry;
    }

    /**
     * Validates a vote. Checks that it comes from a known player, that it pertains to the current iteration, that the proposal is known,
     * and that the signature is valid, indicating that it really came from the player that the player ID indicates.
     *
     * @param currentIteration the current iteration number
     * @param signedVote the signed vote to validate
     * @return true if the vote is valid, false otherwise
     * @throws JsonProcessingException if an error occurs during JSON serialization
     * @throws GeneralSecurityException if there is a security exception during signature verification
     */
    public boolean validateVote(int currentIteration, SignedVote signedVote) throws JsonProcessingException, GeneralSecurityException {
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
        byte[] input = SimplexConstants.voteToBytes(vote);
        return CryptoUtil.verifySignature(playerPublicKey, input, signedVote.signature());
    }

    /**
     * Checks if a proposal has enough votes to indicate successful consensus.
     *
     * @param proposalIdentifier the identifier of the proposal
     * @return true if the proposal has a quorum of votes, false otherwise
     */
    public boolean hasQuorum(String proposalIdentifier) {
        Set<Vote> votes = voteRegistry.get(proposalIdentifier).votes();
        if (votes == null) {
            return false;
        }

        int numPlayers = playerIdsToPublicKeys.size();
        int quorumSize = (int) Math.ceil(numPlayers * 2 / 3.0);
        return votes.size() >= quorumSize;
    }

    /**
     * Processes a vote by validating it and adding it to the vote registry.
     * If the proposal has a quorum of votes, this method will return true.  Otherwise, false is returned.
     *
     * @param currentIteration the current iteration number
     * @param signedVote the signed vote to process
     * @return true if the vote is valid and the proposal has a quorum of votes, false otherwise
     * @throws GeneralSecurityException if there is a security exception during signature verification
     * @throws JsonProcessingException if an error occurs during JSON serialization
     */
    public boolean processVote(int currentIteration, SignedVote signedVote) throws GeneralSecurityException, JsonProcessingException {
        if (!validateVote(currentIteration, signedVote)) {
            return false;
        }

        Vote vote = signedVote.vote();
        String proposalIdentifier = VoteService.getProposalIdentifier.apply(vote);

        // Keep track of the votes for this proposal
        VoteRegistryEntry<T> voteRegistryEntry = voteRegistry.get(proposalIdentifier);
        voteRegistryEntry.votes().add(vote);
        return hasQuorum(proposalIdentifier);
    }
}
