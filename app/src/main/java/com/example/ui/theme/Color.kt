package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Výchozí královská modrá a břidlicová paleta (Redakční styl)
val RoyalBluePrimaryLight = Color(0xFF1E40AF)
val RoyalBlueOnPrimaryLight = Color(0xFFFFFFFF)
val RoyalBlueContainerLight = Color(0xFFDBEAFE)
val RoyalBlueOnContainerLight = Color(0xFF1E3A8A)

val SlateSecondaryLight = Color(0xFF475569)
val SlateOnSecondaryLight = Color(0xFFFFFFFF)
val SlateContainerLight = Color(0xFFE2E8F0)
val SlateOnContainerLight = Color(0xFF0F172A)

val EditorialBackgroundLight = Color(0xFFF8FAFC)
val EditorialSurfaceLight = Color(0xFFFFFFFF)
val EditorialSurfaceVariantLight = Color(0xFFF1F5F9)
val EditorialOutlineLight = Color(0xFFCBD5E1)

// Tmavý břidlicový režim
val RoyalBluePrimaryDark = Color(0xFF60A5FA)
val RoyalBlueOnPrimaryDark = Color(0xFF0F172A)
val RoyalBlueContainerDark = Color(0xFF1E3A8A)
val RoyalBlueOnContainerDark = Color(0xFFDBEAFE)

val SlateSecondaryDark = Color(0xFF94A3B8)
val SlateOnSecondaryDark = Color(0xFF0F172A)
val SlateContainerDark = Color(0xFF334155)
val SlateOnContainerDark = Color(0xFFF1F5F9)

val EditorialBackgroundDark = Color(0xFF0F172A)
val EditorialSurfaceDark = Color(0xFF1E293B)
val EditorialSurfaceVariantDark = Color(0xFF334155)
val EditorialOutlineDark = Color(0xFF475569)

// AMOLED čistě černý režim
val AmoledBackground = Color(0xFF000000)
val AmoledSurface = Color(0xFF070B14)
val AmoledSurfaceVariant = Color(0xFF111827)

// Předvolby vlastních akcentních barev v nastavení vzhledu
data class AccentColorOption(
    val name: String,
    val colorHex: Long
)

val KuryrAccentPalette = listOf(
    AccentColorOption("Královská modrá", 0xFF1E40AF),
    AccentColorOption("Břidlicová modř", 0xFF0369A1),
    AccentColorOption("Smaragdová", 0xFF047857),
    AccentColorOption("Teplá terakota", 0xFFB45309),
    AccentColorOption("Královský ametyst", 0xFF6D28D9),
    AccentColorOption("Karmínová", 0xFFBE123C)
)
