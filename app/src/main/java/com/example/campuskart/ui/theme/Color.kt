package com.example.campuskart.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * CampusKart's fixed brand palette - a green-teal, chosen on Day 2.
 *
 * Material You dynamic colour is deliberately switched off (see [CampusKartTheme]): it recolours
 * the app from the device wallpaper, so the app would look different on the development phone,
 * the demo phone and in the README screenshots. A fixed palette keeps all three identical.
 *
 * The tonal values follow Material 3's light/dark scheme roles.
 */

// --- Light scheme ---
val GreenPrimaryLight = Color(0xFF006B5B)
val GreenOnPrimaryLight = Color(0xFFFFFFFF)
val GreenPrimaryContainerLight = Color(0xFF7FF7DF)
val GreenOnPrimaryContainerLight = Color(0xFF00201A)

val GreenSecondaryLight = Color(0xFF4A635C)
val GreenOnSecondaryLight = Color(0xFFFFFFFF)
val GreenSecondaryContainerLight = Color(0xFFCCE8DF)
val GreenOnSecondaryContainerLight = Color(0xFF06201A)

val BlueTertiaryLight = Color(0xFF416277)
val BlueOnTertiaryLight = Color(0xFFFFFFFF)
val BlueTertiaryContainerLight = Color(0xFFC5E7FF)
val BlueOnTertiaryContainerLight = Color(0xFF001E2E)

val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

val SurfaceLight = Color(0xFFF5FBF7)
val OnSurfaceLight = Color(0xFF171D1B)
val SurfaceVariantLight = Color(0xFFDBE5E0)
val OnSurfaceVariantLight = Color(0xFF3F4945)
val OutlineLight = Color(0xFF6F7975)
val OutlineVariantLight = Color(0xFFBFC9C4)

val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFEFF5F1)
val SurfaceContainerLight = Color(0xFFE9EFEB)
val SurfaceContainerHighLight = Color(0xFFE3EAE6)
val SurfaceContainerHighestLight = Color(0xFFDEE4E0)

// --- Dark scheme ---
val GreenPrimaryDark = Color(0xFF61DAC2)
val GreenOnPrimaryDark = Color(0xFF00382F)
val GreenPrimaryContainerDark = Color(0xFF005045)
val GreenOnPrimaryContainerDark = Color(0xFF7FF7DF)

val GreenSecondaryDark = Color(0xFFB1CCC3)
val GreenOnSecondaryDark = Color(0xFF1C352F)
val GreenSecondaryContainerDark = Color(0xFF334B45)
val GreenOnSecondaryContainerDark = Color(0xFFCCE8DF)

val BlueTertiaryDark = Color(0xFFA9CBE3)
val BlueOnTertiaryDark = Color(0xFF0F3448)
val BlueTertiaryContainerDark = Color(0xFF284B60)
val BlueOnTertiaryContainerDark = Color(0xFFC5E7FF)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

val SurfaceDark = Color(0xFF0E1513)
val OnSurfaceDark = Color(0xFFDEE4E0)
val SurfaceVariantDark = Color(0xFF3F4945)
val OnSurfaceVariantDark = Color(0xFFBFC9C4)
val OutlineDark = Color(0xFF899390)
val OutlineVariantDark = Color(0xFF3F4945)

val SurfaceContainerLowestDark = Color(0xFF090F0E)
val SurfaceContainerLowDark = Color(0xFF171D1B)
val SurfaceContainerDark = Color(0xFF1B211F)
val SurfaceContainerHighDark = Color(0xFF252B2A)
val SurfaceContainerHighestDark = Color(0xFF303634)

/**
 * WhatsApp's own brand green. Used only for the "Chat on WhatsApp" button on Item Detail, so the
 * app's single most important action is instantly recognisable. Fixed in both light and dark
 * because it belongs to WhatsApp, not to CampusKart's palette.
 */
val WhatsAppGreen = Color(0xFF25D366)
val OnWhatsAppGreen = Color(0xFF00330F)
