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

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, StudioCardBorder, RoundedCornerShape(16.dp))
            .testTag("video_player_card"),
        colors = CardDefaults.cardColors(containerColor = StudioDarkBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(aspectRatioFloat)
                    .clip(RoundedCornerShape(8.dp))
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
                                onTogglePlay()
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
                    // Background preview graphic
                    val imageRes = thumbnailDrawableRes ?: R.drawable.img_hero_banner_1785585794962
                    Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = "Preview Video Clip",
                        modifier = Modifier.fillMaxSize(),
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

                    // --- KEYFRAME ANIMATED OVERLAYS, TEXT & STICKERS LAYER ---
                    activeClips.forEach { clip ->
                        val isSelected = clip.id == selectedClipId
                        val clipOffsetMs = (currentTimeMs - clip.startTimeMs).coerceIn(0L, clip.durationMs)
                        val keyframes = remember(clip.keyframeData) {
                            KeyframeHelper.parseKeyframes(clip.keyframeData)
                        }

                        val transform = remember(keyframes, clipOffsetMs, clip.rotationDegrees, clip.opacity) {
                            KeyframeHelper.evaluateTransform(
                                keyframes = keyframes,
                                clipTimeOffsetMs = clipOffsetMs,
                                defaultRotation = clip.rotationDegrees.toFloat(),
                                defaultOpacity = clip.opacity
                            )
                        }

                        // Video Overlay PIP / Graphic Layer
                        if (clip.mediaUri.contains("overlay", ignoreCase = true) || clip.title.startsWith("Overlay")) {
                            KeyframeOverlayItem(
                                clip = clip,
                                transform = transform,
                                isSelected = isSelected,
                                onTransformChanged = { x, y, s, r ->
                                    onAddOrUpdateKeyframe?.invoke(clip, clipOffsetMs, x, y, s, r, transform.opacity)
                                }
                            )
                        }

                        // Text / Subtitle Caption Layer
                        if (clip.textContent != null) {
                            KeyframeTextItem(
                                clip = clip,
                                text = clip.textContent,
                                transform = transform,
                                isSelected = isSelected,
                                onTransformChanged = { x, y, s, r ->
                                    onAddOrUpdateKeyframe?.invoke(clip, clipOffsetMs, x, y, s, r, transform.opacity)
                                }
                            )
                        }

                        // Sticker & Badge Layer
                        if (clip.stickerIcon.isNotBlank() && clip.stickerIcon != "None") {
                            KeyframeStickerItem(
                                clip = clip,
                                sticker = clip.stickerIcon,
                                transform = transform,
                                isSelected = isSelected,
                                onTransformChanged = { x, y, s, r ->
                                    onAddOrUpdateKeyframe?.invoke(clip, clipOffsetMs, x, y, s, r, transform.opacity)
                                }
                            )
                        }
                    }
                }

                // Center Play Icon Button if Paused
                if (!isPlaying) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(52.dp)
                            .clip(CircleShape)
                            .testTag("play_pause_button"),
                        color = StudioPrimaryViolet.copy(alpha = 0.85f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }

                // --- FLOATING KEYFRAME HUD TOOLBAR OVERLAY FOR ACTIVE/SELECTED CLIP ---
                if (activeSelectedClip != null && (activeSelectedClip.hasKeyframe || activeSelectedClip.textContent != null || activeSelectedClip.stickerIcon.isNotBlank() || activeSelectedClip.mediaUri.contains("overlay"))) {
                    val clip = activeSelectedClip
                    val clipOffsetMs = (currentTimeMs - clip.startTimeMs).coerceIn(0L, clip.durationMs)
                    val keyframes = remember(clip.keyframeData) {
                        KeyframeHelper.parseKeyframes(clip.keyframeData)
                    }
                    val isAtKeyframe = keyframes.any { Math.abs(it.timeOffsetMs - clipOffsetMs) < 70L }
                    val currentTransform = KeyframeHelper.evaluateTransform(keyframes, clipOffsetMs, defaultRotation = clip.rotationDegrees.toFloat(), defaultOpacity = clip.opacity)

                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                            .testTag("keyframe_hud_panel"),
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, StudioAccentAmber.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Prev Keyframe Button
                            IconButton(
                                onClick = {
                                    val prev = keyframes.filter { it.timeOffsetMs < clipOffsetMs - 50L }.maxByOrNull { it.timeOffsetMs }
                                    if (prev != null) {
                                        onSeek?.invoke(clip.startTimeMs + prev.timeOffsetMs)
                                    }
                                },
                                enabled = keyframes.any { it.timeOffsetMs < clipOffsetMs - 50L },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Prev Keyframe",
                                    tint = if (keyframes.any { it.timeOffsetMs < clipOffsetMs - 50L }) StudioAccentAmber else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Add / Remove Keyframe Diamond Button
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (isAtKeyframe) {
                                            onRemoveKeyframe?.invoke(clip, clipOffsetMs)
                                        } else {
                                            onAddOrUpdateKeyframe?.invoke(
                                                clip,
                                                clipOffsetMs,
                                                currentTransform.posX,
                                                currentTransform.posY,
                                                currentTransform.scale,
                                                currentTransform.rotation,
                                                currentTransform.opacity
                                            )
                                        }
                                    }
                                    .testTag("add_remove_keyframe_hud_button"),
                                color = if (isAtKeyframe) StudioAccentAmber else StudioCardBg,
                                border = BorderStroke(1.dp, StudioAccentAmber)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isAtKeyframe) Icons.Default.Delete else Icons.Default.Add,
                                        contentDescription = null,
                                        tint = if (isAtKeyframe) Color.Black else StudioAccentAmber,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isAtKeyframe) "◆ Keyframe" else "+ Keyframe",
                                        color = if (isAtKeyframe) Color.Black else StudioAccentAmber,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Next Keyframe Button
                            IconButton(
                                onClick = {
                                    val next = keyframes.filter { it.timeOffsetMs > clipOffsetMs + 50L }.minByOrNull { it.timeOffsetMs }
                                    if (next != null) {
                                        onSeek?.invoke(clip.startTimeMs + next.timeOffsetMs)
                                    }
                                },
                                enabled = keyframes.any { it.timeOffsetMs > clipOffsetMs + 50L },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Next Keyframe",
                                    tint = if (keyframes.any { it.timeOffsetMs > clipOffsetMs + 50L }) StudioAccentAmber else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Keyframe counter badge
                            Text(
                                text = "KF: ${keyframes.size}",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
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
    var localDragX by remember(transform.posX) { mutableFloatStateOf(transform.posX) }
    var localDragY by remember(transform.posY) { mutableFloatStateOf(transform.posY) }
    var localScale by remember(transform.scale) { mutableFloatStateOf(transform.scale) }
    var localRot by remember(transform.rotation) { mutableFloatStateOf(transform.rotation) }

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(localDragX.roundToInt(), localDragY.roundToInt()) }
                .graphicsLayer {
                    scaleX = localScale
                    scaleY = localScale
                    rotationZ = localRot
                    alpha = transform.opacity
                }
                .pointerInput(clip.id) {
                    detectTransformGestures { _, pan, zoom, rotation ->
                        localDragX += pan.x
                        localDragY += pan.y
                        localScale = (localScale * zoom).coerceIn(0.2f, 4f)
                        localRot += rotation
                        onTransformChanged(localDragX, localDragY, localScale, localRot)
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
    var localDragX by remember(transform.posX) { mutableFloatStateOf(transform.posX) }
    var localDragY by remember(transform.posY) { mutableFloatStateOf(transform.posY) }
    var localScale by remember(transform.scale) { mutableFloatStateOf(transform.scale) }
    var localRot by remember(transform.rotation) { mutableFloatStateOf(transform.rotation) }

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
                .offset { IntOffset(localDragX.roundToInt(), localDragY.roundToInt()) }
                .graphicsLayer {
                    scaleX = localScale
                    scaleY = localScale
                    rotationZ = localRot
                    alpha = transform.opacity
                }
                .pointerInput(clip.id) {
                    detectTransformGestures { _, pan, zoom, rotation ->
                        localDragX += pan.x
                        localDragY += pan.y
                        localScale = (localScale * zoom).coerceIn(0.2f, 4f)
                        localRot += rotation
                        onTransformChanged(localDragX, localDragY, localScale, localRot)
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
    var localDragX by remember(transform.posX) { mutableFloatStateOf(transform.posX) }
    var localDragY by remember(transform.posY) { mutableFloatStateOf(transform.posY) }
    var localScale by remember(transform.scale) { mutableFloatStateOf(transform.scale) }
    var localRot by remember(transform.rotation) { mutableFloatStateOf(transform.rotation) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(localDragX.roundToInt(), localDragY.roundToInt()) }
                .graphicsLayer {
                    scaleX = localScale
                    scaleY = localScale
                    rotationZ = localRot
                    alpha = transform.opacity
                }
                .pointerInput(clip.id) {
                    detectTransformGestures { _, pan, zoom, rotation ->
                        localDragX += pan.x
                        localDragY += pan.y
                        localScale = (localScale * zoom).coerceIn(0.2f, 4f)
                        localRot += rotation
                        onTransformChanged(localDragX, localDragY, localScale, localRot)
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
