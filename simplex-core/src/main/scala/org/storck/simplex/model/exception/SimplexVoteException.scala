package org.storck.simplex.model.exception

/**
 * Indicates an error or problem when processing a vote.
 */
class SimplexVoteException(message: String = "", cause: Throwable = null) extends RuntimeException(message, cause)