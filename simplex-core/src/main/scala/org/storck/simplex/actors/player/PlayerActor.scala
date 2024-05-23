package org.storck.simplex.actors.player

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import org.storck.simplex.actors.common.SelfActorNameAware

import java.security.PublicKey

object PlayerActor extends SelfActorNameAware:

  sealed trait PlayerCommand

  final case class AddPlayer(playerId: String, publicKey: PublicKey) extends PlayerCommand
  final case class RemovePlayer(playerId: String) extends PlayerCommand
  final case class GetPlayerPublicKey(playerId: String, replyTo: ActorRef[Option[PublicKey]]) extends PlayerCommand
  final case class GetAllPlayerIds(replyTo: ActorRef[Seq[String]]) extends PlayerCommand
  final case class GetPlayerPublicKeyRegistry(replyTo: ActorRef[Map[String, PublicKey]]) extends PlayerCommand

  def apply()
           (using ActorSystem[?]): Behavior[PlayerCommand] =
    Behaviors.setup[PlayerCommand] { context =>
      registerServiceKey[PlayerCommand](generateServiceKey[PlayerCommand](), context.self)
      playerBehavior(Map.empty, context)
    }

  private def playerBehavior(state: Map[String, PublicKey], context: ActorContext[PlayerCommand]): Behavior[PlayerCommand] =
    Behaviors.receiveMessage {
      case AddPlayer(playerId, publicKey) =>
        playerBehavior(state + (playerId -> publicKey), context)

      case RemovePlayer(playerId) =>
        playerBehavior(state - playerId, context)

      case GetPlayerPublicKey(playerId, replyTo) =>
        replyTo ! state.get(playerId)
        Behaviors.same

      case GetAllPlayerIds(replyTo) =>
        replyTo ! state.keys.toSeq
        Behaviors.same

      case GetPlayerPublicKeyRegistry(replyTo) =>
        replyTo ! state
        Behaviors.same
    }
