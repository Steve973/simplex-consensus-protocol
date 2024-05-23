package org.storck.simplex.model.exception

/**
 * Indicates that an actor could not be found.
 */
class NoSuchActorException(message: String = "", cause: Throwable = null) extends RuntimeException(message, cause)
