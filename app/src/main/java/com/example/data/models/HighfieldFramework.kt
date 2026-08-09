package com.example.data.models

data class HighfieldSettings(
    val isHighfieldModeEnabled: Boolean = true,
    val selectedEngine: String = "Studio 3.1 Pro Engine",
    val fpsTarget: Int = 30, // 24, 30, 60
    val resolution: String = "1080p", // "720p", "1080p", "4K Ultra HD"
    val motionSmoothness: Float = 0.9f, // 0.0 - 1.0
    val raytracingSimulation: Boolean = true,
    val colorGradePreset: String = "Vibrant HDR Studio",
    val bitRateMbps: Int = 25,
    val aiVoiceoverSync: Boolean = true,
    val autoDenoiseAndSharpen: Boolean = true
)

object HighfieldDefaults {
    val engines = listOf(
        "Studio 3.1 Pro Engine",
        "Studio 3.1 Fast Preview",
        "OpenAI Sora Ultra Engine",
        "Runway Gen-3 Alpha",
        "Luma Dream Machine 1.5",
        "Kimi AI Director Pro"
    )

    val colorPresets = listOf(
        "Vibrant HDR Studio",
        "Teal & Orange Hollywood",
        "Cyberpunk Neon Glow",
        "Rec.709 Natural Cinematic",
        "Vintage Monochrome Noir",
        "Pastel Dreamlike Soft"
    )
}
