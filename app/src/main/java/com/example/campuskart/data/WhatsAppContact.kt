package com.example.campuskart.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import java.net.URLEncoder

/**
 * The app's entire messaging feature: a link into WhatsApp (FR-CONTACT-001/002).
 *
 * CampusKart deliberately implements no chat of its own (SRS 2.1). Students already have
 * WhatsApp, already trust it, and a home-grown inbox would mean push notifications, read
 * receipts, moderation and a second unread badge to maintain - for a course project whose point
 * is the marketplace, not the messenger.
 *
 * The seller's number never reaches the screen (FR-CONTACT-003). It is read from the seller's
 * `users` document when Item Detail loads and is only ever spent here, building the link at the
 * moment the button is tapped.
 *
 * [chatUrl] and its helpers are pure Kotlin with no Android imports so they can be unit tested
 * on the JVM (see WhatsAppContactTest); only [openChat] needs a Context.
 */
object WhatsAppContact {

    private const val TAG = "CampusKartWhatsApp"

    /** India. Signup collects a ten-digit national number, so the code is added here. */
    private const val COUNTRY_CODE = "91"

    /** WhatsApp proper, then WhatsApp Business - a fair number of student sellers use it. */
    private val WHATSAPP_PACKAGES = listOf("com.whatsapp", "com.whatsapp.w4b")

    /**
     * The message the buyer arrives with, already typed.
     *
     * It names the listing because a seller with four items posted cannot otherwise tell which
     * one a bare "is this available?" is about - and it stops short of the price, so the
     * conversation opens on the item rather than on a negotiation.
     */
    fun message(listingTitle: String): String =
        "Hi! I saw your \"${listingTitle.trim()}\" listing on CampusKart. Is it still available?"

    /**
     * The `wa.me` link for [number], carrying [listingTitle] as the prefilled text.
     *
     * Returns null when the number cannot be made sense of, which is what the screen uses to
     * disable the button rather than opening a chat with nobody.
     */
    fun chatUrl(number: String, listingTitle: String): String? {
        val international = internationalNumber(number) ?: return null
        val text = URLEncoder.encode(message(listingTitle), "UTF-8")
        // wa.me rather than the whatsapp:// scheme: the same URL works as an app deep link and,
        // when WhatsApp is not installed, as an ordinary web page that offers to open it.
        return "https://wa.me/$international?text=$text"
    }

    /**
     * Normalises whatever is stored on the profile into the digits-only international form
     * `wa.me` expects - no plus, no spaces, no dashes.
     *
     * Signup validates and stores exactly ten national digits, so in practice this only ever
     * prefixes the country code. The other branches exist because the `users` collection is
     * older than that validation and a profile written by hand in the Firebase Console can hold
     * anything; a number that is already international, or carries the domestic trunk `0`,
     * should still produce a working link rather than a silently broken button.
     */
    internal fun internationalNumber(raw: String): String? {
        val digits = raw.filter(Char::isDigit)
        return when {
            // "9876543210" - what signup writes.
            digits.length == 10 -> "$COUNTRY_CODE$digits"

            // "09876543210" - the trunk prefix used for domestic dialling, meaningless here.
            digits.length == 11 && digits.startsWith("0") -> "$COUNTRY_CODE${digits.drop(1)}"

            // "+91 98765 43210" - already international, and the plus is stripped above.
            digits.length == 12 && digits.startsWith(COUNTRY_CODE) -> digits

            // Anything else is not a number this app can build a link from.
            else -> null
        }
    }

    /**
     * Opens the chat, and reports whether anything on the device was able to take it.
     *
     * Three attempts in descending order of directness:
     *
     *  1. WhatsApp itself, by package, so the chat opens with no chooser and no browser hop.
     *  2. WhatsApp Business, same link.
     *  3. Whatever handles https - almost always a browser, which lands on WhatsApp's own
     *     "continue to chat" page. This is the path an emulator without WhatsApp takes, which is
     *     the difference between the button being demonstrable during development and being a
     *     dead control until the app is on a real phone.
     *
     * Returns false only when all three fail, which the screen reports as a message rather than
     * letting an ActivityNotFoundException take the app down.
     */
    fun openChat(context: Context, number: String, listingTitle: String): Boolean {
        val url = chatUrl(number, listingTitle) ?: return false
        val uri = Uri.parse(url)

        for (whatsapp in WHATSAPP_PACKAGES) {
            val intent = Intent(Intent.ACTION_VIEW, uri).setPackage(whatsapp)
            if (start(context, intent)) return true
        }

        return start(context, Intent(Intent.ACTION_VIEW, uri))
    }

    /**
     * `startActivity` is what decides whether an app is really there.
     *
     * `resolveActivity` would be the usual check, but since Android 11 it answers "no" for any
     * package this app has not declared in the manifest's `<queries>` block - so a check that
     * looks correct silently reports WhatsApp as missing on every modern phone. The manifest
     * does declare both WhatsApp packages, but launching and catching is what actually works
     * either way, and it is one call instead of two.
     */
    private fun start(context: Context, intent: Intent): Boolean = try {
        // The activity is started from a Context that may not be an Activity (a ViewModel's
        // application context reaching this through a callback), which Android requires a new
        // task for.
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        Log.i(TAG, "Nothing on this device handles ${intent.`package` ?: "https"}", e)
        false
    }
}
