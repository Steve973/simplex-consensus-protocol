package org.storck.simplex.util

import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull

class CryptoUtilTest : FunSpec({
    test("testGenerateKeypair") {
        val result = CryptoUtil.generateKeyPair()

        result.shouldNotBeNull()
        result.private.shouldNotBeNull()
        result.public.shouldNotBeNull()
    }

    test("testGenerateSignatureSuccessful") {
        val keyPair = CryptoUtil.generateKeyPair()
        val input = byteArrayOf(1, 2, 3, 4, 5)

        val result = CryptoUtil.generateSignature(keyPair.private, input)

        result.shouldNotBeNull()
    }

    test("testVerifySignatureInvalid") {
        val keyPair = CryptoUtil.generateKeyPair()
        val input = byteArrayOf(1, 2, 3, 4, 5)
        val signature = CryptoUtil.generateSignature(keyPair.private, input)
        val wrongPublicKey = CryptoUtil.generateKeyPair().public

        val result = CryptoUtil.verifySignature(wrongPublicKey, input, signature)
        result.shouldBe(false)
    }

    test("testVerifySignatureValid") {
        val keyPair = CryptoUtil.generateKeyPair()
        val input = byteArrayOf(1, 2, 3, 4, 5)
        val signature = CryptoUtil.generateSignature(keyPair.private, input)

        val result = CryptoUtil.verifySignature(keyPair.public, input, signature)
        result.shouldBe(true)
    }
})