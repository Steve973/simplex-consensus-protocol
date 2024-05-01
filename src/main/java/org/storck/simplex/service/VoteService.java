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

public class VoteService<T> {

    public static final Function<Vote, String> getProposalIdentifier = (vote) -> vote.iteration() + ":" + vote.blockHash();

    private final Map<String, PublicKey> playerIdsToPublicKeys;

    private final Map<String, VoteRegistryEntry<T>> voteRegistry;

    public VoteService(Map<String, PublicKey> playerIdsToPublicKeys, Map<String, VoteRegistryEntry<T>> voteRegistry) {
        this.playerIdsToPublicKeys = playerIdsToPublicKeys;
        this.voteRegistry = voteRegistry;
    }

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

    public boolean hasQuorum(String proposalIdentifier) {
        Set<Vote> votes = voteRegistry.get(proposalIdentifier).votes();
        if (votes == null) {
            return false;
        }

        int numPlayers = playerIdsToPublicKeys.size();
        int quorumSize = (int) Math.ceil(numPlayers * 2 / 3.0);
        return votes.size() >= quorumSize;
    }

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
