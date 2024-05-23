package org.storck.simplex.actors.voting

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Scheduler}
import akka.util.Timeout
import org.storck.simplex.actors.common.{ActorInteraction, SelfActorNameAware, WithServiceKey}
import org.storck.simplex.actors.net.PeerNetworkActor
import org.storck.simplex.actors.net.PeerNetworkActor.PeerNetworkCommand
import org.storck.simplex.util.SignedMessageBuilder
import org.storck.simplex.message.Vote

import java.security.PrivateKey
import scala.concurrent.ExecutionContextExecutor

/**
 * Actor to handle voting process for a proposal.
 */
object VoteActor extends SelfActorNameAware, ActorInteraction:

  sealed trait VoteCommand

  final case class ProcessVoteCommand(vote: Vote) extends VoteCommand

  final case class VoteForDummyBlock(iterationNumber: Int) extends VoteCommand

  final case class VoteValidationResult(result: Boolean, vote: Vote) extends VoteCommand

  final case class GetVotes(replyTo: ActorRef[Seq[Vote]]) extends VoteCommand

  def apply(iterationNumber: Int, proposedBlockId: String, localPlayerId: String, privateKey: PrivateKey)
           (using ActorSystem[?], Timeout, Scheduler, ExecutionContextExecutor): Behavior[VoteCommand] =
    Behaviors.setup { context =>
      registerServiceKey[VoteCommand](generateServiceKey[VoteCommand](), context.self)
      voteBehavior(iterationNumber, proposedBlockId, localPlayerId, privateKey, Map.empty, context)
    }

  private def voteBehavior(iterationNumber: Int, proposedBlockId: String, localPlayerId: String, privateKey: PrivateKey, votes: Map[String, Vote], context: ActorContext[VoteCommand])
                          (using ActorSystem[?], Timeout, Scheduler, ExecutionContextExecutor): Behavior[VoteCommand] =
    Behaviors.receiveMessage[VoteCommand] {
      case ProcessVoteCommand(vote) =>
        if (votes.contains(vote.playerId)) 
          Behaviors.same
        else 
          val voteProcessingActor = context.spawn(VoteProcessingActor(iterationNumber, vote.blockHash), VoteProcessingActor.createActorName(Some(iterationNumber)))
          voteProcessingActor ! VoteProcessingActor.ProcessVote(vote, context.self)
          voteBehavior(iterationNumber, proposedBlockId, localPlayerId, privateKey, votes + (vote.playerId -> vote), context)

      case VoteValidationResult(result, vote) =>
        val updatedVotes = votes.updated(vote.playerId, vote)
        if (result)
          val quorumActor = context.spawn(QuorumActor(), QuorumActor.createActorName(Some(iterationNumber), true))
          quorumActor ! QuorumActor.EvaluateQuorum(updatedVotes.keys.toSeq)
        voteBehavior(iterationNumber, proposedBlockId, localPlayerId, privateKey, updatedVotes, context)

      case VoteForDummyBlock(iterationNumber) =>
        val message: ActorRef[PeerNetworkCommand] => PeerNetworkActor.BroadcastPlayerMessage = { _ =>
          val vote = new Vote(localPlayerId, iterationNumber, "")
          PeerNetworkActor.BroadcastPlayerMessage(
            new SignedMessageBuilder[Vote]
              .content(vote)
              .privateKey(privateKey)
              .build())
        }
        val failureToCommand: Throwable => Unit = { exception =>
          context.log.error("Failed to obtain actor reference: ", exception)
        }
        tellActor(context, WithServiceKey[PeerNetworkCommand](PeerNetworkActor.peerNetworkServiceKey), message, failureToCommand)
        Behaviors.same

      case GetVotes(replyTo) =>
        replyTo ! votes.values.filter(_.blockHash == proposedBlockId).toSeq
        Behaviors.same

      case unexpected => Behaviors.unhandled
    }
