package com.example.data.models

data class HighfieldSettings(
    val isHighfieldModeEnabled: Boolean = true,
    val selectedEngine: String = "Veo 3.1 Pro (Highfield Engine)",
    val fpsTarget: Int = 30, // 24, 30, 60
    val resolution: String = "1080p", // "720p", "1080p", "4K Ultra HD"
    val motionSmoothness: Float = 0.9f, // 0.0 - 1.0
    val raytracingSimulation: Boolean = true,
    val colorGradePreset: String = "Vibrant HDR Highfield",
    val bitRateMbps: Int = 25,
    val aiVoiceoverSync: Boolean = true,
    val autoDenoiseAndSharpen: Boolean = true
)

object HighfieldDefaults {
    val engines = listOf(
        "Veo 3.1 Pro (Highfield Engine)",
        "Veo 3.1 Fast Preview",
        "OpenAI Sora (Highfield Ultra)",
        "Runway Gen-3 Alpha",
        "Luma Dream Machine 1.5",
        "Kimi AI Director Pro"
    )

    val colorPresets = listOf(
        "Vibrant HDR Highfield",
        "Teal & Orange Hollywood",
        "Cyberpunk Neon Glow",
        "Rec.709 Natural Cinematic",
        "Vintage Monochrome Noir",
        "Pastel Dreamlike Soft"
    )
}
