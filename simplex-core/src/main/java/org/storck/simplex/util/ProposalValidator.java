package org.storck.simplex.util;

import lombok.experimental.UtilityClass;
import org.storck.simplex.model.Block;
import org.storck.simplex.model.NotarizedBlock;
import org.storck.simplex.model.NotarizedBlockchain;
import org.storck.simplex.model.Proposal;
import org.storck.simplex.model.SignedProposal;

import java.util.List;
import java.util.function.Predicate;

/**
 * Validates {@link Proposal}s.
 */
@UtilityClass
public class ProposalValidator {

    static <T> boolean isForCurrentIteration(Proposal<T> proposal, NotarizedBlockchain<T> currentNotarizedChain) {
        return proposal.iteration() == currentNotarizedChain.blocks().size() + 1;
    }

    static <T> boolean isParentChainCurrentNotarizedChain(Proposal<T> proposal, NotarizedBlockchain<T> currentNotarizedChain) {
        return proposal.parentChain().equals(currentNotarizedChain);
    }

    static <T> boolean isHeightValid(NotarizedBlock<T> parentBlock, Block<T> newBlock) {
        return newBlock.height() == parentBlock.block().height() + 1;
    }

    static <T> boolean isParentHashValid(NotarizedBlock<T> parentBlock, Block<T> newBlock) {
        String expectedParentHash = CryptoUtil.computeHash(parentBlock.block());
        return newBlock.parentHash().equals(expectedParentHash);
    }

    static <T> boolean isProperBlockchainExtension(Proposal<T> proposal, NotarizedBlockchain<T> currentNotarizedChain) {
        List<NotarizedBlock<T>> blocks = currentNotarizedChain.blocks();
        NotarizedBlock<T> parentBlock = blocks.get(blocks.size() - 1);
        Block<T> newBlock = proposal.newBlock();
        return isHeightValid(parentBlock, newBlock) && isParentHashValid(parentBlock, newBlock);
    }

    /**
     * Determines if the given proposal is valid.
     *
     * @param <T>
     *            the type of the transactions stored in the block
     * @param signedProposal
     *            the signed proposal for a new block in a blockchain
     * @param currentNotarizedChain
     *            the current notarized chain
     * @return true if the proposal is valid, false otherwise
     */
    public static <T> boolean isValidProposal(SignedProposal<T> signedProposal, NotarizedBlockchain<T> currentNotarizedChain) {
        Proposal<T> proposal = signedProposal.proposal();
        List<Predicate<Proposal<T>>> checks = List.of(
                p -> isForCurrentIteration(p, currentNotarizedChain),
                p -> isParentChainCurrentNotarizedChain(p, currentNotarizedChain),
                p -> isProperBlockchainExtension(p, currentNotarizedChain));
        return checks.stream().allMatch(check -> check.test(proposal));
    }
}
