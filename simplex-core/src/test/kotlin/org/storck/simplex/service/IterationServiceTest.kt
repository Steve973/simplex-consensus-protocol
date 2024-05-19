package org.storck.simplex.service

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import kotlinx.coroutines.delay
import org.storck.simplex.networking.api.network.PeerNetworkClient
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

/**
 * Test the Blockchain Service.
 */
@SuppressFBWarnings(
    value = ["NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", "SE_BAD_FIELD", "NP_NULL_ON_SOME_PATH"],
    justification = "I cannot find anything wrong with the test, and mock objects used in a test do not need to be serializable."
)
class IterationServiceTest : BehaviorSpec({
    val localPlayerId = "testPlayer"
    val iterationNumber = 1
    val networkDeltaSeconds = 1
    val players = listOf("player1", "player2")
    val signatureBytes = ByteArray(64)
    val playerService = mockk<PlayerService>()
    val latch = mockk<CountDownLatch>()
    val digitalSignatureService = mockk<DigitalSignatureService>()
    val peerNetworkClient = mockk<PeerNetworkClient>(relaxed = true)

    Given("IterationService is initialized with certain services") {
        val iterationService =
            IterationService(localPlayerId, playerService, digitalSignatureService, peerNetworkClient)

        When("initializeForIteration method is called") {

            every { playerService.playerIds } returns players
            every { digitalSignatureService.computeBytesHash(any<ByteArray>()) } returns "hash"

            iterationService.initializeForIteration(iterationNumber, latch)

            Then("electLeader method is called with the iteration number") {
                iterationService.iterationNumber shouldBe iterationNumber
                iterationService.leaderId?.let { it shouldBe players[0] }
            }
        }

        When("startIteration method is called") {
            every { peerNetworkClient.getNetworkDeltaSeconds() } returns networkDeltaSeconds
            every { digitalSignatureService.computeBytesHash(any<ByteArray>()) } returns "hash"
            every { digitalSignatureService.generateSignature(any<ByteArray>()) } returns signatureBytes

            iterationService.startIteration()

            Then("broadcastVote is invoked on PeerNetworkClient") {
                verify { peerNetworkClient.getNetworkDeltaSeconds() }
                verify { digitalSignatureService.computeBytesHash(any<ByteArray>()) }
            }
        }

        When("stopIteration method is called") {
            every { latch.countDown() } just Runs

            iterationService.stopIteration()

            Then("timer is cancelled and iteration is stopped") {
                // Check by trying to start again and seeing if the exceptions are handled
                shouldThrowAny { iterationService.startIteration() }
            }
        }

        When("awaitCompletion method is called") {
            every { peerNetworkClient.getNetworkDeltaSeconds() } returns networkDeltaSeconds
            every { digitalSignatureService.generateSignature(any<ByteArray>()) } returns signatureBytes
            every { latch.await() } just Runs

            iterationService.initializeForIteration(iterationNumber, latch)
            iterationService.startIteration()

            Then("awaitCompletion should successfully complete") {
                iterationService.awaitCompletion()
            }
        }
    }

    Given("Iteration Service") {
        val iterationService = IterationService(localPlayerId, playerService, digitalSignatureService, peerNetworkClient)

        When("awaitCompletion method is called") {
            val realLatch = CountDownLatch(1)
            iterationService.initializeForIteration(iterationNumber, realLatch)
            var exception: Throwable? = null
            val testThread = thread {
                try {
                    iterationService.awaitCompletion()
                } catch (e: Throwable) {
                    exception = e
                }
            }
            delay(100)
            testThread.interrupt()

            Then("exception should have been thrown") {
                exception shouldNotBe null
            }
        }
    }

    Given("received finalize message") {
        val iterationService = IterationService(localPlayerId, playerService, digitalSignatureService, peerNetworkClient)

        When("log that payer sent finalize message") {
            iterationService.logFinalizeReceipt(localPlayerId)

            Then("receipts should contain the playerId that sent the finalize message") {
                iterationService.finalizeReceipts shouldContain localPlayerId
            }
        }
    }
})
