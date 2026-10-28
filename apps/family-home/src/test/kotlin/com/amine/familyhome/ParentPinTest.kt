package com.amine.familyhome

import org.junit.Assert.*
import org.junit.Test

class ParentPinTest {
    @Test fun absentPinPermitsTrustedFamilyActions() {
        assertTrue(ParentPin.accepts(FamilyState(), ""))
    }

    @Test fun saltedCredentialAcceptsOnlyCorrectPin() {
        val first = ParentPin.enable(FamilyState(), "1234")
        val second = ParentPin.enable(FamilyState(), "1234")
        assertTrue(first.pinEnabled)
        assertNotEquals(first.pinSalt, second.pinSalt)
        assertFalse(first.pinHash.contains("1234"))
        assertTrue(ParentPin.accepts(first, "1234"))
        assertFalse(ParentPin.accepts(first, "4321"))
        assertFalse(ParentPin.accepts(first, ""))
    }

    @Test fun pinRequiresFourDigits() {
        for (pin in listOf("", "123", "12345", "abcd", "12 4")) {
            assertThrows(IllegalArgumentException::class.java) { ParentPin.enable(FamilyState(), pin) }
        }
    }

    @Test fun malformedStoredCredentialNeverAuthenticates() {
        assertFalse(ParentPin.accepts(FamilyState(pinHash = "invalid", pinSalt = "broken"), "1234"))
    }
}
