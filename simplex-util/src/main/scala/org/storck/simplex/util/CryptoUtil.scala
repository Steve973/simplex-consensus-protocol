package org.storck.simplex.util

import org.bouncycastle.crypto.fips.{FipsDRBG, FipsSecureRandom}
import org.bouncycastle.crypto.util.BasicEntropySourceProvider
import org.storck.simplex.message.Block
import org.storck.simplex.util.SimplexConstants._

import java.security._
import java.security.spec.X509EncodedKeySpec
import scala.util.control.Exception.allCatch


/** Contains utility functions for cryptological operations, including signing.
 *
 *  This class is the Scala equivalent of the Java utility class with the same functionality.
 */
object CryptoUtil {

  /**
   * Computes the hash value of the given byte array using the specified message digest algorithm and returns it as a hexadecimal string.
   *
   * @param input the byte array to compute the hash value of
   * @param messageDigestAlgorithm the algorithm to compute the hash value with
   *
   * @throws IllegalStateException if the specified message digest algorithm is
   * not available
   */
  def computeBytesHash(input: Array[Byte], messageDigestAlgorithm: String = MESSAGE_DIGEST_ALGORITHM): String = {
    val digest = MessageDigest.getInstance(messageDigestAlgorithm, BOUNCY_CASTLE_FIPS_PROVIDER)
    val hashBytes = digest.digest(input)
    hashBytes.map("%02x".format(_)).mkString
  }

  /**
   * Computes the hash value of the given Block object using the specified message digest algorithm and returns it as a hexadecimal string.
   *
   * @param block the Block object to compute the hash value of
   * @param messageDigestAlgorithm the algorithm to compute the hash value with
   * @return the computed hash value as a hexadecimal string
   *
   * @throws IllegalStateException if the specified message digest algorithm is
   * not available
   */
  def computeBlockHash(block: Block, messageDigestAlgorithm: String = MESSAGE_DIGEST_ALGORITHM): String = {
    val input = JSON_MAPPER.writeValueAsBytes(block)
    computeBytesHash(input, messageDigestAlgorithm)
  }

  /**
   * Generates a FIPS-compliant secure random number generator. As described in
   * "The Bouncy Castle FIPS Java API in 100 Examples" by David Hook: "The next
   * thing to note in the example is that the SecureRandom is not created via the
   * Java provider mechanism. This is because the SecureRandom returned is an
   * extension of the regular SecureRandom class called FipsSecureRandom. In this
   * case an extension class was necessary as a NIST DRBG requires methods such as
   * FipsSecureRandom.reseed() which are not available on SecureRandom (in this
   * case even SecureRandom.setSeed() is not really a suitable candidate).
   *
   * @return a FIPS-compliant secure random number generator
   * @see "http://git.bouncycastle.org/fips-java/BCFipsIn100.pdf"
   */
  def getSecureRandom: FipsSecureRandom = {
    val entSource = new BasicEntropySourceProvider(new SecureRandom(), true)
    val fipsDrbgBuilder = FipsDRBG.SHA512_HMAC
      .fromEntropySource(entSource)
      .setSecurityStrength(256)
      .setEntropyBitsRequired(256)
      .setPersonalizationString(PERSONALIZATION_STRING)
    fipsDrbgBuilder.build(NONCE, true)
  }

  /**
   * Converts the given byte array representation of a public key into a PublicKey instance.
   *
   * @param keyBytes the byte array representation of the public key
   * @param keypairGeneratorAlgorithm the algorithm to generate the key with
   *
   * @return the PublicKey instance
   *
   * @throws IllegalStateException if the encoded public key cannot be converted to a public key instance
   */
  def publicKeyFromBytes(keyBytes: Array[Byte], keypairGeneratorAlgorithm: String = KEYPAIR_GENERATOR_ALGORITHM): PublicKey = allCatch.either {
    val spec = new X509EncodedKeySpec(keyBytes)
    val keyFactory = KeyFactory.getInstance(keypairGeneratorAlgorithm, BOUNCY_CASTLE_FIPS_PROVIDER)
    keyFactory.generatePublic(spec)
  }.fold(e => throw new IllegalStateException("Could not convert encoded public key to a public key instance", e), identity)

  /**
   * Generates a signature for the given input using the specified private key.
   *
   * @param input the input data to sign
   * @param privateKey the privateKey to generate the signature with
   * @param signatureAlgorithm the algorithm to generate the signature with
   *
   * @return the generated signature as an array of bytes
   *
   * @throws IllegalStateException if any exception related to generating the signature occurs
   */
  def generateSignature(input: Array[Byte], privateKey: PrivateKey, signatureAlgorithm: String = SIGNATURE_ALGORITHM): Array[Byte] = allCatch.either {
    val signature = Signature.getInstance(signatureAlgorithm, BOUNCY_CASTLE_FIPS_PROVIDER)
    signature.initSign(privateKey)
    signature.update(input)
    signature.sign
  }.fold(e => throw new IllegalStateException("Unexpected error when generating a signature", e), identity)

  /**
   * Verifies the signature of the given input using the provided public key and encrypted signature.
   *
   * @param input the input data to verify the signature of
   * @param encSignature the encrypted signature to be verified
   * @param publicKey the public key of the player that signed this message
   * @param signatureAlgorithm the algorithm to verify the signature with
   *
   * @return true if the signature is valid, false otherwise
   *
   * @throws GeneralSecurityException if any security-related exceptions occur during the signature verification process
   */
  @throws[GeneralSecurityException]
  def verifySignature(input: Array[Byte], encSignature: Array[Byte], publicKey: PublicKey, signatureAlgorithm: String = SIGNATURE_ALGORITHM): Boolean = {
    val signature = Signature.getInstance(signatureAlgorithm, BOUNCY_CASTLE_FIPS_PROVIDER)
    signature.initVerify(publicKey)
    signature.update(input)
    signature.verify(encSignature)
  }

  def generateKeyPair(): KeyPair = {
    val keyPairGenerator = KeyPairGenerator.getInstance(KEYPAIR_GENERATOR_ALGORITHM, BOUNCY_CASTLE_FIPS_PROVIDER)
    keyPairGenerator.initialize(384, getSecureRandom)
    keyPairGenerator.generateKeyPair()
  }
}