package org.storck.simplex.actors.net

import akka.actor.typed.receptionist.ServiceKey
import org.storck.simplex.actors.common.SelfActorNameAware
import org.storck.simplex.message.SignedMessage

object PeerNetworkActor extends SelfActorNameAware:
  val peerNetworkServiceKey: ServiceKey[PeerNetworkCommand] = ServiceKey[PeerNetworkCommand]("peer-network-service-key")
  sealed trait PeerNetworkCommand
  final case class BroadcastPlayerMessage(message: SignedMessage[?]) extends PeerNetworkCommand
  final case class BroadcastNetworkMessage(message: SignedMessage[?]) extends PeerNetworkCommand
