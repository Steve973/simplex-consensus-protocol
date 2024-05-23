package org.storck.simplex.actors.iteration

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import org.storck.simplex.actors.common.SimplexImplicits.given
import org.storck.simplex.actors.common.{ActorInteraction, SelfActorNameAware, WithServiceKey}
import org.storck.simplex.actors.iteration.IterationActor.ElectedLeaderId
import org.storck.simplex.actors.player.PlayerActor
import org.storck.simplex.util.CryptoUtil
import org.storck.simplex.util.SimplexConstants.MESSAGE_DIGEST_ALGORITHM

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

object IterationLeaderElectionActor extends ActorInteraction, SelfActorNameAware:
  sealed trait ElectIterationLeaderCommand
  final case class StartElection(iterationNumber: Int, replyTo: ActorRef[ElectedLeaderId]) extends ElectIterationLeaderCommand
  final case class ElectLeader(playerIds: Seq[String], replyTo: ActorRef[ElectedLeaderId]) extends ElectIterationLeaderCommand
  final case class ElectionResult(result: String, error: Some[Throwable], replyTo: ActorRef[ElectedLeaderId]) extends ElectIterationLeaderCommand

  def apply(iterationNumber: Int): Behavior[ElectIterationLeaderCommand] =
    Behaviors.setup { context =>
      electionBehavior(iterationNumber, context)
    }

  private def electionBehavior(iterationNumber: Int, context: ActorContext[ElectIterationLeaderCommand]): Behavior[ElectIterationLeaderCommand] =

    Behaviors.receiveMessage {
      case StartElection(iterationNumber, replyTo) =>
        context.log.debug(s"Starting election for iteration $iterationNumber")
        val message: ActorRef[Seq[String]] => PlayerActor.GetAllPlayerIds = { replyTo =>
          PlayerActor.GetAllPlayerIds(replyTo)
        }
        val successToCommand: Seq[String] => ElectLeader = { playerIds =>
          ElectLeader(playerIds, replyTo)
        }
        val failureToCommand: Throwable => ElectionResult = { exception =>
          ElectionResult(result = null, Some(exception), replyTo)
        }
        askActor(context, WithServiceKey[PlayerActor.PlayerCommand](PlayerActor.generateServiceKey()), message, successToCommand, failureToCommand)
        Behaviors.same

      case ElectLeader(playerIds, replyTo) =>
        val hash = CryptoUtil.computeBytesHash(ByteBuffer.allocate(4).putInt(iterationNumber).array(), MESSAGE_DIGEST_ALGORITHM)
        val hashInt = BigInt(1, hash.getBytes(StandardCharsets.UTF_8))
        val electedLeaderId = playerIds.apply(hashInt.mod(playerIds.size).toInt)
        context.self ! ElectionResult(result = electedLeaderId, Some(null), replyTo)
        Behaviors.same

      case ElectionResult(result, Some(error), replyTo) =>
        if (error != null) {
          context.log.error(s"Error when attempting to elect the iteration leader: ${error.getMessage}")
        } else {
          context.log.debug(s"Election result -- leader ID: $result")
        }
        replyTo ! ElectedLeaderId(result)
        Behaviors.stopped
    }
