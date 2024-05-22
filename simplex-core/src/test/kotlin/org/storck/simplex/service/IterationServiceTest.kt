package org.storck.simplex.service

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.kotest.assertions.throwables.shouldThrow
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
    val peerNetworkClient = mockk<PeerNetworkClient>()

    afterTest {
        clearAllMocks()
    }

    Given("an iteration to initialize") {
        val iterationService =
            IterationService(localPlayerId, playerService, digitalSignatureService, peerNetworkClient)
        every { playerService.playerIds } returns listOf(localPlayerId)
        every { digitalSignatureService.computeBytesHash(any<ByteArray>()) } returns "hash"
        iterationService.initializeForIteration(5, latch)


        When("initializing iteration for same iteration number") {
            val exception = shouldThrow<IllegalArgumentException> { iterationService.initializeForIteration(5, latch) }

            Then("there should be an error message indicating that the iteration number must increase") {
                exception.message shouldBe "Iteration number must only increase"
            }
        }

        When("initializing iteration for lower iteration number") {
            val exception = shouldThrow<IllegalArgumentException> { iterationService.initializeForIteration(1, latch) }

            Then("there should be an error message indicating that the iteration number must increase") {
                exception.message shouldBe "Iteration number must only increase"
            }
        }
    }

    Given("IterationService is initialized with certain services") {
        val iterationService =
            IterationService(localPlayerId, playerService, digitalSignatureService, peerNetworkClient)
        val realLatch = CountDownLatch(1)
        every { peerNetworkClient.broadcastVote(any()) } just Runs

        When("initializeForIteration method is called") {
            every { playerService.playerIds } returns players
            every { digitalSignatureService.computeBytesHash(any<ByteArray>()) } returns "hash"

            iterationService.initializeForIteration(iterationNumber, realLatch)

            Then("electLeader method is called with the iteration number") {
                iterationService.iterationNumber shouldBe iterationNumber
                iterationService.leaderId?.let { it shouldBe players[0] }
                realLatch.count shouldBe 1
                iterationService.isShutdown shouldBe false
            }
        }

        When("startIteration method is called") {
            every { peerNetworkClient.networkDeltaSeconds } returns networkDeltaSeconds
            every { digitalSignatureService.generateSignature(any<ByteArray>()) } returns signatureBytes

            iterationService.startIteration()

            Then("the timer is started with a delay based on the network delta seconds from the peer client") {
                verify { peerNetworkClient.getNetworkDeltaSeconds() }
                realLatch.count shouldBe 1
                iterationService.isShutdown shouldBe false
            }
        }

        When("stopIteration method is called") {
            iterationService.stopIteration()

            Then("timer is cancelled and iteration is stopped") {
                realLatch.count shouldBe 0
                iterationService.isShutdown shouldBe true
                // Check by trying to start again and seeing if the exceptions are thrown
                shouldThrowAny { iterationService.startIteration() }
            }
        }

        When("awaitCompletion method is called") {
            every { playerService.playerIds } returns players
            every { peerNetworkClient.networkDeltaSeconds } returns networkDeltaSeconds
            every { digitalSignatureService.computeBytesHash(any<ByteArray>()) } returns "hash"
            every { digitalSignatureService.generateSignature(any<ByteArray>()) } returns signatureBytes

            iterationService.awaitCompletion()

            Then("awaitCompletion should successfully complete") {
                realLatch.count shouldBe 0
            }
        }
    }

    Given("a running iteration to time out") {
        val iterationService =
            IterationService(localPlayerId, playerService, digitalSignatureService, peerNetworkClient)
        every { playerService.playerIds } returns players
        every { digitalSignatureService.computeBytesHash(any<ByteArray>()) } returns "hash"
        every { digitalSignatureService.generateSignature(any<ByteArray>()) } returns signatureBytes
        every { peerNetworkClient.getNetworkDeltaSeconds() } returns 0
        every { peerNetworkClient.broadcastVote(any()) } just Runs
        iterationService.initializeForIteration(iterationNumber, latch)

        When("iteration does not complete before duration expires") {
            iterationService.startIteration()
            delay(100)

            Then("timer task fires to run iteration timeout tasks") {
                verify { peerNetworkClient.broadcastVote(any()) }
                verify { latch.countDown() }
            }
        }
    }

    Given("Iteration Service") {
        every { playerService.playerIds } returns players
        every { digitalSignatureService.computeBytesHash(any<ByteArray>()) } returns "hash"
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

        When("log that player sent finalize message") {
            iterationService.logFinalizeReceipt(localPlayerId)

            Then("receipts should contain the playerId that sent the finalize message") {
                iterationService.finalizeReceipts shouldContain localPlayerId
            }
        }
    }
})
