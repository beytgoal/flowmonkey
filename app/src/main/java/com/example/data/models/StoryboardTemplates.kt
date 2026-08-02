package com.example.data.models

data class TemplateSceneSpec(
    val title: String,
    val scriptText: String,
    val visualPrompt: String,
    val cameraMovement: String,
    val durationSeconds: Int
)

data class StoryboardTemplate(
    val id: String,
    val genre: String,
    val name: String,
    val description: String,
    val defaultStyle: String,
    val recommendedAspectRatio: String,
    val targetPlatform: String,
    val scenes: List<TemplateSceneSpec>
)

object StoryboardTemplatesRepository {
    val templates = listOf(
        StoryboardTemplate(
            id = "iklan_produk",
            genre = "E-Commerce & Produk",
            name = "Iklan Produk High-End",
            description = "Template komersial 3-scene untuk promosi produk dengan hook visual kuat, keunggulan fitur, dan CTA promosi.",
            defaultStyle = "Cinematic 8K",
            recommendedAspectRatio = "9:16",
            targetPlatform = "Instagram Reels & TikTok",
            scenes = listOf(
                TemplateSceneSpec(
                    title = "Scene 1: Hook Visual Premium",
                    scriptText = "Memperkenalkan lini produk terbaru dengan desain elegan dan futuristik.",
                    visualPrompt = "Close-up macro shot of premium product spinning slowly on glossy black glass platform with dramatic side rim lighting",
                    cameraMovement = "Slow Push In",
                    durationSeconds = 4
                ),
                TemplateSceneSpec(
                    title = "Scene 2: Sorotan Fitur Unggulan",
                    scriptText = "Dibuat dengan presisi tinggi untuk performa maksimal sehari-hari.",
                    visualPrompt = "Dynamic side pan showing sleek metal finish and glowing LED interface indicator on product body",
                    cameraMovement = "Tracking Right",
                    durationSeconds = 5
                ),
                TemplateSceneSpec(
                    title = "Scene 3: Call To Action & Penutup",
                    scriptText = "Dapatkan diskon spesial hari ini. Klik link di bio sekarang!",
                    visualPrompt = "Hero product pose centered with glowing holographic typography 'DISCOUNT 50% NOW' in background",
                    cameraMovement = "Orbit 360",
                    durationSeconds = 4
                )
            )
        ),
        StoryboardTemplate(
            id = "promo_tiktok",
            genre = "Social Media",
            name = "Promo Viral TikTok / Reels",
            description = "Pacing cepat dan dinamis yang dirancang khusus untuk menarik perhatian penonton dalam 3 detik pertama.",
            defaultStyle = "Neon Synthwave 80s",
            recommendedAspectRatio = "9:16",
            targetPlatform = "TikTok & Shorts",
            scenes = listOf(
                TemplateSceneSpec(
                    title = "Scene 1: High Energy Attention Grabber",
                    scriptText = "Stop scrolling! Ini dia rahasia pembuat konten profesional di tahun 2026!",
                    visualPrompt = "Fast zoom into a cyber neon studio setup with glowing floating screens and vibrant neon light flare",
                    cameraMovement = "Zoom Sweep In",
                    durationSeconds = 3
                ),
                TemplateSceneSpec(
                    title = "Scene 2: Demo Cepat Hasil Kreatif",
                    scriptText = "Cukup ketik prompt atau upload foto, AI langsung buat video 4K otomatis!",
                    visualPrompt = "Split screen effect transforming a flat sketch image into a rich 3D animated cinematic scene",
                    cameraMovement = "Whip Pan Left",
                    durationSeconds = 5
                ),
                TemplateSceneSpec(
                    title = "Scene 3: Outro Trend & Bio Hook",
                    scriptText = "Coba sekarang gratis sebelum promo habis. Tag teman kamu!",
                    visualPrompt = "Energetic character pointing towards bottom screen with bright animated subscribe badge",
                    cameraMovement = "Handheld Shake",
                    durationSeconds = 4
                )
            )
        ),
        StoryboardTemplate(
            id = "cinematic_trailer",
            genre = "Film & Game",
            name = "Cinematic Movie / Game Trailer",
            description = "Penceritaan dramatis atmosferik khas cuplikan film blockbuster atau game AAA dengan bangun tensi.",
            defaultStyle = "Cinematic 8K",
            recommendedAspectRatio = "16:9",
            targetPlatform = "YouTube & TV",
            scenes = listOf(
                TemplateSceneSpec(
                    title = "Scene 1: Pembuka Atmosferik Dunia",
                    scriptText = "Di masa depan di mana peradaban manusia tinggal di kota apung di atas awan...",
                    visualPrompt = "Wide panoramic shot of futuristic floating metropolis above golden cloud sea at sunset, atmospheric fog",
                    cameraMovement = "Pan Up Drone",
                    durationSeconds = 6
                ),
                TemplateSceneSpec(
                    title = "Scene 2: Konflik & Misteri Utama",
                    scriptText = "Ancaman rahasia yang telah tertidur selama seribu tahun kini kembali bangkit.",
                    visualPrompt = "Mysterious hooded figure standing atop a glowing neon tower gazing at dark storm clouds in distance",
                    cameraMovement = "Slow Dolly Back",
                    durationSeconds = 6
                ),
                TemplateSceneSpec(
                    title = "Scene 3: Klimaks & Judul Film",
                    scriptText = "Persiapkan dirimu untuk petualangan terbesar abad ini. Segera di bioskop.",
                    visualPrompt = "Epic battle explosions in space with glowing laser beams flashing into metallic title logo 'NEO CHRONICLES'",
                    cameraMovement = "Rapid Tracking",
                    durationSeconds = 5
                )
            )
        ),
        StoryboardTemplate(
            id = "video_edukasi",
            genre = "Pendidikan & Explainer",
            name = "Video Edukasi / Motion Graphics",
            description = "Struktur penjelasan langkah demi langkah yang bersih untuk tutorial, edukasi sains, atau presentasi ide.",
            defaultStyle = "Kartun 3D Pixar",
            recommendedAspectRatio = "16:9",
            targetPlatform = "YouTube & E-Learning",
            scenes = listOf(
                TemplateSceneSpec(
                    title = "Scene 1: Masalah & Pertanyaan Utama",
                    scriptText = "Pernahkah kamu bertanya-tanya bagaimana sistem AI Veo bisa memahami teks menjadi video?",
                    visualPrompt = "3D cute robot character scratching head in front of a giant glowing chalkboard filled with floating formulas",
                    cameraMovement = "Static Medium Shot",
                    durationSeconds = 5
                ),
                TemplateSceneSpec(
                    title = "Scene 2: Visualisasi Proses Langkah 1",
                    scriptText = "Pertama, teks dianalisis oleh Neural Network menjadi representasi vektor spasial.",
                    visualPrompt = "Colorful 3D geometric nodes lighting up in a neural network grid with glowing data streams flowing",
                    cameraMovement = "Pan Right",
                    durationSeconds = 6
                ),
                TemplateSceneSpec(
                    title = "Scene 3: Kesimpulan & Rangkuman",
                    scriptText = "Itulah mengapa AI masa kini bisa menciptakan klip realistis hanya dalam hitungan detik!",
                    visualPrompt = "Friendly 3D robot character holding a glowing trophy with celebratory confetti around",
                    cameraMovement = "Tilt Up",
                    durationSeconds = 4
                )
            )
        ),
        StoryboardTemplate(
            id = "music_video",
            genre = "Musik & Seni",
            name = "Music Video Teaser",
            description = "Atmosfer estetik dengan sinkronisasi ritme visual, efek pencahayaan surealis, dan nuansa artistik.",
            defaultStyle = "Abstrak Hologram Cyber",
            recommendedAspectRatio = "9:16",
            targetPlatform = "Instagram & TikTok",
            scenes = listOf(
                TemplateSceneSpec(
                    title = "Scene 1: Visual Beat Intro",
                    scriptText = "[Instrumen Beat Drop] Nuansa malam kota dengan refleksi hujan dan lampu neon berkilau.",
                    visualPrompt = "Silhouette of musician playing synthesizer on a rainy neon rooftop overlooking glowing cyberpunk city",
                    cameraMovement = "Orbit 180",
                    durationSeconds = 4
                ),
                TemplateSceneSpec(
                    title = "Scene 2: Surealisme Visual Musik",
                    scriptText = "Lirik bermakna dalam yang menyentuh perasaan penikmat musik.",
                    visualPrompt = "Abstract liquid metallic shapes dancing floating in zero gravity surrounded by glowing particle waves",
                    cameraMovement = "Spiral Zoom Out",
                    durationSeconds = 5
                ),
                TemplateSceneSpec(
                    title = "Scene 3: Tanggal Rilis Album",
                    scriptText = "Single terbaru 'Cyber Echoes' rilis di seluruh platform musik digital Jumat ini!",
                    visualPrompt = "Glossy CD vinyl spinning with neon holographic text 'CYBER ECHOES - OUT THIS FRIDAY'",
                    cameraMovement = "Slow Zoom In",
                    durationSeconds = 4
                )
            )
        ),
        StoryboardTemplate(
            id = "vlog_cinematic",
            genre = "Gaya Hidup & Travel",
            name = "Cinematic Travel Vlog",
            description = "Gaya dokumentasi perjalanan dengan transisi mulus, pemandangan indah, dan penceritaan personal.",
            defaultStyle = "Vintage 35mm Film",
            recommendedAspectRatio = "16:9",
            targetPlatform = "YouTube & Travel Vlog",
            scenes = listOf(
                TemplateSceneSpec(
                    title = "Scene 1: B-Roll Pemandangan Alam",
                    scriptText = "Menyusuri keindahan bukit hijau dan danau jernih di pagi hari yang menenangkan.",
                    visualPrompt = "Breathtaking aerial drone view of misty pine forest mountain lake during golden hour sunrise",
                    cameraMovement = "Drone Flyover",
                    durationSeconds = 5
                ),
                TemplateSceneSpec(
                    title = "Scene 2: Aktivitas & Budaya Lokal",
                    scriptText = "Menikmati teh hangat tradisional sambil berinteraksi dengan wawasan hangat masyarakat lokal.",
                    visualPrompt = "Warm cozy tea house in traditional wooden village with steam rising from ceramic cup in close-up",
                    cameraMovement = "Slow Crane Down",
                    durationSeconds = 5
                ),
                TemplateSceneSpec(
                    title = "Scene 3: Penutup Momen Sunset",
                    scriptText = "Sampai jumpa di petualangan berikutnya! Jangan lupa tonton video lengkapnya di channel kami.",
                    visualPrompt = "Traveler silhouette sitting on ocean cliff watching golden orange sunset with gentle sea waves below",
                    cameraMovement = "Pan Right Sunset",
                    durationSeconds = 5
                )
            )
        )
    )

    fun getById(id: String): StoryboardTemplate {
        return templates.find { it.id == id } ?: templates.first()
    }
}
