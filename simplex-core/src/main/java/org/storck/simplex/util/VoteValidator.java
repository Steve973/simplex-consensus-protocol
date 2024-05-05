package org.storck.simplex.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.experimental.UtilityClass;
import org.storck.simplex.model.SignedVote;
import org.storck.simplex.model.Vote;

import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.Map;

@UtilityClass
public class VoteValidator {

    /**
     * Validates a vote. Checks that it comes from a known player, that it pertains
     * to the current iteration, that the proposal is known, and that the signature
     * is valid, indicating that it really came from the player that the player ID
     * indicates.
     *
     * @param currentIteration
     *            the current iteration number
     * @param proposalId
     *            the id of the proposal for this iteration
     * @param signedVote
     *            the signed vote to validate
     * @param playerIdsToPublicKeys
     *            a map keyed by player IDs and the value of each entry is the
     *            public key of that player
     * @return true if the vote is valid, false otherwise
     * @throws JsonProcessingException
     *             if an error occurs during JSON serialization
     * @throws GeneralSecurityException
     *             if there is a security exception during signature verification
     */
    public static boolean validateVote(int currentIteration, String proposalId, SignedVote signedVote, Map<String, PublicKey> playerIdsToPublicKeys)
            throws JsonProcessingException, GeneralSecurityException {
        Vote vote = signedVote.vote();
        String playerId = vote.playerId();
        if (vote.iteration() != currentIteration) {
            // Vote is for a different iteration
            return false;
        }
        if (!proposalId.equals(vote.blockHash())) {
            // Vote is for an unknown proposal
            return false;
        }
        if (!playerIdsToPublicKeys.containsKey(playerId)) {
            // Vote is from an unknown player
            return false;
        }
        PublicKey playerPublicKey = playerIdsToPublicKeys.get(playerId);
        byte[] input = SimplexConstants.voteToBytes(vote);
        return CryptoUtil.verifySignature(playerPublicKey, input, signedVote.signature());
    }
}
