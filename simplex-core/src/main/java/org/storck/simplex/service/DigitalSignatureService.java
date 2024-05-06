package org.storck.simplex.service;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.storck.simplex.model.Block;

import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Handles digital signature operations.
 */
@Getter
public class DigitalSignatureService {

    /**
     * Instance of the JsonMapper class, which is used to serialize and deserialize
     * JSON data.
     */
    public static final JsonMapper JSON_MAPPER = new JsonMapper(new JsonFactory());

    /**
     * The key pair used for cryptographic operations, including signing and
     * verifying proposals and votes.
     */
    private final KeyPair keyPair;

    /** Algorithm used for message digest computation. */
    private static final String MESSAGE_DIGEST_ALGORITHM = "SHA3-512";

    /** The algorithm used for generating key pairs. */
    private static final String KEYPAIR_GENERATOR_ALGORITHM = "EC";

    /**
     * Represents the algorithm used for generating digital signatures using ECDSA
     * (Elliptic Curve Digital Signature Algorithm).
     */
    private static final String SIGNATURE_ALGORITHM = "SHA384withECDSA";

    /**
     * Instance of the BouncyCastleFipsProvider class used as a provider for
     * cryptographic operations.
     */
    private static final BouncyCastleFipsProvider BOUNCY_CASTLE_FIPS_PROVIDER = new BouncyCastleFipsProvider();

    /**
     * Create an instance. A keypair will be generated.
     */
    @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW", justification = "If the keypair cannot be generated, this node cannot run anyway")
    public DigitalSignatureService() {
        this.keyPair = generateKeyPair();
    }

    /**
     * Computes the hash value of the given byte array using the specified message
     * digest algorithm and returns it as a hexadecimal string.
     *
     * @param input the byte array to compute the hash value of
     *
     * @return the computed hash value as a hexadecimal string
     *
     * @throws IllegalStateException if the specified message digest algorithm is
     *     not available
     */
    public String computeBytesHash(final byte[] input) {
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
     * Computes the hash value of the given Block object using the specified message
     * digest algorithm and returns it as a hexadecimal string.
     *
     * @param block the Block object to compute the hash value of
     * @param <T> the type of the transactions stored in the block
     *
     * @return the computed hash value as a hexadecimal string
     *
     * @throws IllegalStateException if the specified message digest algorithm is
     *     not available
     */
    public <T> String computeBlockHash(final Block<T> block) {
        try {
            byte[] input = JSON_MAPPER.writeValueAsBytes(block);
            return computeBytesHash(input);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error when computing block hash", e);
        }
    }

    /**
     * Generates a new KeyPair using the Bouncy Castle FIPS provider.
     *
     * @return a new KeyPair
     */
    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEYPAIR_GENERATOR_ALGORITHM, BOUNCY_CASTLE_FIPS_PROVIDER);
            keyPairGenerator.initialize(384);
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not generate a key pair for cryptological operations", e);
        }
    }

    /**
     * Generates a signature for the given input using the specified private key.
     *
     * @param input the input data to sign
     *
     * @return the generated signature as an array of bytes
     */
    public byte[] generateSignature(final byte[] input) {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM, BOUNCY_CASTLE_FIPS_PROVIDER);
            signature.initSign(keyPair.getPrivate());
            signature.update(input);
            return signature.sign();
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            throw new IllegalStateException("Unexpected error when generating a signature", e);
        }
    }

    /**
     * Verifies the signature of the given input using the provided public key and
     * encrypted signature.
     *
     * @param input the input data to verify the signature of
     * @param encSignature the encrypted signature to be verified
     * @param publicKey public key of the player that signed this message
     *
     * @return true if the signature is valid, false otherwise
     *
     * @throws GeneralSecurityException if any security-related exceptions occur
     *     during the signature verification process
     */
    public boolean verifySignature(final byte[] input, final byte[] encSignature, final PublicKey publicKey) throws GeneralSecurityException {
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM, BOUNCY_CASTLE_FIPS_PROVIDER);
        signature.initVerify(publicKey);
        signature.update(input);
        return signature.verify(encSignature);
    }
}