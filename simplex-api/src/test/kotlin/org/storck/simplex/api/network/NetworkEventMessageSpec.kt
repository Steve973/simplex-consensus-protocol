package org.storck.simplex.api.network

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.storck.simplex.api.message.NetworkMessageType

/**
 * Tests the NetworkEventMessage.
 */
@SuppressFBWarnings(
  value = ["NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", "SE_BAD_FIELD", "SE_BAD_FIELD_STORE"],
  justification = "It is a test.")
class NetworkEventMessageSpec : BehaviorSpec({

  for (networkEvent in NetworkEvent.entries) {
    val details = "Dummy details for ${networkEvent.name}"
    val networkEventMessage = NetworkEventMessage(networkEvent, details)

    given("A NetworkEventMessage for ${networkEvent.name}") {

      `when`("getMessageType is called") {
        val messageType = networkEventMessage.messageType

        then("Returned message type should be NETWORK_EVENT") {
          messageType shouldBe NetworkMessageType.NETWORK_EVENT
        }
      }

      `when`("event is accessed") {
        val event = networkEventMessage.event

        then("Returned event should be same as original") {
          event shouldBe networkEvent
        }
      }

      `when`("details is accessed") {
        val detailsOutput = networkEventMessage.details

        then("Returned details should be same as original") {
          detailsOutput shouldBe details
        }
      }
    }
  }
})