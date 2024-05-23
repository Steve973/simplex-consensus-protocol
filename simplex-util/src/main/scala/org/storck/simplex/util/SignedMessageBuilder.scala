package org.storck.simplex.util

import org.storck.simplex.message.{ProtocolMessage, SignedMessage}
import org.storck.simplex.util.SimplexConstants.SIGNATURE_ALGORITHM

import java.security.PrivateKey
import scala.reflect.ClassTag

/**
 * A builder for the `SignedMessage` class.
 *
 * This builder allows for the step-by-step construction of a `SignedMessage` instance. It ensures that the
 * private key is used only for the signing process and is not stored in the `SignedMessage` instance.
 *
 * @tparam T The type of the content in the message. `T` must extend `PlayerNetworkMessage`.
 */
class SignedMessageBuilder[T <: ProtocolMessage : ClassTag] {
  private var content: Option[T] = None
  private var metadata: Map[String, Any] = Map.empty
  private var privateKey: Option[PrivateKey] = None

  /**
   * Sets the content of the message.
   *
   * @param content The content of the message.
   * @return The builder instance.
   */
  def content(content: T): SignedMessageBuilder[T] = {
    this.content = Some(content)
    this
  }

  /**
   * Sets the metadata of the message.
   *
   * @param metadata The metadata of the message.
   * @return The builder instance.
   */
  def metadataMap(metadata: Map[String, Any]): SignedMessageBuilder[T] = {
    this.metadata = this.metadata ++ metadata
    this
  }

  /**
   * Adds a key-value entry to the metadata of the message.
   *
   * @param key   The key of the metadata entry.
   * @param value The value of the metadata entry.
   * @return The builder instance.
   */
  def metadataEntry(key: String, value: Any): SignedMessageBuilder[T] = {
    this.metadata = this.metadata + (key -> value)
    this
  }

  /**
   * Sets the private key for signing the message.
   *
   * @param privateKey The private key for signing the message.
   * @return The builder instance.
   */
  def privateKey(privateKey: PrivateKey): SignedMessageBuilder[T] = {
    this.privateKey = Some(privateKey)
    this
  }

  /**
   * Builds the `SignedMessage` instance.
   *
   * @return The `SignedMessage` instance.
   */
  def build(): SignedMessage[T] = {
    require(content.isDefined, "Content must be defined")
    require(privateKey.isDefined, "Private key must be defined")
    val signature = sign(content.get, privateKey.get)
    SignedMessage(content.get, metadata, signature)
  }


  /**
   * Signs the content with the private key.
   *
   * @param content    The content of the message.
   * @param privateKey The private key for signing the message.
   * @return The signature of the message.
   */
  private def sign(content: T, privateKey: PrivateKey): Array[Byte] = {
    val contentBytes: Array[Byte] = MessageUtil.toBytes(content)
    CryptoUtil.generateSignature(contentBytes, privateKey, SIGNATURE_ALGORITHM)
  }
}