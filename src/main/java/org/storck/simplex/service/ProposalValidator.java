package org.storck.simplex.service;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Responsible for validating proposals and producing signed votes.
 *
 * @param <T> the type of the transactions stored in the blocks
 */
public class ProposalValidator<T> {

    /**
     * The ID of the (local) player participating in the protocol.
     */
    private final String playerId;

    /**
     * A KeyPair object that consists of a public key and corresponding private key.
     * It is used for cryptographic operations such as signing and verifying signatures.
     *
     * @see KeyPair
     */
    private final KeyPair keyPair;

    /**
     * Map that associates player IDs with their corresponding public keys.
     * The player ID is a string identifier and the public key is used for verification purposes.
     */
    private final Map<String, PublicKey> playerIdsToPublicKeys;

    /**
     * A set of IDs representing the proposal IDs that have already been seen.
     */
    private final Set<String> processedProposalIds;

    /**
     * Create the instance for validating proposals in a blockchain consensus protocol.
     */
    public ProposalValidator(String playerId, KeyPair keyPair, Map<String, PublicKey> playerIdsToPublicKeys) {
        this.playerId = playerId;
        this.keyPair = keyPair;
        this.playerIdsToPublicKeys = playerIdsToPublicKeys;
        processedProposalIds = new HashSet<>();
    }

    /**
     * Determines if the given proposal is valid.
     *
     * @param <T> the type of the transactions stored in the block
     * @param signedProposal the signed proposal for a new block in a blockchain
     * @param proposalHash the hash value of the proposal
     * @param currentNotarizedChain the current notarized chain
     * @param processedProposalIds the set of processed proposal IDs
     * @param playerIdsToPublicKeys the map of player IDs to public keys
     * @return true if the proposal is valid, false otherwise
     * @throws JsonProcessingException if an error occurs during JSON processing
     * @throws GeneralSecurityException if a security exception occurs during signature verification
     */
    static <T> boolean isValidProposal(SignedProposal<T> signedProposal, String proposalHash, NotarizedBlockchain<T> currentNotarizedChain, Set<String> processedProposalIds,
            Map<String, PublicKey> playerIdsToPublicKeys) throws JsonProcessingException, GeneralSecurityException {
        Proposal<T> proposal = signedProposal.proposal();
        // Check if we have seen this proposal before
        if (processedProposalIds.contains(proposalHash)) {
            return false;
        } else {
            processedProposalIds.add(proposalHash);
        }

        PublicKey proposalPlayerPublicKey = playerIdsToPublicKeys.get(proposal.playerId());
        byte[] proposalBytes = SimplexConstants.proposalToBytes(proposal);
        if (!CryptoUtil.verifySignature(proposalPlayerPublicKey, proposalBytes, signedProposal.signature())) {
            return false;
        }

        // Check if the proposal is for the expected iteration
        int expectedIteration = currentNotarizedChain.blocks().size() + 1;
        if (proposal.iteration() != expectedIteration) {
            return false;
        }

        // Check if the parent chain is the current notarized chain
        if (!proposal.parentChain().equals(currentNotarizedChain)) {
            return false;
        }

        // Check if the new block extends the parent chain correctly
        NotarizedBlock<T> parentBlock = currentNotarizedChain.blocks().get(currentNotarizedChain.blocks().size() - 1);
        Block<T> newBlock = proposal.newBlock();
        return isValidBlockExtension(newBlock, parentBlock);
    }

    /**
     * Determines if the new block is a valid extension of the parent block.
     *
     * @param <T> the type of the transactions stored in the block
     * @param newBlock the new block to be validated
     * @param parentBlock the parent block of the new block
     * @return true if the extension is valid, false otherwise
     */
    static <T> boolean isValidBlockExtension(Block<T> newBlock, NotarizedBlock<T> parentBlock) {
        int expectedHeight = parentBlock.block().height() + 1;
        if (newBlock.height() != expectedHeight) {
            return false;
        }

        String expectedParentHash = CryptoUtil.computeHash(parentBlock.block());
        return newBlock.parentHash().equals(expectedParentHash);
    }

    /**
     * Validates a proposal. If it is valid, it adds the proposal to the vote registry and returns a {@link SignedVote}.
     *
     * @param currentIteration the current iteration number
     * @param notarizedBlockchain the current notarized blockchain
     * @param signedProposal the signed proposal for a new block
     * @param voteRegistry the registry of votes
     * @return a signed vote if the proposal is valid, otherwise null
     * @throws JsonProcessingException if an error occurs during JSON processing
     * @throws GeneralSecurityException if a security exception occurs during signature verification
     */
    public SignedVote validateProposal(int currentIteration, NotarizedBlockchain<T> notarizedBlockchain, SignedProposal<T> signedProposal,
            Map<String, VoteRegistryEntry<T>> voteRegistry) throws JsonProcessingException, GeneralSecurityException {
        Proposal<T> proposal = signedProposal.proposal();
        Block<T> newBlock = proposal.newBlock();
        String proposalHash = CryptoUtil.computeHash(newBlock);
        if (isValidProposal(signedProposal, proposalHash, notarizedBlockchain, processedProposalIds, playerIdsToPublicKeys)) {
            voteRegistry.put(currentIteration + ":" + proposalHash, new VoteRegistryEntry<>(newBlock, new HashSet<>()));
            Vote vote = new Vote(playerId, currentIteration, proposalHash);
            byte[] voteBytes = SimplexConstants.voteToBytes(vote);
            byte[] voteSignature = CryptoUtil.generateSignature(keyPair.getPrivate(), voteBytes);
            return new SignedVote(vote, voteSignature);
        }
        return null;
    }
}
