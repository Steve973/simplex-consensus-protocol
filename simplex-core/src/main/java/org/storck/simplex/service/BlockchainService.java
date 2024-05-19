package org.storck.simplex.service;

import org.storck.simplex.model.Block;
import org.storck.simplex.model.BlockNotarized;
import org.storck.simplex.model.BlockchainNotarized;
import org.storck.simplex.model.Vote;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * Maintains the blockchain, and provides convenience methods to create
 * specialized blocks, including the genesis block, dummy block, and finalizeMsg
 * block.
 *
 * @param <T> the type of block transactions
 */
public class BlockchainService<T> {

    /** Contains all notarized (accepted) blocks. */
    private final BlockchainNotarized<T> notarizedBlockchain;

    /**
     * Creates a dummy block with the specified iteration number.
     */
    public final IntFunction<Block<T>> createDummyBlock = i -> new Block<>(i, "", List.of());

    /**
     * Creates the genesis block of a blockchain.
     */
    public final Supplier<Block<T>> createGenesisBlock = () -> createDummyBlock.apply(0);

    /**
     * Creates the genesis block of a blockchain.
     */
    public final IntFunction<Block<T>> createFinalizeBlock = i -> new Block<>(i, "FINALIZE", List.of());

    /**
     * Creates a notarized block using the given block and set of quorum votes.
     */
    public final BiFunction<Block<T>, List<Vote>, BlockNotarized<T>> createNotarizedBlock = BlockNotarized::new;

    /**
     * Create the service to maintain the blockchain and create specialized blocks.
     */
    public BlockchainService() {
        this.notarizedBlockchain = new BlockchainNotarized<>(new ArrayList<>());
        notarizedBlockchain.addBlock(new BlockNotarized<>(createGenesisBlock.get(), List.of()));
    }

    /**
     * Returns an immutable copy of the current blockchain state.
     *
     * @return an immutable copy of the current blockchain state
     */
    public List<BlockNotarized<T>> getBlockchain() {
        return List.copyOf(notarizedBlockchain.blocks());
    }
}
