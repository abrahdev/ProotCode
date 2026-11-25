package dev.prootcode.app.ui.screens

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class GuestLinkTest {
    @Test
    fun `adds a default scheme to bare hosts`() {
        assertEquals("http://192.168.1.10:8080", normalizeGuestUrl("192.168.1.10:8080"))
    }

    @Test
    fun `keeps https URLs`() {
        assertEquals("https://code.example.com", normalizeGuestUrl("https://code.example.com"))
    }

    @Test
    fun `rejects empty and non-http URLs`() {
        assertNull(normalizeGuestUrl("  "))
        assertNull(normalizeGuestUrl("ftp://example.com"))
    }

    @Test
    fun `auto-login script is empty without a password`() {
        assertEquals("", autoLoginScript(null))
        assertEquals("", autoLoginScript("  "))
    }
}
