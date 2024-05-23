package org.storck.simplex.actors.net

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import org.storck.simplex.actors.net.PeerNetworkActor.BroadcastNetworkMessage
import org.storck.simplex.message.ProtocolNetworkMessage
import org.storck.simplex.util.SignedMessageBuilder

import java.security.PrivateKey
import scala.collection.immutable.Queue
import scala.concurrent.duration.*

/**
 * Actor for calculating network delta in a distributed system.
 */
object NetworkDeltaActor:

  /**
   * Sealed trait for all commands that NetworkDeltaActor can handle.
   */
  sealed trait NetworkDeltaActorCommand

  /**
   * Represents a ping message with a timestamp and the sender's ActorRef.
   *
   * @param playerId  The ID of the player sending the ping.
   * @param timestamp The time when the ping was sent.
   * @param sender    The ActorRef of the sender.
   */
  private case class NodePing(playerId: String, timestamp: Long, sender: ActorRef[Pong]) extends ProtocolNetworkMessage

  /**
   * Represents a ping message with a timestamp and the sender's ActorRef.
   *
   * @param timestamp The time when the ping was sent.
   * @param sender    The ActorRef of the sender.
   */
  private case class Ping(timestamp: Long, sender: ActorRef[Pong]) extends NetworkDeltaActorCommand

  /**
   * Represents a pong message with the original ping timestamp.
   *
   * @param originalTimestamp The timestamp from the original ping message.
   */
  private case class Pong(originalTimestamp: Long) extends NetworkDeltaActorCommand

  /**
   * Triggers the calculation of network delta.
   */
  private case object CalculateNetworkDelta extends NetworkDeltaActorCommand

  /**
   * Represents the command to send pings to other actors.
   */
  private case object SendPings extends NetworkDeltaActorCommand

  /**
   * Creates the behavior for the NetworkDeltaActor.
   *
   * @param nodeId           The ID of the node.
   * @param peerNetworkActor The network actor to broadcast ping messages to the other players/nodes.
   * @param privateKey       The private key to sign messages.
   * @param maxRttSamples    The maximum number of RTT samples to keep per peer.
   * @param outlierThreshold The threshold (in standard deviations) for outlier detection.
   * @param pingInterval     The interval between ping messages.
   * @return The behavior of the NetworkDeltaActor.
   */
  def apply(nodeId: String,
            peerNetworkActor: ActorRef[BroadcastNetworkMessage],
            privateKey: PrivateKey,
            maxRttSamples: Int = 10,
            outlierThreshold: Double = 3.0,
            pingInterval: FiniteDuration = 5.seconds): Behavior[NetworkDeltaActorCommand] =
    Behaviors.setup { context =>
      Behaviors.withTimers { timers =>
        timers.startTimerWithFixedDelay(SendPings, pingInterval)
        networkDeltaBehavior(nodeId, peerNetworkActor, privateKey, maxRttSamples, outlierThreshold, Map.empty, 0L, context, timers)
      }
    }

  /**
   * The main behavior of the NetworkDeltaActor.
   *
   * @param nodeId           The ID of the node.
   * @param peerNetworkActor The network actor to broadcast ping messages to the other players/nodes.
   * @param privateKey       The private key to sign messages.
   * @param maxRttSamples    The maximum number of RTT samples to keep per peer.
   * @param outlierThreshold The threshold for outlier detection.
   * @param rttData          The current RTT data for all peers.
   * @param currentDelta     The current calculated network delta.
   * @param context          The actor context.
   * @param timers           The timer scheduler for the actor.
   * @return The updated behavior of the NetworkDeltaActor.
   */
  private def networkDeltaBehavior(nodeId: String,
                                   peerNetworkActor: ActorRef[BroadcastNetworkMessage],
                                   privateKey: PrivateKey,
                                   maxRttSamples: Int,
                                   outlierThreshold: Double,
                                   rttData: Map[ActorRef[Pong], Queue[Long]],
                                   currentDelta: Long,
                                   context: ActorContext[NetworkDeltaActorCommand],
                                   timers: TimerScheduler[NetworkDeltaActorCommand]): Behavior[NetworkDeltaActorCommand] =
    Behaviors.receiveMessage {
      case Ping(timestamp, replyTo) =>
        replyTo ! Pong(timestamp)
        Behaviors.same

      case Pong(originalTimestamp) =>
        val rtt = System.currentTimeMillis() - originalTimestamp
        val sender = context.self
        val newRttData = updateRttData(rttData, sender, rtt, maxRttSamples)
        networkDeltaBehavior(nodeId, peerNetworkActor, privateKey, maxRttSamples, outlierThreshold, newRttData, currentDelta, context, timers)

      case CalculateNetworkDelta =>
        val newDelta = calculateNetworkDelta(rttData, outlierThreshold)
        context.log.info(s"Calculated new network delta: $newDelta ms")
        networkDeltaBehavior(nodeId, peerNetworkActor, privateKey, maxRttSamples, outlierThreshold, rttData, newDelta, context, timers)

      case SendPings =>
        peerNetworkActor ! BroadcastNetworkMessage(
          SignedMessageBuilder[NodePing]
            .content(NodePing(nodeId, System.currentTimeMillis(), context.self))
            .privateKey(privateKey)
            .build())
        Behaviors.same
    }

  /**
   * Updates the RTT data for a given actor.
   *
   * @param rttData    The current RTT data.
   * @param actor      The actor reference.
   * @param rtt        The new RTT value.
   * @param maxSamples The maximum number of samples to keep.
   * @return The updated RTT data.
   */
  private def updateRttData(rttData: Map[ActorRef[Pong], Queue[Long]],
                            actor: ActorRef[Pong],
                            rtt: Long,
                            maxSamples: Int): Map[ActorRef[Pong], Queue[Long]] =
    val updatedQueue = rttData.getOrElse(actor, Queue.empty)
      .enqueue(rtt)
      .takeRight(maxSamples)
    rttData.updated(actor, updatedQueue)

  /**
   * Calculates the network delta based on the collected RTT data.
   *
   * @param rttData          The map of RTT data for all actors.
   * @param outlierThreshold The threshold for outlier detection.
   * @return The calculated network delta in milliseconds.
   */
  private def calculateNetworkDelta(rttData: Map[ActorRef[Pong], Queue[Long]], outlierThreshold: Double): Long =
    val allRtts = rttData.values.flatten.toSeq
    val filteredRtts = removeOutliers(allRtts, outlierThreshold)
    if filteredRtts.isEmpty then 0L else filteredRtts.sorted.apply(filteredRtts.size / 2) / 2

  /**
   * Removes outliers from the given RTT data.
   *
   * @param rtts      The sequence of RTT values.
   * @param threshold The threshold (in standard deviations) for outlier detection.
   * @return The filtered sequence of RTT values with outliers removed.
   */
  private def removeOutliers(rtts: Seq[Long], threshold: Double): Seq[Long] =
    if rtts.isEmpty then Seq.empty
    else
      val mean = rtts.sum.toDouble / rtts.size
      val stdDev = math.sqrt(rtts.map(rtt => math.pow(rtt - mean, 2)).sum / rtts.size)
      rtts.filter(rtt => math.abs(rtt - mean) <= threshold * stdDev)
