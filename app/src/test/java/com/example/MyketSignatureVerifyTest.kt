package com.example

import com.example.billing.BillingManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.KeyPairGenerator
import java.security.Signature

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MyketSignatureVerifyTest {

    @Test
    fun testMyketPublicKeyConstant() {
        assertEquals(
            "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQChuEJCVjyXbvgOF74J3Dzbo86482Q+HI7UFFX1odEhqQ4qBO/62dEtjBvMzxUe8rZeVEpxJeATTx95+FIZJPiogXVCYhWwXokrSYO0F67wgTivoapyUUIevzZtZhi6JTEQebIPNR2J8qIRPzzC09FOVylhNobJGqXXcet1fzlrOQIDAQAB",
            BillingManager.MYKET_PUBLIC_KEY
        )
    }

    @Test
    fun testRsaSignatureVerificationValid() {
        // Generate dynamic RSA key pair
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(1024)
        val kp = kpg.generateKeyPair()

        val sampleData = "{\"orderId\":\"1234\",\"packageName\":\"com.example\",\"productId\":\"kiseh_pro_lifetime\",\"purchaseTime\":1700000000,\"purchaseState\":0,\"purchaseToken\":\"valid_token_xyz\"}"

        // Sign data using private key
        val signer = Signature.getInstance("SHA1withRSA")
        signer.initSign(kp.private)
        signer.update(sampleData.toByteArray(Charsets.UTF_8))
        val signedBytes = signer.sign()
        val base64Signature = android.util.Base64.encodeToString(signedBytes, android.util.Base64.NO_WRAP)
        val base64PublicKey = android.util.Base64.encodeToString(kp.public.encoded, android.util.Base64.NO_WRAP)

        val isValid = BillingManager.verifyPurchase(sampleData, base64Signature, base64PublicKey)
        assertTrue("Signature should be valid with matching key", isValid)
    }

    @Test
    fun testRsaSignatureVerificationInvalid() {
        val sampleData = "{\"orderId\":\"1234\",\"productId\":\"kiseh_pro_lifetime\"}"
        val invalidSignature = "invalid_base64_signature_here"

        val isValid = BillingManager.verifyPurchase(sampleData, invalidSignature, BillingManager.MYKET_PUBLIC_KEY)
        assertFalse("Invalid signature should fail verification", isValid)
    }

    @Test
    fun testRsaSignatureVerificationEmpty() {
        assertFalse(BillingManager.verifyPurchase("", null))
        assertFalse(BillingManager.verifyPurchase("data", ""))
        assertFalse(BillingManager.verifyPurchase("data", "sig", ""))
    }
}
