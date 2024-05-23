package org.storck.simplex.util

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.json.JsonMapper
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider

import java.nio.charset.StandardCharsets
import java.security.Provider

object SimplexConstants {

  /** A byte array used for personalization in creating the random number generator. */
  val PERSONALIZATION_STRING: Array[Byte] = "Abracadabra".getBytes(StandardCharsets.UTF_8)

  /** A "number used once" for the random number generator. Here, we use a string representation of the current system time, then convert to a byte array. */
  val NONCE: Array[Byte] = String.valueOf(System.currentTimeMillis).getBytes(StandardCharsets.UTF_8)

  /** Algorithm used for message digest computation. */
  val MESSAGE_DIGEST_ALGORITHM: String = "SHA3-512"

  /** The algorithm used for generating key pairs. */
  val KEYPAIR_GENERATOR_ALGORITHM: String = "EC"

  /**
   * Represents the algorithm used for generating digital signatures using ECDSA
   * (Elliptic Curve Digital Signature Algorithm).
   */
  val SIGNATURE_ALGORITHM = "SHA384withECDSA"

  /** Instance of the JsonMapper class, which is used to serialize and deserialize JSON data. */
  val JSON_MAPPER: JsonMapper = new JsonMapper(new JsonFactory)

  /** Instance of the BouncyCastleFipsProvider class used as a provider for cryptographic operations. */
  val BOUNCY_CASTLE_FIPS_PROVIDER: Provider = new BouncyCastleFipsProvider

}
