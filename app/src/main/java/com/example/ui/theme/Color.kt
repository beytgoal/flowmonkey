package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Modern Solid iOS Palette (High Contrast & Clear Legibility)
val StudioCleanCanvas = Color(0xFFF8FAFC)
val StudioCanvasSlate = Color(0xFFF1F5F9)
val StudioCardWhite = Color(0xFFFFFFFF)
val StudioGlassWhite = Color(0xFFFFFFFF) // 100% Solid White
val StudioGlassPanel = Color(0xFFF8FAFC) // 100% Solid Slate Canvas
val StudioCardHairline = Color(0xFFE2E8F0) // Crisp solid border (Slate 200)
val StudioGlassHighlight = Color(0xFFCBD5E1) // Solid Slate 300
val StudioDarkCharcoal = Color(0xFF0F172A) // iOS Deep Midnight / Black CTA
val StudioDarkCTA = Color(0xFF0F172A)
val StudioDarkCTASecondary = Color(0xFF1E293B)
val StudioPillBg = Color(0xFFF1F5F9) // Solid Slate 100
val StudioPillHover = Color(0xFFE2E8F0)
val StudioTextDark = Color(0xFF0F172A) // Primary Black (Deep Slate)
val StudioTextMuted = Color(0xFF475569) // Slate 600 Subtitle/Meta (High Contrast)
val StudioTextSubtle = Color(0xFF64748B) // Slate 500 Subtitle/Meta (Clearly Legible)

// Soft Pastel Ambient Gradients (Solid Depth)
val StudioPastelSky = Color(0xFFE0F2FE)
val StudioPastelRose = Color(0xFFFDE2E4)
val StudioPastelLavender = Color(0xFFEDE9FE)
val StudioPastelAmber = Color(0xFFFEF3C7)
val StudioPastelMint = Color(0xFFD1FAE5)

// Ambient Canvas Background Gradient Brush
val StudioAmbientCanvasBrush = Brush.verticalGradient(
    listOf(
        Color(0xFFF8FAFC),
        Color(0xFFF1F5F9),
        Color(0xFFF8FAFC)
    )
)

// Glass Card Gradient Highlight Brush (Solid Contrast)
val StudioGlassCardBrush = Brush.linearGradient(
    listOf(
        Color(0xFFFFFFFF),
        Color(0xFFF8FAFC),
        Color(0xFFFFFFFF)
    )
)

// Vibrant Modern Accents (iOS Standards)
val StudioElectricBlue = Color(0xFF2563EB) // iOS Primary Active Blue
val StudioEmeraldGreen = Color(0xFF10B981) // Success / Rendered
val StudioAmberGold = Color(0xFFF59E0B) // Warning / Attention
val StudioRosePink = Color(0xFFF43F5E) // Accent Rose
val StudioVioletIndigo = Color(0xFF6366F1) // AI Sparkle

// Legacy Aliases for seamless component compatibility
val StudioDarkBg = StudioCleanCanvas
val StudioCardBg = StudioCardWhite
val StudioCardSolid = StudioCardWhite
val StudioCardBorder = StudioCardHairline
val StudioGlassFill = Color(0xFFF1F5F9)
val StudioPrimaryViolet = StudioElectricBlue
val StudioSecondaryTeal = StudioEmeraldGreen
val StudioAccentAmber = StudioAmberGold
val StudioAccentPink = StudioRosePink
val StudioSurfaceDark = StudioCardWhite
val StudioTextPrimary = StudioTextDark
val StudioTextSecondary = StudioTextMuted
val StudioLightBg = StudioCleanCanvas
val StudioSurfaceLight = StudioCardWhite

val LightColorScheme = androidx.compose.material3.lightColorScheme(
    primary = StudioDarkCharcoal,
    onPrimary = Color.White,
    secondary = StudioElectricBlue,
    onSecondary = Color.White,
    tertiary = StudioEmeraldGreen,
    background = StudioCleanCanvas,
    onBackground = StudioTextDark,
    surface = StudioCardWhite,
    onSurface = StudioTextDark,
    surfaceVariant = StudioPillBg,
    onSurfaceVariant = StudioTextMuted
)

val DarkColorScheme = LightColorScheme




