package org.storck.simplex.util;

import lombok.experimental.UtilityClass;
import org.storck.simplex.model.Block;

import java.math.BigInteger;
import java.util.List;

@UtilityClass
public class SimplexConsensusProtocolConstants {

    public static final String DUMMY_BLOCK_HASH = "0000000000";

    public static <T> Block<T> dummyBlock(int iteration) {
        return new Block<>(iteration, "", List.of());
    }

    public static <T> Block<T> genesisBlock() {
        return dummyBlock(0);
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
