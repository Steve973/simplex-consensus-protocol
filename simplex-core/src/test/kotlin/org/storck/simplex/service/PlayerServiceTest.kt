package org.storck.simplex.service

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.security.PublicKey

/**
 * Test the Player Service.
 */
@SuppressFBWarnings(
    value = ["NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", "SE_BAD_FIELD"],
    justification = "It is a test.")
class PlayerServiceTest : BehaviorSpec({

    val playerId = "testPlayerId"
    val publicKeyMock: PublicKey = mockk()

    Given("a new PlayerService") {
        val playerService = PlayerService()

        When("no players have been added") {
            Then("the list of player IDs is empty") {
                playerService.playerIds.shouldBeEmpty()
            }

            Then("public key retrieval for any ID returns null") {
                playerService.getPublicKey(playerId).shouldBe(null)
            }

            Then("player removal for any ID does not affect the ID list") {
                playerService.removePlayer(playerId)
                playerService.playerIds.shouldBeEmpty()
            }
        }

        When("a player is added") {
            playerService.addPlayer(playerId, publicKeyMock)

            Then("the list of player IDs contains the added player") {
                playerService.playerIds shouldBe listOf(playerId)
            }

            Then("public key retrieval returns the correct key for the added player") {
                playerService.getPublicKey(playerId) shouldBe publicKeyMock
            }

            Then("player removal returns null for an unknown player and does not affect the list") {
                playerService.removePlayer("unknown_id").shouldBe(null)
                playerService.playerIds shouldBe listOf(playerId)
            }

            Then("public key retrieval returns null for an unknown player") {
                playerService.getPublicKey("unknown_id").shouldBe(null)
            }

            Then("player removal returns the correct key for the added player and removes them from the list") {
                playerService.removePlayer(playerId) shouldBe publicKeyMock
                playerService.playerIds.shouldBeEmpty()
            }
        }
    }

    Given("a PlayerService initialized with a known player") {
        val initialPlayers = mapOf(playerId to publicKeyMock)
        val playerService = PlayerService(initialPlayers)

        Then("the list of player IDs contains the known player") {
            playerService.playerIds shouldBe listOf(playerId)
        }

        Then("public key retrieval returns the correct key for the known player") {
            playerService.getPublicKey(playerId) shouldBe publicKeyMock
        }
    }
})