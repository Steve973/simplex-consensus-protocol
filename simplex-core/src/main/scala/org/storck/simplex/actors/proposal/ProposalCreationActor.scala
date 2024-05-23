package org.storck.simplex.actors.proposal

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import org.storck.simplex.actors.blockchain.BlockchainActor
import org.storck.simplex.actors.blockchain.BlockchainActor.BlockchainCommand
import org.storck.simplex.actors.common.SimplexImplicits.given
import org.storck.simplex.actors.common.{ActorInteraction, WithServiceKey}
import org.storck.simplex.actors.net.PeerNetworkActor
import org.storck.simplex.actors.net.PeerNetworkActor.PeerNetworkCommand
import org.storck.simplex.message.{Block, BlockchainNotarized, Proposal}
import org.storck.simplex.util.SimplexConstants.MESSAGE_DIGEST_ALGORITHM
import org.storck.simplex.util.{CryptoUtil, SignedMessageBuilder}

import java.security.PrivateKey

/**
 * The ProposalCreationActor is responsible for creating proposals.
 * It receives a CreateProposal command and creates the proposal.
 */
object ProposalCreationActor extends ActorInteraction:

  /**
   * Messages for ProposalCreationActor.
   */
  sealed trait ProposalCreationCommand

  /**
   * Command to start the proposal creation process.
   *
   * @param iterationNumber The iteration number
   */
  final case class StartProposalCreation(iterationNumber: Int, transactions: Seq[Any]) extends ProposalCreationCommand

  /**
   * Command to create a proposal.
   *
   * @param transactions The transactions
   * @param blockchain The notarized blocks
   * @param iterationNumber The iteration number
   */
  final case class CreateProposal(transactions: Seq[Any], blockchain: BlockchainNotarized, iterationNumber: Int) extends ProposalCreationCommand
  
  /**
   * Command to handle a failure to obtain the blockchain.
   *
   * @param exception The exception
   */
  final case class FailureToObtainBlockchain(exception: Throwable) extends ProposalCreationCommand

  /**
   * Command to sign a proposal.
   *
   * @param proposal The proposal
   */
  final case class SignProposal(proposal: Proposal) extends ProposalCreationCommand

  /**
   * Command to broadcast a proposal.
   *
   * @param proposal The proposal
   */
  final case class BroadcastProposal(proposal: Proposal) extends ProposalCreationCommand

  /**
   * Creates a new ProposalCreationActor behavior.
   *
   * @param localPlayerId The local player id
   * @param privateKey The private key for signing
   * @return The behavior of the ProposalCreationActor.
   */
  def apply(localPlayerId: String, privateKey: PrivateKey): Behavior[ProposalCreationCommand] =
    Behaviors.setup { context =>
      proposalCreationBehavior(context, localPlayerId, privateKey)
    }

  /**
   * The behavior of the ProposalCreationActor.
   *
   * @param context The actor context
   * @param localPlayerId The local player id
   * @param privateKey The private key for signing
   * @return The behavior of the ProposalCreationActor.
   */
  private def proposalCreationBehavior(context: ActorContext[ProposalCreationCommand], localPlayerId: String, privateKey: PrivateKey): Behavior[ProposalCreationCommand] =

    Behaviors.receiveMessage {
      case StartProposalCreation(iterationNumber, transactions) =>
        val message: ActorRef[BlockchainNotarized] => BlockchainActor.GetBlockchain = { replyTo =>
          BlockchainActor.GetBlockchain(replyTo)
        }
        val successToCommand: BlockchainNotarized => CreateProposal = { blockchain =>
          CreateProposal(transactions, blockchain, iterationNumber)
        }
        val failureToCommand: Throwable => FailureToObtainBlockchain = { exception =>
          FailureToObtainBlockchain(exception)
        }
        askActor(context, WithServiceKey[BlockchainActor.BlockchainCommand](BlockchainActor.generateServiceKey[BlockchainCommand]()), message, successToCommand, failureToCommand)
        Behaviors.same

      case CreateProposal(transactions, blockchain, iterationNumber) =>
        val currentBlockchainSize = blockchain.blocks.size
        val parentHash = CryptoUtil.computeBlockHash(blockchain.blocks.last.block, MESSAGE_DIGEST_ALGORITHM)
        val proposal = Proposal(localPlayerId, iterationNumber, new Block(currentBlockchainSize + 1, parentHash, transactions), blockchain)
        context.self ! SignProposal(proposal)
        Behaviors.same

      case SignProposal(proposal) =>
        context.self ! BroadcastProposal(proposal)
        Behaviors.same

      case BroadcastProposal(proposal) =>
        val message: ActorRef[PeerNetworkCommand] => PeerNetworkActor.BroadcastPlayerMessage = { _ =>
          PeerNetworkActor.BroadcastPlayerMessage(
            new SignedMessageBuilder[Proposal]
              .content(proposal)
              .privateKey(privateKey)
              .build())
        }
        val failureToCommand: Throwable => Unit = { exception =>
          context.log.error("Failed to obtain actor reference: ", exception)
        }
        tellActor(context, WithServiceKey[PeerNetworkCommand](PeerNetworkActor.peerNetworkServiceKey), message, failureToCommand)
        Behaviors.stopped
        
      case FailureToObtainBlockchain(exception) =>
        context.log.error("Failed to obtain blockchain, so new proposal cannot be created: ", exception)
        Behaviors.stopped
    }
