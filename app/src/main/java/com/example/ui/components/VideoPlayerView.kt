package com.example.ui.components

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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.data.db.TimelineClipEntity
import com.example.data.models.KeyframeHelper
import com.example.data.models.KeyframePoint
import com.example.data.models.KeyframeTransform
import com.example.ui.theme.*
import kotlin.math.roundToInt

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

    // Active clips at current playback timestamp
    val activeClips = remember(clips, currentTimeMs) {
        clips.filter { it.startTimeMs <= currentTimeMs && it.endTimeMs > currentTimeMs }
    }

    // Selected clip if active or in project
    val activeSelectedClip = remember(clips, selectedClipId, currentTimeMs) {
        clips.find { it.id == selectedClipId }
    }

    val activeMainClip = remember(activeClips, activeSelectedClip) {
        activeSelectedClip?.takeIf { !it.mediaUri.contains("overlay", ignoreCase = true) && it.stickerIcon.isBlank() && it.textContent == null }
            ?: activeClips.firstOrNull { !it.mediaUri.contains("overlay", ignoreCase = true) && it.stickerIcon.isBlank() && it.textContent == null }
    }

    val playerHeight = when (aspectRatioStr) {
        "1:1" -> 260.dp
        "9:16" -> 280.dp
        else -> 210.dp
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(playerHeight)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, StudioCardHairline, RoundedCornerShape(20.dp))
            .testTag("video_player_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0E12))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF14161F), Color(0xFF0A0B0E))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.92f)
                    .aspectRatio(aspectRatioFloat)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
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
                    // Background preview graphic with horizontal mirror support
                    val imageRes = thumbnailDrawableRes ?: R.drawable.img_hero_banner_1785585794962
                    Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = "Preview Video Clip",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = if (activeMainClip?.isMirrored == true) -1f else 1f
                            ),
                        contentScale = ContentScale.Crop
                    )

                    // Low-Res Proxy Preview Mode Downsampling Layer
                    if (isProxyMode) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.06f))
                                .drawWithCache {
                                    onDrawWithContent {
                                        drawContent()
                                        val step = 16f
                                        for (x in 0..size.width.toInt() step step.toInt()) {
                                            drawLine(
                                                color = Color.Black.copy(alpha = 0.025f),
                                                start = Offset(x.toFloat(), 0f),
                                                end = Offset(x.toFloat(), size.height),
                                                strokeWidth = 1f
                                            )
                                        }
                                    }
                                }
                        )
                    }

                    // Visual Filter Overlay simulation
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                when {
                                    activeFilter.contains("Pembalik Warna") || activeFilter.contains("Invert") -> Brush.verticalGradient(
                                        listOf(Color(0x66FFFFFF), Color(0x33000000))
                                    )
                                    activeFilter.contains("Cyberpunk") || activeFilter.contains("Neon") -> Brush.verticalGradient(
                                        listOf(Color(0x33FF00FF), Color(0x3300FFFF))
                                    )
                                    activeFilter.contains("Teal & Orange") || activeFilter.contains("TealOrange") -> Brush.verticalGradient(
                                        listOf(Color(0x33008080), Color(0x33FF7F50))
                                    )
                                    activeFilter.contains("Cinematic") || activeFilter.contains("Hollywood") -> Brush.radialGradient(
                                        listOf(Color(0x22FFD700), Color(0x66000000))
                                    )
                                    activeFilter.contains("Vintage") || activeFilter.contains("Fuji") || activeFilter.contains("Kodak") -> Brush.verticalGradient(
                                        listOf(Color(0x33A0522D), Color(0x228B4513))
                                    )
                                    activeFilter.contains("Sunset") || activeFilter.contains("Amber") -> Brush.horizontalGradient(
                                        listOf(Color(0x33FF7F50), Color(0x33FFD700))
                                    )
                                    activeFilter.contains("Noir") || activeFilter.contains("B&W") -> Brush.linearGradient(
                                        listOf(Color(0x88000000), Color(0x88333333))
                                    )
                                    activeFilter.contains("LOG") || activeFilter.contains("ARRI") || activeFilter.contains("Rec709") -> Brush.verticalGradient(
                                        listOf(Color(0x2200BFFF), Color(0x44000000))
                                    )
                                    activeFilter.contains("LUT:") -> Brush.radialGradient(
                                        listOf(Color(0x2200FA9A), Color(0x55000000))
                                    )
                                    else -> Brush.verticalGradient(
                                        listOf(Color.Transparent, Color(0x44000000))
                                    )
                                }
                            )
                    )

                    // True Pembalik Warna (Invert Colors) Blend Mode Layer
                    if (isInvertActive) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .drawWithCache {
                                    onDrawWithContent {
                                        drawContent()
                                        drawRect(
                                            color = Color.White,
                                            blendMode = BlendMode.Difference
                                        )
                                    }
                                }
                        )
                    }

                    // Kedip / Flash / Strobe Animasi Overlay Layer
                    if (isBlinkActive && blinkAlpha > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = blinkAlpha))
                        )
                    }

                    // Transition Dynamic Effect Layer (Active at clip boundaries)
                    val activeMainClip = activeClips.find { it.trackId == (clips.firstOrNull { c -> c.mediaUri.contains("video", ignoreCase = true) || c.trackId == 1L }?.trackId ?: 1L) }
                    if (activeMainClip != null && activeMainClip.transitionType.isNotBlank() && activeMainClip.transitionType != "None") {
                        val transDurationMs = 600L
                        val timeFromStart = currentTimeMs - activeMainClip.startTimeMs
                        val timeToEnd = activeMainClip.endTimeMs - currentTimeMs

                        if (timeFromStart in 0..transDurationMs) {
                            val progress = (timeFromStart.toFloat() / transDurationMs).coerceIn(0f, 1f)
                            RenderSmoothTransition(
                                transitionType = activeMainClip.transitionType,
                                progress = 1f - progress, // Transition In
                                isIncoming = true
                            )
                        } else if (timeToEnd in 0..transDurationMs) {
                            val progress = (1f - (timeToEnd.toFloat() / transDurationMs)).coerceIn(0f, 1f)
                            RenderSmoothTransition(
                                transitionType = activeMainClip.transitionType,
                                progress = progress, // Transition Out
                                isIncoming = false
                            )
                        }
                    }

                    // --- KEYFRAME ANIMATED OVERLAYS, TEXT & STICKERS LAYER ---
                    activeClips.forEach { clip ->
                        val isSelected = clip.id == selectedClipId
                        val clipOffsetMs = (currentTimeMs - clip.startTimeMs).coerceIn(0L, clip.durationMs)
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

                        // Apply smooth in/out/combo animation modifier
                        val animatedTransform = evaluateSmoothClipAnimation(
                            clip = clip,
                            clipOffsetMs = clipOffsetMs,
                            baseTransform = baseTransform
                        )

                        // Video Overlay PIP / Graphic Layer
                        if (clip.mediaUri.contains("overlay", ignoreCase = true) || clip.title.startsWith("Overlay")) {
                            KeyframeOverlayItem(
                                clip = clip,
                                transform = animatedTransform,
                                isSelected = isSelected,
                                onTransformChanged = { x, y, s, r ->
                                    onAddOrUpdateKeyframe?.invoke(clip, clipOffsetMs, x, y, s, r, animatedTransform.opacity)
                                }
                            )
                        }

                        // Text / Subtitle Caption Layer
                        if (clip.textContent != null) {
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

                        // Sticker & Badge Layer
                        if (clip.stickerIcon.isNotBlank() && clip.stickerIcon != "None") {
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

                // Center Play Icon Button if Paused and Timeline has clips
                if (!isPlaying && clips.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(54.dp)
                            .clip(CircleShape)
                            .clickable { onTogglePlay() }
                            .testTag("play_pause_button"),
                        color = StudioElectricBlue.copy(alpha = 0.9f),
                        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.8f)),
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
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
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
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeyframeOverlayItem(
    clip: TimelineClipEntity,
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
                .clip(RoundedCornerShape(8.dp))
                .border(
                    if (isSelected) 2.dp else 1.dp,
                    if (isSelected) StudioAccentAmber else Color.White.copy(alpha = 0.4f),
                    RoundedCornerShape(8.dp)
                )
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(8.dp)
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
    progress: Float, // 0.0f to 1.0f
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
        val smoothT = t * t * (3f - 2f * t) // Hermite smoothstep
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
                // Spring overshoot bounce formula
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

    // 3. Combo Animation Easing (Continuous motion along the clip)
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
