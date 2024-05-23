package org.storck.simplex.actors.iteration

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Scheduler}
import akka.util.Timeout
import org.storck.simplex.actors.blockchain.BlockchainActor
import org.storck.simplex.actors.blockchain.BlockchainActor.BlockchainCommand
import org.storck.simplex.actors.common.{ActorInteraction, SelfActorNameAware, WithActorRef, WithServiceKey}
import org.storck.simplex.actors.net.PeerNetworkActor
import org.storck.simplex.actors.net.PeerNetworkActor.PeerNetworkCommand
import org.storck.simplex.actors.player.PlayerActor
import org.storck.simplex.actors.player.PlayerActor.PlayerCommand
import org.storck.simplex.actors.proposal.ProposalActor
import org.storck.simplex.actors.voting.VoteActor
import org.storck.simplex.actors.voting.VoteActor.VoteCommand
import org.storck.simplex.message.*
import org.storck.simplex.util.BlockchainUtil.createDummyBlock
import org.storck.simplex.util.{CryptoUtil, SignedMessageBuilder}

import java.security.{KeyPair, PrivateKey}
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

/**
 * Handles the lifecycle of an iteration, including backup steps, timers, leader
 * proposal, voting, and transitioning to the next iteration.
 */
object IterationActor extends SelfActorNameAware, ActorInteraction:

  sealed trait IterationCommand

  case class ProposalVerified(proposal: Proposal) extends IterationCommand

  case class LogFinalizeReceipt(finalizeMessage: Finalize) extends IterationCommand

  case class ProcessProposal(proposal: Proposal) extends IterationCommand

  private case class FinalizeReceiptVerified(finalizeReceipt: String) extends IterationCommand

  private case class CheckFinalizeIteration(finalizeReceipts: Seq[String], playerIds: Seq[String]) extends IterationCommand
  
  private case class SendFinalizeMessage(iterationNumber: Int, notarizedBlock: BlockNotarized, context: ActorContext[IterationActor.IterationCommand]) extends IterationCommand

  private case class FailureToObtainPlayerIds(exception: Throwable) extends IterationCommand
  
  case class QuorumReached(localPlayerId: String, privateKey: PrivateKey, iterationNumber: Int) extends IterationCommand

  case class StopIteration(timedOut: Boolean) extends IterationCommand

  /**
   * Case class representing the message containing the elected leader ID.
   *
   * @param leaderId the ID of the elected leader
   */
  case class ElectedLeaderId(leaderId: String) extends IterationCommand

  /**
   * The behavior of the IterationActor.
   *
   * @param iterationNumber   the iteration number
   * @param localPlayerId     the ID of the local player
   * @param keyPair           the key pair for signing messages
   * @return the behavior of the IterationActor
   */
  def apply(iterationNumber: Int, localPlayerId: String, keyPair: KeyPair)
           (using ActorSystem[?], Timeout, Scheduler, ExecutionContextExecutor): Behavior[IterationCommand] =
    Behaviors.setup { context =>
      registerServiceKey[IterationCommand](generateServiceKey[IterationCommand](), context.self)
      val temporaryBehavior = Behaviors.receiveMessage[IterationCommand] {
        case ElectedLeaderId(leaderId) =>
          val proposalActorName = ProposalActor.createActorName(Some(iterationNumber))
          val proposalActor = context.spawn(ProposalActor(iterationNumber, localPlayerId, leaderId, keyPair.getPrivate, context.self), proposalActorName)
          val voteActorName = VoteActor.createActorName(Some(iterationNumber))
          iterationBehavior(iterationNumber, localPlayerId, leaderId, keyPair, createDummyBlock(0), Seq.empty, proposalActorName,
            proposalActor, voteActorName, context.system.ignoreRef, context)
        case unexpected =>
          Behaviors.unhandled
      }

      val electionActor = context.spawn(IterationLeaderElectionActor(iterationNumber), IterationLeaderElectionActor.createActorName(Some(iterationNumber)))
      electionActor ! IterationLeaderElectionActor.StartElection(iterationNumber, context.self)
      temporaryBehavior
    }

  /**
   * The final behavior of the IterationActor, handling stop iteration and finalize receipt messages.
   *
   * @param iterationNumber   the iteration number
   * @param localPlayerId     the ID of the local player
   * @param leaderId          the ID of the elected leader
   * @param keyPair           the key pair for signing messages
   * @param proposedBlock     the proposed block
   * @param finalizeReceipts  the sequence of finalize receipts
   * @param proposalActorName the name of the proposal actor
   * @param proposalActor     the proposal actor reference
   * @param voteActorName     the name of the vote actor
   * @param voteActor         the vote actor reference
   * @param context           the actor context
   * @return the final behavior of the IterationActor
   */
  private def iterationBehavior(iterationNumber: Int,
                                localPlayerId: String,
                                leaderId: String,
                                keyPair: KeyPair,
                                proposedBlock: Block,
                                finalizeReceipts: Seq[String],
                                proposalActorName: String,
                                proposalActor: ActorRef[ProposalActor.ProposalCommand],
                                voteActorName: String,
                                voteActor: ActorRef[VoteActor.VoteCommand],
                                context: ActorContext[IterationCommand])
                               (using ActorSystem[?], Timeout, Scheduler, ExecutionContextExecutor): Behavior[IterationCommand] =
    Behaviors.receiveMessage {
      case ProcessProposal(proposal) =>
        proposalActor ! ProposalActor.ProcessProposal(proposal)
        Behaviors.same

      case ProposalVerified(proposal) =>
        val proposedBlock = proposal.newBlock
        val proposalId = CryptoUtil.computeBlockHash(proposedBlock)
        val voteActor = context.spawn(VoteActor(iterationNumber, proposalId, localPlayerId, keyPair.getPrivate), voteActorName)
        iterationBehavior(iterationNumber, localPlayerId, leaderId, keyPair, proposedBlock, finalizeReceipts, proposalActorName,
          proposalActor, voteActorName, voteActor, context)

      case LogFinalizeReceipt(finalizeMessage) =>
        processFinalizeMessage(iterationNumber, finalizeMessage, context)
        Behaviors.same

      case FinalizeReceiptVerified(finalizeReceipt) =>
        val playerActorServiceKey = PlayerActor.generateServiceKey[PlayerCommand]()
        val message: ActorRef[Seq[String]] => PlayerActor.GetAllPlayerIds = { replyTo =>
          PlayerActor.GetAllPlayerIds(replyTo)
        }
        val successToCommand: Seq[String] => CheckFinalizeIteration = { playerIds =>
          CheckFinalizeIteration(finalizeReceipts, playerIds)
        }
        val failureToCommand: Throwable => FailureToObtainPlayerIds = { exception =>
          FailureToObtainPlayerIds(exception)
        }
        askActor(context, WithServiceKey[PlayerActor.PlayerCommand](playerActorServiceKey), message, successToCommand, failureToCommand)
        iterationBehavior(iterationNumber, localPlayerId, leaderId, keyPair, proposedBlock, finalizeReceipts :+ finalizeReceipt,
          proposalActorName, proposalActor, voteActorName, voteActor, context)

      case CheckFinalizeIteration(finalizeReceipts, playerIds) =>
        if (finalizeReceipts.count(playerIds.contains) >= 2 * playerIds.size / 3) {
          val message: ActorRef[Seq[Vote]] => VoteActor.GetVotes = { replyTo =>
            VoteActor.GetVotes(replyTo)
          }
          val successToCommand: Seq[Vote] => SendFinalizeMessage = { votes =>
            SendFinalizeMessage(iterationNumber, new BlockNotarized(proposedBlock, votes), context)
          }
          val failureToCommand: Throwable => FailureToObtainPlayerIds = { exception =>
            FailureToObtainPlayerIds(exception)
          }
          askActor(context, WithActorRef[VoteActor.GetVotes](voteActor), message, successToCommand, failureToCommand)
          Behaviors.stopped
        } else {
          Behaviors.same
        }

      case SendFinalizeMessage(iterationNumber, notarizedBlock, context) =>
        finalizeIteration(iterationNumber, notarizedBlock, context)
        Behaviors.stopped

      case QuorumReached(localPlayerId, privateKey, iterationNumber) =>
        quorumReached(localPlayerId, privateKey, iterationNumber, context)
        Behaviors.same

      case StopIteration(timedOut) =>
        if (timedOut) {
          // TODO: The vote actor would probably need to vote for the dummy block.
        }
        Behaviors.stopped

      case unexpected =>
        context.log.error(s"Unexpected message received of type: ${unexpected.getClass}")
        Behaviors.unhandled
    }

  /**
   * Processes the finalize message by verifying the signature.
   *
   * @param iterationNumber the iteration number
   * @param finalizeMessage the signed finalize message
   * @param context the actor context
   */
  private def processFinalizeMessage(iterationNumber: Int,
                                     finalizeMessage: Finalize,
                                     context: ActorContext[IterationCommand])
                                    (using Timeout, ExecutionContext, Scheduler): Unit =
    if (finalizeMessage.iterationNumber == iterationNumber) {
      context.self ! FinalizeReceiptVerified(finalizeMessage.playerId)
    }

  private def quorumReached(localPlayerId: String,
                                privateKey: PrivateKey,
                                iterationNumber: Int,
                                context: ActorContext[IterationCommand])
                               (using Timeout, ExecutionContextExecutor, Scheduler): Unit =
    val message: ActorRef[PeerNetworkCommand] => PeerNetworkActor.BroadcastPlayerMessage = { _ =>
      val finalize = new Finalize(localPlayerId, iterationNumber)
      PeerNetworkActor.BroadcastPlayerMessage(
        new SignedMessageBuilder[Finalize]
          .content(finalize)
          .privateKey(privateKey)
          .build())
    }
    val failureToCommand: Throwable => Unit = { exception =>
      context.log.error("Failed to obtain actor reference: ", exception)
    }
    tellActor(context, WithServiceKey[PeerNetworkActor.PeerNetworkCommand](PeerNetworkActor.peerNetworkServiceKey), message, failureToCommand)

  private def finalizeIteration(iterationNumber: Int,
                                notarizedBlock: BlockNotarized,
                                context: ActorContext[IterationCommand])
                               (using Timeout, ExecutionContextExecutor, Scheduler): Unit =
    val message: ActorRef[BlockchainCommand] => BlockchainActor.FinalizeIteration = { _ =>
      BlockchainActor.FinalizeIteration(iterationNumber, notarizedBlock)
    }
    val failureToCommand: Throwable => Unit = { exception =>
      context.log.error("Failed to obtain actor reference: ", exception)
    }
    tellActor(context, WithServiceKey[BlockchainActor.BlockchainCommand](BlockchainActor.generateServiceKey[BlockchainCommand]()), message, failureToCommand)
