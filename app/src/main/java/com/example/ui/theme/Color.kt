package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Modern iOS Glassmorphism & Neumorphism Palette
val StudioCleanCanvas = Color(0xFFF8FAFC)
val StudioCanvasSlate = Color(0xFFF1F5F9)
val StudioCardWhite = Color(0xFFFFFFFF)
val StudioGlassWhite = Color(0xE6FFFFFF) // 90% semi-transparent liquid glass
val StudioGlassPanel = Color(0xD9FFFFFF) // 85% transparent glass
val StudioCardHairline = Color(0x14000000) // Super-thin soft border (8% black)
val StudioGlassHighlight = Color(0xB3FFFFFF) // 70% white glossy reflection border
val StudioDarkCharcoal = Color(0xFF0F172A) // iOS Deep Midnight / Black CTA
val StudioDarkCTA = Color(0xFF0F172A)
val StudioDarkCTASecondary = Color(0xFF1E293B)
val StudioPillBg = Color(0xFFF1F5F9)
val StudioPillHover = Color(0xFFE2E8F0)
val StudioTextDark = Color(0xFF0F172A) // Primary Black (Deep Slate)
val StudioTextMuted = Color(0xFF64748B) // Slate 500 Subtitle/Meta
val StudioTextSubtle = Color(0xFF94A3B8) // Slate 400 Inactive

// Soft Pastel Ambient Gradients (Glass Depth)
val StudioPastelSky = Color(0xFFE0F2FE)
val StudioPastelRose = Color(0xFFFDE2E4)
val StudioPastelLavender = Color(0xFFEDE9FE)
val StudioPastelAmber = Color(0xFFFEF3C7)
val StudioPastelMint = Color(0xFFD1FAE5)

// Ambient Canvas Background Gradient Brush
val StudioAmbientCanvasBrush = Brush.verticalGradient(
    listOf(
        Color(0xFFF8FAFC),
        Color(0xFFEFF6FF),
        Color(0xFFF5F3FF),
        Color(0xFFF8FAFC)
    )
)

// Glass Card Gradient Highlight Brush
val StudioGlassCardBrush = Brush.linearGradient(
    listOf(
        Color(0xF5FFFFFF),
        Color(0xEAFFFFFF),
        Color(0xE0F8FAFC)
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
val StudioCardBg = StudioGlassWhite
val StudioCardSolid = StudioCardWhite
val StudioCardBorder = StudioCardHairline
val StudioGlassFill = Color(0x14FFFFFF)
val StudioPrimaryViolet = StudioElectricBlue
val StudioSecondaryTeal = StudioEmeraldGreen
val StudioAccentAmber = StudioAmberGold
val StudioAccentPink = StudioRosePink
val StudioSurfaceDark = StudioPillBg
val StudioTextPrimary = StudioTextDark
val StudioTextSecondary = StudioTextMuted
val StudioLightBg = StudioCleanCanvas
val StudioSurfaceLight = StudioPillBg

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




