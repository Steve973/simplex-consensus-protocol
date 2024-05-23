package org.storck.simplex.actors.protocol

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, Scheduler}
import akka.util.Timeout
import org.storck.simplex.actors.blockchain.BlockchainActor
import org.storck.simplex.actors.common.{ActorInteraction, SelfActorNameAware, WithServiceKey}
import org.storck.simplex.actors.iteration.IterationActor
import org.storck.simplex.actors.player.PlayerActor
import org.storck.simplex.actors.voting.VoteActor
import org.storck.simplex.message.{Finalize, Proposal, ProtocolIterationMessage, SignedMessage, Transactions, Vote}
import org.storck.simplex.util.SimplexConstants.SIGNATURE_ALGORITHM
import org.storck.simplex.util.{CryptoUtil, MessageUtil}

import java.security.{KeyPair, PublicKey}
import scala.concurrent.ExecutionContextExecutor
import scala.util.Try

/**
 * Actor for processing protocol messages.
 */
object ProtocolMessageProcessingActor extends ActorInteraction, SelfActorNameAware:

  /**
   * Commands that this actor can handle.
   */
  sealed trait ProtocolMessageProcessorCommand

  /**
   * Command to process a signed player message.
   */
  case class ProcessSignedPlayerMessage(signedPlayerMessage: SignedMessage[? <: ProtocolIterationMessage]) extends ProtocolMessageProcessorCommand

  /**
   * Command to handle the result of a signature validation.
   */
  case class SignatureValidationResult(signedPlayerMessage: SignedMessage[? <: ProtocolIterationMessage], result: Boolean) extends ProtocolMessageProcessorCommand

  /**
   * Defines the behavior of this actor.
   */
  def apply(iterationNumber: Int, actorRefs: Map[String, ActorRef[?]], localPlayerId: String, keyPair: KeyPair)
           (using Timeout, Scheduler, ExecutionContextExecutor): Behavior[ProtocolMessageProcessorCommand] =
    Behaviors.setup { context =>
      Behaviors.receiveMessage {
        case ProcessSignedPlayerMessage(signedPlayerMessage) =>
          validateSignature(signedPlayerMessage, context)
          Behaviors.same
        case SignatureValidationResult(signedPlayerMessage, result) =>
          if result then
            dispatchMessage(signedPlayerMessage, actorRefs, iterationNumber, localPlayerId, keyPair, context)
          else
            context.log.warn(s"Signature validation failed for ${signedPlayerMessage.contentClass} message from player with ID: ${signedPlayerMessage.playerId}")
          Behaviors.stopped
      }
  }

  /**
   * Validates the signature of a signed player message.
   */
  def validateSignature(signedPlayerMessage: SignedMessage[? <: ProtocolIterationMessage], context: ActorContext[ProtocolMessageProcessorCommand])
                       (using Timeout, Scheduler, ExecutionContextExecutor): Unit =
    val message: ActorRef[Map[String, PublicKey]] => PlayerActor.GetPlayerPublicKeyRegistry = replyTo =>
      PlayerActor.GetPlayerPublicKeyRegistry(replyTo)

    val successToCommand: Map[String, PublicKey] => ProtocolMessageProcessorCommand = playerPublicKeyRegistry =>
      playerPublicKeyRegistry.get(signedPlayerMessage.content.playerId) match {
        case Some(publicKey) =>
          val messageBytes = MessageUtil.toBytes(signedPlayerMessage.content)
          val isValid = CryptoUtil.verifySignature(messageBytes, signedPlayerMessage.signature, publicKey, SIGNATURE_ALGORITHM)
          SignatureValidationResult(signedPlayerMessage, isValid)
        case None =>
          context.log.error(s"Public key not found for player ID: ${signedPlayerMessage.content.playerId}")
          SignatureValidationResult(signedPlayerMessage, result = false)
      }

    val failureToCommand: Throwable => ProtocolMessageProcessorCommand = _ =>
      SignatureValidationResult(signedPlayerMessage, result = false)

    Try(askActor(context, WithServiceKey[PlayerActor.PlayerCommand](PlayerActor.generateServiceKey()), message, successToCommand, failureToCommand))
      .recover {
        case exception: Throwable =>
          context.log.error(s"Failed to validate signature: ${exception.getMessage}")
          SignatureValidationResult(signedPlayerMessage, result = false)
      }

  /**
   * Dispatches a signed player message that has been verified.
   */
  def dispatchMessage(signedPlayerMessage: SignedMessage[? <: ProtocolIterationMessage],
                      actorRefs: Map[String, ActorRef[?]],
                      iterationNumber: Int,
                      localPlayerId: String,
                      keyPair: KeyPair,
                      context: ActorContext[ProtocolMessageProcessorCommand]): Unit =
    signedPlayerMessage.content match
      case transactions: Transactions =>
        val blockchainActorName = BlockchainActor.createActorName()
        actorRefs.get(blockchainActorName) match
          case Some(actorRef) =>
            context.log.info(s"Dispatching Transactions message")
            val blockchainActorRef = actorRef.asInstanceOf[ActorRef[BlockchainActor.BlockchainCommand]]
            blockchainActorRef ! BlockchainActor.AddTransactions(transactions.transactions)
          case None =>
            context.log.error(s"Could not find blockchain actor with name: $blockchainActorName")

      case vote: Vote =>
        val voteActorName = VoteActor.createActorName(Some(iterationNumber))
        actorRefs.get(voteActorName) match
          case Some(actorRef) =>
            context.log.info(s"Dispatching Vote message")
            val voteActorRef = actorRef.asInstanceOf[ActorRef[VoteActor.VoteCommand]]
            voteActorRef ! VoteActor.ProcessVoteCommand(vote)
          case None =>
            context.log.error(s"Could not find vote actor with name: $voteActorName")

      case proposal: Proposal =>
        val iterationActorName = IterationActor.createActorName(Some(iterationNumber))
        actorRefs.get(iterationActorName) match
          case Some(actorRef) =>
            context.log.info(s"Dispatching Proposal message")
            val iterationActorRef = actorRef.asInstanceOf[ActorRef[IterationActor.IterationCommand]]
            iterationActorRef ! IterationActor.ProcessProposal(proposal)
          case None =>
            context.log.error(s"Could not find iteration actor with name: $iterationActorName")

      case finalize: Finalize =>
        val iterationActorName = IterationActor.createActorName(Some(iterationNumber))
        actorRefs.get(iterationActorName) match
          case Some(actorRef) =>
            context.log.info(s"Dispatching Finalize message")
            val iterationActorRef = actorRef.asInstanceOf[ActorRef[IterationActor.IterationCommand]]
            iterationActorRef ! IterationActor.LogFinalizeReceipt(finalize)
          case None =>
            context.log.error(s"Could not find iteration actor with name: $iterationActorName")

      case unexpected =>
        context.log.info(s"Cannot dispatch unexpected message type: ${signedPlayerMessage.contentClass}")
