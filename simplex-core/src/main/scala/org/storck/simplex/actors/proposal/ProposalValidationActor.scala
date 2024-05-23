package org.storck.simplex.actors.proposal

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import org.storck.simplex.actors.blockchain.BlockchainActor
import org.storck.simplex.actors.blockchain.BlockchainActor.BlockchainCommand
import org.storck.simplex.actors.common.SimplexImplicits.given
import org.storck.simplex.actors.common.{ActorInteraction, WithServiceKey}
import org.storck.simplex.actors.player.PlayerActor
import org.storck.simplex.actors.proposal.ProposalActor.ProposalValidationResult
import org.storck.simplex.message.{BlockchainNotarized, Proposal}
import org.storck.simplex.util.SimplexConstants.MESSAGE_DIGEST_ALGORITHM
import org.storck.simplex.util.{ProposalUtil, SimplexConstants}

import java.security.PublicKey
import scala.util.{Failure, Success}

/**
 * The ProposalValidationActor is responsible for validating proposals.
 * It receives a ValidateProposal command and validates the proposal.
 */
object ProposalValidationActor extends ActorInteraction:

  /**
   * Messages for ProposalValidationActor.
   */
  sealed trait ProposalValidationCommand

  /**
   * Command to process a proposal.
   *
   * @param proposal The proposal to process
   */
  final case class ProcessProposal(proposal: Proposal, replyTo: ActorRef[ProposalValidationResult]) extends ProposalValidationCommand

  /**
   * Command to obtain the public key of the proposal creator.
   *
   * @param proposal The proposal to process
   * @param playerPublicKeyRegistry The public key registry of the players
   */
  final case class ObtainPublicKey(proposal: Proposal, playerPublicKeyRegistry: Map[String, PublicKey], replyTo: ActorRef[ProposalValidationResult]) extends ProposalValidationCommand

  /**
   * Command to obtain the current list of notarized blocks in the blockchain for proposal validation purposes.
   *
   * @param proposal The proposal to process
   * @param playerIds The player IDs
   * @param publicKey The public key of the player
   */
  final case class ObtainNotarizedBlocks(proposal: Proposal, playerIds: Seq[String], publicKey: Option[PublicKey], replyTo: ActorRef[ProposalValidationResult]) extends ProposalValidationCommand

  /**
   * Command to validate a proposal.
   *
   * @param proposal The proposal to process
   * @param notarizedBlocks The notarized blocks
   * @param playerIds The player IDs
   * @param publicKey The public key of the player
   */
  final case class PerformProposalValidation(proposal: Proposal, notarizedBlocks: BlockchainNotarized, playerIds: Seq[String], publicKey: Option[PublicKey], replyTo: ActorRef[ProposalValidationResult]) extends ProposalValidationCommand

  /**
   * Command to indicate that the validation of a proposal is complete.
   *
   * @param result The result of the validation
   * @param proposal The proposal that was validated
   * @param error The error that occurred during validation
   */
  final case class ValidationComplete(result: Boolean, proposal: Proposal, error: Option[Throwable], replyTo: ActorRef[ProposalValidationResult]) extends ProposalValidationCommand

  /**
   * Creates a new ProposalValidationActor behavior.
   *
   * @return The behavior of the ProposalValidationActor.
   */
  def apply(): Behavior[ProposalValidationCommand] =
    Behaviors.setup { context =>
      proposalValidationBehavior(context)
    }

  /**
   * The behavior of the ProposalValidationActor.
   *
   * @param context The actor context
   * @return The behavior of the ProposalValidationActor.
   */
  private def proposalValidationBehavior(context: ActorContext[ProposalValidationCommand]): Behavior[ProposalValidationCommand] =
    Behaviors.receiveMessage {
      /**
       * Receive the proposal for all validation steps.
       */
      case ProcessProposal(proposal, replyTo) =>
        context.log.debug("Received proposal for validation")
        val message: ActorRef[Map[String, PublicKey]] => PlayerActor.GetPlayerPublicKeyRegistry = { replyTo =>
          PlayerActor.GetPlayerPublicKeyRegistry(replyTo)
        }
        val successToCommand: Map[String, PublicKey] => ObtainPublicKey = { playerPublicKeyRegistry =>
          ObtainPublicKey(proposal, playerPublicKeyRegistry, replyTo)
        }
        val failureToCommand: Throwable => ValidationComplete = { exception =>
          ValidationComplete(result = false, proposal, Some(exception), replyTo)
        }
        askActor(context, WithServiceKey[PlayerActor.PlayerCommand](PlayerActor.generateServiceKey()), message, successToCommand, failureToCommand)
        Behaviors.stopped

      /**
       * Obtain the public key of the proposal creator.
       */
      case ObtainPublicKey(proposal, playerPublicKeyRegistry, replyTo) =>
        context.log.debug("Performing proposal validation")
        val publicKeyOption = playerPublicKeyRegistry.get(proposal.playerId)
        context.self ! ObtainNotarizedBlocks(proposal, playerPublicKeyRegistry.keys.toSeq, publicKeyOption, replyTo)
        Behaviors.same

      /**
       * Obtain the current list of notarized blocks in the blockchain for proposal validation purposes.
       */
      case ObtainNotarizedBlocks(proposal, playerIds, publicKeyOption, replyTo) =>
        val message: ActorRef[BlockchainNotarized] => BlockchainActor.GetBlockchain = { replyTo =>
          BlockchainActor.GetBlockchain(replyTo)
        }
        val successToCommand: BlockchainNotarized => PerformProposalValidation = { notarizedBlocks =>
          PerformProposalValidation(proposal, notarizedBlocks, playerIds, publicKeyOption, replyTo)
        }
        val failureToCommand: Throwable => ValidationComplete = { exception =>
          ValidationComplete(result = false, proposal, Some(exception), replyTo)
        }
        askActor(context, WithServiceKey[BlockchainActor.BlockchainCommand](BlockchainActor.generateServiceKey[BlockchainCommand]()), message, successToCommand, failureToCommand)
        Behaviors.same

      /**
       * Validate the proposal.
       */
      case PerformProposalValidation(proposal, notarizedBlocks, playerIds, Some(publicKey), replyTo) =>
        ProposalUtil.isValidProposal(proposal, notarizedBlocks.blocks, playerIds, MESSAGE_DIGEST_ALGORITHM) match {
          case Success(true) =>
            replyTo ! ProposalValidationResult(result = true, proposal)
          case Success(false) | Failure(_) =>
            replyTo ! ProposalValidationResult(result = false, proposal)
        }
        Behaviors.same

      case ValidationComplete(result, playerId, error, replyTo) =>
        context.log.debug(s"Validation of proposal for player $playerId completed with result: $result")
        replyTo ! ProposalValidationResult(result, playerId)
        Behaviors.stopped

      case unexpected =>
        context.log.error(s"Unexpected message received of type: ${unexpected.getClass}")
        Behaviors.unhandled
    }
