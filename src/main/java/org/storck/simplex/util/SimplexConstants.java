package org.storck.simplex.util;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.experimental.UtilityClass;
import org.storck.simplex.model.Block;
import org.storck.simplex.model.NotarizedBlock;
import org.storck.simplex.model.NotarizedBlockchain;
import org.storck.simplex.model.Proposal;
import org.storck.simplex.model.Vote;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;

/**
 * Provides constants and utility methods for the Simplex consensus blockchain protocol.
 */
@UtilityClass
public class SimplexConstants {

    /**
     * Represents the hash value of a dummy block in a blockchain.
     * It is used as a placeholder or default value for a block's hash when needed.
     */
    public static final String DUMMY_BLOCK_HASH = "0000000000";

    /**
     * Instance of the JsonMapper class, which is used to serialize and deserialize JSON data.
     */
    public static final JsonMapper JSON_MAPPER = new JsonMapper(new JsonFactory());

    /**
     * Returns a dummy block with the specified iteration number.
     *
     * @param iteration the iteration number of the block
     * @param <T> the type of the transactions stored in the block
     * @return a dummy block with the specified iteration number
     */
    public static <T> Block<T> dummyBlock(int iteration) {
        return new Block<>(iteration, "", List.of());
    }

    /**
     * This method returns the genesis block of a blockchain.
     *
     * @param <T> the type of the transactions stored in the block
     * @return the genesis block
     */
    public static <T> Block<T> genesisBlock() {
        return dummyBlock(0);
    }

    /**
     * Creates a notarized block using the given block and set of quorum votes.
     *
     * @param <T> the type of the transactions stored in the block
     * @param block the block of transactions
     * @param quorumVotes the set of quorum votes
     * @return a notarized block with the given block and quorum votes
     */
    public static <T> NotarizedBlock<T> createNotarizedBlock(Block<T> block, Set<Vote> quorumVotes) {
        return new NotarizedBlock<>(block, quorumVotes);
    }

    /**
     * Finalizes the current iteration by adding the notarized block to the notarized blockchain.
     *
     * @param <T> the type of the transactions stored in the block
     * @param notarizedBlock the notarized block to be added
     * @param notarizedBlockchain the notarized blockchain to add the block to
     */
    public static <T> void finalizeIteration(NotarizedBlock<T> notarizedBlock, NotarizedBlockchain<T> notarizedBlockchain) {
        notarizedBlockchain.blocks().add(notarizedBlock);
    }

    /**
     * Converts a Proposal object to a byte array using JSON serialization.
     *
     * @param <T> the type of the transactions stored in the block
     * @param proposal the Proposal object to be converted
     * @return a byte array representing the Proposal object
     * @throws JsonProcessingException if an error occurs during JSON serialization
     */
    public static <T> byte[] proposalToBytes(Proposal<T> proposal) throws JsonProcessingException {
        return JSON_MAPPER.writeValueAsBytes(proposal);
    }

    /**
     * Converts a Vote object to a byte array using JSON serialization.
     *
     * @param vote the Vote object to be converted
     * @return a byte array representing the Vote object
     * @throws JsonProcessingException if an error occurs during JSON serialization
     */
    public static byte[] voteToBytes(Vote vote) throws JsonProcessingException {
        return JSON_MAPPER.writeValueAsBytes(vote);
    }

    /**
     * For each iteration, its leader is chosen by hashing the iteration number h
     * using some public hash function.
     * In other words, L = H * h % n, where H is a predetermined hash function.
     *
     * @param iteration
     *            the iteration number used to calculate the leader
     * @param participantIds
     *            the IDs of all known players/participants
     */
    public static String electLeader(long iteration, List<String> participantIds) {
        String hash = CryptoUtil.computeHash(Long.toString(iteration).getBytes());
        BigInteger hashInt = new BigInteger(1, hash.getBytes());
        return participantIds.get(hashInt.mod(BigInteger.valueOf(participantIds.size())).intValue());
    }
}
