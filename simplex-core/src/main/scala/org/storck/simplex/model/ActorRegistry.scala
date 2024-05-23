package org.storck.simplex.model

import akka.actor.typed.ActorRef

import scala.reflect.ClassTag

/**
 * A registry for actor references.
 *
 * This class provides a way to store and retrieve actor references of any type.
 * It uses a map where the keys are actor IDs and the values are actor references.
 *
 * @param actorRefs a map from actor IDs to actor references
 */
case class ActorRegistry(actorRefs: Map[String, ActorRef[?]] = Map.empty):

  /**
   * Adds an actor reference to the registry.
   *
   * This method creates a new ActorRegistry instance with the added actor reference.
   *
   * @param id the ID of the actor
   * @param actorRef the actor reference
   * @return a new ActorRegistry with the added actor reference
   */
  def addActorRef(id: String, actorRef: ActorRef[?]): ActorRegistry =
    ActorRegistry(actorRefs + (id -> actorRef))

  /**
   * Removes an actor reference from the registry.
   *
   * This method creates a new ActorRegistry instance without the removed actor reference.
   *
   * @param id the ID of the actor
   * @return a new ActorRegistry without the removed actor reference
   */
  def removeActorRef(id: String): ActorRegistry =
    ActorRegistry(actorRefs - id)

  /**
   * Gets an actor reference from the registry.
   *
   * This method retrieves the actor reference associated with the given ID.
   *
   * @param id the ID of the actor
   * @return an Option containing the actor reference if it exists, None otherwise
   */
  def getActorRef(id: String): Option[ActorRef[?]] =
    actorRefs.get(id)

  /**
   * Gets an actor reference from the registry as a specific type.
   *
   * This method attempts to cast the actor reference associated with the given ID to the requested type.
   * If the cast is successful, it returns the actor reference as the requested type.
   * If the cast is not successful, it returns None.
   *
   * @param id the ID of the actor
   * @param classTag the ClassTag of the type T
   * @return an Option containing the actor reference cast to type T if it exists and is of type T, None otherwise
   */
  def getActorRefAs[T](id: String)(using classTag: ClassTag[T]): Option[ActorRef[T]] =
    actorRefs.get(id).flatMap { ref =>
      if (classTag.runtimeClass.isInstance(ref)) Some(ref.asInstanceOf[ActorRef[T]])
      else None
    }
