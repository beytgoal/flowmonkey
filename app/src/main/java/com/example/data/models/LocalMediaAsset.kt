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
    // Only real system color grade LUT profiles are bundled by default.
    // User videos, audios, and images are imported dynamically from real device storage.
    val initialAssets = listOf(
        LocalMediaAsset(
            id = "lut_asset_1",
            title = "Kodak Vision3 250D Film Look",
            category = "LUT",
            uri = "luts/Kodak-Vision3-250D.cube",
            durationText = "3D LUT",
            durationMs = 0L,
            resolutionOrType = ".CUBE 33x33x33",
            isAiGenerated = false,
            dateAdded = "Preset",
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
            dateAdded = "Preset",
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
            dateAdded = "Preset",
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
            dateAdded = "Preset",
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
            dateAdded = "Preset",
            tags = listOf("LUT", "Rec709", "Normalizer", "LOG")
        )
    )
}
