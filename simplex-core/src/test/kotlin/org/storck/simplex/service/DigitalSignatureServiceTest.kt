package org.storck.simplex.service

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import java.security.KeyPair
import java.security.PublicKey

@SuppressFBWarnings(
    value = ["NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE"],
    justification = "I cannot find anything wrong with the test.")
class DigitalSignatureServiceTest : FunSpec({
    test("testGenerateKeypair") {
        val result : KeyPair = DigitalSignatureService.generateKeyPair()!!

        result.shouldNotBeNull()
        result.private!!.shouldNotBeNull()
        result.public!!.shouldNotBeNull()
    }

    test("testGenerateSignatureSuccessful") {
        val signatureService = DigitalSignatureService()
        val input = byteArrayOf(1, 2, 3, 4, 5)

        val result : ByteArray = signatureService.generateSignature(input)!!

        result.shouldNotBeNull()
    }

    test("testVerifySignatureInvalid") {
        val signatureService = DigitalSignatureService()
        val input = byteArrayOf(1, 2, 3, 4, 5)
        val signature : ByteArray = signatureService.generateSignature(input)!!
        val wrongPublicKey : PublicKey = DigitalSignatureService.generateKeyPair()!!.public!!

        val result : Boolean = signatureService.verifySignature(input, signature, wrongPublicKey)
        result.shouldBe(false)
    }

    test("testVerifySignatureValid") {
        val signatureService = DigitalSignatureService()
        val input : ByteArray = byteArrayOf(1, 2, 3, 4, 5)
        val signature : ByteArray = signatureService.generateSignature(input)!!

        val result : Boolean = signatureService.verifySignature(input, signature, signatureService.keyPair!!.public!!)
        result.shouldBe(true)
    }
})