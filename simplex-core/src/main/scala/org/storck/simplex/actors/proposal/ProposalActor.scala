package org.storck.simplex.actors.proposal

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import org.storck.simplex.actors.common.SelfActorNameAware
import org.storck.simplex.actors.iteration.IterationActor
import org.storck.simplex.actors.proposal.ProposalCreationActor.{ProposalCreationCommand, StartProposalCreation}
import org.storck.simplex.actors.proposal.ProposalValidationActor.ProposalValidationCommand
import org.storck.simplex.message.Proposal

import java.security.PrivateKey

/**
 * The ProposalActor is responsible for managing proposals. It maintains a queue of transactions,
 * and when it receives a ProposeNewBlock command, it creates a new block proposal using the transactions
 * in the queue and then clears the queue.
 */
object ProposalActor extends SelfActorNameAware:

  /**
   * Messages for ProposalActor.
   */
  sealed trait ProposalCommand

  /**
   * Command to process a proposal.
   */
  case class ProcessProposal(proposal: Proposal) extends ProposalCommand

  /**
   * Command to validate a proposal.
   */
  case class ProposalValidationResult(result: Boolean, proposal: Proposal) extends ProposalCommand

  /**
   * Creates a new ProposalActor behavior.
   *
   * @return The behavior of the ProposalActor.
   */
  def apply(iterationNumber: Int, localPlayerId: String, leaderId: String, privateKey: PrivateKey, parent: ActorRef[IterationActor.IterationCommand]): Behavior[ProposalCommand] =
    Behaviors.setup { context =>
      proposalActorBehavior(iterationNumber, localPlayerId, privateKey, parent, localPlayerId == leaderId)
    }

  /**
   * The behavior of the ProposalActor.
   *
   * @param iterationNumber The current iteration number.
   * @param localPlayerId   The ID of the local player.
   * @param privateKey      The private key of the local player.
   * @return The behavior of the ProposalActor.
   */
  private def proposalActorBehavior(iterationNumber: Int, localPlayerId: String, privateKey: PrivateKey, parent: ActorRef[IterationActor.IterationCommand], broadcastProposal: Boolean): Behavior[ProposalCommand] =
    Behaviors.receive { (context, message) =>
      if (broadcastProposal) {
        val proposalCreationActor: ActorRef[ProposalCreationCommand] = context.spawn(ProposalCreationActor(localPlayerId, privateKey), "proposalCreationActor")
        val transactions = List.empty // TODO: Get transactions from the queue
        proposalCreationActor ! StartProposalCreation(iterationNumber, transactions)
      }

      message match {
        /**
         * When the ProposalActor receives a ValidateProposal command, it delegates the validation to the ProposalValidationActor.
         */
        case ProcessProposal(proposal) =>
          val proposalValidationActor: ActorRef[ProposalValidationCommand] = context.spawn(ProposalValidationActor(), "proposalValidationActor")
          proposalValidationActor ! ProposalValidationActor.ProcessProposal(proposal, context.self)
          Behaviors.same

          /**
           * When the ProposalValidationActor returns a ProposalValidationResult we send it back to the parent IterationActor and this actor can stop.
           */
        case ProposalValidationResult(result, proposal) =>
          context.log.info(s"Proposal validation result: $result")
          parent ! IterationActor.ProposalVerified(proposal)
          Behaviors.stopped
      }
    }
