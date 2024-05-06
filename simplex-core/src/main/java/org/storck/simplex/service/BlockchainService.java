package org.storck.simplex.service;

import org.storck.simplex.model.Block;
import org.storck.simplex.model.NotarizedBlock;
import org.storck.simplex.model.NotarizedBlockchain;
import org.storck.simplex.model.Vote;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Maintains the blockchain, and provides convenience methods to create
 * specialized blocks, including the genesis block, dummy block, and finalize
 * block.
 *
 * @param <T> the type of block transactions
 */
public class BlockchainService<T> {

    /** Contains all notarized (accepted) blocks. */
    private final NotarizedBlockchain<T> notarizedBlockchain;

    /**
     * Creates a dummy block with the specified iteration number.
     */
    public final Function<Integer, Block<T>> createDummyBlock = i -> new Block<>(i, "", List.of());

    /**
     * Creates the genesis block of a blockchain.
     */
    public final Supplier<Block<T>> createGenesisBlock = () -> createDummyBlock.apply(0);

    /**
     * Creates the genesis block of a blockchain.
     */
    public final Function<Integer, Block<T>> createFinalizeBlock = i -> new Block<>(i, "FINALIZE", List.of());

    /**
     * Creates a notarized block using the given block and set of quorum votes.
     *
     * @param <T> the type of the transactions stored in the block
     * @param block the block of transactions
     * @param quorumVotes the list of quorum votes
     *
     * @return a notarized block with the given block and quorum votes
     */
    public static <T> NotarizedBlock<T> createNotarizedBlock(final Block<T> block, final List<Vote> quorumVotes) {
        return new NotarizedBlock<>(block, quorumVotes);
    }

    /**
     * Create the service to maintain the blockchain and create specialized blocks.
     */
    public BlockchainService() {
        this.notarizedBlockchain = new NotarizedBlockchain<>(new ArrayList<>());
        notarizedBlockchain.blocks().add(new NotarizedBlock<>(createGenesisBlock.get(), List.of()));
    }

    /**
     * Returns an immutable copy of the current blockchain state.
     *
     * @return an immutable copy of the current blockchain state
     */
    public List<NotarizedBlock<T>> getBlockchain() {
        return List.copyOf(notarizedBlockchain.blocks());
    }
}
