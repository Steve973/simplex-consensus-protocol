package org.storck.simplex.actors.iteration

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import org.storck.simplex.actors.common.ActorInteraction

object IterationFinalizingActor extends ActorInteraction:

  sealed trait IterationFinalizingCommand

  final case class FinalizeIteration(iterationNumber: Int, actorRefs: Map[String, ActorRef[?]]) extends IterationFinalizingCommand

  def apply(): Behavior[IterationFinalizingCommand] =
    Behaviors.setup { context =>
      Behaviors.receiveMessage {
        // TODO: this is generated, so it is NOT CORRECT
        case FinalizeIteration(iterationNumber, actorRefs) =>
          context.log.info(s"Finalizing iteration $iterationNumber")
          actorRefs.values.foreach(context.stop)
          Behaviors.same
      }
    }
