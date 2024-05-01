package org.storck.simplex.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.experimental.UtilityClass;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.storck.simplex.model.Block;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.storck.simplex.util.SimplexConstants.JSON_MAPPER;

@UtilityClass
public class CryptoUtil {

    private static final String MESSAGE_DIGEST_ALGORITHM = "SHA3-512";

    private static final String KEYPAIR_GENERATOR_ALGORITHM = "EC";

    private static final String SIGNATURE_ALGORITHM = "SHA384withECDSA";

    private static final BouncyCastleFipsProvider BOUNCY_CASTLE_FIPS_PROVIDER = new BouncyCastleFipsProvider();

    public static String computeHash(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM, BOUNCY_CASTLE_FIPS_PROVIDER);
            byte[] hashBytes = digest.digest(input);
            return IntStream.range(0, hashBytes.length)
                    .mapToObj(i -> String.format("%02x", hashBytes[i]))
                    .collect(Collectors.joining());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(MESSAGE_DIGEST_ALGORITHM + " algorithm not available", e);
        }
    }

    public static <T> String computeHash(Block<T> block) {
        try {
            byte[] input = JSON_MAPPER.writeValueAsBytes(block);
            return computeHash(input);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPair = KeyPairGenerator.getInstance(KEYPAIR_GENERATOR_ALGORITHM, BOUNCY_CASTLE_FIPS_PROVIDER);
        keyPair.initialize(384);
        return keyPair.generateKeyPair();
    }

    public static byte[] generateSignature(PrivateKey ecPrivate, byte[] input) throws GeneralSecurityException {
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM, BOUNCY_CASTLE_FIPS_PROVIDER);
        signature.initSign(ecPrivate);
        signature.update(input);
        return signature.sign();
    }

    public static boolean verifySignature(PublicKey ecPublic, byte[] input, byte[] encSignature) throws GeneralSecurityException {
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM, BOUNCY_CASTLE_FIPS_PROVIDER);
        signature.initVerify(ecPublic);
        signature.update(input);
        return signature.verify(encSignature);
    }
}
