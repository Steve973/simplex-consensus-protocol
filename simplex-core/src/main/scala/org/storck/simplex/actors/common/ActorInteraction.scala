package org.storck.simplex.actors.common

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, Scheduler}
import akka.util.Timeout

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

/**
 * The `ActorParam` trait is used to define the parameters that can be passed to an actor interaction method.
 * It is a sealed trait with two case classes that represent the two possible parameters that can be passed
 * to an actor interaction method. The two case classes are `WithActorRef` and `WithServiceKey`.  Sometimes,
 * we already have the actor reference, so we can pass that, rather than requiring the actor interaction method
 * to obtain the actor reference from the receptionist. In such cases, we can use the `WithActorRef` case class.
 * If we do not have the actor reference, we can pass the service key to the actor interaction method, which
 * will make it possible to obtain the actor reference from the receptionist. In such cases, we can use the
 * `WithServiceKey` case class.
 *
 * @tparam T The type of the actor reference.
 */
sealed trait ActorParam[T]

/**
 * The `WithActorRef` case class is used to pass an actor reference to an actor interaction method. It is
 * a case class that extends the `ActorParam` trait. It takes an actor reference as a parameter and stores
 * it in the `actorRef` field. This case class is used when we already have the actor reference and do not
 * need to obtain it from the receptionist.
 *
 * @param actorRef The actor reference to pass to the actor interaction method.
 * @tparam T The type of the actor reference.
 */
case class WithActorRef[T](actorRef: ActorRef[T]) extends ActorParam[T]

/**
 * The `WithServiceKey` case class is used to pass a service key to an actor interaction method. It is a case
 * class that extends the `ActorParam` trait. It takes a service key as a parameter and stores it in the
 * `serviceKey` field. This case class is used when we do not have the actor reference and need to obtain it
 * from the receptionist. The service key is used to find the actor reference in the receptionist.
 *
 * @param serviceKey The service key to pass to the actor interaction method.
 * @tparam T The type of the actor reference.
 */
case class WithServiceKey[T](serviceKey: ServiceKey[T]) extends ActorParam[T]

/**
 * The `ActorInteraction` trait provides a mechanism for an actor to interact with another actor in an
 * asynchronous manner. It encapsulates the complexity of dealing with futures and allows the calling
 * actor to remain responsive to other messages. The interaction result is sent back to the calling actor
 * as a message, which can then be handled in the actor's message handling logic.
 */
trait ActorInteraction:

  /**
   * This method is used to obtain an actor reference from the receptionist using the provided service key.
   * It returns a future that resolves to the actor reference. If the actor reference is not found, the future
   * fails with an exception.
   *
   * @param context The context of the calling actor. This provides access to various actor related functionalities.
   * @param serviceKey The service key used to find the actor. This is a unique identifier for the actor service.
   * @tparam T The type of the actor reference.
   * @return A future that resolves to the actor reference.
   */
  private def obtainActorRef[T](context: ActorContext[?], serviceKey: ServiceKey[T])
                       (using Timeout, Scheduler, ExecutionContextExecutor): Future[ActorRef[T]] =
    val receptionist = context.system.receptionist
    val serviceRefFuture: Future[Receptionist.Listing] = receptionist.ask(Receptionist.Find(serviceKey))
    serviceRefFuture.flatMap { listing =>
      listing.serviceInstances(serviceKey).headOption match
        case Some(actorRef) => Future.successful(actorRef)
        case None => Future.failed(new Exception(s"No actor found for serviceKey: $serviceKey"))
    }.recoverWith {
      case exception: Throwable =>
        context.log.error("Failed to obtain actor reference from receptionist: ", exception)
        Future.failed(exception)
    }

  /**
   * This method is used to interact with an actor by sending a message and handling the response.
   * It first obtains a reference to the actor using the provided actor parameter, then sends a message to
   * the actor. The response from the actor is then piped back to the calling actor as a message. This
   * method hides the complexity of dealing with futures and allows the calling actor to remain
   * responsive to other messages.
   *
   * @param context The context of the calling actor. This provides access to various actor related functionalities.
   * @param actorParam The actor parameter used to obtain the actor reference. This can be an actor reference or a service key.
   * @param message The message to send to the actor. This is a function that takes an `ActorRef` and returns a message.
   * @param successToCommand A function that handles a successful response from the actor
   * @param failureToCommand A function that handles the [[Throwable]] in case of a failure
   * @tparam T The type of the message to send to the actor.
   * @tparam U The type of the response from the actor.
   * @tparam V The type of the command in the actor.
   */
  def askActor[T, U, V](context: ActorContext[V], actorParam: ActorParam[T],
                        message: ActorRef[U] => T, successToCommand: U => V, failureToCommand: Throwable => V)
                       (using Timeout, Scheduler, ExecutionContextExecutor): Unit =
    actorParam match
      case WithActorRef(actorRef) =>
        sendAsk(actorRef, message, context, successToCommand, failureToCommand)
      case WithServiceKey(serviceKey) =>
        val actorRefFuture = obtainActorRef(context, serviceKey)
        actorRefFuture.onComplete {
          case Success(actorRef) =>
            sendAsk(actorRef, message, context, successToCommand, failureToCommand)
          case Failure(exception) =>
            context.self ! failureToCommand(exception)
        }

  /**
   * This method is used to interact with an actor by sending a message and handling the response.
   * It sends a message to the actor and handles the response. The response from the actor is then piped
   * back to the calling actor as a message. This method hides the complexity of dealing with futures and
   * allows the calling actor to remain responsive to other messages.
   *
   * @param actorRef The actor reference to interact with.
   * @param message The message to send to the actor. This is a function that takes an `ActorRef` and returns a message.
   * @param context The context of the calling actor. This provides access to various actor related functionalities.
   * @param successToCommand A function that handles a successful response from the actor
   * @param failureToCommand A function that handles the [[Throwable]] in case of a failure
   * @tparam T The type of the message to send to the actor.
   * @tparam U The type of the response from the actor.
   * @tparam V The type of the command in the actor.
   */
  private def sendAsk[T, U, V](actorRef: ActorRef[T], message: ActorRef[U] => T, context: ActorContext[V], successToCommand: U => V, failureToCommand: Throwable => V)
                              (using Timeout, Scheduler, ExecutionContextExecutor): Unit =
    val responseFuture: Future[U] = actorRef.ask(message)
    context.pipeToSelf(responseFuture) {
      case Success(value) => successToCommand(value)
      case Failure(exception) =>
        context.log.error("Error in asking actor: ", exception)
        failureToCommand(exception)
    }

  /**
   * This method is used to interact with an actor by sending a message without waiting for a response.
   * It first obtains a reference to the actor using the provided actor parameter, then sends a message to
   * the actor. This method hides the complexity of dealing with futures and allows the calling actor to remain
   * responsive to other messages.
   *
   * @param context The context of the calling actor. This provides access to various actor related functionalities.
   * @param actorParam The actor parameter used to obtain the actor reference. This can be an actor reference or a service key.
   * @param message The message to send to the actor. This is a function that takes an `ActorRef` and returns a message.
   * @param failureToCommand A function that transforms a `Throwable` into a `Unit`. This function is used to handle the failure in obtaining the actor reference.
   * @tparam T The type of the message to send to the actor.
   * @tparam V The type of the command in the actor.
   */
  def tellActor[T, V](context: ActorContext[V], actorParam: ActorParam[T], message: ActorRef[T] => T, failureToCommand: Throwable => Any)
                     (using timeout: Timeout, scheduler: Scheduler, ec: ExecutionContextExecutor, classTag: ClassTag[V]): Unit =
    actorParam match
      case WithActorRef(actorRef) =>
        actorRef ! message(actorRef)
      case WithServiceKey(serviceKey) =>
        val actorRefFuture = obtainActorRef(context, serviceKey)
        actorRefFuture.onComplete {
          case Success(actorRef) =>
            actorRef ! message(actorRef)
          case Failure(exception) =>
            val result = failureToCommand(exception)
            if (classTag.runtimeClass.isAssignableFrom(result.getClass)) {
              context.self ! result.asInstanceOf[V]
            }
        }
