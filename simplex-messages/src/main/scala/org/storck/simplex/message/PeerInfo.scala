package org.storck.simplex.message

/**
 * Peer information for messages.
 *
 * @param playerId  The ID of the player sending the ping.
 * @param timestamp The time when the ping was sent.
 * @param peerId the player peer ID
 * @param publicKeyBytes the player public key encoded in bytes
 */
case class PeerInfo(playerId: String, timestamp: Long, peerId: String, publicKeyBytes: Array[Byte]) extends ProtocolNetworkMessage

object PeerInfo {

  /**
   * Create the peer info.
   *
   * @param playerId  The ID of the player sending the ping.
   * @param timestamp The time when the ping was sent.
   * @param peerId the id of the peer
   * @param publicKeyBytes the public key of the peer encoded in bytes
   */
  def apply(playerId: String, timestamp: Long, peerId: String, publicKeyBytes: Array[Byte]): PeerInfo =
    new PeerInfo(playerId, timestamp, peerId, publicKeyBytes.clone())
}