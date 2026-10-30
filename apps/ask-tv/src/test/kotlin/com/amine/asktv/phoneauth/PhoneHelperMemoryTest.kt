package com.amine.asktv.phoneauth

import org.junit.Assert.*
import org.junit.Test

/** Pure process-memory confirmation ownership, including the permission-result interval. */
class PhoneHelperMemoryTest {
    private val first = PhonePairingLink("192.168.1.2", 8443, "ab".repeat(32), "a".repeat(43))
    private val other = PhonePairingLink("192.168.1.3", 8443, "cd".repeat(32), "b".repeat(43))

    @Test fun newLinkDuringPermissionsCannotReplaceTheConfirmedCandidate() {
        val memory = PhoneHelperMemory()
        assertTrue(memory.offerCandidate(first))
        assertTrue(memory.confirmCandidate())
        assertTrue(memory.continueRequested)
        assertFalse(memory.permissionsReady)
        assertFalse(memory.offerCandidate(other))
        memory.permissionsReady = true
        assertEquals(first, memory.consumeConfirmedCandidate())
        assertNull(memory.consumeConfirmedCandidate())
    }
    @Test fun asynchronousCandidateChangeCannotChangeTheExactConfirmedLink() {
        val memory = PhoneHelperMemory()
        memory.candidate = first
        assertTrue(memory.confirmCandidate())
        memory.candidate = other
        assertEquals(first, memory.consumeConfirmedCandidate())
    }
    @Test fun explicitCancelAllowsANewLinkButNeverResumesTheOldConfirmation() {
        val memory = PhoneHelperMemory()
        memory.offerCandidate(first)
        assertTrue(memory.confirmCandidate())
        memory.cancelConfirmation()
        assertNull(memory.consumeConfirmedCandidate())
        assertTrue(memory.offerCandidate(other))
        assertFalse(memory.continueRequested)
        assertTrue(memory.confirmCandidate())
        assertEquals(other, memory.consumeConfirmedCandidate())
    }
}
