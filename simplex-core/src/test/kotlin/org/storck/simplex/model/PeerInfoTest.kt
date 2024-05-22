package org.storck.simplex.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.*

/**
 * Tests the PeerInfo customized methods.
 */
@SuppressFBWarnings(
   value = ["NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", "SE_BAD_FIELD"],
   justification = "It is a test.")
class PeerInfoShouldSpecTest : ShouldSpec({

   should("initialize PeerInfo") {
      val peerId = "peerId"
      val publicKeyBytes = byteArrayOf(1, 2, 3)
      val peerInfo = PeerInfo(peerId, publicKeyBytes)

      // Verification
      peerInfo shouldNotBe null
      peerInfo.peerId shouldBe peerId
      peerInfo.publicKeyBytes shouldBe publicKeyBytes
   }

   should("test equals method of PeerInfo") {
      val publicKeyBytes = byteArrayOf(1, 2, 3)
      val peerInfo1 = PeerInfo("peerId1", publicKeyBytes)
      val peerInfo2 = PeerInfo("peerId1", publicKeyBytes)

      // Verification
      (peerInfo1 == peerInfo2) shouldBe true
   }

   should("test equals method of PeerInfo with different peerIds") {
      val publicKeyBytes = byteArrayOf(1, 2, 3)
      val peerInfo1 = PeerInfo("peerId1", publicKeyBytes)
      val peerInfo2 = PeerInfo("peerId2", publicKeyBytes)

      // Verification
      (peerInfo1 == peerInfo2) shouldBe false
   }

   should("test equals method of PeerInfo with different public key bytes") {
      val publicKeyBytes1 = byteArrayOf(1, 2, 3)
      val publicKeyBytes2 = byteArrayOf(2, 3, 4)
      val peerInfo1 = PeerInfo("peerId1", publicKeyBytes1)
      val peerInfo2 = PeerInfo("peerId1", publicKeyBytes2)

      // Verification
      (peerInfo1 == peerInfo2) shouldBe false
   }

   should("test equals method of PeerInfo with same instance compared") {
      val publicKeyBytes = byteArrayOf(1, 2, 3)
      val peerInfo = PeerInfo("peerId1", publicKeyBytes)

      // Verification
      (peerInfo == peerInfo) shouldBe true
   }

   should("test hashCode method of PeerInfo") {
      val publicKeyBytes = byteArrayOf(1, 2, 3)
      val peerInfo = PeerInfo("peerId", publicKeyBytes)

      // Verification
      peerInfo.hashCode() shouldBe Objects.hash(peerInfo.peerId) * 31 + Arrays.hashCode(publicKeyBytes)
   }

   should("test toString method of PeerInfo") {
      val publicKeyBytes = byteArrayOf(1, 2, 3)
      val peerInfo = PeerInfo("peerId", publicKeyBytes)

      // Verification
      peerInfo.toString() shouldBe "PeerInfo{peerId=peerId, publicKeyBytes=[1, 2, 3]}"
   }

   should("throw NullPointerException when initialize PeerInfo with null peerId") {
      shouldThrow<NullPointerException> {
         PeerInfo(null, byteArrayOf(1, 2, 3))
      }
   }

   should("throw NullPointerException when initialize PeerInfo with null publicKeyBytes") {
      shouldThrow<NullPointerException> {
         PeerInfo("peerId", null)
      }
   }
})