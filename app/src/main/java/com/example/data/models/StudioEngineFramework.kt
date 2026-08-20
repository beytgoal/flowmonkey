package com.example.data.models

data class StudioEngineSettings(
    val isEngineModeEnabled: Boolean = true,
    val selectedEngine: String = "Studio AI Pro (Default)",
    val fpsTarget: Int = 60, // 24, 30, 60
    val resolution: String = "1080p", // "720p", "1080p", "4K Ultra HD"
    val motionSmoothness: Float = 0.95f, // 0.0 - 1.0
    val raytracingSimulation: Boolean = true,
    val colorGradePreset: String = "Vibrant HDR Studio",
    val bitRateMbps: Int = 30,
    val aiVoiceoverSync: Boolean = true,
    val autoDenoiseAndSharpen: Boolean = true
)

typealias HighfieldSettings = StudioEngineSettings

object StudioEngineDefaults {
    val engines = listOf(
        "Studio AI Pro (Default)",
        "Studio AI Fast Preview",
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

object HighfieldDefaults {
    val engines = StudioEngineDefaults.engines
    val colorPresets = StudioEngineDefaults.colorPresets
}

