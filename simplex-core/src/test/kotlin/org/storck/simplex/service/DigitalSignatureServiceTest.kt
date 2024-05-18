package org.storck.simplex.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull
import io.mockk.mockk
import org.storck.simplex.model.Block
import java.security.KeyPair
import java.security.PublicKey

/**
 * This class contains unit tests for the DigitalSignatureService class.
 */
class DigitalSignatureServiceTest : BehaviorSpec({

    val signatureService = DigitalSignatureService()
    val messageDigestAlgorithm = "SHA-256"
    val keypairGeneratorAlgorithm = "RSA"
    val signatureAlgorithm = "SHA256withRSA"
    val badAlgorithm = "OOPS"

    Given("a DigitalSignatureService instance with key pair") {
        val input = byteArrayOf(1, 2, 3, 4, 5)

        When("key pair is generated") {
            val result: KeyPair = signatureService.generateKeyPair()!!

            Then("key pair should not be null") {
                result.shouldNotBeNull()
                result.private!!.shouldNotBeNull()
                result.public!!.shouldNotBeNull()
            }
        }

        When("signature is generated") {
            val result: ByteArray = signatureService.generateSignature(input)!!

            Then("generated signature should not be null") {
                result.shouldNotBeNull()
            }
        }

        When("wrong public key is used") {
            val signature: ByteArray = signatureService.generateSignature(input)!!
            val wrongPublicKey: PublicKey = signatureService.generateKeyPair()!!.public!!
            val result: Boolean = signatureService.verifySignature(input, signature, wrongPublicKey)

            Then("signature validation should fail") {
                result.shouldBe(false)
            }
        }

        When("correct public key is used") {
            val signature: ByteArray = signatureService.generateSignature(input)!!
            val result: Boolean = signatureService.verifySignature(input, signature, signatureService.keyPair!!.public!!)

            Then("signature validation should succeed") {
                result.shouldBe(true)
            }
        }
    }

    Given("the input byte array") {
        val input = byteArrayOf(1, 2, 3, 4, 5)
        When("computeBytesHash is called") {
            val result = signatureService.computeBytesHash(input)
            Then("computed hash should not be null and have correct length") {
                result.shouldNotBeNull()
                result.length.shouldBe(128)
            }
        }
    }

    Given("the Block input") {
        val block = Block<String>(1, "parentHash", listOf())
        When("computeBlockHash is called") {
            val result = signatureService.computeBlockHash(block)
            Then("computed hash should not be null and have correct length") {
                result.shouldNotBeNull()
                result.length.shouldBe(128)
            }
        }
    }

    Given("invalid Block input") {
        val block = mockk<Block<String>>()
        When("computeBlockHash is called") {
            val exception = shouldThrow<IllegalStateException> {
                signatureService.computeBlockHash(block)
            }
            Then("exception message should indicate a problem with computing the block hash") {
                exception.message shouldBe "Error when computing block hash"
            }
        }
    }

    Given("a DigitalSignatureService instance") {
        When("a new KeyPair is generated") {
            val result = signatureService.generateKeyPair()
            Then("KeyPair should not be null") {
                result.shouldNotBeNull()
                result.private.shouldNotBeNull()
                result.public.shouldNotBeNull()
            }
        }
    }

    Given("the byte array representation of a public key") {
        val keyPair = signatureService.generateKeyPair()!!
        val keyBytes = keyPair.public.encoded
        When("publicKeyFromBytes is called") {
            val result = signatureService.publicKeyFromBytes(keyBytes)
            Then("PublicKey instance should not be null and match initial key") {
                result.shouldNotBeNull()
                result.shouldBe(keyPair.public)
            }
        }
    }

    Given("the instance is constructed with algorithm names") {
        val sigSvc = DigitalSignatureService(messageDigestAlgorithm, keypairGeneratorAlgorithm, signatureAlgorithm)

        When("the algorithm names are retrieved") {
            val mdaResult = sigSvc.messageDigestAlgorithm
            val kpgaResult = sigSvc.keypairGeneratorAlgorithm
            val saResult = sigSvc.signatureAlgorithm

            Then("KeyPair should not be null and have correct length") {
                mdaResult shouldBe messageDigestAlgorithm
                kpgaResult shouldBe keypairGeneratorAlgorithm
                saResult shouldBe saResult
            }
        }
    }

    Given("the service is created with a bad message digest algorithm") {
        val sigSvc = DigitalSignatureService(badAlgorithm, keypairGeneratorAlgorithm, signatureAlgorithm)

        When("a message digest is requested") {
            val exception = shouldThrow<IllegalStateException> {
                sigSvc.computeBytesHash(byteArrayOf(1, 2, 3, 4, 5))
            }

            Then("Error message should indicate that the message digest algorithm is invalid") {
                exception.message shouldBe "$badAlgorithm algorithm not available"
            }
        }
    }

    Given("the service is created with a bad signature algorithm") {
        val sigSvc = DigitalSignatureService(messageDigestAlgorithm, keypairGeneratorAlgorithm, badAlgorithm)

        When("a digital signature is requested") {
            val exception = shouldThrow<IllegalStateException> {
                sigSvc.generateSignature(byteArrayOf(1, 2, 3, 4, 5))
            }

            Then("Error message should indicate that the digital signature algorithm is invalid") {
                exception.message shouldBe "Unexpected error when generating a signature"
            }
        }
    }

    Given("the service is created with a bad keypair generator algorithm") {

        When("a keypair is requested") {
            val exception = shouldThrow<IllegalStateException> {
                DigitalSignatureService(messageDigestAlgorithm, badAlgorithm, signatureAlgorithm)
            }

            Then("Error message should indicate that the key generation algorithm is invalid") {
                exception.message shouldBe "Could not generate a key pair for cryptological operations"
            }
        }
    }

    Given("key bytes to convert back to a key") {
        val keyBytes = signatureService.keyPair.private.encoded
        val invalidKeyBytes = keyBytes.sliceArray(50..100)

        When("a conversion from bytes to a public key is requested") {
            val exception = shouldThrow<IllegalStateException> {
                signatureService.publicKeyFromBytes(invalidKeyBytes)
            }

            Then("Error message should indicate that the key byte array is invalid") {
                exception.message shouldBe "Could not convert encoded public key to a public key instance"
            }
        }
    }
})
