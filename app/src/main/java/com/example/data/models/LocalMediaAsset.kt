package com.example.data.models

import java.util.UUID

data class LocalMediaAsset(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val category: String, // "VIDEO", "AUDIO", "IMAGE"
    val uri: String = "",
    val durationText: String = "00:05",
    val durationMs: Long = 5000L,
    val resolutionOrType: String = "1080p FHD",
    val isAiGenerated: Boolean = true,
    val dateAdded: String = "Terbaru",
    val tags: List<String> = emptyList()
)

object DefaultMediaAssets {
    val initialAssets = listOf(
        LocalMediaAsset(
            id = "asset_1",
            title = "Cyberpunk Night City Visual",
            category = "VIDEO",
            uri = "sample_cyberpunk_night",
            durationText = "00:08",
            durationMs = 8000L,
            resolutionOrType = "1080p FHD (24 FPS)",
            isAiGenerated = true,
            dateAdded = "03 Agu 2026",
            tags = listOf("AI", "Cinematic", "Neon")
        ),
        LocalMediaAsset(
            id = "asset_2",
            title = "Futuristic Drone Sunset",
            category = "VIDEO",
            uri = "sample_drone_sunset",
            durationText = "00:06",
            durationMs = 6000L,
            resolutionOrType = "1080p FHD (30 FPS)",
            isAiGenerated = true,
            dateAdded = "03 Agu 2026",
            tags = listOf("Drone", "Sunset", "4K")
        ),
        LocalMediaAsset(
            id = "asset_3",
            title = "Lofi Synthwave Background Track",
            category = "AUDIO",
            uri = "sfx_synth_lofi",
            durationText = "00:30",
            durationMs = 30000L,
            resolutionOrType = "MP3 (320kbps)",
            isAiGenerated = false,
            dateAdded = "02 Agu 2026",
            tags = listOf("Musik", "Lofi", "BGM")
        ),
        LocalMediaAsset(
            id = "asset_4",
            title = "Cinematic Whoosh SFX",
            category = "AUDIO",
            uri = "sfx_whoosh_trans",
            durationText = "00:02",
            durationMs = 2000L,
            resolutionOrType = "WAV Audio",
            isAiGenerated = false,
            dateAdded = "02 Agu 2026",
            tags = listOf("SFX", "Transisi")
        ),
        LocalMediaAsset(
            id = "asset_5",
            title = "Neon Glow Frame Overlay",
            category = "IMAGE",
            uri = "graphic_neon_frame",
            durationText = "00:05",
            durationMs = 5000L,
            resolutionOrType = "PNG Transparent",
            isAiGenerated = true,
            dateAdded = "01 Agu 2026",
            tags = listOf("Grafis", "Overlay", "Framing")
        ),
        LocalMediaAsset(
            id = "asset_6",
            title = "Lower Third Subtitle Badge",
            category = "IMAGE",
            uri = "graphic_lower_third",
            durationText = "00:04",
            durationMs = 4000L,
            resolutionOrType = "SVG Vector",
            isAiGenerated = false,
            dateAdded = "01 Agu 2026",
            tags = listOf("Teks", "LowerThird", "Stiker")
        ),
        LocalMediaAsset(
            id = "lut_asset_1",
            title = "Kodak Vision3 250D Film Look",
            category = "LUT",
            uri = "luts/Kodak-Vision3-250D.cube",
            durationText = "3D LUT",
            durationMs = 0L,
            resolutionOrType = ".CUBE 33x33x33",
            isAiGenerated = false,
            dateAdded = "03 Agu 2026",
            tags = listOf("LUT", "Kodak", "FilmGrade", "Hollywood")
        ),
        LocalMediaAsset(
            id = "lut_asset_2",
            title = "Sony SLOG3 Teal & Orange",
            category = "LUT",
            uri = "luts/Sony-SLOG3-TealOrange.cube",
            durationText = "3D LUT",
            durationMs = 0L,
            resolutionOrType = ".CUBE Film Print",
            isAiGenerated = false,
            dateAdded = "03 Agu 2026",
            tags = listOf("LUT", "TealOrange", "Sony", "Blockbuster")
        ),
        LocalMediaAsset(
            id = "lut_asset_3",
            title = "Fuji Film 35mm Nostalgic",
            category = "LUT",
            uri = "luts/Fuji-Film-35mm.cube",
            durationText = "3D LUT",
            durationMs = 0L,
            resolutionOrType = ".CUBE 3D",
            isAiGenerated = false,
            dateAdded = "02 Agu 2026",
            tags = listOf("LUT", "Fuji", "Vintage", "Warm")
        ),
        LocalMediaAsset(
            id = "lut_asset_4",
            title = "ARRI Alexa Cinematic Look",
            category = "LUT",
            uri = "luts/ARRI-Alexa-Cinematic.cube",
            durationText = "3D LUT",
            durationMs = 0L,
            resolutionOrType = ".CUBE Cinema HDR",
            isAiGenerated = false,
            dateAdded = "02 Agu 2026",
            tags = listOf("LUT", "ARRI", "Cinematic", "HDR")
        ),
        LocalMediaAsset(
            id = "lut_asset_5",
            title = "LOG to Rec709 SDR Normalizer",
            category = "LUT",
            uri = "luts/LOG-to-Rec709.cube",
            durationText = "3D LUT",
            durationMs = 0L,
            resolutionOrType = ".CUBE SDR Standard",
            isAiGenerated = false,
            dateAdded = "01 Agu 2026",
            tags = listOf("LUT", "Rec709", "Normalizer", "LOG")
        )
    )
}
