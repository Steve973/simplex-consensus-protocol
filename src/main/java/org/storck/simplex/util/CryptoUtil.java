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

/**
 * The CryptoUtil class provides utility methods for cryptographic operations.
 */
@UtilityClass
public class CryptoUtil {

    /**
     * Algorithm used for message digest computation.
     */
    private static final String MESSAGE_DIGEST_ALGORITHM = "SHA3-512";

    /**
     * The algorithm used for generating key pairs.
     */
    private static final String KEYPAIR_GENERATOR_ALGORITHM = "EC";

    /**
     * Represents the algorithm used for generating digital signatures using ECDSA (Elliptic Curve Digital Signature Algorithm).
     */
    private static final String SIGNATURE_ALGORITHM = "SHA384withECDSA";

    /**
     * Instance of the BouncyCastleFipsProvider class. used as a provider for cryptographic operations.
     * <p>
     * The BouncyCastleFipsProvider class is part of the Bouncy Castle library,
     * which is widely used for cryptographic operations and provides FIPS-approved algorithms.
     * The FIPS (Federal Information Processing Standards) approved algorithms are certified
     * by the US government for use in cryptographic applications.
     * <p>
     * For more information, refer to the Bouncy Castle documentation.
     */
    private static final BouncyCastleFipsProvider BOUNCY_CASTLE_FIPS_PROVIDER = new BouncyCastleFipsProvider();

    /**
     * Computes the hash value of the given byte array using the specified message digest algorithm and returns it as a hexadecimal string.
     *
     * @param input the byte array to compute the hash value of
     * @return the computed hash value as a hexadecimal string
     * @throws IllegalStateException if the specified message digest algorithm is not available
     */
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

    /**
     * Computes the hash value of the given Block object using the specified message digest algorithm and returns it as a hexadecimal string.
     *
     * @param block the Block object to compute the hash value of
     * @param <T>   the type of the transactions stored in the block
     * @return the computed hash value as a hexadecimal string
     * @throws IllegalStateException if the specified message digest algorithm is not available
     */
    public static <T> String computeHash(Block<T> block) {
        try {
            byte[] input = JSON_MAPPER.writeValueAsBytes(block);
            return computeHash(input);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generates a new KeyPair using the Bouncy Castle FIPS provider.
     *
     * @return a new KeyPair
     * @throws NoSuchAlgorithmException if the requested algorithm is not available
     */
    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPair = KeyPairGenerator.getInstance(KEYPAIR_GENERATOR_ALGORITHM, BOUNCY_CASTLE_FIPS_PROVIDER);
        keyPair.initialize(384);
        return keyPair.generateKeyPair();
    }

    /**
     * Generates a signature for the given input using the specified private key.
     *
     * @param ecPrivate the private key to use for generating the signature
     * @param input the input data to sign
     * @return the generated signature as an array of bytes
     * @throws GeneralSecurityException if any security-related exceptions occur during the signature generation process
     */
    public static byte[] generateSignature(PrivateKey ecPrivate, byte[] input) throws GeneralSecurityException {
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM, BOUNCY_CASTLE_FIPS_PROVIDER);
        signature.initSign(ecPrivate);
        signature.update(input);
        return signature.sign();
    }

    /**
     * Verifies the signature of the given input using the provided public key and encrypted signature.
     *
     * @param ecPublic      the public key used to verify the signature
     * @param input         the input data to verify the signature of
     * @param encSignature  the encrypted signature to be verified
     * @return true if the signature is valid, false otherwise
     * @throws GeneralSecurityException if any security-related exceptions occur during the signature verification process
     */
    public static boolean verifySignature(PublicKey ecPublic, byte[] input, byte[] encSignature) throws GeneralSecurityException {
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM, BOUNCY_CASTLE_FIPS_PROVIDER);
        signature.initVerify(ecPublic);
        signature.update(input);
        return signature.verify(encSignature);
    }
}
