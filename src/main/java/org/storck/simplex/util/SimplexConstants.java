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

@UtilityClass
public class SimplexConstants {

    public static final String DUMMY_BLOCK_HASH = "0000000000";

    public static final JsonMapper JSON_MAPPER = new JsonMapper(new JsonFactory());

    public static <T> Block<T> dummyBlock(int iteration) {
        return new Block<>(iteration, "", List.of());
    }

    public static <T> Block<T> genesisBlock() {
        return dummyBlock(0);
    }

    public static <T> NotarizedBlock<T> createNotarizedBlock(Block<T> block, Set<Vote> quorumVotes) {
        return new NotarizedBlock<>(block, quorumVotes);
    }

    public static <T> void finalizeIteration(NotarizedBlock<T> notarizedBlock, NotarizedBlockchain<T> notarizedBlockchain) {
        notarizedBlockchain.blocks().add(notarizedBlock);
    }

    public static <T> byte[] proposalToBytes(Proposal<T> proposal) throws JsonProcessingException {
        return JSON_MAPPER.writeValueAsBytes(proposal);
    }

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
