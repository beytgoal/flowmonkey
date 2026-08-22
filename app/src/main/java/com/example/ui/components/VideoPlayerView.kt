package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.R
import com.example.data.db.TimelineClipEntity
import com.example.data.models.KeyframeHelper
import com.example.data.models.KeyframeTransform
import com.example.media.RealMediaManager
import com.example.media.VideoProcessor
import com.example.ui.theme.*
import java.io.File

@Composable
fun VideoPlayerView(
    aspectRatioStr: String = "16:9",
    isPlaying: Boolean = false,
    currentTimeMs: Long = 0L,
    totalDurationMs: Long = 15000L,
    activeFilter: String = "None",
    activeAnimation: String = "None",
    activeEffect: String = "None",
    thumbnailDrawableRes: Int? = null,
    isProxyMode: Boolean = true,
    proxyResolution: String = "360p Proxy",
    clips: List<TimelineClipEntity> = emptyList(),
    selectedClipId: Long? = null,
    onAddOrUpdateKeyframe: ((clip: TimelineClipEntity, timeOffsetMs: Long, posX: Float, posY: Float, scale: Float, rotation: Float, opacity: Float) -> Unit)? = null,
    onRemoveKeyframe: ((clip: TimelineClipEntity, timeOffsetMs: Long) -> Unit)? = null,
    onSeek: ((Long) -> Unit)? = null,
    onToggleProxyMode: () -> Unit = {},
    onTogglePlay: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val aspectRatioFloat = when (aspectRatioStr) {
        "9:16" -> 9f / 16f
        "1:1" -> 1f
        "4:5" -> 4f / 5f
        "21:9" -> 21f / 9f
        else -> 16f / 9f
    }

    // Zoom & Pan state for interactive video preview frame
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Kedip / Flash / Strobe Animation State
    val isBlinkActive = activeAnimation.contains("Kedip", ignoreCase = true) ||
            activeAnimation.contains("Flash", ignoreCase = true) ||
            activeAnimation.contains("Blink", ignoreCase = true) ||
            activeEffect.contains("Kedip", ignoreCase = true) ||
            activeEffect.contains("Flash", ignoreCase = true) ||
            activeEffect.contains("Blink", ignoreCase = true)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val blinkAlpha by if (isBlinkActive) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(
                animation = tween(180, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "blinkAlpha"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    // Pembalik Warna (Color Invert) Filter State
    val isInvertActive = activeFilter.contains("Pembalik Warna", ignoreCase = true) ||
            activeFilter.contains("Invert", ignoreCase = true) ||
            activeEffect.contains("Pembalik Warna", ignoreCase = true) ||
            activeEffect.contains("Invert", ignoreCase = true)

    // Active main video clip currently under the playhead
    val activeMainClip = remember(clips, selectedClipId, currentTimeMs) {
        // Priority 1: Main track video clip active at current playhead time
        val atPlayhead = clips.find { clip ->
            (clip.stickerIcon == "None" || clip.stickerIcon.isBlank()) &&
                    clip.textContent == null &&
                    (clip.audioSfx == "None" || clip.audioSfx.isBlank()) &&
                    !clip.mediaUri.contains("overlay", ignoreCase = true) &&
                    !clip.mediaUri.contains("photo", ignoreCase = true) &&
                    !clip.mediaUri.contains("image", ignoreCase = true) &&
                    currentTimeMs >= clip.startTimeMs && currentTimeMs < clip.endTimeMs
        } ?: clips.find { clip ->
            (clip.stickerIcon == "None" || clip.stickerIcon.isBlank()) &&
                    clip.textContent == null &&
                    (clip.audioSfx == "None" || clip.audioSfx.isBlank()) &&
                    currentTimeMs >= clip.startTimeMs && currentTimeMs < clip.endTimeMs
        } ?: clips.find { clip ->
            currentTimeMs >= clip.startTimeMs && currentTimeMs < clip.endTimeMs
        }

        // Priority 2: If playhead is at the end of timeline
        val maxEndTime = clips.maxOfOrNull { it.endTimeMs } ?: 0L
        val atEnd = if (atPlayhead == null && clips.isNotEmpty() && currentTimeMs >= maxEndTime && maxEndTime > 0) {
            clips.maxByOrNull { it.endTimeMs }
        } else null

        // Priority 3: Fallbacks
        atPlayhead ?: atEnd ?: clips.find { it.id == selectedClipId } ?: clips.firstOrNull()
    }

    // All active clips at current playback timestamp (for overlays, text, stickers, audio)
    val activeClips = remember(clips, currentTimeMs) {
        clips.filter { it.startTimeMs <= currentTimeMs && it.endTimeMs > currentTimeMs }
    }

    val playerHeight = when (aspectRatioStr) {
        "1:1" -> 260.dp
        "9:16" -> 300.dp
        else -> 220.dp
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(playerHeight)
            .border(1.dp, StudioCardHairline, RectangleShape)
            .testTag("video_player_card"),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0E12))
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF14161F), Color(0xFF0A0B0E))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            val containerAspect = (maxWidth.value / maxHeight.value.coerceAtLeast(1f))
            val canvasModifier = if (aspectRatioFloat > containerAspect) {
                Modifier.fillMaxWidth().aspectRatio(aspectRatioFloat)
            } else {
                Modifier.fillMaxHeight().aspectRatio(aspectRatioFloat)
            }

            Box(
                modifier = canvasModifier
                    .clip(RectangleShape)
                    .background(Color.Black)
                    .border(1.dp, StudioCardHairline, RectangleShape)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 5f)
                            offset = Offset(offset.x + pan.x, offset.y + pan.y)
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                scale = 1f
                                offset = Offset.Zero
                            },
                            onTap = {
                                if (clips.isNotEmpty()) {
                                    onTogglePlay()
                                }
                            }
                        )
                    }
            ) {
                // Interactive Zoom & Pan Container
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                ) {
                    val activeUri = activeMainClip?.mediaUri ?: ""
                    val isImage = remember(activeUri) {
                        activeUri.endsWith(".jpg", true) || activeUri.endsWith(".jpeg", true) ||
                                activeUri.endsWith(".png", true) || activeUri.endsWith(".webp", true) ||
                                activeUri.contains("image", true) || activeUri.contains("photo", true)
                    }
                    val isVideo = remember(activeUri, isImage) {
                        !isImage && (activeUri.endsWith(".mp4", true) || activeUri.endsWith(".mkv", true) ||
                                activeUri.endsWith(".mov", true) || activeUri.endsWith(".webm", true) ||
                                activeUri.startsWith("content://") || activeUri.startsWith("file://") ||
                                File(activeUri).exists())
                    }

                    val activeOffsetMs = (currentTimeMs - (activeMainClip?.startTimeMs ?: 0L)).coerceAtLeast(0L)
                    val effectivePlaybackSpeed = remember(activeMainClip, activeOffsetMs) {
                        if (activeMainClip != null) {
                            VideoProcessor.instance.calculateEffectiveSpeed(activeMainClip, activeOffsetMs)
                        } else {
                            1.0f
                        }
                    }
                    val mainTransform = remember(activeMainClip, activeOffsetMs) {
                        if (activeMainClip != null) {
                            VideoProcessor.instance.calculateKeyframeTransform(activeMainClip, activeOffsetMs)
                        } else {
                            KeyframeTransform()
                        }
                    }

                    // 1. FULL FRAME VIDEO PLAYBACK SURFACE (MediaPlayer + TextureView)
                    if (isVideo && activeUri.isNotBlank()) {
                        NativeFullFrameVideoSurface(
                            mediaUri = activeUri,
                            isPlaying = isPlaying,
                            currentTimeMs = currentTimeMs,
                            clipStartTimeMs = activeMainClip?.startTimeMs ?: 0L,
                            clipEndTimeMs = activeMainClip?.endTimeMs ?: (activeOffsetMs + 5000L),
                            speedMultiplier = effectivePlaybackSpeed,
                            volume = activeMainClip?.volume ?: 1f,
                            isMirrored = activeMainClip?.isMirrored == true,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    translationX = mainTransform.posX,
                                    translationY = mainTransform.posY,
                                    scaleX = (if (activeMainClip?.isMirrored == true) -1f else 1f) * mainTransform.scale,
                                    scaleY = mainTransform.scale,
                                    rotationZ = mainTransform.rotation,
                                    alpha = mainTransform.opacity
                                )
                        )
                    } else if (isImage && activeUri.isNotBlank()) {
                        // 2. FULL FRAME PHOTO / IMAGE DISPLAY
                        val imageModel = remember(activeUri) {
                            if (activeUri.startsWith("/") || activeUri.startsWith("file://")) {
                                File(activeUri.removePrefix("file://"))
                            } else {
                                Uri.parse(activeUri)
                            }
                        }
                        AsyncImage(
                            model = imageModel,
                            contentDescription = activeMainClip?.title ?: "Full Frame Photo",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    translationX = mainTransform.posX,
                                    translationY = mainTransform.posY,
                                    scaleX = (if (activeMainClip?.isMirrored == true) -1f else 1f) * mainTransform.scale,
                                    scaleY = mainTransform.scale,
                                    rotationZ = mainTransform.rotation,
                                    alpha = mainTransform.opacity
                                ),
                            contentScale = ContentScale.Crop
                        )
                    } else if (thumbnailDrawableRes != null) {
                        // 3. FULL FRAME DRAWABLE ASSET
                        Image(
                            painter = painterResource(id = thumbnailDrawableRes),
                            contentDescription = "Full Frame Video Clip",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    translationX = mainTransform.posX,
                                    translationY = mainTransform.posY,
                                    scaleX = (if (activeMainClip?.isMirrored == true) -1f else 1f) * mainTransform.scale,
                                    scaleY = mainTransform.scale,
                                    rotationZ = mainTransform.rotation,
                                    alpha = mainTransform.opacity
                                ),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // 4. EMPTY / FALLBACK FULL FRAME CANVAS
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                    )
                                )
                        )
                    }

                    // Low-Res Proxy Preview Mode Downsampling Layer
                    if (isProxyMode) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.04f))
                        )
                    }

                    // Brightness Color Grade Adjustment Layer
                    val brightnessVal = activeMainClip?.brightness ?: 0f
                    if (brightnessVal > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = (brightnessVal * 0.7f).coerceIn(0f, 0.7f)))
                        )
                    } else if (brightnessVal < 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = (-brightnessVal * 0.8f).coerceIn(0f, 0.8f)))
                        )
                    }

                    // Color Temperature Adjustment Layer
                    val tempVal = activeMainClip?.temperature ?: 0f
                    if (tempVal > 0.05f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFFF9800).copy(alpha = (tempVal * 0.25f).coerceIn(0f, 0.4f)))
                        )
                    } else if (tempVal < -0.05f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF00E5FF).copy(alpha = (-tempVal * 0.25f).coerceIn(0f, 0.4f)))
                        )
                    }

                    // Active Visual Filter Layer
                    when {
                        activeFilter.contains("Teal", ignoreCase = true) || activeFilter.contains("Orange", ignoreCase = true) || activeFilter == "Cool Cyber" -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0xFF00E5FF).copy(alpha = 0.18f),
                                                Color(0xFFFF6D00).copy(alpha = 0.18f)
                                            )
                                        )
                                    )
                            )
                        }
                        activeFilter.contains("Moody", ignoreCase = true) || activeFilter.contains("Dark Film", ignoreCase = true) -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF1E1B4B).copy(alpha = 0.25f))
                            )
                        }
                        activeFilter.contains("Vintage", ignoreCase = true) || activeFilter.contains("35mm", ignoreCase = true) -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFD97706).copy(alpha = 0.15f))
                            )
                        }
                        activeFilter.contains("Cyberpunk", ignoreCase = true) || activeFilter.contains("Neon Glow", ignoreCase = true) -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color(0xFFEC4899).copy(alpha = 0.2f),
                                                Color(0xFF6366F1).copy(alpha = 0.2f)
                                            )
                                        )
                                    )
                            )
                        }
                        activeFilter.contains("Warm Sunset", ignoreCase = true) || activeFilter.contains("Warm Sun", ignoreCase = true) || activeFilter == "Warm Cinema" -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFFF9800).copy(alpha = 0.18f))
                            )
                        }
                        activeFilter.contains("B&W", ignoreCase = true) || activeFilter.contains("Noir", ignoreCase = true) || activeFilter.contains("Monochrome", ignoreCase = true) -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.35f))
                            )
                        }
                        activeFilter.contains("HDR", ignoreCase = true) -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White.copy(alpha = 0.08f))
                            )
                        }
                        activeFilter.startsWith("LUT:") -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                Color(0xFF6366F1).copy(alpha = 0.15f),
                                                Color(0xFF14B8A6).copy(alpha = 0.20f)
                                            )
                                        )
                                    )
                            )
                        }
                    }

                    // Vignette Lens Falloff Adjustment Layer
                    val vignetteVal = activeMainClip?.vignette ?: 0f
                    if (vignetteVal > 0.05f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = (vignetteVal * 0.85f).coerceIn(0f, 0.9f))
                                        )
                                    )
                                )
                        )
                    }

                    // Active Visual FX Strobe / Flash Layer
                    if (isBlinkActive) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = blinkAlpha))
                        )
                    }

                    // Color Inversion FX Layer
                    if (isInvertActive) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.5f))
                        )
                    }

                    // Multi-Track Overlays (PIP Videos, Subtitles, Text, Stickers)
                    Box(modifier = Modifier.fillMaxSize()) {
                        activeClips.forEach { clip ->
                            val isSelected = clip.id == selectedClipId
                            val clipOffsetMs = (currentTimeMs - clip.startTimeMs).coerceAtLeast(0L)
                            val keyframes = remember(clip.keyframeData) {
                                KeyframeHelper.parseKeyframes(clip.keyframeData)
                            }

                            // Base transform from keyframe interpolation
                            val baseTransform = remember(keyframes, clipOffsetMs, clip.rotationDegrees, clip.opacity) {
                                KeyframeHelper.evaluateTransform(
                                    keyframes = keyframes,
                                    clipTimeOffsetMs = clipOffsetMs,
                                    defaultRotation = clip.rotationDegrees.toFloat(),
                                    defaultOpacity = clip.opacity
                                )
                            }

                            val animatedTransform = evaluateSmoothClipAnimation(
                                clip = clip,
                                clipOffsetMs = clipOffsetMs,
                                baseTransform = baseTransform
                            )

                            // Video Overlay PIP / Graphic Photo Layer
                            if (clip.mediaUri.isNotBlank() && clip.id != activeMainClip?.id && (clip.trackId != (activeMainClip?.trackId ?: -1L) || clip.mediaUri.contains("overlay", ignoreCase = true) || clip.title.startsWith("Overlay") || clip.stickerIcon == "IMAGE_OVERLAY" || clip.mediaUri.endsWith(".jpg", true) || clip.mediaUri.endsWith(".jpeg", true) || clip.mediaUri.endsWith(".png", true) || clip.mediaUri.endsWith(".webp", true))) {
                                KeyframeOverlayItem(
                                    clip = clip,
                                    transform = animatedTransform,
                                    clipOffsetMs = clipOffsetMs,
                                    isSelected = isSelected,
                                    onTransformChanged = { x, y, s, r ->
                                        onAddOrUpdateKeyframe?.invoke(clip, clipOffsetMs, x, y, s, r, animatedTransform.opacity)
                                    }
                                )
                            }

                            // Text / Subtitle Caption Layer
                            if (clip.textContent != null && clip.textContent.isNotBlank()) {
                                KeyframeTextItem(
                                    clip = clip,
                                    text = clip.textContent,
                                    transform = animatedTransform,
                                    isSelected = isSelected,
                                    onTransformChanged = { x, y, s, r ->
                                        onAddOrUpdateKeyframe?.invoke(clip, clipOffsetMs, x, y, s, r, animatedTransform.opacity)
                                    }
                                )
                            }

                            // Sticker & Badge Layer (Must NOT be IMAGE_OVERLAY and NOT None/blank)
                            if (clip.stickerIcon.isNotBlank() && clip.stickerIcon != "None" && clip.stickerIcon != "IMAGE_OVERLAY") {
                                KeyframeStickerItem(
                                    clip = clip,
                                    sticker = clip.stickerIcon,
                                    transform = animatedTransform,
                                    isSelected = isSelected,
                                    onTransformChanged = { x, y, s, r ->
                                        onAddOrUpdateKeyframe?.invoke(clip, clipOffsetMs, x, y, s, r, animatedTransform.opacity)
                                    }
                                )
                            }
                        }
                    }
                }

                // Center Play Icon Button if Paused and Timeline has clips
                if (!isPlaying && clips.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(54.dp)
                            .clip(CircleShape)
                            .clickable { onTogglePlay() }
                            .testTag("play_pause_button"),
                        color = StudioElectricBlue,
                        border = BorderStroke(2.dp, Color.White),
                        shadowElevation = 4.dp,
                        shape = CircleShape
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                } else if (clips.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        color = StudioDarkCharcoal,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, StudioElectricBlue)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = null,
                                tint = StudioElectricBlue,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Timeline Kosong",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Tambahkan media untuk memulai",
                                color = StudioPastelSky,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Native hardware-accelerated video rendering surface with automatic Full-Frame scaling (ContentScale.Crop).
 */
@Composable
fun NativeFullFrameVideoSurface(
    mediaUri: String,
    isPlaying: Boolean,
    currentTimeMs: Long,
    clipStartTimeMs: Long,
    clipEndTimeMs: Long,
    speedMultiplier: Float,
    volume: Float,
    isMirrored: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipOffsetMs = (currentTimeMs - clipStartTimeMs).coerceIn(0L, (clipEndTimeMs - clipStartTimeMs).coerceAtLeast(100L))

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPrepared by remember { mutableStateOf(false) }
    var textureViewRef by remember { mutableStateOf<TextureView?>(null) }
    var surfaceRef by remember { mutableStateOf<Surface?>(null) }
    var videoWidth by remember { mutableIntStateOf(1280) }
    var videoHeight by remember { mutableIntStateOf(720) }

    fun applyCropMatrix(tv: TextureView, vw: Int, vh: Int) {
        if (vw <= 0 || vh <= 0 || tv.width <= 0 || tv.height <= 0) return
        val viewWidth = tv.width.toFloat()
        val viewHeight = tv.height.toFloat()
        val videoW = vw.toFloat()
        val videoH = vh.toFloat()

        val scaleX: Float
        val scaleY: Float
        val videoAspect = videoW / videoH
        val viewAspect = viewWidth / viewHeight

        if (videoAspect > viewAspect) {
            scaleY = 1f
            scaleX = (videoW * (viewHeight / videoH)) / viewWidth
        } else {
            scaleX = 1f
            scaleY = (videoH * (viewWidth / videoW)) / viewHeight
        }

        val matrix = Matrix()
        matrix.setScale(if (isMirrored) -scaleX else scaleX, scaleY, viewWidth / 2f, viewHeight / 2f)
        tv.setTransform(matrix)
    }

    DisposableEffect(mediaUri) {
        val player = MediaPlayer()
        mediaPlayer = player
        isPrepared = false

        try {
            player.setOnErrorListener { _, what, extra ->
                Log.w("VideoPlayer", "MediaPlayer onError caught: what=$what, extra=$extra")
                true // Handled to prevent unhandled crash
            }

            if (mediaUri.startsWith("content://") || mediaUri.startsWith("android.resource://")) {
                player.setDataSource(context, Uri.parse(mediaUri))
            } else {
                val cleanPath = mediaUri.removePrefix("file://")
                player.setDataSource(cleanPath)
            }

            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            player.setVolume(volume.coerceIn(0f, 1f), volume.coerceIn(0f, 1f))
            player.isLooping = false

            player.setOnVideoSizeChangedListener { _, w, h ->
                if (w > 0 && h > 0) {
                    videoWidth = w
                    videoHeight = h
                    textureViewRef?.let { applyCropMatrix(it, w, h) }
                }
            }

            player.setOnPreparedListener { mp ->
                isPrepared = true
                surfaceRef?.let { if (it.isValid) mp.setSurface(it) }
                try {
                    mp.seekTo(clipOffsetMs.toInt())
                } catch (e: Exception) {
                    Log.w("VideoPlayer", "Initial seek error: ${e.message}")
                }
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && speedMultiplier > 0f) {
                        mp.playbackParams = mp.playbackParams.setSpeed(speedMultiplier)
                        // CRITICAL: setPlaybackParams automatically sets state to started, pause immediately if not playing
                        if (!isPlaying) {
                            mp.pause()
                        }
                    }
                } catch (e: Exception) {
                    // Ignore
                }
                if (isPlaying) {
                    try {
                        mp.start()
                    } catch (e: Exception) {
                        Log.w("VideoPlayer", "Start error on prepared: ${e.message}")
                    }
                } else {
                    try {
                        if (mp.isPlaying) {
                            mp.pause()
                        }
                    } catch (e: Exception) {}
                }
            }
            player.prepareAsync()
        } catch (e: Exception) {
            Log.w("VideoPlayer", "MediaPlayer setup failed: ${e.message}")
        }

        onDispose {
            isPrepared = false
            try {
                player.setOnPreparedListener(null)
                player.setOnVideoSizeChangedListener(null)
                player.setOnErrorListener(null)
            } catch (e: Exception) {}

            try {
                if (player.isPlaying) {
                    player.stop()
                }
            } catch (e: Exception) {}

            try {
                player.reset()
                player.release()
            } catch (e: Exception) {}

            mediaPlayer = null
        }
    }

    // Play/Pause State Sync (Safe only when prepared)
    LaunchedEffect(isPlaying, isPrepared) {
        if (!isPrepared) return@LaunchedEffect
        mediaPlayer?.let { mp ->
            try {
                if (isPlaying) {
                    if (!mp.isPlaying) {
                        mp.seekTo(clipOffsetMs.toInt())
                        mp.start()
                    }
                } else {
                    if (mp.isPlaying) {
                        mp.pause()
                    }
                }
            } catch (e: Exception) {
                Log.w("VideoPlayer", "Play/Pause toggle failed: ${e.message}")
            }
        }
    }

    // Scrub / Seek Sync when paused or seeking (Safe only when prepared)
    LaunchedEffect(clipOffsetMs, isPrepared) {
        if (!isPrepared) return@LaunchedEffect
        mediaPlayer?.let { mp ->
            try {
                if (!isPlaying) {
                    mp.seekTo(clipOffsetMs.toInt())
                }
            } catch (e: Exception) {
                Log.w("VideoPlayer", "Seek failed: ${e.message}")
            }
        }
    }

    // Volume Sync (Safe only when prepared)
    LaunchedEffect(volume, isPrepared) {
        if (!isPrepared) return@LaunchedEffect
        try {
            mediaPlayer?.setVolume(volume.coerceIn(0f, 1f), volume.coerceIn(0f, 1f))
        } catch (e: Exception) {}
    }

    // Speed Sync (Safe only when prepared)
    LaunchedEffect(speedMultiplier, isPrepared) {
        if (!isPrepared) return@LaunchedEffect
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && speedMultiplier > 0f) {
                mediaPlayer?.let { mp ->
                    mp.playbackParams = mp.playbackParams.setSpeed(speedMultiplier)
                    if (!isPlaying) {
                        mp.pause()
                    }
                }
            }
        } catch (e: Exception) {}
    }

    AndroidView(
        factory = { ctx ->
            TextureView(ctx).apply {
                textureViewRef = this
                addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                    applyCropMatrix(this, videoWidth, videoHeight)
                }
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                        val surface = Surface(st)
                        surfaceRef = surface
                        if (isPrepared) {
                            try {
                                mediaPlayer?.setSurface(surface)
                            } catch (e: Exception) {}
                        }
                        applyCropMatrix(this@apply, videoWidth, videoHeight)
                    }

                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {
                        applyCropMatrix(this@apply, videoWidth, videoHeight)
                    }

                    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                        surfaceRef = null
                        try {
                            mediaPlayer?.setSurface(null)
                        } catch (e: Exception) {}
                        return true
                    }

                    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                }
            }
        },
        update = { tv ->
            textureViewRef = tv
            applyCropMatrix(tv, videoWidth, videoHeight)
        },
        modifier = modifier.fillMaxSize()
    )
}

@Composable
fun KeyframeOverlayItem(
    clip: TimelineClipEntity,
    transform: KeyframeTransform,
    clipOffsetMs: Long,
    isSelected: Boolean,
    onTransformChanged: (posX: Float, posY: Float, scale: Float, rotation: Float) -> Unit
) {
    val isImage = remember(clip.mediaUri) {
        clip.mediaUri.endsWith(".jpg", true) || clip.mediaUri.endsWith(".jpeg", true) ||
                clip.mediaUri.endsWith(".png", true) || clip.mediaUri.endsWith(".webp", true) ||
                clip.mediaUri.contains("image", true) || clip.mediaUri.contains("photo", true) ||
                clip.stickerIcon == "IMAGE_OVERLAY"
    }

    val overlayFrame by produceState<Bitmap?>(initialValue = null, key1 = clip.mediaUri, key2 = clipOffsetMs) {
        if (clip.mediaUri.isNotBlank() && !isImage) {
            value = RealMediaManager.extractVideoFrame(clip.mediaUri, clipOffsetMs)
        } else {
            value = null
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationX = transform.posX
                    translationY = transform.posY
                    scaleX = if (clip.isMirrored) -transform.scale else transform.scale
                    scaleY = transform.scale
                    rotationZ = transform.rotation
                    alpha = transform.opacity
                }
                .pointerInput(clip.id) {
                    detectTransformGestures { _, pan, zoom, rotation ->
                        val newX = transform.posX + pan.x
                        val newY = transform.posY + pan.y
                        val newScale = (transform.scale * zoom).coerceIn(0.2f, 4f)
                        val newRot = transform.rotation + rotation
                        onTransformChanged(newX, newY, newScale, newRot)
                    }
                }
                .size(160.dp, 100.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(
                    if (isSelected) 2.dp else 1.dp,
                    if (isSelected) StudioAccentAmber else StudioCardHairline,
                    RoundedCornerShape(8.dp)
                )
                .background(StudioDarkCharcoal)
        ) {
            if (overlayFrame != null) {
                Image(
                    bitmap = overlayFrame!!.asImageBitmap(),
                    contentDescription = clip.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (clip.mediaUri.isNotBlank()) {
                val overlayModel = remember(clip.mediaUri) {
                    if (clip.mediaUri.startsWith("/") || clip.mediaUri.startsWith("file://")) {
                        File(clip.mediaUri.removePrefix("file://"))
                    } else {
                        Uri.parse(clip.mediaUri)
                    }
                }
                AsyncImage(
                    model = overlayModel,
                    contentDescription = clip.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = StudioSecondaryTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = clip.title.take(18),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KeyframeTextItem(
    clip: TimelineClipEntity,
    text: String,
    transform: KeyframeTransform,
    isSelected: Boolean,
    onTransformChanged: (posX: Float, posY: Float, scale: Float, rotation: Float) -> Unit
) {
    val parsedColor = remember(clip.fontColor) {
        try {
            Color(android.graphics.Color.parseColor(clip.fontColor))
        } catch (e: Exception) {
            Color.White
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationX = transform.posX
                    translationY = transform.posY
                    scaleX = transform.scale
                    scaleY = transform.scale
                    rotationZ = transform.rotation
                    alpha = transform.opacity
                }
                .pointerInput(clip.id) {
                    detectTransformGestures { _, pan, zoom, rotation ->
                        val newX = transform.posX + pan.x
                        val newY = transform.posY + pan.y
                        val newScale = (transform.scale * zoom).coerceIn(0.2f, 4f)
                        val newRot = transform.rotation + rotation
                        onTransformChanged(newX, newY, newScale, newRot)
                    }
                }
                .border(
                    if (isSelected) 1.5.dp else 0.dp,
                    if (isSelected) StudioSecondaryTeal else Color.Transparent,
                    RoundedCornerShape(6.dp)
                )
                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = text,
                color = parsedColor,
                fontSize = (clip.fontSize * 0.8f).sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = when (clip.textAlignment) {
                    "LEFT" -> TextAlign.Left
                    "RIGHT" -> TextAlign.Right
                    else -> TextAlign.Center
                }
            )
        }
    }
}

@Composable
fun KeyframeStickerItem(
    clip: TimelineClipEntity,
    sticker: String,
    transform: KeyframeTransform,
    isSelected: Boolean,
    onTransformChanged: (posX: Float, posY: Float, scale: Float, rotation: Float) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationX = transform.posX
                    translationY = transform.posY
                    scaleX = if (clip.isMirrored) -transform.scale else transform.scale
                    scaleY = transform.scale
                    rotationZ = transform.rotation
                    alpha = transform.opacity
                }
                .pointerInput(clip.id) {
                    detectTransformGestures { _, pan, zoom, rotation ->
                        val newX = transform.posX + pan.x
                        val newY = transform.posY + pan.y
                        val newScale = (transform.scale * zoom).coerceIn(0.2f, 4f)
                        val newRot = transform.rotation + rotation
                        onTransformChanged(newX, newY, newScale, newRot)
                    }
                }
                .border(
                    if (isSelected) 1.5.dp else 0.dp,
                    if (isSelected) Color(0xFFFFB74D) else Color.Transparent,
                    RoundedCornerShape(8.dp)
                )
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when {
                        sticker.contains("Star") -> Icons.Default.Star
                        sticker.contains("Verifikasi") -> Icons.Default.Verified
                        sticker.contains("Arrow") -> Icons.Default.ArrowForward
                        sticker.contains("Fire") || sticker.contains("Flame") -> Icons.Default.LocalFireDepartment
                        else -> Icons.Default.AutoAwesome
                    },
                    contentDescription = sticker,
                    tint = Color(0xFFFFB74D),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = sticker,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

fun formatTimeMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
fun RenderSmoothTransition(
    transitionType: String,
    progress: Float,
    isIncoming: Boolean
) {
    val smoothProgress = (progress * progress * (3f - 2f * progress)).coerceIn(0f, 1f)
    when {
        transitionType.contains("Crossfade", ignoreCase = true) || transitionType.contains("Dissolve", ignoreCase = true) -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = (smoothProgress * 0.7f).coerceIn(0f, 0.85f)))
            )
        }
        transitionType.contains("Fade In", ignoreCase = true) || transitionType.contains("Fade Out", ignoreCase = true) -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = smoothProgress))
            )
        }
        transitionType.contains("Zoom", ignoreCase = true) -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1f + smoothProgress * 0.35f
                        scaleY = 1f + smoothProgress * 0.35f
                        alpha = (1f - smoothProgress * 0.4f).coerceIn(0f, 1f)
                    }
                    .background(StudioSecondaryTeal.copy(alpha = smoothProgress * 0.25f))
            )
        }
        transitionType.contains("Pan", ignoreCase = true) || transitionType.contains("Whip", ignoreCase = true) -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = (if (isIncoming) -1f else 1f) * smoothProgress * 150f
                        alpha = (1f - smoothProgress * 0.3f).coerceIn(0f, 1f)
                    }
                    .background(StudioPrimaryViolet.copy(alpha = smoothProgress * 0.2f))
            )
        }
        transitionType.contains("Glitch", ignoreCase = true) -> {
            val glitchAlpha = if ((smoothProgress * 10).toInt() % 2 == 0) 0.45f else 0.1f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(StudioAccentPink.copy(alpha = glitchAlpha * smoothProgress))
            )
        }
        else -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = smoothProgress * 0.5f))
            )
        }
    }
}

fun evaluateSmoothClipAnimation(
    clip: TimelineClipEntity,
    clipOffsetMs: Long,
    baseTransform: KeyframeTransform
): KeyframeTransform {
    var posX = baseTransform.posX
    var posY = baseTransform.posY
    var scale = baseTransform.scale
    var rotation = baseTransform.rotation
    var opacity = baseTransform.opacity

    val inDurationMs = 500L
    val outDurationMs = 500L
    val totalDur = clip.durationMs.coerceAtLeast(100L)

    // 1. In-Animation Easing
    if (clip.animationIn.isNotBlank() && clip.animationIn != "None" && clipOffsetMs < inDurationMs) {
        val t = (clipOffsetMs.toFloat() / inDurationMs.toFloat()).coerceIn(0f, 1f)
        val smoothT = t * t * (3f - 2f * t)
        when {
            clip.animationIn.contains("Fade In", ignoreCase = true) -> {
                opacity *= smoothT
            }
            clip.animationIn.contains("Slide Right", ignoreCase = true) -> {
                posX += (1f - smoothT) * -220f
                opacity *= smoothT
            }
            clip.animationIn.contains("Zoom In", ignoreCase = true) -> {
                scale *= (0.3f + smoothT * 0.7f)
                opacity *= smoothT
            }
            clip.animationIn.contains("Bounce In", ignoreCase = true) -> {
                val bounce = kotlin.math.sin(t * kotlin.math.PI * 1.5).toFloat()
                scale *= (0.2f + bounce * 0.8f).coerceAtLeast(0.1f)
                opacity *= smoothT
            }
            clip.animationIn.contains("Rotate Entrance", ignoreCase = true) -> {
                rotation += (1f - smoothT) * -180f
                scale *= (0.4f + smoothT * 0.6f)
                opacity *= smoothT
            }
            clip.animationIn.contains("Kedip", ignoreCase = true) || clip.animationIn.contains("Flash", ignoreCase = true) -> {
                val strobeCount = (t * 8).toInt()
                opacity = if (strobeCount % 2 == 0) opacity else 0.1f
            }
        }
    }

    // 2. Out-Animation Easing
    val timeToEnd = totalDur - clipOffsetMs
    if (clip.animationOut.isNotBlank() && clip.animationOut != "None" && timeToEnd < outDurationMs) {
        val t = ((outDurationMs - timeToEnd).toFloat() / outDurationMs.toFloat()).coerceIn(0f, 1f)
        val smoothT = t * t * (3f - 2f * t)
        when {
            clip.animationOut.contains("Fade Out", ignoreCase = true) -> {
                opacity *= (1f - smoothT)
            }
            clip.animationOut.contains("Slide Left", ignoreCase = true) -> {
                posX += smoothT * -220f
                opacity *= (1f - smoothT)
            }
            clip.animationOut.contains("Zoom Out", ignoreCase = true) -> {
                scale *= (1f - smoothT * 0.7f)
                opacity *= (1f - smoothT)
            }
            clip.animationOut.contains("Dissolve Out", ignoreCase = true) -> {
                opacity *= (1f - smoothT)
                scale *= (1f + smoothT * 0.15f)
            }
            clip.animationOut.contains("Glitch Exit", ignoreCase = true) -> {
                posX += (if ((smoothT * 12).toInt() % 2 == 0) 12f else -12f)
                opacity *= (1f - smoothT)
            }
        }
    }

    // 3. Combo Animation Easing
    if (clip.animationCombo.isNotBlank() && clip.animationCombo != "None") {
        val progress = (clipOffsetMs.toFloat() / totalDur.toFloat()).coerceIn(0f, 1f)
        when {
            clip.animationCombo.contains("Spin", ignoreCase = true) -> {
                rotation += progress * 360f
            }
            clip.animationCombo.contains("Flash Kedip", ignoreCase = true) || clip.animationCombo.contains("Strobe", ignoreCase = true) -> {
                val cycle = (progress * 16).toInt()
                if (cycle % 2 != 0) {
                    opacity *= 0.35f
                }
            }
            clip.animationCombo.contains("Elastic Pop", ignoreCase = true) || clip.animationCombo.contains("Pulse", ignoreCase = true) -> {
                val popWave = kotlin.math.sin(progress * kotlin.math.PI * 6).toFloat()
                scale *= (1.0f + popWave * 0.12f)
            }
            clip.animationCombo.contains("Bounce", ignoreCase = true) -> {
                val bounceWave = kotlin.math.abs(kotlin.math.sin(progress * kotlin.math.PI * 4)).toFloat()
                posY += bounceWave * -25f
            }
        }
    }

    return KeyframeTransform(
        posX = posX,
        posY = posY,
        scale = scale.coerceIn(0.1f, 5f),
        rotation = rotation,
        opacity = opacity.coerceIn(0f, 1f)
    )
}
