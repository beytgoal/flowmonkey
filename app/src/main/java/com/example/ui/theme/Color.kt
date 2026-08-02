package com.example.ui.theme

import androidx.compose.ui.graphics.Color

val StudioDarkBg = Color(0xFF0F0F14)
val StudioCardBg = Color(0xFF181820)
val StudioCardSolid = Color(0xFF1E1E28)
val StudioCardBorder = Color(0xFF282836)
val StudioGlassFill = Color(0x1AFFFFFF)
val StudioPrimaryViolet = Color(0xFF6366F1)
val StudioSecondaryTeal = Color(0xFF10B981)
val StudioAccentAmber = Color(0xFFF59E0B)
val StudioAccentPink = Color(0xFFF43F5E)
val StudioSurfaceDark = Color(0xFF181820)
val StudioTextPrimary = Color(0xFFF3F4F6)
val StudioTextSecondary = Color(0xFF9CA3AF)

val StudioLightBg = StudioDarkBg
val StudioSurfaceLight = StudioSurfaceDark

val DarkColorScheme = androidx.compose.material3.darkColorScheme(
    primary = StudioPrimaryViolet,
    onPrimary = Color.White,
    secondary = StudioSecondaryTeal,
    onSecondary = Color.Black,
    tertiary = StudioAccentPink,
    background = StudioDarkBg,
    onBackground = StudioTextPrimary,
    surface = StudioCardBg,
    onSurface = StudioTextPrimary,
    surfaceVariant = StudioSurfaceDark,
    onSurfaceVariant = StudioTextSecondary
)

val LightColorScheme = DarkColorScheme



