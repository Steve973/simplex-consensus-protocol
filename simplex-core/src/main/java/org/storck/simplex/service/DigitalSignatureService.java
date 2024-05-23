package org.storck.simplex.service;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import org.bouncycastle.crypto.EntropySourceProvider;
import org.bouncycastle.crypto.fips.FipsDRBG;
import org.bouncycastle.crypto.util.BasicEntropySourceProvider;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.storck.simplex.model.Block;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Handles digital signature operations.
 */
@Getter
public class DigitalSignatureService {

    /**
     * A byte array used for personalization in creating the random number
     * generator.
     */
    private static final byte[] PERSONALIZATION_STRING = "".getBytes(StandardCharsets.UTF_8);

    /**
     * A "number used once" for the random number generator. Here, we use a string
     * representation of the current system time, then convert to a byte array.
     */
    private static final byte[] NONCE = String.valueOf(System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8);

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
     * Instance of the JsonMapper class, which is used to serialize and deserialize
     * JSON data.
     */
    public static final JsonMapper JSON_MAPPER = new JsonMapper(new JsonFactory());

    /**
     * The messageDigestAlgorithm variable represents the name of the message digest
     * algorithm used for hashing.
     */
    private final String messageDigestAlgorithm;

    /**
     * The algorithm used for generating a new KeyPair.
     *
     * @see DigitalSignatureService#generateKeyPair()
     */
    private final String keypairGeneratorAlgorithm;

    /**
     * The algorithm used for generating signatures.
     */
    private final String signatureAlgorithm;

    /**
     * The key pair used for cryptographic operations, including signing and
     * verifying proposals and votes.
     */
    private final KeyPair keyPair;

    /**
     * Instance of the BouncyCastleFipsProvider class used as a provider for
     * cryptographic operations.
     */
    private static final BouncyCastleFipsProvider BOUNCY_CASTLE_FIPS_PROVIDER = new BouncyCastleFipsProvider();

    /**
     * Create an instance with the default algorithm names. A keypair will be
     * generated.
     */
    @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW", justification = "If the keypair cannot be generated, this node cannot run anyway")
    public DigitalSignatureService() {
        this(MESSAGE_DIGEST_ALGORITHM, KEYPAIR_GENERATOR_ALGORITHM, SIGNATURE_ALGORITHM);
    }

    /**
     * Create an instance by specifying the algorithm names. A keypair will be
     * generated.
     *
     * @param messageDigestAlgorithm The algorithm used for computing hash values.
     * @param keypairGeneratorAlgorithm The algorithm used for generating key pairs.
     * @param signatureAlgorithm The algorithm used for generating signatures.
     */
    @SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW", justification = "If the keypair cannot be generated, this node cannot run anyway")
    public DigitalSignatureService(final String messageDigestAlgorithm, final String keypairGeneratorAlgorithm, final String signatureAlgorithm) {
        this.messageDigestAlgorithm = messageDigestAlgorithm;
        this.keypairGeneratorAlgorithm = keypairGeneratorAlgorithm;
        this.signatureAlgorithm = signatureAlgorithm;
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
            MessageDigest digest = MessageDigest.getInstance(messageDigestAlgorithm, BOUNCY_CASTLE_FIPS_PROVIDER);
            byte[] hashBytes = digest.digest(input);
            return IntStream.range(0, hashBytes.length)
                    .mapToObj(i -> String.format("%02x", hashBytes[i]))
                    .collect(Collectors.joining());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(messageDigestAlgorithm + " algorithm not available", e);
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
     * Generates a FIPS-compliant secure random number generator. As described in
     * "The Bouncy Castle FIPS Java API in 100 Examples" by David Hook:
     * <blockquote>
     * The next thing to note in the example is that the SecureRandom is not created
     * via the Java provider mechanism. This is because the SecureRandom returned is
     * an extension of the regular SecureRandom class called FipsSecureRandom. In
     * this case an extension class was necessary as a NIST DRBG requires methods
     * such as FipsSecureRandom.reseed() which are not available on SecureRandom (in
     * this case even SecureRandom.setSeed() is not really a suitable candidate).
     * The second thing to note is the false parameter value on the
     * FipsDRBG.Builder.build() method. This refers to the “prediction resistance”
     * required of the constructed DRBG. In this case we are willing to assume that
     * the DRBG function will do a good job producing a random stream and that's
     * enough. In the case of keys or components of keys we need a higher standard
     * to be reached so we set “prediction resistance” to true as in the following
     * example.
     * </blockquote>
     *
     * @return a FIPS-compliant secure random number generator
     *
     * @see <a href=
     *     "http://git.bouncycastle.org/fips-java/BCFipsIn100.pdf">BCFipsIn100</a>
     */
    private static SecureRandom getSecureRandom() {
        EntropySourceProvider entSource = new BasicEntropySourceProvider(new SecureRandom(), true);
        FipsDRBG.Builder fipsDrbgBuilder = FipsDRBG.SHA512_HMAC.fromEntropySource(entSource)
                .setSecurityStrength(256)
                .setEntropyBitsRequired(256)
                .setPersonalizationString(PERSONALIZATION_STRING);
        return fipsDrbgBuilder.build(NONCE, true);
    }

    /**
     * Generates a new KeyPair using the Bouncy Castle FIPS provider.
     *
     * @return a new KeyPair
     */
    private KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(keypairGeneratorAlgorithm, BOUNCY_CASTLE_FIPS_PROVIDER);
            keyPairGenerator.initialize(384, getSecureRandom());
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not generate a key pair for cryptological operations", e);
        }
    }

    /**
     * Converts the given byte array representation of a public key into a PublicKey
     * instance.
     *
     * @param keyBytes the byte array representation of the public key
     * 
     * @return the PublicKey instance
     * 
     * @throws IllegalStateException if the encoded public key cannot be converted
     *     to a public key instance
     */
    public PublicKey publicKeyFromBytes(final byte[] keyBytes) {
        try {
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(keypairGeneratorAlgorithm, BOUNCY_CASTLE_FIPS_PROVIDER);
            return keyFactory.generatePublic(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Could not convert encoded public key to a public key instance", e);
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
            Signature signature = Signature.getInstance(signatureAlgorithm, BOUNCY_CASTLE_FIPS_PROVIDER);
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
        Signature signature = Signature.getInstance(signatureAlgorithm, BOUNCY_CASTLE_FIPS_PROVIDER);
        signature.initVerify(publicKey);
        signature.update(input);
        return signature.verify(encSignature);
    }
}
