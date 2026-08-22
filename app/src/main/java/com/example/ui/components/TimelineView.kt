package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.hypot
import kotlin.math.roundToInt
import com.example.data.db.TimelineClipEntity
import com.example.data.db.TimelineTrackEntity
import com.example.data.models.KeyframeHelper
import com.example.ui.theme.*

enum class TimelineScope(val title: String, val subtitle: String, val themeColor: Color) {
    MAIN("Timeline Utama", "Komposisi Penuh Multitrack", StudioElectricBlue),
    VIDEO_SUBTIMELINE("Sub-Timeline Video & Overlay", "Video Utama & Layer PIP / Foto", StudioElectricBlue),
    AUDIO_SUBTIMELINE("Sub-Timeline Musik & Audio", "Musik BGM, Voiceover AI, & SFX", StudioRosePink),
    TEXT_SUBTIMELINE("Sub-Timeline Tipografi & Caption", "Teks Subjudul, Judul, & Auto-Captions", StudioEmeraldGreen),
    STICKER_SUBTIMELINE("Sub-Timeline Stiker & Overlay Foto", "Stiker Grafis, Emoji, & Badge Foto", StudioAmberGold)
}

@Composable
fun TimelineView(
    tracks: List<TimelineTrackEntity>,
    clips: List<TimelineClipEntity>,
    currentTimeMs: Long,
    totalDurationMs: Long = 15000L,
    isPlaying: Boolean = false,
    timelineScope: TimelineScope = TimelineScope.MAIN,
    selectedClip: TimelineClipEntity? = null,
    onScopeChanged: (TimelineScope) -> Unit = {},
    onSeek: (Long) -> Unit,
    onClipSelected: (TimelineClipEntity) -> Unit,
    onClipMoved: (clipId: Long, newStartTimeMs: Long, newTrackId: Long) -> Unit = { _, _, _ -> },
    onTrimClip: (clipId: Long, newStartMs: Long, newEndMs: Long) -> Unit = { _, _, _ -> },
    onTransitionClicked: (clipA: TimelineClipEntity, clipB: TimelineClipEntity) -> Unit = { _, _ -> },
    onAddClipRequested: () -> Unit = {},
    onAddOverlayTrackRequested: (String) -> Unit = {},
    onAddSoundRequested: () -> Unit = {},
    onAddTextRequested: () -> Unit = {},
    onAddStickerRequested: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    // Track last haptic step to deliver tactile feedback at discrete scrub ticks
    var lastHapticTick by remember { mutableLongStateOf(-1L) }

    // Dynamic horizontal timeline canvas scale (0.03f = Overview, 0.08f = Normal 1s, 0.35f = Detail, 0.70f = 0.10s Ultra-Precision: 1.00s-1.90s)
    var pxPerMs by remember { mutableFloatStateOf(0.08f) }

    val mainVideoTrack = tracks.find { it.trackType == "VIDEO" && it.trackIndex == 0 } ?: tracks.find { it.trackType == "VIDEO" }
    val overlayVideoTracks = tracks.filter { it.trackType == "VIDEO" && it.id != mainVideoTrack?.id }
    val textTracksAll = tracks.filter { it.trackType == "TEXT" }
    val photoTracksAll = tracks.filter { it.trackType == "PHOTO" }
    val stickerTracksAll = tracks.filter { it.trackType == "STICKER" }
    val audioTracksAll = tracks.filter { it.trackType == "AUDIO" }

    // Synchronize scroll position smoothly when playing or seeking externally
    LaunchedEffect(currentTimeMs, pxPerMs) {
        if (!scrollState.isScrollInProgress) {
            val targetPx = with(density) { (currentTimeMs * pxPerMs).dp.toPx() }
            scrollState.scrollTo(targetPx.roundToInt().coerceIn(0, scrollState.maxValue))
        }
    }

    // Two-way sync: when user swipes / scrolls the timeline with finger, seek the video
    LaunchedEffect(scrollState.value) {
        if (scrollState.isScrollInProgress) {
            val pxPerMsDensity = with(density) { pxPerMs.dp.toPx() }
            if (pxPerMsDensity > 0.0001f) {
                val scrolledMs = (scrollState.value / pxPerMsDensity).toLong().coerceIn(0L, totalDurationMs)
                if (scrolledMs != currentTimeMs) {
                    val tick = scrolledMs / 100L
                    if (tick != lastHapticTick) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        lastHapticTick = tick
                    }
                    onSeek(scrolledMs)
                }
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, StudioCardHairline, RoundedCornerShape(20.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    do {
                        val event = awaitPointerEvent()
                        val downPointers = event.changes.filter { it.pressed }
                        if (downPointers.size >= 2) {
                            val p1 = downPointers[0].position
                            val p2 = downPointers[1].position
                            val prevP1 = downPointers[0].previousPosition
                            val prevP2 = downPointers[1].previousPosition

                            val currentDist = hypot((p1.x - p2.x).toDouble(), (p1.y - p2.y).toDouble()).toFloat()
                            val prevDist = hypot((prevP1.x - prevP2.x).toDouble(), (prevP1.y - prevP2.y).toDouble()).toFloat()

                            if (prevDist > 6f && currentDist > 6f) {
                                val factor = currentDist / prevDist
                                if (kotlin.math.abs(factor - 1f) > 0.0015f) {
                                    val newPx = (pxPerMs * factor).coerceIn(0.02f, 0.75f)
                                    if (newPx != pxPerMs) {
                                        pxPerMs = newPx
                                    }
                                }
                            }
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .testTag("timeline_editor_container"),
        colors = CardDefaults.cardColors(containerColor = StudioGlassWhite)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val containerWidth = maxWidth
            // Playhead is fixed at 1/3 of the visible timeline width
            val fixedPlayheadOffsetDp = (containerWidth / 3f).coerceIn(90.dp, 160.dp)
            val endPaddingDp = (containerWidth - fixedPlayheadOffsetDp).coerceAtLeast(120.dp)
            val timelineWidthDp = fixedPlayheadOffsetDp + (totalDurationMs * pxPerMs).dp + endPaddingDp

            Column(modifier = Modifier.fillMaxSize()) {
                // Main Multitrack Scrollable Timeline Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .horizontalScroll(scrollState)
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(
                        modifier = Modifier.width(timelineWidthDp)
                    ) {
                        // Time Ruler
                        TimeRulerCanvas(
                            startOffsetDp = fixedPlayheadOffsetDp,
                            totalDurationMs = totalDurationMs,
                            pxPerMs = pxPerMs,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp)
                                .background(StudioPillBg, RoundedCornerShape(8.dp))
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                    // Multitrack Rows
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        when (timelineScope) {
                            TimelineScope.MAIN -> {
                                // 1. Video Track Row
                                val mainVideoClips = clips.filter { clip ->
                                    if (mainVideoTrack != null) {
                                        clip.trackId == mainVideoTrack.id
                                    } else {
                                        (clip.stickerIcon == "None" || clip.stickerIcon.isBlank()) &&
                                        clip.textContent == null &&
                                        (clip.audioSfx == "None" || clip.audioSfx.isBlank())
                                    }
                                }
                                TrackClipsRowWithIcon(
                                    icon = Icons.Default.Movie,
                                    trackId = mainVideoTrack?.id ?: -1L,
                                    trackType = "VIDEO",
                                    trackColor = StudioPrimaryViolet,
                                    clips = mainVideoClips,
                                    selectedClipId = selectedClip?.id,
                                    currentTimeMs = currentTimeMs,
                                    isPlaying = isPlaying,
                                    allTracks = tracks,
                                    pxPerMs = pxPerMs,
                                    startOffsetDp = fixedPlayheadOffsetDp,
                                    onClipSelected = onClipSelected,
                                    onClipMoved = onClipMoved,
                                    onTrimClip = onTrimClip,
                                    onTransitionClicked = onTransitionClicked,
                                    onSeek = onSeek,
                                    onIconClick = { onScopeChanged(TimelineScope.VIDEO_SUBTIMELINE) },
                                    onAddClick = onAddClipRequested
                                )

                                // 2. Audio / Sound Track Row
                                val audioClips = clips.filter { clip ->
                                    val clipTrack = tracks.find { it.id == clip.trackId }
                                    if (clipTrack != null) {
                                        clipTrack.trackType == "AUDIO"
                                    } else {
                                        (clip.audioSfx.isNotBlank() && clip.audioSfx != "None") || clip.isVoiceover
                                    }
                                }
                                TrackClipsRowWithIcon(
                                    icon = Icons.Default.Audiotrack,
                                    trackId = audioTracksAll.firstOrNull()?.id ?: -10L,
                                    trackType = "AUDIO",
                                    trackColor = StudioAccentPink,
                                    clips = audioClips,
                                    selectedClipId = selectedClip?.id,
                                    currentTimeMs = currentTimeMs,
                                    isPlaying = isPlaying,
                                    allTracks = tracks,
                                    pxPerMs = pxPerMs,
                                    startOffsetDp = fixedPlayheadOffsetDp,
                                    onClipSelected = onClipSelected,
                                    onClipMoved = onClipMoved,
                                    onTrimClip = onTrimClip,
                                    onTransitionClicked = onTransitionClicked,
                                    onSeek = onSeek,
                                    onIconClick = { onScopeChanged(TimelineScope.AUDIO_SUBTIMELINE) },
                                    onAddClick = onAddSoundRequested
                                )

                                // 3. Text / Caption Track Row
                                val textClips = clips.filter { clip ->
                                    val clipTrack = tracks.find { it.id == clip.trackId }
                                    if (clipTrack != null) {
                                        clipTrack.trackType == "TEXT"
                                    } else {
                                        clip.textContent != null && clip.textContent.isNotBlank()
                                    }
                                }
                                TrackClipsRowWithIcon(
                                    icon = Icons.Default.Subtitles,
                                    trackId = textTracksAll.firstOrNull()?.id ?: -20L,
                                    trackType = "TEXT",
                                    trackColor = StudioSecondaryTeal,
                                    clips = textClips,
                                    selectedClipId = selectedClip?.id,
                                    currentTimeMs = currentTimeMs,
                                    isPlaying = isPlaying,
                                    allTracks = tracks,
                                    pxPerMs = pxPerMs,
                                    startOffsetDp = fixedPlayheadOffsetDp,
                                    onClipSelected = onClipSelected,
                                    onClipMoved = onClipMoved,
                                    onTrimClip = onTrimClip,
                                    onTransitionClicked = onTransitionClicked,
                                    onSeek = onSeek,
                                    onIconClick = { onScopeChanged(TimelineScope.TEXT_SUBTIMELINE) },
                                    onAddClick = onAddTextRequested
                                )

                                // 4. Overlay Photo Track Row
                                val photoClips = clips.filter { clip ->
                                    val clipTrack = tracks.find { it.id == clip.trackId }
                                    if (clipTrack != null) {
                                        clipTrack.trackType == "PHOTO"
                                    } else {
                                        clip.mediaUri.isNotBlank() && (clip.mediaUri.contains("photo", ignoreCase = true) || clip.mediaUri.contains("image", ignoreCase = true) || clip.title.startsWith("Overlay Foto"))
                                    }
                                }
                                TrackClipsRowWithIcon(
                                    icon = Icons.Default.Image,
                                    trackId = photoTracksAll.firstOrNull()?.id ?: -30L,
                                    trackType = "PHOTO",
                                    trackColor = StudioAccentAmber,
                                    clips = photoClips,
                                    selectedClipId = selectedClip?.id,
                                    currentTimeMs = currentTimeMs,
                                    isPlaying = isPlaying,
                                    allTracks = tracks,
                                    pxPerMs = pxPerMs,
                                    startOffsetDp = fixedPlayheadOffsetDp,
                                    onClipSelected = onClipSelected,
                                    onClipMoved = onClipMoved,
                                    onTrimClip = onTrimClip,
                                    onTransitionClicked = onTransitionClicked,
                                    onSeek = onSeek,
                                    onIconClick = { onScopeChanged(TimelineScope.STICKER_SUBTIMELINE) },
                                    onAddClick = onAddStickerRequested
                                )

                                // 5. Sticker & Emoji Track Row
                                val stickerClips = clips.filter { clip ->
                                    val clipTrack = tracks.find { it.id == clip.trackId }
                                    if (clipTrack != null) {
                                        clipTrack.trackType == "STICKER"
                                    } else {
                                        clip.stickerIcon.isNotBlank() && clip.stickerIcon != "None" && clip.stickerIcon != "IMAGE_OVERLAY"
                                    }
                                }
                                TrackClipsRowWithIcon(
                                    icon = Icons.Default.AutoAwesome,
                                    trackId = stickerTracksAll.firstOrNull()?.id ?: -40L,
                                    trackType = "STICKER",
                                    trackColor = StudioAmberGold,
                                    clips = stickerClips,
                                    selectedClipId = selectedClip?.id,
                                    currentTimeMs = currentTimeMs,
                                    isPlaying = isPlaying,
                                    allTracks = tracks,
                                    pxPerMs = pxPerMs,
                                    startOffsetDp = fixedPlayheadOffsetDp,
                                    onClipSelected = onClipSelected,
                                    onClipMoved = onClipMoved,
                                    onTrimClip = onTrimClip,
                                    onTransitionClicked = onTransitionClicked,
                                    onSeek = onSeek,
                                    onIconClick = { onScopeChanged(TimelineScope.STICKER_SUBTIMELINE) },
                                    onAddClick = onAddStickerRequested
                                )
                            }

                            TimelineScope.VIDEO_SUBTIMELINE -> {
                                if (mainVideoTrack != null) {
                                    key(mainVideoTrack.id) {
                                        val mainClips = clips.filter { it.trackId == mainVideoTrack.id }
                                        TrackClipsRowWithIcon(
                                            icon = Icons.Default.Movie,
                                            trackId = mainVideoTrack.id,
                                            trackType = "VIDEO",
                                            trackColor = StudioPrimaryViolet,
                                            clips = mainClips,
                                            selectedClipId = selectedClip?.id,
                                            currentTimeMs = currentTimeMs,
                                            isPlaying = isPlaying,
                                            allTracks = tracks,
                                            pxPerMs = pxPerMs,
                                            startOffsetDp = fixedPlayheadOffsetDp,
                                            onClipSelected = onClipSelected,
                                            onClipMoved = onClipMoved,
                                            onTrimClip = onTrimClip,
                                            onTransitionClicked = onTransitionClicked,
                                            onSeek = onSeek,
                                            onIconClick = onAddClipRequested,
                                            onAddClick = onAddClipRequested
                                        )
                                    }
                                }

                                overlayVideoTracks.forEach { trk ->
                                    key(trk.id) {
                                        val overlayClips = clips.filter { it.trackId == trk.id }
                                        TrackClipsRowWithIcon(
                                            icon = Icons.Default.Layers,
                                            trackId = trk.id,
                                            trackType = "VIDEO",
                                            trackColor = StudioSecondaryTeal,
                                            clips = overlayClips,
                                            selectedClipId = selectedClip?.id,
                                            currentTimeMs = currentTimeMs,
                                            isPlaying = isPlaying,
                                            allTracks = tracks,
                                            pxPerMs = pxPerMs,
                                            startOffsetDp = fixedPlayheadOffsetDp,
                                            onClipSelected = onClipSelected,
                                            onClipMoved = onClipMoved,
                                            onTrimClip = onTrimClip,
                                            onTransitionClicked = onTransitionClicked,
                                            onSeek = onSeek,
                                            onIconClick = onAddClipRequested,
                                            onAddClick = onAddClipRequested
                                        )
                                    }
                                }
                            }

                            TimelineScope.AUDIO_SUBTIMELINE -> {
                                if (audioTracksAll.isNotEmpty()) {
                                    audioTracksAll.forEach { trk ->
                                        key(trk.id) {
                                            val audioClips = clips.filter { it.trackId == trk.id }
                                            TrackClipsRowWithIcon(
                                                icon = Icons.Default.Audiotrack,
                                                trackId = trk.id,
                                                trackType = "AUDIO",
                                                trackColor = StudioAccentPink,
                                                clips = audioClips,
                                                selectedClipId = selectedClip?.id,
                                                currentTimeMs = currentTimeMs,
                                                isPlaying = isPlaying,
                                                allTracks = tracks,
                                                pxPerMs = pxPerMs,
                                                startOffsetDp = fixedPlayheadOffsetDp,
                                                onClipSelected = onClipSelected,
                                                onClipMoved = onClipMoved,
                                                onTrimClip = onTrimClip,
                                                onTransitionClicked = onTransitionClicked,
                                                onSeek = onSeek,
                                                onIconClick = onAddSoundRequested,
                                                onAddClick = onAddSoundRequested
                                            )
                                        }
                                    }
                                } else {
                                    EmptyTrackPlaceholder(
                                        title = "+ Tambah Musik",
                                        color = StudioAccentPink,
                                        onClick = onAddSoundRequested
                                    )
                                }
                            }

                            TimelineScope.TEXT_SUBTIMELINE -> {
                                if (textTracksAll.isNotEmpty()) {
                                    textTracksAll.forEach { trk ->
                                        key(trk.id) {
                                            val textClips = clips.filter { it.trackId == trk.id || (trk == textTracksAll.first() && it.textContent != null) }
                                            TrackClipsRowWithIcon(
                                                icon = Icons.Default.Subtitles,
                                                trackId = trk.id,
                                                trackType = "TEXT",
                                                trackColor = StudioSecondaryTeal,
                                                clips = textClips,
                                                selectedClipId = selectedClip?.id,
                                                currentTimeMs = currentTimeMs,
                                                isPlaying = isPlaying,
                                                allTracks = tracks,
                                                pxPerMs = pxPerMs,
                                                startOffsetDp = fixedPlayheadOffsetDp,
                                                onClipSelected = onClipSelected,
                                                onClipMoved = onClipMoved,
                                                onTrimClip = onTrimClip,
                                                onTransitionClicked = onTransitionClicked,
                                                onSeek = onSeek,
                                                onIconClick = onAddTextRequested,
                                                onAddClick = onAddTextRequested
                                            )
                                        }
                                    }
                                } else {
                                    EmptyTrackPlaceholder(
                                        title = "+ Tambah Teks",
                                        color = StudioSecondaryTeal,
                                        onClick = onAddTextRequested
                                    )
                                }
                            }

                            TimelineScope.STICKER_SUBTIMELINE -> {
                                if (stickerTracksAll.isNotEmpty()) {
                                    stickerTracksAll.forEach { trk ->
                                        key(trk.id) {
                                            val stickerClips = clips.filter {
                                                val clipTrack = tracks.find { t -> t.id == it.trackId }
                                                if (clipTrack != null) {
                                                    clipTrack.id == trk.id || clipTrack.trackType in listOf("STICKER", "PHOTO")
                                                } else {
                                                    it.stickerIcon.isNotBlank() && it.stickerIcon != "None"
                                                }
                                            }
                                            TrackClipsRowWithIcon(
                                                icon = Icons.Default.AutoAwesome,
                                                trackId = trk.id,
                                                trackType = "STICKER",
                                                trackColor = Color(0xFFFFB74D),
                                                clips = stickerClips,
                                                selectedClipId = selectedClip?.id,
                                                currentTimeMs = currentTimeMs,
                                                isPlaying = isPlaying,
                                                allTracks = tracks,
                                                pxPerMs = pxPerMs,
                                                startOffsetDp = fixedPlayheadOffsetDp,
                                                onClipSelected = onClipSelected,
                                                onClipMoved = onClipMoved,
                                                onTrimClip = onTrimClip,
                                                onTransitionClicked = onTransitionClicked,
                                                onSeek = onSeek,
                                                onIconClick = onAddStickerRequested,
                                                onAddClick = onAddStickerRequested
                                            )
                                        }
                                    }
                                } else {
                                    val stickerClips = clips.filter {
                                        val clipTrack = tracks.find { t -> t.id == it.trackId }
                                        if (clipTrack != null) {
                                            clipTrack.trackType in listOf("STICKER", "PHOTO")
                                        } else {
                                            it.stickerIcon.isNotBlank() && it.stickerIcon != "None"
                                        }
                                    }
                                    TrackClipsRowWithIcon(
                                        icon = Icons.Default.AutoAwesome,
                                        trackId = -999L,
                                        trackType = "STICKER",
                                        trackColor = Color(0xFFFFB74D),
                                        clips = stickerClips,
                                        selectedClipId = selectedClip?.id,
                                        currentTimeMs = currentTimeMs,
                                        isPlaying = isPlaying,
                                        allTracks = tracks,
                                        pxPerMs = pxPerMs,
                                        startOffsetDp = fixedPlayheadOffsetDp,
                                        onClipSelected = onClipSelected,
                                        onClipMoved = onClipMoved,
                                        onTrimClip = onTrimClip,
                                        onTransitionClicked = onTransitionClicked,
                                        onSeek = onSeek,
                                        onIconClick = onAddStickerRequested,
                                        onAddClick = onAddStickerRequested
                                    )
                                }
                            }
                        }
                    }
                }
            } // Close horizontal/vertical scroll Box
        } // Close Column

        // Fixed stationary playhead positioned at exactly 1/3 of the screen with active drag-to-scrub
        StaticFixedPlayhead(
            offsetDp = fixedPlayheadOffsetDp,
            pxPerMs = pxPerMs,
            currentTimeMs = currentTimeMs,
            totalDurationMs = totalDurationMs,
            onSeek = onSeek,
            modifier = Modifier
                .fillMaxHeight()
                .padding(top = 28.dp)
        )
    } // Close BoxWithConstraints
} // Close Card
} // Close TimelineView

@Composable
fun TrackClipsRowWithIcon(
    icon: ImageVector,
    trackId: Long,
    trackType: String,
    trackColor: Color,
    clips: List<TimelineClipEntity>,
    selectedClipId: Long?,
    currentTimeMs: Long = 0L,
    isPlaying: Boolean = false,
    allTracks: List<TimelineTrackEntity>,
    pxPerMs: Float,
    startOffsetDp: Dp,
    onClipSelected: (TimelineClipEntity) -> Unit,
    onClipMoved: (clipId: Long, newStartTimeMs: Long, newTrackId: Long) -> Unit,
    onTrimClip: (clipId: Long, newStartMs: Long, newEndMs: Long) -> Unit = { _, _, _ -> },
    onTransitionClicked: (clipA: TimelineClipEntity, clipB: TimelineClipEntity) -> Unit,
    onSeek: (Long) -> Unit,
    onIconClick: () -> Unit,
    onAddClick: () -> Unit = onIconClick
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val sortedClips = remember(clips) { clips.sortedBy { it.startTimeMs } }

    val infiniteTransition = rememberInfiniteTransition(label = "playback_pulse")
    val playbackPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        // Track Icon Badge (Icon only, inside the scroll canvas so it scrolls naturally)
        Surface(
            modifier = Modifier
                .offset(x = 2.dp, y = 2.dp)
                .size(34.dp, 48.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable { onIconClick() },
            color = StudioCardWhite,
            border = BorderStroke(1.5.dp, trackColor),
            shadowElevation = 1.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = trackType,
                    tint = trackColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Track Clips Canvas Row
        Box(
            modifier = Modifier
                .offset(x = startOffsetDp)
                .fillMaxWidth()
                .height(52.dp)
                .background(StudioCanvasSlate, RoundedCornerShape(10.dp))
                .border(1.dp, StudioCardHairline, RoundedCornerShape(10.dp))
        ) {
            if (sortedClips.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                        .clickable { onAddClick() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = trackColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (trackType) {
                            "VIDEO" -> "+ Tambah Klip Video"
                            "AUDIO" -> "+ Tambah Musik / Audio"
                            "TEXT" -> "+ Tambah Teks / Subjudul"
                            "STICKER" -> "+ Tambah Stiker / Foto"
                            else -> "+ Tambah Media"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = trackColor
                    )
                }
            }

            sortedClips.forEachIndexed { index, clip ->
                key(clip.id) {
                    var isTrimmingLeft by remember { mutableStateOf(false) }
                    var isTrimmingRight by remember { mutableStateOf(false) }
                    var trimStartDeltaMs by remember { mutableLongStateOf(0L) }
                    var trimEndDeltaMs by remember { mutableLongStateOf(0L) }

                    val effectiveStartMs = (clip.startTimeMs + trimStartDeltaMs).coerceAtLeast(0L)
                    val effectiveEndMs = (clip.endTimeMs + trimEndDeltaMs).coerceAtLeast(effectiveStartMs + 200L)
                    val effectiveDurationMs = effectiveEndMs - effectiveStartMs

                    val startDp = (effectiveStartMs * pxPerMs).dp
                    val widthDp = (effectiveDurationMs * pxPerMs).dp.coerceAtLeast(42.dp)
                    val isSelected = clip.id == selectedClipId
                    val isPlayheadOverClip = currentTimeMs in effectiveStartMs until effectiveEndMs
                    val isActivelyPlaying = isPlaying && isPlayheadOverClip

                    val elapsedInClipMs = (currentTimeMs - effectiveStartMs).coerceIn(0L, effectiveDurationMs)
                    val clipProgressFraction = if (effectiveDurationMs > 0L) {
                        (elapsedInClipMs.toFloat() / effectiveDurationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    var isDragging by remember { mutableStateOf(false) }
                    var dragOffsetX by remember { mutableFloatStateOf(0f) }
                    var dragOffsetY by remember { mutableFloatStateOf(0f) }

                    val keyframes = remember(clip.keyframeData) {
                        KeyframeHelper.parseKeyframes(clip.keyframeData)
                    }

                    Box(
                        modifier = Modifier
                            .offset(x = startDp)
                            .offset { IntOffset(dragOffsetX.roundToInt(), dragOffsetY.roundToInt()) }
                            .width(widthDp)
                            .fillMaxHeight()
                            .padding(vertical = 2.dp, horizontal = 1.dp)
                            .zIndex(if (isDragging) 15f else if (isSelected) 10f else if (isActivelyPlaying) 5f else 1f)
                            .pointerInput(clip.id) {
                                detectTapGestures(
                                    onTap = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onClipSelected(clip)
                                    }
                                )
                            }
                            .pointerInput(clip.id, allTracks, pxPerMs) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        isDragging = true
                                        dragOffsetX = 0f
                                        dragOffsetY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffsetX += dragAmount.x
                                        dragOffsetY += dragAmount.y
                                    },
                                    onDragEnd = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (isDragging) {
                                            val densityPxPerMs = pxPerMs.dp.toPx()
                                            val deltaMs = (dragOffsetX / densityPxPerMs).toLong()
                                            val newStartMs = (clip.startTimeMs + deltaMs).coerceAtLeast(0L)

                                             val trackHeightPx = with(density) { 54.dp.toPx() }
                                            val indexShift = (dragOffsetY / trackHeightPx).roundToInt()

                                            val sameTypeTracks = allTracks.filter { it.trackType == trackType }
                                            val currentTrackIdx = sameTypeTracks.indexOfFirst { it.id == trackId }.coerceAtLeast(0)
                                            val targetTrackIdx = (currentTrackIdx + indexShift).coerceIn(0, sameTypeTracks.lastIndex)
                                            val targetTrackId = sameTypeTracks.getOrNull(targetTrackIdx)?.id ?: trackId

                                            onClipMoved(clip.id, newStartMs, targetTrackId)
                                        }
                                        isDragging = false
                                        dragOffsetX = 0f
                                        dragOffsetY = 0f
                                    },
                                    onDragCancel = {
                                        isDragging = false
                                        dragOffsetX = 0f
                                        dragOffsetY = 0f
                                    }
                                )
                            }
                    ) {
                        // Main Clip Surface
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                                .testTag("clip_item_${clip.id}"),
                            color = if (isDragging) {
                                StudioElectricBlue
                            } else {
                                trackColor
                            },
                            border = BorderStroke(
                                width = if (isDragging) 2.5.dp else if (isSelected) 2.5.dp else if (isActivelyPlaying) 2.dp else 1.2.dp,
                                color = when {
                                    isDragging -> StudioAmberGold
                                    isSelected -> Color.White
                                    isActivelyPlaying -> StudioEmeraldGreen.copy(alpha = playbackPulseAlpha)
                                    isPlayheadOverClip -> Color.White.copy(alpha = 0.85f)
                                    else -> Color.White.copy(alpha = 0.35f)
                                }
                            ),
                            shadowElevation = if (isDragging || isSelected) 6.dp else if (isActivelyPlaying) 3.dp else 1.dp
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                // 1. Dynamic Playback Elapsed Progress Overlay
                                if (isPlayheadOverClip && clipProgressFraction > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(clipProgressFraction)
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(
                                                        Color.White.copy(alpha = 0.30f),
                                                        Color.White.copy(alpha = 0.15f)
                                                    )
                                                )
                                            )
                                    )
                                }

                                // 2. Clip Content & Info Row
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = if (isSelected) 10.dp else 6.dp, vertical = 3.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Top Row: Title + Status Indicators
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.weight(1f, fill = false)
                                        ) {
                                            // Active Playing Indicator Dot or Selection Pin
                                            if (isActivelyPlaying) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .background(StudioEmeraldGreen, CircleShape)
                                                )
                                            } else if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .background(Color.White, CircleShape)
                                                )
                                            }

                                            Text(
                                                text = if (trackType == "TEXT") clip.textContent ?: clip.title else clip.title,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                                maxLines = 1
                                            )
                                        }

                                        // Playing Animated Equalizer
                                        if (isActivelyPlaying) {
                                            ClipEqualizerIndicator()
                                        }
                                    }

                                    // Bottom Row: Engine Badges (Speed, AI Cutout, FX, Keyframe, SFX)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        // Speed Multiplier / Curve badge
                                        if (clip.speedMultiplier != 1.0f || (clip.speedCurve.isNotBlank() && clip.speedCurve != "Normal")) {
                                            Surface(
                                                color = StudioDarkCharcoal,
                                                shape = RoundedCornerShape(3.dp)
                                            ) {
                                                Text(
                                                    text = if (clip.speedCurve.isNotBlank() && clip.speedCurve != "Normal") "${clip.speedMultiplier}x⚡" else "${clip.speedMultiplier}x",
                                                    color = Color.White,
                                                    fontSize = 7.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                                )
                                            }
                                        }

                                        // AI Cutout badge
                                        if (clip.cutoutMode.isNotBlank() && clip.cutoutMode != "None") {
                                            Surface(
                                                color = Color(0xDD6200EA),
                                                shape = RoundedCornerShape(3.dp)
                                            ) {
                                                Text(
                                                    text = "AI Cutout",
                                                    color = Color.White,
                                                    fontSize = 7.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                                )
                                            }
                                        }

                                        // FX badge
                                        if (clip.effectName.isNotBlank() && clip.effectName != "None") {
                                            Surface(
                                                color = Color(0xDD00838F),
                                                shape = RoundedCornerShape(3.dp)
                                            ) {
                                                Text(
                                                    text = "FX",
                                                    color = Color.White,
                                                    fontSize = 7.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                                )
                                            }
                                        }

                                        // Keyframes Badge
                                        if (keyframes.isNotEmpty()) {
                                            Surface(
                                                color = Color(0xDDFF8F00),
                                                shape = RoundedCornerShape(3.dp)
                                            ) {
                                                Text(
                                                    text = "◆ ${keyframes.size}",
                                                    color = Color.White,
                                                    fontSize = 7.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                                )
                                            }
                                        }

                                        // SFX badge
                                        if (clip.audioSfx.isNotBlank() && clip.audioSfx != "None") {
                                            Surface(
                                                color = Color(0xFFE91E63),
                                                shape = RoundedCornerShape(3.dp)
                                            ) {
                                                Text(
                                                    text = "SFX",
                                                    color = Color.White,
                                                    fontSize = 7.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Keyframe diamond markers (◆) along bottom edge
                                if (keyframes.isNotEmpty()) {
                                    keyframes.forEach { kf ->
                                        key(kf.timeOffsetMs) {
                                            val kfOffsetDp = (kf.timeOffsetMs * pxPerMs).dp
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .offset(x = (kfOffsetDp - 4.dp).coerceAtLeast(0.dp), y = (-2).dp)
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) StudioAmberGold else Color.White)
                                                    .border(1.dp, StudioDarkCharcoal, CircleShape)
                                                    .clickable {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        onSeek(clip.startTimeMs + kf.timeOffsetMs)
                                                    }
                                            )
                                        }
                                    }
                                }

                                // 3. Left and Right Selection Grip Trim Handles (iOS / CapCut style) with interactive Drag Trimming
                                if (isSelected) {
                                    // Left Handle (Trims clip start)
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .width(16.dp)
                                            .fillMaxHeight()
                                            .background(
                                                if (isTrimmingLeft) StudioAmberGold else Color.White,
                                                RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp)
                                            )
                                            .pointerInput(clip.id, pxPerMs) {
                                                detectHorizontalDragGestures(
                                                    onDragStart = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        isTrimmingLeft = true
                                                    },
                                                    onHorizontalDrag = { change, dragAmount ->
                                                        change.consume()
                                                        val densityPxPerMs = pxPerMs.dp.toPx()
                                                        val deltaMs = (dragAmount / densityPxPerMs).toLong()
                                                        trimStartDeltaMs = (trimStartDeltaMs + deltaMs).coerceIn(-clip.startTimeMs, (clip.endTimeMs - clip.startTimeMs - 200L))
                                                    },
                                                    onDragEnd = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        if (isTrimmingLeft) {
                                                            val newStart = (clip.startTimeMs + trimStartDeltaMs).coerceAtLeast(0L)
                                                            onTrimClip(clip.id, newStart, clip.endTimeMs)
                                                            trimStartDeltaMs = 0L
                                                        }
                                                        isTrimmingLeft = false
                                                    },
                                                    onDragCancel = {
                                                        trimStartDeltaMs = 0L
                                                        isTrimmingLeft = false
                                                    }
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(2.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(modifier = Modifier.size(2.dp).background(StudioDarkCharcoal, CircleShape))
                                            Box(modifier = Modifier.size(2.dp).background(StudioDarkCharcoal, CircleShape))
                                            Box(modifier = Modifier.size(2.dp).background(StudioDarkCharcoal, CircleShape))
                                        }
                                    }

                                    // Right Handle (Trims clip end)
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .width(16.dp)
                                            .fillMaxHeight()
                                            .background(
                                                if (isTrimmingRight) StudioAmberGold else Color.White,
                                                RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp)
                                            )
                                            .pointerInput(clip.id, pxPerMs) {
                                                detectHorizontalDragGestures(
                                                    onDragStart = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        isTrimmingRight = true
                                                    },
                                                    onHorizontalDrag = { change, dragAmount ->
                                                        change.consume()
                                                        val densityPxPerMs = pxPerMs.dp.toPx()
                                                        val deltaMs = (dragAmount / densityPxPerMs).toLong()
                                                        trimEndDeltaMs = (trimEndDeltaMs + deltaMs).coerceAtLeast(-(clip.endTimeMs - clip.startTimeMs - 200L))
                                                    },
                                                    onDragEnd = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        if (isTrimmingRight) {
                                                            val newEnd = (clip.endTimeMs + trimEndDeltaMs).coerceAtLeast(clip.startTimeMs + 200L)
                                                            onTrimClip(clip.id, clip.startTimeMs, newEnd)
                                                            trimEndDeltaMs = 0L
                                                        }
                                                        isTrimmingRight = false
                                                    },
                                                    onDragCancel = {
                                                        trimEndDeltaMs = 0L
                                                        isTrimmingRight = false
                                                    }
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(2.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(modifier = Modifier.size(2.dp).background(StudioDarkCharcoal, CircleShape))
                                            Box(modifier = Modifier.size(2.dp).background(StudioDarkCharcoal, CircleShape))
                                            Box(modifier = Modifier.size(2.dp).background(StudioDarkCharcoal, CircleShape))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Transition junction button between adjacent clips
                    if (index < sortedClips.size - 1) {
                        val nextClip = sortedClips[index + 1]
                        val junctionTimeMs = clip.endTimeMs
                        val junctionOffsetDp = (junctionTimeMs * pxPerMs).dp - 11.dp

                        Surface(
                            modifier = Modifier
                                .offset(x = junctionOffsetDp, y = 14.dp)
                                .size(24.dp)
                                .clip(CircleShape)
                                .zIndex(20f)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onTransitionClicked(clip, nextClip)
                                }
                                .testTag("transition_button_${clip.id}_${nextClip.id}"),
                            color = if (clip.transitionType.isNotBlank() && clip.transitionType != "None") StudioElectricBlue else StudioCardWhite,
                            border = BorderStroke(1.dp, if (clip.transitionType.isNotBlank() && clip.transitionType != "None") StudioElectricBlue else StudioCardHairline),
                            shadowElevation = 2.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CompareArrows,
                                    contentDescription = "Transisi ${clip.transitionType}",
                                    tint = if (clip.transitionType.isNotBlank() && clip.transitionType != "None") Color.White else StudioTextDark,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClipEqualizerIndicator() {
    val transition = rememberInfiniteTransition(label = "clip_eq")
    val bar1Height by transition.animateFloat(
        initialValue = 3f,
        targetValue = 9f,
        animationSpec = infiniteRepeatable(tween(320, easing = LinearEasing), RepeatMode.Reverse),
        label = "b1"
    )
    val bar2Height by transition.animateFloat(
        initialValue = 8f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(250, easing = LinearEasing), RepeatMode.Reverse),
        label = "b2"
    )
    val bar3Height by transition.animateFloat(
        initialValue = 4f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(tween(400, easing = LinearEasing), RepeatMode.Reverse),
        label = "b3"
    )

    Row(
        modifier = Modifier
            .background(Color(0xAA0D0E12), RoundedCornerShape(3.dp))
            .padding(horizontal = 3.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(bar1Height.dp)
                .background(StudioEmeraldGreen, RoundedCornerShape(1.dp))
        )
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(bar2Height.dp)
                .background(StudioEmeraldGreen, RoundedCornerShape(1.dp))
        )
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(bar3Height.dp)
                .background(StudioEmeraldGreen, RoundedCornerShape(1.dp))
        )
    }
}

@Composable
fun EmptyTrackPlaceholder(
    title: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable { onClick() },
        color = StudioCardWhite,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.5.dp, color),
        shadowElevation = 1.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TimeRulerCanvas(
    startOffsetDp: Dp = 0.dp,
    totalDurationMs: Long,
    pxPerMs: Float,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val majorTextStyle = remember {
        androidx.compose.ui.text.TextStyle(
            color = StudioElectricBlue,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
    val halfSecTextStyle = remember {
        androidx.compose.ui.text.TextStyle(
            color = StudioTextDark,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        )
    }
    val minorTextStyle = remember {
        androidx.compose.ui.text.TextStyle(
            color = StudioTextMuted,
            fontSize = 7.5.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
    val defaultSecTextStyle = remember {
        androidx.compose.ui.text.TextStyle(
            color = StudioTextDark,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }

    Canvas(modifier = modifier) {
        val densityPxPerMs = pxPerMs.dp.toPx()
        val startOffsetPx = startOffsetDp.toPx()
        val totalWidthPx = startOffsetPx + (totalDurationMs * densityPxPerMs)

        // Draw horizontal baseline along bottom of ruler
        drawLine(
            color = StudioCardHairline,
            start = Offset(0f, size.height),
            end = Offset(totalWidthPx, size.height),
            strokeWidth = 1.5.dp.toPx()
        )

        val isUltraZoom = pxPerMs >= 0.22f          // 0.1s (100ms) resolution mode with 1.00s..1.90s labels
        val isMediumZoom = pxPerMs in 0.10f..0.219f // 0.5s resolution mode

        val stepMs: Long = when {
            isUltraZoom -> 100L   // Subdivide every 0.1s (100ms)
            isMediumZoom -> 500L  // Subdivide every 0.5s (500ms)
            else -> 1000L         // Subdivide every 1.0s (1000ms)
        }

        var currentMs = 0L
        while (currentMs <= totalDurationMs) {
            val xPx = startOffsetPx + (currentMs * densityPxPerMs)
            val sec = currentMs / 1000
            val centis = ((currentMs % 1000) / 10).toInt() // 00, 10, 20, 30... 90

            if (isUltraZoom) {
                val isSecond = currentMs % 1000L == 0L
                val isHalfSecond = currentMs % 500L == 0L

                val timeLabel = String.format(java.util.Locale.US, "%d.%02ds", sec, centis)

                if (isSecond) {
                    drawLine(
                        color = StudioElectricBlue,
                        start = Offset(xPx, size.height - 16.dp.toPx()),
                        end = Offset(xPx, size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                    val textResult = textMeasurer.measure(
                        text = timeLabel,
                        style = majorTextStyle
                    )
                    drawText(
                        textLayoutResult = textResult,
                        topLeft = Offset((xPx + 2.dp.toPx()).coerceAtMost(totalWidthPx - textResult.size.width), 1.dp.toPx())
                    )
                } else if (isHalfSecond) {
                    drawLine(
                        color = StudioTextDark,
                        start = Offset(xPx, size.height - 13.dp.toPx()),
                        end = Offset(xPx, size.height),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    val textResult = textMeasurer.measure(
                        text = timeLabel,
                        style = halfSecTextStyle
                    )
                    drawText(
                        textLayoutResult = textResult,
                        topLeft = Offset((xPx + 2.dp.toPx()).coerceAtMost(totalWidthPx - textResult.size.width), 2.dp.toPx())
                    )
                } else {
                    // Fractional subdivisions: 1.10s, 1.20s, 1.30s, 1.40s, 1.60s, 1.70s, 1.80s, 1.90s
                    drawLine(
                        color = StudioTextSubtle,
                        start = Offset(xPx, size.height - 8.dp.toPx()),
                        end = Offset(xPx, size.height),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Draw text label on every 0.10s when zoomed in
                    if (pxPerMs >= 0.32f || (pxPerMs >= 0.22f && centis % 20 == 0)) {
                        val textResult = textMeasurer.measure(
                            text = timeLabel,
                            style = minorTextStyle
                        )
                        drawText(
                            textLayoutResult = textResult,
                            topLeft = Offset((xPx + 2.dp.toPx()).coerceAtMost(totalWidthPx - textResult.size.width), 3.dp.toPx())
                        )
                    }
                }
            } else if (isMediumZoom) {
                val isSecond = currentMs % 1000L == 0L
                val isHalfSecond = currentMs % 500L == 0L
                val timeLabel = String.format(java.util.Locale.US, "%d.%02ds", sec, centis)

                if (isSecond) {
                    drawLine(
                        color = StudioElectricBlue,
                        start = Offset(xPx, size.height - 14.dp.toPx()),
                        end = Offset(xPx, size.height),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    val textResult = textMeasurer.measure(
                        text = timeLabel,
                        style = majorTextStyle
                    )
                    drawText(
                        textLayoutResult = textResult,
                        topLeft = Offset((xPx + 3.dp.toPx()).coerceAtMost(totalWidthPx - textResult.size.width), 2.dp.toPx())
                    )
                } else if (isHalfSecond) {
                    drawLine(
                        color = StudioTextMuted,
                        start = Offset(xPx, size.height - 9.dp.toPx()),
                        end = Offset(xPx, size.height),
                        strokeWidth = 1.2.dp.toPx()
                    )
                    val textResult = textMeasurer.measure(
                        text = timeLabel,
                        style = minorTextStyle
                    )
                    drawText(
                        textLayoutResult = textResult,
                        topLeft = Offset((xPx + 2.dp.toPx()).coerceAtMost(totalWidthPx - textResult.size.width), 3.dp.toPx())
                    )
                } else {
                    drawLine(
                        color = StudioTextSubtle,
                        start = Offset(xPx, size.height - 6.dp.toPx()),
                        end = Offset(xPx, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            } else {
                val isMajor = currentMs % 5000L == 0L
                val isSecond = currentMs % 1000L == 0L
                if (isMajor || isSecond) {
                    drawLine(
                        color = if (isMajor) StudioElectricBlue else StudioTextDark,
                        start = Offset(xPx, size.height - (if (isMajor) 14.dp.toPx() else 10.dp.toPx())),
                        end = Offset(xPx, size.height),
                        strokeWidth = if (isMajor) 2.dp.toPx() else 1.2.dp.toPx()
                    )
                    if (isMajor || pxPerMs >= 0.06f) {
                        val textResult = textMeasurer.measure(
                            text = formatTimeMs(currentMs),
                            style = if (isMajor) majorTextStyle else defaultSecTextStyle
                        )
                        drawText(
                            textLayoutResult = textResult,
                            topLeft = Offset((xPx + 4.dp.toPx()).coerceAtMost(totalWidthPx - textResult.size.width), 2.dp.toPx())
                        )
                    }
                } else {
                    drawLine(
                        color = StudioTextSubtle,
                        start = Offset(xPx, size.height - 5.dp.toPx()),
                        end = Offset(xPx, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }

            currentMs += stepMs
        }
    }
}

@Composable
fun StaticFixedPlayhead(
    offsetDp: Dp,
    pxPerMs: Float = 0.05f,
    currentTimeMs: Long = 0L,
    totalDurationMs: Long = 60000L,
    onSeek: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val playheadColor = StudioRosePink
    val haptic = LocalHapticFeedback.current
    var isScrubbing by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .offset(x = offsetDp - 14.dp)
            .width(28.dp)
            .zIndex(600f)
            .pointerInput(pxPerMs, currentTimeMs, totalDurationMs) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        isScrubbing = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val densityPxPerMs = pxPerMs.dp.toPx()
                        if (densityPxPerMs > 0.0001f) {
                            val deltaMs = (dragAmount / densityPxPerMs).toLong()
                            val newTime = (currentTimeMs + deltaMs).coerceIn(0L, totalDurationMs)
                            if (newTime != currentTimeMs) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSeek(newTime)
                            }
                        }
                    },
                    onDragEnd = {
                        isScrubbing = false
                    },
                    onDragCancel = {
                        isScrubbing = false
                    }
                )
            }
    ) {
        // 1. Interactive circular indicator dot at the top of the playhead ruler
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(if (isScrubbing) 14.dp else 10.dp),
            shape = CircleShape,
            color = if (isScrubbing) StudioAmberGold else playheadColor,
            border = BorderStroke(1.5.dp, Color.White),
            shadowElevation = if (isScrubbing) 4.dp else 2.dp
        ) {}

        // 2. Crisp straight vertical playhead line fixed at 1/3 of the timeline
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
                .width(if (isScrubbing) 2.5.dp else 2.dp)
                .fillMaxHeight()
                .background(if (isScrubbing) StudioAmberGold else playheadColor)
        )
    }
}

fun formatTimeMsPrecise(ms: Long, showTenths: Boolean = false): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val tenths = (ms % 1000) / 100
    return if (showTenths) {
        String.format("%02d:%02d.%d", minutes, seconds, tenths)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
