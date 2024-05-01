package org.storck.simplex.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.PublicKey;

class CryptoUtilTest {

    @Test
    public void testGenerateKeypair() throws Exception {
        // When
        KeyPair result = CryptoUtil.generateKeyPair();

        // Then
        Assertions.assertNotNull(result, "Generated KeyPair should not be null");
        Assertions.assertNotNull(result.getPrivate(), "Generated private key should not be null");
        Assertions.assertNotNull(result.getPublic(), "Generated public key should not be null");
    }

    @Test
    public void testGenerateSignatureSuccessful() throws Exception {
        // Given
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        byte[] input = new byte[] { 1, 2, 3, 4, 5 };

        // When
        byte[] result = CryptoUtil.generateSignature(keyPair.getPrivate(), input);

        // Then
        Assertions.assertNotNull(result, "Generated Signature should not be null");
    }

    @Test
    public void testVerifySignatureInvalid() throws Exception {
        // Given
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        byte[] input = new byte[] { 1, 2, 3, 4, 5 };
        byte[] signature = CryptoUtil.generateSignature(keyPair.getPrivate(), input);
        // generate a different public key to get a wrong/bad verification
        PublicKey wrongPublicKey = CryptoUtil.generateKeyPair().getPublic();

        // When
        boolean result = CryptoUtil.verifySignature(wrongPublicKey, input, signature);

        // Then
        Assertions.assertFalse(result);
    }

    @Test
    public void testVerifySignatureValid() throws Exception {
        // Given
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        byte[] input = new byte[] { 1, 2, 3, 4, 5 };
        byte[] signature = CryptoUtil.generateSignature(keyPair.getPrivate(), input);

        // When
        boolean result = CryptoUtil.verifySignature(keyPair.getPublic(), input, signature);

        // Then
        Assertions.assertTrue(result);
    }
}