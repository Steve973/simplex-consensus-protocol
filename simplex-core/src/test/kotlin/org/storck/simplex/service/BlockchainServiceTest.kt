package org.storck.simplex.service

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.storck.simplex.model.Block
import org.storck.simplex.model.BlockNotarized

/**
 * Test the Blockchain Service.
 */
@SuppressFBWarnings(
    value = ["NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", "SE_BAD_FIELD"],
    justification = "It is a test.")
class BlockchainServiceTest : BehaviorSpec({

    given("A BlockchainService") {
        val blockchainService = BlockchainService<String>()
        val dummyBlock = mockk<Block<String>>()
        val dummyNotarizedBlock = mockk<BlockNotarized<String>>()

        every { dummyBlock.height } returns 0
        every { dummyBlock.parentHash } returns ""
        every { dummyBlock.transactions } returns emptyList()
        every { dummyNotarizedBlock.block } returns dummyBlock
        every { dummyNotarizedBlock.votes } returns emptyList()

        `when`("creating a dummy block") {
            val createdDummyBlock = blockchainService.createDummyBlock.apply(0)
            then("it should match the dummy block") {
                createdDummyBlock.height shouldBe dummyBlock.height
                createdDummyBlock.parentHash shouldBe dummyBlock.parentHash
                createdDummyBlock.transactions shouldBe dummyBlock.transactions
            }
        }

        `when`("creating a genesis block") {
            val genesisBlock = blockchainService.createGenesisBlock.get()
            then("it should match the genesis block") {
                genesisBlock.height shouldBe 0
                genesisBlock.parentHash shouldBe ""
                genesisBlock.transactions shouldBe emptyList()
            }
        }

        `when`("creating a finalizeMsg block") {
            val finalizeBlock = blockchainService.createFinalizeBlock.apply(0)
            then("it should match the finalizeMsg block") {
                finalizeBlock.height shouldBe 0
                finalizeBlock.parentHash shouldBe "FINALIZE"
                finalizeBlock.transactions shouldBe emptyList()
            }
        }

        `when`("creating a notarized block") {
            every { dummyBlock.transactions } returns listOf()
            val notarizedBlock = blockchainService.createNotarizedBlock.apply(dummyBlock, listOf())
            then("it should match the notarized block") {
                notarizedBlock.block shouldBe dummyBlock
                notarizedBlock.votes shouldBe emptyList()
            }
        }

        `when`("getting the blockchain") {
            val blockchain = blockchainService.blockchain
            val chainBlock = blockchain[0]
            then("it should return the correct blockchain") {
                blockchain.size shouldBe 1
                chainBlock.votes shouldBe emptyList()
                chainBlock.block.height shouldBe 0
                chainBlock.block.parentHash shouldBe ""
                chainBlock.block.transactions shouldBe emptyList()
            }
        }
    }
})