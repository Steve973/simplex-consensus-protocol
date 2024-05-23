package org.storck.simplex.actors.common

import akka.actor.typed.{ActorSystem, Scheduler}
import akka.util.Timeout

import scala.compiletime.uninitialized
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt

/**
 * Implicits for the actors module.
 */
object SimplexImplicits:

  private var _system: ActorSystem[?] = uninitialized

  private var _scheduler: Scheduler = uninitialized

  def setupGivenInstances(actorSystem: ActorSystem[?]): Unit = {
    _system = actorSystem
    _scheduler = actorSystem.scheduler
  }

  given ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global
  given timeout: Timeout = Timeout(1.second)
  given system: ActorSystem[?] = _system
  given scheduler: Scheduler = _scheduler
