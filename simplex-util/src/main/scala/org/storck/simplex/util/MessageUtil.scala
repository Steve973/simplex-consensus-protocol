package org.storck.simplex.util

import com.fasterxml.jackson.core.`type`.TypeReference
import org.storck.simplex.message.PeerInfo
import org.storck.simplex.util.SimplexConstants.JSON_MAPPER

import java.io.IOException
import scala.util.{Failure, Success, Try}

/**
 * Utility singleton that provides methods to convert messages between formats.
 */
object MessageUtil {

  /**
   * Convert the message details from a [[NetworkEventMessage]] for messages
   * indicating that a peer has connected or disconnected.
   *
   * @param json the message details to convert
   *
   * @return the [[PeerInfo]]
   */
  def peerInfoFromJson(json: String): PeerInfo =
    try {
      JSON_MAPPER.readValue(json, classOf[PeerInfo])
    } catch {
      case e: Error => throw new IllegalStateException("Unexpected error when getting peer information from JSON string", e)
    }

  /**
   * Method to convert to the specified type from a byte array.
   *
   * @param bytes the byte array to be converted
   * @param typeRef the type reference of the destination type
   *
   * @return an object of type T converted from the byte array
   */
  def fromBytes[T](bytes: Array[Byte], typeRef: TypeReference[T]): Either[Throwable, T] =
    Try(JSON_MAPPER.readValue(bytes, typeRef)) match {
      case Success(value) =>
        Right(value)
      case Failure(e: IOException) =>
        val typeName = Option(typeRef)
          .map(tr => tr.getType)
          .map(t => t.getTypeName)
          .getOrElse("Unknown")
        Left(new IllegalStateException(s"Unexpected error when converting from a byte array to '$typeName'", e))
      case Failure(e) =>
        Left(e)
    }

  /**
   * Converts an object of type T to a byte array.
   *
   * @param obj the object to convert to bytes
   *
   * @return a byte array representation of the given object
   */
  def toBytes[T](obj: T): Array[Byte] =
    try {
      JSON_MAPPER.writeValueAsBytes(obj)
    } catch {
      case e: Error => throw new IllegalStateException("Unexpected error when converting object to byte array", e)
    }
}
