package org.storck.simplex.actors.voting

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Scheduler}
import akka.util.Timeout
import org.storck.simplex.actors.common.{ActorInteraction, SelfActorNameAware, WithServiceKey}
import org.storck.simplex.actors.player.PlayerActor
import org.storck.simplex.actors.player.PlayerActor.PlayerCommand

import scala.concurrent.ExecutionContextExecutor

object QuorumActor extends ActorInteraction, SelfActorNameAware:

  sealed trait QuorumCommand
  final case class EvaluateQuorum(votes: Seq[String]) extends QuorumCommand
  final case class CheckQuorumReached(votes: Seq[String], playerIds: Seq[String]) extends QuorumCommand
  final case class QuorumReached(iterationNumber: Int) extends QuorumCommand
  final case class FailureToObtainPlayerIds(exception: Throwable) extends QuorumCommand

  def apply()
           (using ActorSystem[?], Timeout, Scheduler, ExecutionContextExecutor): Behavior[QuorumCommand] =
    quorumBehavior()

  private def quorumBehavior()
                            (using ActorSystem[?], Timeout, Scheduler, ExecutionContextExecutor): Behavior[QuorumCommand] =
    Behaviors.receive { (context, message) =>
      message match
        case EvaluateQuorum(votes) =>
          val serviceKey = PlayerActor.generateServiceKey[PlayerCommand]()
          val message: ActorRef[Seq[String]] => PlayerActor.GetAllPlayerIds = { replyTo =>
            PlayerActor.GetAllPlayerIds(replyTo)
          }
          val successToCommand: Seq[String] => CheckQuorumReached = { playerIds =>
            CheckQuorumReached(votes, playerIds)
          }
          val failureToCommand: Throwable => FailureToObtainPlayerIds = { exception =>
            FailureToObtainPlayerIds(exception)
          }
          askActor(context, WithServiceKey[PlayerActor.PlayerCommand](serviceKey), message, successToCommand, failureToCommand)
          Behaviors.stopped

        case CheckQuorumReached(votes, playerIds) =>
          if (votes.count(playerIds.contains) >= 2 * playerIds.size / 3) {
            // TODO: notify (probably) the iteration actor that quorum has been reached
          }
          Behaviors.stopped

        case FailureToObtainPlayerIds(exception) =>
          context.log.error(s"Quorum evaluation failed due to an error: ${exception.getMessage}")
          Behaviors.stopped

        case unexpected => Behaviors.unhandled
    }
