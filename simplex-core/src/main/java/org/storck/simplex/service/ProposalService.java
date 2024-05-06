package org.storck.simplex.service;

import org.storck.simplex.model.Block;
import org.storck.simplex.model.NotarizedBlock;
import org.storck.simplex.model.NotarizedBlockchain;
import org.storck.simplex.model.Proposal;
import org.storck.simplex.model.SignedProposal;
import org.storck.simplex.networking.api.network.PeerNetworkClient;
import org.storck.simplex.networking.api.protocol.ProposalProtocolMessage;
import org.storck.simplex.util.MessageUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;
import java.util.function.Predicate;

/**
 * Manages the creation, validation, and broadcasting of block proposals by the
 * leader.
 *
 * @param <T> the type of transaction data
 */
public class ProposalService<T> {

    /** The ID of the local player (i.e., the ID for this local node). */
    private final String localPlayerId;

    /**
     * Stores transactions until this player is the leader, and can propose them as
     * a block.
     */
    private final TransferQueue<T> transactionQueue;

    /**
     * Service that provides digital signature operations.
     */
    private final DigitalSignatureService signatureService;

    /**
     * Service that handles communication/messaging between peers/players.
     */
    private final PeerNetworkClient peerNetworkClient;

    /**
     * A set of all proposal IDs that have been seen and/or processed.
     */
    private final Set<String> processedProposalIds;

    /**
     * Creates the proposal service instance.
     *
     * @param localPlayerId the player ID of this node
     * @param signatureService the service that handles digital signatures
     * @param peerNetworkClient the client for network interoperability
     */
    public ProposalService(final String localPlayerId, final DigitalSignatureService signatureService, final PeerNetworkClient peerNetworkClient) {
        this.localPlayerId = localPlayerId;
        this.transactionQueue = new LinkedTransferQueue<>();
        this.signatureService = signatureService;
        this.peerNetworkClient = peerNetworkClient;
        this.processedProposalIds = new HashSet<>();
    }

    /**
     * Determines if the given proposal is for the current iteration of the
     * notarized chain.
     *
     * @param <T> the proposal type
     * @param proposal the proposal for a new block in a blockchain
     * @param notarizedBlocks the current notarized chain
     *
     * @return true if the proposal is for the current iteration, false otherwise
     */
    static <T> boolean isForCurrentIteration(final Proposal<T> proposal, final List<NotarizedBlock<T>> notarizedBlocks) {
        return proposal.iteration() == notarizedBlocks.size() + 1;
    }

    /**
     * Determines if the parent chain of a proposal is the current notarized chain.
     *
     * @param <T> the type of transactions stored in the block
     * @param proposal the proposal for a new block in a blockchain
     * @param notarizedBlocks the current notarized chain
     *
     * @return true if the parent chain of the proposal is the current notarized
     *     chain, false otherwise
     */
    static <T> boolean isParentChainCurrentChain(final Proposal<T> proposal, final List<NotarizedBlock<T>> notarizedBlocks) {
        return proposal.parentChain().blocks().equals(notarizedBlocks);
    }

    /**
     * Determines if the height of a new block is valid based on the parent block's
     * height.
     *
     * @param parentBlock the parent block in the blockchain
     * @param newBlock the new block for which the height needs to be validated
     * @param <T> the type of the transactions stored in the block
     *
     * @return true if the height of the new block is valid, false otherwise
     */
    static <T> boolean isHeightValid(final NotarizedBlock<T> parentBlock, final Block<T> newBlock) {
        return newBlock.height() == parentBlock.block().height() + 1;
    }

    /**
     * Checks if the parent hash of a new block is valid based on the expected
     * parent hash computed from the parent block.
     *
     * @param parentBlock the parent block in the blockchain
     * @param newBlock the new block for which the parent hash needs to be validated
     *
     * @return true if the parent hash of the new block is valid, false otherwise
     */
    boolean isParentHashValid(final NotarizedBlock<T> parentBlock, final Block<T> newBlock) {
        String expectedParentHash = signatureService.computeBlockHash(parentBlock.block());
        return newBlock.parentHash().equals(expectedParentHash);
    }

    /**
     * Determines if the given proposal is a proper extension of the current
     * notarized blockchain.
     *
     * @param proposal the proposal for a new block in the blockchain
     * @param notarizedBlocks the current notarized chain
     *
     * @return true if the proposal is a proper extension, false otherwise
     */
    boolean isProperBlockchainExtension(final Proposal<T> proposal, final List<NotarizedBlock<T>> notarizedBlocks) {
        NotarizedBlock<T> parentBlock = notarizedBlocks.get(notarizedBlocks.size() - 1);
        Block<T> newBlock = proposal.newBlock();
        return isHeightValid(parentBlock, newBlock) && isParentHashValid(parentBlock, newBlock);
    }

    /**
     * Determines if the given proposal is valid.
     *
     * @param signedProposal the signed proposal for a new block in a blockchain
     * @param notarizedBlocks the current notarized chain
     *
     * @return true if the proposal is valid, false otherwise
     */
    public boolean isValidProposal(final SignedProposal<T> signedProposal, final List<NotarizedBlock<T>> notarizedBlocks) {
        Proposal<T> proposal = signedProposal.proposal();
        List<Predicate<Proposal<T>>> checks = List.of(
                p -> isForCurrentIteration(p, notarizedBlocks),
                p -> isParentChainCurrentChain(p, notarizedBlocks),
                p -> isProperBlockchainExtension(p, notarizedBlocks));
        return checks.stream().allMatch(check -> check.test(proposal));
    }

    /**
     * Add a collection of transactions to be proposed as a new block when this
     * player/node is the iteration leader.
     *
     * @param transactions collection of transactions to add
     */
    public void addTransactions(final Collection<T> transactions) {
        this.transactionQueue.addAll(transactions);
    }

    /**
     * If the local player is the leader, it retrieves pending transactions, creates
     * a proposal, and broadcasts it to peers.
     */
    void proposeNewBlock(final List<NotarizedBlock<T>> notarizedBlocks, final int iterationNumber) {
        Collection<T> transactions = new ArrayList<>();
        transactionQueue.drainTo(transactions);
        int currentBlockchainSize = notarizedBlocks.size();
        String parentHash = Optional.of(notarizedBlocks)
                .map(blocks -> blocks.get(currentBlockchainSize - 1))
                .map(NotarizedBlock::block)
                .map(signatureService::computeBlockHash)
                .orElseThrow(() -> new IllegalArgumentException("Could not get parent block hash for proposal"));
        Proposal<T> proposal = new Proposal<>(iterationNumber, localPlayerId, new Block<>(currentBlockchainSize + 1, parentHash, transactions),
                new NotarizedBlockchain<>(notarizedBlocks));
        byte[] proposalBytes = MessageUtils.toBytes(proposal);
        byte[] proposalSignature = signatureService.generateSignature(proposalBytes);
        SignedProposal<T> signedProposal = new SignedProposal<>(proposal, proposalSignature);
        peerNetworkClient.broadcastProposal(new ProposalProtocolMessage(MessageUtils.toBytes(signedProposal)));
    }

    /**
     * Processes a proposal by validating it and broadcasting the resulting vote.
     *
     * @param signedProposal the proposal message to process
     */
    public void processProposal(final SignedProposal<T> signedProposal, final List<NotarizedBlock<T>> notarizedBlocks) {
        Proposal<T> proposal = signedProposal.proposal();
        String proposalId = signatureService.computeBlockHash(proposal.newBlock());
        if (!processedProposalIds.contains(proposalId) && isValidProposal(signedProposal, notarizedBlocks)) {
            processedProposalIds.add(proposalId);
        }
    }
}
