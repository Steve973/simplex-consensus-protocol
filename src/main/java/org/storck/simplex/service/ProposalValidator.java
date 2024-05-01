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

public class ProposalValidator<T> {

    private final String playerId;

    private final KeyPair keyPair;

    private final Map<String, PublicKey> playerIdsToPublicKeys;

    private final Set<String> processedProposalIds;

    public ProposalValidator(String playerId, KeyPair keyPair, Map<String, PublicKey> playerIdsToPublicKeys) {
        this.playerId = playerId;
        this.keyPair = keyPair;
        this.playerIdsToPublicKeys = playerIdsToPublicKeys;
        processedProposalIds = new HashSet<>();
    }

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

    static <T> boolean isValidBlockExtension(Block<T> newBlock, NotarizedBlock<T> parentBlock) {
        int expectedHeight = parentBlock.block().height() + 1;
        if (newBlock.height() != expectedHeight) {
            return false;
        }

        String expectedParentHash = CryptoUtil.computeHash(parentBlock.block());
        return newBlock.parentHash().equals(expectedParentHash);
    }

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
