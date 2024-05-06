package org.storck.simplex.service

import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import org.storck.simplex.service.DigitalSignatureService

class DigitalSignatureServiceTest : FunSpec({
    test("testGenerateKeypair") {
        val result = DigitalSignatureService.generateKeyPair()

        result.shouldNotBeNull()
        result.private.shouldNotBeNull()
        result.public.shouldNotBeNull()
    }

    test("testGenerateSignatureSuccessful") {
        val signatureService = DigitalSignatureService()
        val input = byteArrayOf(1, 2, 3, 4, 5)

        val result = signatureService.generateSignature(input)

        result.shouldNotBeNull()
    }

    test("testVerifySignatureInvalid") {
        val signatureService = DigitalSignatureService()
        val input = byteArrayOf(1, 2, 3, 4, 5)
        val signature = signatureService.generateSignature(input)
        val wrongPublicKey = DigitalSignatureService.generateKeyPair().public

        val result = signatureService.verifySignature(input, signature, wrongPublicKey)
        result.shouldBe(false)
    }

    test("testVerifySignatureValid") {
        val signatureService = DigitalSignatureService()
        val input = byteArrayOf(1, 2, 3, 4, 5)
        val signature = signatureService.generateSignature(input)

        val result = signatureService.verifySignature(input, signature, signatureService.keyPair.public)
        result.shouldBe(true)
    }
})