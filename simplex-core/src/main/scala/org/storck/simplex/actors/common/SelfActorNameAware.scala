package org.storck.simplex.actors.common

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.{ActorRef, ActorSystem}
import org.storck.simplex.actors.common.SimplexImplicits.given

/**
 * Trait allowing actors to be aware of their own actor name so that they can create unique actor names,
 * and service keys, for instances.
 */
trait SelfActorNameAware:

  /**
   * Converts a camel case string to a lowercase string with dashes between the words.  This method is
   * smart enough to handle acronyms, such as "ID" or "URL", and will not insert a dash between the
   * consecutive capital letters.  In the case of a class like MyABCDActor, this method will return
   * "my-abcd-actor", preserving the all-caps acronym as a single word.  Note that there may be some
   * limitations, so be sure to verify the results for odd cases.
   *
   * @param name the camel case string
   * @return the lowercase string with dashes
   */
  private def camelCaseToLowercaseWithDashes(name: String): String =
    name.replaceAll("([A-Z][a-z]*)(?=([A-Z][a-z]*))", "$1-").toLowerCase

  /**
   * The base name of the actor, derived from the class name, and converted to lower-case
   * where the capitalized "words" are lower-cased and separated by dashes.  For example,
   * this class would return "self-actor-name-aware".
   */
  private def baseActorName: String = camelCaseToLowercaseWithDashes(this.getClass.getSimpleName)

  /**
   * Creates a unique actor name for an instance of this actor.  The name is derived from the class name,
   * converted to lower-case where the capitalized "words" are lower-cased and separated by dashes.  If
   * an iteration number is provided, it is appended to the base name with a dash.  If the withTimestamp
   * flag is set, the current timestamp is appended to the name with a dash.
   *
   * @param iterationNumber the iteration number, if any
   * @param withTimestamp    whether to append the current timestamp
   * @return the unique actor name
   */
  def createActorName(iterationNumber: Option[Int] = None, withTimestamp: Boolean = false): String =
    val nameWithIteration = iterationNumber.fold(baseActorName)(number => s"$baseActorName-$number")
    if (withTimestamp) s"$nameWithIteration-${System.currentTimeMillis()}" else nameWithIteration

  /**
   * Generates a service key for the given type.  It is the responsibility of an actor that extends this
   * trait to register this service key with the receptionist, or not, as appropriate.
   *
   * @tparam T the type of the service key
   * @return the service key
   */
  def generateServiceKey[T](iterationNumber: Option[Int] = None)
                           (using tag: scala.reflect.ClassTag[T]): ServiceKey[T] =
    ServiceKey[T](createActorName(iterationNumber))

  /**
   * Registers the given actor reference with the given service key.
   *
   * @param serviceKey the service key
   * @param actorRef   the actor reference
   * @tparam T the type of the service key
   */
  def registerServiceKey[T](serviceKey: ServiceKey[T], actorRef: ActorRef[T])
                           (using ActorSystem[?]): Unit =
    system.receptionist ! Receptionist.Register(serviceKey, actorRef)
