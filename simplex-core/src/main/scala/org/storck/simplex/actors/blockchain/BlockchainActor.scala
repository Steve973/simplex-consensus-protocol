package org.storck.simplex.actors.blockchain

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import org.storck.simplex.actors.common.SelfActorNameAware
import org.storck.simplex.message.{BlockNotarized, BlockchainNotarized}
import org.storck.simplex.util.BlockchainUtil

/**
 * This is an actor that creates and maintains the state of the notarized
 * blockchain.
 */
object BlockchainActor extends SelfActorNameAware:

  sealed trait BlockchainCommand

  case class GetBlockchain(replyTo: ActorRef[BlockchainNotarized]) extends BlockchainCommand

  /**
   * Command to add a list of one or more transactions to the "queue".
   *
   * @param transactions The transaction to add
   */
  case class AddTransactions(transactions: Seq[Any]) extends BlockchainCommand

  /**
   * Command to dequeue transactions.  They will be moved to [[#pendingTransactions]] until a
   * [[FinalizeIteration]] command is received for the iteration number, or it is determined
   * that the transactions were not successfully proposed as a new block.
   *
   * @param iterationNumber The iteration number for which to dequeue transactions
   * @param replyTo         The actor to which to send the dequeued transactions
   */
  case class DequeueTransactions(iterationNumber: Int, replyTo: ActorRef[Seq[Any]]) extends BlockchainCommand

  /**
   * Command to reset the pending transactions for a given iteration number.  If the iteration
   * where the transactions were proposed results in votes for the dummy block, then the
   * pending transactions will be reset.
   * TODO: verify that this is the entirety of the logic for resetting pending transactions.
   *
   * @param iterationNumber The iteration number for which to reset the pending transactions
   */
  case class ResetPendingTransactions(iterationNumber: Int) extends BlockchainCommand

  case class FinalizeIteration(iterationNumber: Int, notarizedBlock: BlockNotarized) extends BlockchainCommand

  def apply()
           (using ActorSystem[?]): Behavior[BlockchainCommand] =
    Behaviors.setup[BlockchainCommand] { context =>
      registerServiceKey[BlockchainCommand](generateServiceKey[BlockchainCommand](), context.self)
      blockchainBehavior(BlockchainNotarized(Seq(new BlockNotarized(BlockchainUtil.createGenesisBlock(), Seq.empty))), Seq.empty, Map.empty, context)
    }

  private def blockchainBehavior(blockchain: BlockchainNotarized,
                                 transactions: Seq[Any],
                                 pendingTransactions: Map[Int, Seq[Any]],
                                 context: ActorContext[BlockchainCommand]): Behavior[BlockchainCommand] =
    Behaviors.receiveMessage {
      case GetBlockchain(replyTo) =>
        context.log.debug("Sending blockchain to the requester")
        replyTo ! blockchain
        Behaviors.same

      case AddTransactions(newTransactions) =>
        context.log.debug("Adding transactions to the blockchain")
        blockchainBehavior(blockchain, transactions ++ newTransactions, pendingTransactions, context)

      case DequeueTransactions(iterationNumber, replyTo) =>
        context.log.debug("Dequeueing transactions")
        replyTo ! transactions
        blockchainBehavior(blockchain, Seq.empty, pendingTransactions + (iterationNumber -> transactions), context)

      case ResetPendingTransactions(iterationNumber) =>
        context.log.debug(s"Resetting pending transactions for iteration: $iterationNumber")
        blockchainBehavior(blockchain, pendingTransactions.get(iterationNumber).toSeq.flatten ++ transactions, pendingTransactions - iterationNumber, context)

      case FinalizeIteration(iterationNumber, notarizedBlock) =>
        context.log.debug(s"Finalizing iteration: $iterationNumber")
        blockchainBehavior(blockchain.copy(blocks = blockchain.blocks :+ notarizedBlock), transactions, pendingTransactions - iterationNumber, context)
    }
