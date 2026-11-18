package dev.prootcode.app.ui.screens

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LinkWebViewAddressTest {
    @Test
    fun `adds https to bare addresses`() {
        assertEquals("https://example.com", normalizeAddress(" example.com "))
    }

    @Test
    fun `keeps explicit http and https addresses`() {
        assertEquals("http://example.com/path?q=1", normalizeAddress("http://example.com/path?q=1"))
        assertEquals("https://example.com", normalizeAddress("https://example.com"))
    }

    @Test
    fun `rejects blank and non web addresses`() {
        assertNull(normalizeAddress("  "))
        assertNull(normalizeAddress("file:///etc/passwd"))
        assertNull(normalizeAddress("javascript:alert(1)"))
        assertNull(normalizeAddress("https://"))
    }
}
