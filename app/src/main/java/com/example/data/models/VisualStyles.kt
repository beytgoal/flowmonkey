package com.example.data.models

import androidx.compose.ui.graphics.Color

data class VisualStyleOption(
    val id: String,
    val name: String,
    val description: String,
    val promptModifier: String,
    val badge: String,
    val accentColor: Color
)

object VisualStylesRepository {
    val options = listOf(
        VisualStyleOption(
            id = "cinematic",
            name = "Cinematic 8K",
            description = "Pencahayaan dramatis, kedalaman bidang dangkal (shallow depth of field), grading warna kelas film layar lebar.",
            promptModifier = "cinematic 8k resolution, anamorphic lens, dramatic lighting, depth of field, photorealistic movie scene",
            badge = "POPULER",
            accentColor = Color(0xFF6366F1)
        ),
        VisualStyleOption(
            id = "anime",
            name = "Anime Makoto Style",
            description = "Gaya animasi Jepang berkilau dengan langit awan estetik, warna vaskular kaya, dan detail latar belakang halus.",
            promptModifier = "japanese anime style, Makoto Shinkai aesthetics, vibrant sky with detailed clouds, soft atmospheric lighting",
            badge = "JEPANG",
            accentColor = Color(0xFF00E5FF)
        ),
        VisualStyleOption(
            id = "kartun3d",
            name = "Kartun 3D Pixar",
            description = "Karakter 3D ekspresif dengan tekstur halus, pencahayaan hangat, dan pergerakan mulus mirip film studio animasi.",
            promptModifier = "3d pixar animation style, smooth rendered textures, cute expressive character design, warm ambient occlusion",
            badge = "ANIMASI 3D",
            accentColor = Color(0xFFFF4081)
        ),
        VisualStyleOption(
            id = "vintage",
            name = "Vintage 35mm Film",
            description = "Grain film klasik 35mm, warna hangat nostalgia, dengan lekatan abrasi optic dan bokeh lembut.",
            promptModifier = "vintage 35mm film grain, retro Kodachrome color palette, analog warmth, slight chromatic aberration, nostalgic vibe",
            badge = "RETRO",
            accentColor = Color(0xFFF59E0B)
        ),
        VisualStyleOption(
            id = "abstrak_cyber",
            name = "Abstrak Hologram Cyber",
            description = "Visual futuristik dengan elemen hologram bersinar, pola partikel neon, dan distorsi geometris digital.",
            promptModifier = "abstract cybernetic hologram, glowing neon wireframes, particle quantum grid, futuristic digital distortion",
            badge = "FUTURISTIK",
            accentColor = Color(0xFF10B981)
        ),
        VisualStyleOption(
            id = "neon_synthwave",
            name = "Neon Synthwave 80s",
            description = "Aesthetic tahun 80-an dengan cahaya magenta-cyan neon, lanskap grid wireframe, dan matahari terbenam berkabut.",
            promptModifier = "neon synthwave 80s aesthetic, glowing magenta cyan lights, grid horizon, retro futuristic dark skyline",
            badge = "80s NEON",
            accentColor = Color(0xFFEC4899)
        ),
        VisualStyleOption(
            id = "photorealistic",
            name = "Photorealistic Unreal Engine 5",
            description = "Render raytracing realistis dengan detail fisik materi, pantulan air akurat, dan pencahayaan alami presisi.",
            promptModifier = "photorealistic unreal engine 5 render, raytracing, octanerender, ultra detailed materials, natural lighting",
            badge = "HIGHFIELD ULTRA",
            accentColor = Color(0xFF3B82F6)
        ),
        VisualStyleOption(
            id = "claymation",
            name = "Claymation Stop Motion",
            description = "Tekstur tanah liat fisik buatan tangan dengan animasi frame-by-frame tactile dan pencahayaan studio mikro.",
            promptModifier = "claymation stop motion style, handmade clay textures, tactile miniature studio lighting, frame by frame movement",
            badge = "KREATIF",
            accentColor = Color(0xFF8B5CF6)
        ),
        VisualStyleOption(
            id = "noir",
            name = "Noir Monochrome",
            description = "Kontras hitam putih tinggi, bayangan Venetian blind tajam, atmosfer misteri detektif klasik.",
            promptModifier = "noir black and white, high contrast shadows, film noir cinematic lighting, moody mystery atmosphere",
            badge = "KLASIK",
            accentColor = Color(0xFF9CA3AF)
        ),
        VisualStyleOption(
            id = "vhs_90s",
            name = "Retro 90s VHS Camcorder",
            description = "Scanlines khas kaset VHS, glitch pita magnetik, timestamp CRT, dan warna berbayang khas 90-an.",
            promptModifier = "90s VHS footage glitch, analog tape lines, CRT TV glow, home video aesthetic, timestamp overlay",
            badge = "NOSTALGIA",
            accentColor = Color(0xFFEF4444)
        ),
        VisualStyleOption(
            id = "papercraft",
            name = "Papercraft 3D Origami",
            description = "Layer lipatan kertas berbahan karton dengan depth layered shadow, estetika kerajinan tangan artistik.",
            promptModifier = "papercraft origami 3d, layered paper art, soft cast shadows, tactile craft cardboard texture",
            badge = "ARTISTIK",
            accentColor = Color(0xFF84CC16)
        ),
        VisualStyleOption(
            id = "watercolor",
            name = "Watercolor Impressionist",
            description = "Goresan kuas cat air lembut, pendaran pigmen warna cair di atas kertas tekstur, romantis dan bermimpi.",
            promptModifier = "soft watercolor painting, fluid pigment blending, impressionist brushstrokes, textured paper bleed",
            badge = "SAMPEL SENI",
            accentColor = Color(0xFF06B6D4)
        ),
        VisualStyleOption(
            id = "hyper_fantasy",
            name = "Hyper-detailed Fantasy",
            description = "Dunia fantasi magis dengan partikel sihir berpendar, mahluk mitologi kaya detail, dan flora bioluminescent.",
            promptModifier = "hyper detailed epic fantasy art, bioluminescent flora, magical glowing particles, mythical world scene",
            badge = "EPIS",
            accentColor = Color(0xFFA855F7)
        )
    )

    fun getById(id: String): VisualStyleOption {
        return options.find { it.id.equals(id, ignoreCase = true) || it.name.contains(id, ignoreCase = true) }
            ?: options.first()
    }
}
