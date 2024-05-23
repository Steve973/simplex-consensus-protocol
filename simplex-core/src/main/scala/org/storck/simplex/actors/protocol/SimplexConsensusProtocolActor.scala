package org.storck.simplex.actors.protocol

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Scheduler}
import akka.util.Timeout
import org.storck.simplex.actors.blockchain.BlockchainActor
import org.storck.simplex.actors.common.SimplexImplicits
import org.storck.simplex.actors.common.SimplexImplicits.given
import org.storck.simplex.actors.player.PlayerActor
import org.storck.simplex.actors.protocol.SimplexConsensusProtocolActor.{SimplexConsensusProtocolCommand, Start}
import org.storck.simplex.actors.iteration.IterationActor
import org.storck.simplex.message.{ProtocolIterationMessage, SignedMessage}
import org.storck.simplex.model.exception.NoSuchActorException
import org.storck.simplex.util.CryptoUtil
import org.storck.simplex.actors.common.SimplexImplicits.given

import java.security.KeyPair
import java.util.UUID
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt

object SimplexConsensusProtocolActor:

  sealed trait SimplexConsensusProtocolCommand

  final case class Start(networkDeltaSeconds: Int) extends SimplexConsensusProtocolCommand

  final case class UpdateNetworkDeltaSeconds(networkDeltaSeconds: Int) extends SimplexConsensusProtocolCommand

  final case class ProcessPlayerMessage(playerMessage: SignedMessage[? <: ProtocolIterationMessage]) extends SimplexConsensusProtocolCommand

  private final case class IterationTimeout(iterationNumber: Int) extends SimplexConsensusProtocolCommand

  private final case class BeginNextIteration(playerId: String,
                                        actorRefs: Map[String, ActorRef[?]],
                                        networkDeltaSeconds: Int,
                                        iterationNumber: Int,
                                        context: ActorContext[SimplexConsensusProtocolCommand]) extends SimplexConsensusProtocolCommand

  def apply(): Behavior[SimplexConsensusProtocolCommand] =
    Behaviors.setup { context =>
      val playerId: String = UUID.randomUUID().toString
      val blockchainActorName = BlockchainActor.createActorName()
      val blockchainActor = context.spawn(BlockchainActor(), blockchainActorName)
      val playerActorName = PlayerActor.createActorName()
      val playerActor = context.spawn(PlayerActor(), playerActorName)
      val keyPair = CryptoUtil.generateKeyPair()
      val actorRefs = Map[String, ActorRef[?]](
        blockchainActorName -> blockchainActor,
        playerActorName -> playerActor)
      initialBehavior(playerId, keyPair, actorRefs, context)
    }

  private def initialBehavior(playerId: String,
                              keyPair: KeyPair,
                              actorRefs: Map[String, ActorRef[?]],
                              context: ActorContext[SimplexConsensusProtocolCommand])
                             (using ActorSystem[?], Timeout, Scheduler, ExecutionContextExecutor): Behavior[SimplexConsensusProtocolCommand] =
    Behaviors.receiveMessage {
      case Start(networkDeltaSeconds) =>
        context.log.info("Starting Simplex Consensus Protocol")
        protocolBehavior(playerId, keyPair, actorRefs, networkDeltaSeconds, 1, context)
      case unexpected =>
        context.log.warn(s"Received unexpected message of type: ${unexpected.getClass.getSimpleName}")
        Behaviors.same
    }

  private def protocolBehavior(playerId: String,
                               keyPair: KeyPair,
                               actorRefs: Map[String, ActorRef[?]],
                               networkDeltaSeconds: Int,
                               iterationNumber: Int,
                               context: ActorContext[SimplexConsensusProtocolCommand])
                              (using ActorSystem[?], Timeout, Scheduler, ExecutionContextExecutor): Behavior[SimplexConsensusProtocolCommand] =
    context.log.info(s"Beginning iteration $iterationNumber")
    // This starts with the documented "backup step" of the protocol, where the iteration can last a maximum of
    // 3 * networkDeltaSeconds. If the iteration does not complete within this time, the player sends everyone
    // a vote for the dummy block.
    // TODO: Need to make sure that the scheduler does not perform this timeout step if the iteration is successfully finalized
    scheduler.scheduleOnce((networkDeltaSeconds * 3).seconds, () => {
      context.self ! IterationTimeout(iterationNumber)
      context.self ! BeginNextIteration(playerId, actorRefs, networkDeltaSeconds, iterationNumber + 1, context)
    })
    Behaviors.receiveMessage {
      case ProcessPlayerMessage(playerMessage) =>
        context.log.info(s"Received player message: $playerMessage")
        val messageProcessor = context.spawnAnonymous(ProtocolMessageProcessingActor(iterationNumber, actorRefs, playerId, keyPair))
        messageProcessor ! ProtocolMessageProcessingActor.ProcessSignedPlayerMessage(playerMessage)
        Behaviors.same

      case IterationTimeout(iterationNumber) =>
        context.log.info(s"Iteration $iterationNumber timed out")
        def iterationActorName = IterationActor.createActorName(Some(iterationNumber))
        def iterationActor: ActorRef[IterationActor.IterationCommand] = actorRefs.get(iterationActorName) match {
          case Some(actorRef) => actorRef.asInstanceOf[ActorRef[IterationActor.IterationCommand]]
          case None => throw new NoSuchActorException("Iteration actor not found")
        }
        iterationActor ! IterationActor.StopIteration(timedOut = true)
        protocolBehavior(playerId, keyPair, actorRefs - iterationActorName, networkDeltaSeconds, iterationNumber, context)

      case BeginNextIteration(playerId, actorRefs, networkDeltaSeconds, iterationNumber, context) =>
        val blockchainActorName = BlockchainActor.createActorName()
        val playerActorName = PlayerActor.createActorName()
        val nextIterationNumber = iterationNumber + 1
        val iterationActorName = IterationActor.createActorName(Some(nextIterationNumber))
        val iterationActor = context.spawn(IterationActor(nextIterationNumber, playerId, keyPair), iterationActorName)
        val newActorRegistry = Map[String, ActorRef[?]](
          blockchainActorName -> actorRefs(blockchainActorName),
          playerActorName -> actorRefs(playerActorName),
          iterationActorName -> iterationActor)
        protocolBehavior(playerId, keyPair, newActorRegistry, networkDeltaSeconds, nextIterationNumber, context)

      case unexpected =>
        context.log.warn(s"Received unexpected message of type: ${unexpected.getClass.getSimpleName}")
        Behaviors.same
    }

object Main:
  def main(args: Array[String]): Unit =
    val system: ActorSystem[SimplexConsensusProtocolCommand] = ActorSystem(SimplexConsensusProtocolActor(), "SimplexConsensusProtocolActorSystem")
    SimplexImplicits.setupGivenInstances(system)
    val networkDeltaSeconds = if (args.length > 0) args(0).toInt else 1
    system ! Start(networkDeltaSeconds)
