package com.example.campuskart.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the part of the WhatsApp handoff that can be got wrong silently.
 *
 * A broken link does not crash and does not show an error - WhatsApp simply opens on a chat with
 * nobody, or the message arrives mangled. That failure only shows up by tapping the button on a
 * real phone with a real seller, which is exactly the kind of thing worth pinning down in a test
 * that runs in a second on the JVM.
 */
class WhatsAppContactTest {

    @Test
    fun `ten digit number gets the India country code`() {
        assertEquals("919876543210", WhatsAppContact.internationalNumber("9876543210"))
    }

    @Test
    fun `spaces and punctuation are stripped`() {
        assertEquals("919876543210", WhatsAppContact.internationalNumber("+91 98765-43210"))
    }

    @Test
    fun `domestic trunk prefix is dropped`() {
        assertEquals("919876543210", WhatsAppContact.internationalNumber("09876543210"))
    }

    @Test
    fun `already international numbers pass through`() {
        assertEquals("919876543210", WhatsAppContact.internationalNumber("919876543210"))
    }

    @Test
    fun `unusable numbers are rejected rather than guessed at`() {
        assertNull(WhatsAppContact.internationalNumber(""))
        assertNull(WhatsAppContact.internationalNumber("98765"))
        assertNull(WhatsAppContact.internationalNumber("not a number"))
        // Twelve digits, but not an Indian one - prefixing 91 would invent a number.
        assertNull(WhatsAppContact.internationalNumber("449876543210"))
    }

    @Test
    fun `chat url carries the number and the encoded message`() {
        val url = WhatsAppContact.chatUrl("9876543210", "Casio FX-991EX calculator")

        assertTrue(url!!.startsWith("https://wa.me/919876543210?text="))
        // The spaces and the quotes around the title have to survive as percent-escapes, or
        // everything after the first space is dropped from the prefilled message.
        assertTrue(url.contains("Casio+FX-991EX+calculator"))
        assertTrue("the raw title must not appear unescaped", !url.contains(" "))
    }

    @Test
    fun `chat url is null when the number is unusable`() {
        assertNull(WhatsAppContact.chatUrl("12345", "Lab coat"))
    }

    @Test
    fun `message names the listing so a seller knows which item it is about`() {
        val message = WhatsAppContact.message("  Lab coat, size M  ")

        assertTrue(message.contains("\"Lab coat, size M\""))
        assertTrue(message.contains("CampusKart"))
    }
}
