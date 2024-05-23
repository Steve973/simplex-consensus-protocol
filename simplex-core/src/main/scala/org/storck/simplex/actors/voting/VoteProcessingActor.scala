package org.storck.simplex.actors.voting

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import org.storck.simplex.actors.common.SimplexImplicits.given
import org.storck.simplex.actors.common.{ActorInteraction, SelfActorNameAware, WithServiceKey}
import org.storck.simplex.actors.player.PlayerActor
import org.storck.simplex.actors.voting.VoteActor.VoteValidationResult
import org.storck.simplex.message.Vote
import org.storck.simplex.util.VoteUtil

import java.security.PublicKey
import scala.util.{Failure, Success}

object VoteProcessingActor extends ActorInteraction, SelfActorNameAware:
  sealed trait VoteProcessingCommand

  final case class ProcessVote(vote: Vote, replyTo: ActorRef[VoteValidationResult]) extends VoteProcessingCommand

  final case class PerformVoteValidation(vote: Vote, playerPublicKeyRegistry: Map[String, PublicKey], replyTo: ActorRef[VoteValidationResult]) extends VoteProcessingCommand

  final case class PublicKeyRetrieved(vote: Vote, playerIds: Seq[String], publicKey: Option[PublicKey], replyTo: ActorRef[VoteValidationResult]) extends VoteProcessingCommand

  final case class ValidationComplete(result: Boolean, vote: Vote, error: Option[Throwable], replyTo: ActorRef[VoteValidationResult]) extends VoteProcessingCommand

  def apply(iterationNumber: Int, proposalId: String): Behavior[VoteProcessingCommand] =
    Behaviors.setup { context =>
      voteProcessingBehavior(iterationNumber, proposalId, context)
    }

  private def voteProcessingBehavior(iterationNumber: Int, proposalId: String, context: ActorContext[VoteProcessingCommand]): Behavior[VoteProcessingCommand] =
    Behaviors.receiveMessage {
      case ProcessVote(vote, replyTo) =>
        context.log.debug(s"Processing vote ${vote}")
        val message: ActorRef[Map[String, PublicKey]] => PlayerActor.GetPlayerPublicKeyRegistry = { replyTo =>
          PlayerActor.GetPlayerPublicKeyRegistry(replyTo)
        }
        val successToCommand: Map[String, PublicKey] => PerformVoteValidation = { playerPublicKeyRegistry =>
          PerformVoteValidation(vote, playerPublicKeyRegistry, replyTo)
        }
        val failureToCommand: Throwable => ValidationComplete = { exception =>
          ValidationComplete(result = false, vote, Some(exception), replyTo)
        }
        askActor(context, WithServiceKey[PlayerActor.PlayerCommand](PlayerActor.generateServiceKey()), message, successToCommand, failureToCommand)
        Behaviors.same

      case PerformVoteValidation(vote, playerPublicKeyRegistry, replyTo) =>
        context.log.debug("Performing vote validation")
        val publicKeyOption = playerPublicKeyRegistry.get(vote.playerId)
        context.self ! PublicKeyRetrieved(vote, playerPublicKeyRegistry.keys.toSeq, publicKeyOption, replyTo)
        Behaviors.same

      case PublicKeyRetrieved(vote, playerIds, Some(publicKey), replyTo) =>
        VoteUtil.validateVote(iterationNumber, proposalId, playerIds, vote, publicKey) match {
          case Success(true) =>
            context.self ! ValidationComplete(result = true, vote, Some(null), replyTo)
          case Success(false) | Failure(_) =>
            context.self ! ValidationComplete(result = false, vote, Some(null), replyTo)
        }
        Behaviors.same

      case ValidationComplete(result, vote, Some(error), replyTo) =>
        if (error != null) 
          context.log.error(s"Vote validation failed with exception: ${error.getMessage}")
        else 
          context.log.error(s"Vote validation failed")
        replyTo ! VoteValidationResult(result, vote)
        Behaviors.stopped

      case unexpected => Behaviors.unhandled
    }
