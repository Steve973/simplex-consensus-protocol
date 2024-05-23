package org.storck.simplex.message

import scala.reflect.ClassTag

/**
 * A trait representing a common base for player and network messages.
 */
trait ProtocolMessage {
  /**
   * The playerId of the player that sent the message.
   */
  def playerId: String
}

/**
 * A trait representing a network message. A network message is a message that is sent to another player actor,
 * and it contains the playerId that sent the message, and the timestamp.
 */
trait ProtocolNetworkMessage extends ProtocolMessage {
  /**
   * The timestamp.
   */
  val timestamp: Long
}

/**
 * A trait representing a player message. A player message is a message that is sent to another player actor,
 * and it contains the playerId that sent the message, and the iteration to which the message applies.
 */
trait ProtocolIterationMessage extends ProtocolMessage {
  /**
   * The iteration number to which the message applies, if applicable.
   */
  val iterationNumber: Int
}

/**
 * A case class representing a signed message. A signed message is a message that is signed by a player actor,
 * and it contains the content of the message, metadata, and the signature.
 *
 * @constructor Creates a new `SignedMessage` with the specified message type, metadata, content, and signature.
 */
final case class SignedMessage[T <: ProtocolMessage : ClassTag] private(
                                                                               content: T,
                                                                               metadata: Map[String, Any],
                                                                               signature: Array[Byte]) {

  /**
   * The class of the content of the message.
   */
  val contentClass: Class[?] = implicitly[ClassTag[T]].runtimeClass

  /**
   * A convenience method to directly access the `playerId` of the `PlayerNetworkMessage` wrapped in the `SignedMessage`.
   *
   * @return The `playerId` of the `PlayerNetworkMessage` wrapped in the `SignedMessage`.
   */
  def playerId: String = content.playerId
}

object SignedMessage {
  def apply[T <: ProtocolMessage : ClassTag](
                                                   content: T,
                                                   metadata: Map[String, Any],
                                                   signature: Array[Byte]): SignedMessage[T] = {
    new SignedMessage(content, metadata, signature)
  }
}
