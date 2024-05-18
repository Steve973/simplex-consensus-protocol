package org.storck.simplex.service

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.*
import org.storck.simplex.networking.api.network.PeerNetworkClient

/**
 * Test the Blockchain Service.
 */
@OptIn(DelicateCoroutinesApi::class)
@SuppressFBWarnings(
    value = ["NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", "SE_BAD_FIELD", "NP_NULL_ON_SOME_PATH"],
    justification = "I cannot find anything wrong with the test, and mock objects used in a test do not need to be serializable.")
class IterationServiceTest: BehaviorSpec({
    val localPlayerId = "testPlayer"
    val iterationNumber = 1
    val networkDeltaSeconds = 1
    val players = listOf("player1", "player2")
    val signatureBytes = ByteArray(64)
    val playerService = mockk<PlayerService>()
    val digitalSignatureService = mockk<DigitalSignatureService>()
    val peerNetworkClient = mockk<PeerNetworkClient>(relaxed = true)

    Given("IterationService is initialized with certain services"){
        val iterationService = IterationService(localPlayerId, playerService, digitalSignatureService, peerNetworkClient)

        When("initializeForIteration method is called") {

            every { playerService.playerIds } returns players
            every { digitalSignatureService.computeBytesHash(any<ByteArray>()) } returns "hash"

            iterationService.initializeForIteration(iterationNumber)

            Then("electLeader method is called with the iteration number") {
                iterationService.iterationNumber shouldBe iterationNumber
                iterationService.leaderId?.let { it shouldBe players[0] }
            }
        }

        When("startIteration method is called"){
            every { peerNetworkClient.getNetworkDeltaSeconds() } returns networkDeltaSeconds
            every { digitalSignatureService.computeBytesHash(any<ByteArray>()) } returns "hash"
            every { digitalSignatureService.generateSignature(any<ByteArray>()) } returns signatureBytes

            iterationService.startIteration()

            Then("broadcastVote is invoked on PeerNetworkClient"){
                verify { peerNetworkClient.getNetworkDeltaSeconds() }
                verify { digitalSignatureService.computeBytesHash(any<ByteArray>()) }
            }
        }

        When("stopIteration method is called"){
            iterationService.stopIteration()

            Then("timer is cancelled and iteration is stopped"){
                // Check by trying to start again and seeing if the exceptions are handled
                shouldThrowAny { iterationService.startIteration() }
            }
        }

        When("awaitCompletion method is called") {
            every { peerNetworkClient.getNetworkDeltaSeconds() } returns networkDeltaSeconds
            every { digitalSignatureService.generateSignature(any<ByteArray>()) } returns signatureBytes

            iterationService.initializeForIteration(iterationNumber)
            iterationService.startIteration()

            Then("awaitCompletion should successfully complete"){
                iterationService.awaitCompletion()
            }
        }
    }

    Given("Iteration Service") {
        val iterationService = IterationService(localPlayerId, playerService, digitalSignatureService, peerNetworkClient)

        When("awaitCompletion method is called") {
            val exception = shouldThrow<IllegalStateException> {
                withTimeout(100) {
                    runInterruptible {
                        iterationService.awaitCompletion()
                    }
                }
            }

            Then("exception should indicate the interruption while waiting for completion") {
                exception.message shouldBe "Unexpected error while waiting for iteration completion"
            }
        }
    }
})
