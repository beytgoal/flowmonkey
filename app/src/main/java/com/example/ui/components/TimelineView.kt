package com.example.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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

                // Zoom Quick Controls Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = StudioElectricBlue.copy(alpha = 0.12f),
                            modifier = Modifier.size(20.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Timeline,
                                    contentDescription = null,
                                    tint = StudioElectricBlue,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (pxPerMs >= 0.25f) "Zoom: Presisi 0.1s (1.00s - 1.90s)" else "Multitrack Timeline Canvas",
                            color = if (pxPerMs >= 0.25f) StudioElectricBlue else StudioTextDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Zoom Out Button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StudioPillBg,
                            border = BorderStroke(1.dp, StudioCardHairline),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    pxPerMs = (pxPerMs * 0.7f).coerceIn(0.02f, 0.75f)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = StudioTextDark, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("-", color = StudioTextDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Zoom In Button (Zoom into 0.10s precision)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (pxPerMs >= 0.25f) StudioElectricBlue.copy(alpha = 0.12f) else StudioPillBg,
                            border = BorderStroke(1.dp, if (pxPerMs >= 0.25f) StudioElectricBlue.copy(alpha = 0.5f) else StudioCardHairline),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    pxPerMs = (pxPerMs * 1.4f).coerceIn(0.02f, 0.75f)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = if (pxPerMs >= 0.25f) StudioElectricBlue else StudioTextDark, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("+ Zoom", color = if (pxPerMs >= 0.25f) StudioElectricBlue else StudioTextDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

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
                                val mainVideoClips = if (mainVideoTrack != null) clips.filter { it.trackId == mainVideoTrack.id } else emptyList()
                                TrackClipsRowWithIcon(
                                    icon = Icons.Default.Movie,
                                    trackId = mainVideoTrack?.id ?: 0L,
                                    trackType = "VIDEO",
                                    trackColor = StudioPrimaryViolet,
                                    clips = mainVideoClips,
                                    selectedClipId = selectedClip?.id,
                                    allTracks = tracks,
                                    pxPerMs = pxPerMs,
                                    startOffsetDp = fixedPlayheadOffsetDp,
                                    onClipSelected = onClipSelected,
                                    onClipMoved = onClipMoved,
                                    onTransitionClicked = onTransitionClicked,
                                    onSeek = onSeek,
                                    onIconClick = { onScopeChanged(TimelineScope.VIDEO_SUBTIMELINE) },
                                    onAddClick = onAddClipRequested
                                )

                                // 2. Audio / Sound Track Row
                                val audioClips = clips.filter { clip ->
                                    audioTracksAll.any { it.id == clip.trackId } || clip.audioSfx != "None" || clip.isVoiceover
                                }
                                TrackClipsRowWithIcon(
                                    icon = Icons.Default.Audiotrack,
                                    trackId = audioTracksAll.firstOrNull()?.id ?: -10L,
                                    trackType = "AUDIO",
                                    trackColor = StudioAccentPink,
                                    clips = audioClips,
                                    selectedClipId = selectedClip?.id,
                                    allTracks = tracks,
                                    pxPerMs = pxPerMs,
                                    startOffsetDp = fixedPlayheadOffsetDp,
                                    onClipSelected = onClipSelected,
                                    onClipMoved = onClipMoved,
                                    onTransitionClicked = onTransitionClicked,
                                    onSeek = onSeek,
                                    onIconClick = { onScopeChanged(TimelineScope.AUDIO_SUBTIMELINE) },
                                    onAddClick = onAddSoundRequested
                                )

                                // 3. Text / Caption Track Row
                                val textClips = clips.filter { clip ->
                                    textTracksAll.any { it.id == clip.trackId } || clip.textContent != null
                                }
                                TrackClipsRowWithIcon(
                                    icon = Icons.Default.Subtitles,
                                    trackId = textTracksAll.firstOrNull()?.id ?: -20L,
                                    trackType = "TEXT",
                                    trackColor = StudioSecondaryTeal,
                                    clips = textClips,
                                    selectedClipId = selectedClip?.id,
                                    allTracks = tracks,
                                    pxPerMs = pxPerMs,
                                    startOffsetDp = fixedPlayheadOffsetDp,
                                    onClipSelected = onClipSelected,
                                    onClipMoved = onClipMoved,
                                    onTransitionClicked = onTransitionClicked,
                                    onSeek = onSeek,
                                    onIconClick = { onScopeChanged(TimelineScope.TEXT_SUBTIMELINE) },
                                    onAddClick = onAddTextRequested
                                )

                                // 4. Sticker / Overlay Photo Track Row
                                val stickerClips = clips.filter { clip ->
                                    stickerTracksAll.any { it.id == clip.trackId } || clip.stickerIcon.isNotBlank()
                                }
                                TrackClipsRowWithIcon(
                                    icon = Icons.Default.AutoAwesome,
                                    trackId = stickerTracksAll.firstOrNull()?.id ?: -30L,
                                    trackType = "STICKER",
                                    trackColor = Color(0xFFFFB74D),
                                    clips = stickerClips,
                                    selectedClipId = selectedClip?.id,
                                    allTracks = tracks,
                                    pxPerMs = pxPerMs,
                                    startOffsetDp = fixedPlayheadOffsetDp,
                                    onClipSelected = onClipSelected,
                                    onClipMoved = onClipMoved,
                                    onTransitionClicked = onTransitionClicked,
                                    onSeek = onSeek,
                                    onIconClick = { onScopeChanged(TimelineScope.STICKER_SUBTIMELINE) },
                                    onAddClick = onAddStickerRequested
                                )
                            }

                            TimelineScope.VIDEO_SUBTIMELINE -> {
                                if (mainVideoTrack != null) {
                                    val mainClips = clips.filter { it.trackId == mainVideoTrack.id }
                                    TrackClipsRowWithIcon(
                                        icon = Icons.Default.Movie,
                                        trackId = mainVideoTrack.id,
                                        trackType = "VIDEO",
                                        trackColor = StudioPrimaryViolet,
                                        clips = mainClips,
                                        selectedClipId = selectedClip?.id,
                                        allTracks = tracks,
                                        pxPerMs = pxPerMs,
                                        startOffsetDp = fixedPlayheadOffsetDp,
                                        onClipSelected = onClipSelected,
                                        onClipMoved = onClipMoved,
                                        onTransitionClicked = onTransitionClicked,
                                        onSeek = onSeek,
                                        onIconClick = onAddClipRequested,
                                        onAddClick = onAddClipRequested
                                    )
                                }

                                overlayVideoTracks.forEach { trk ->
                                    val overlayClips = clips.filter { it.trackId == trk.id }
                                    TrackClipsRowWithIcon(
                                        icon = Icons.Default.Layers,
                                        trackId = trk.id,
                                        trackType = "VIDEO",
                                        trackColor = StudioSecondaryTeal,
                                        clips = overlayClips,
                                        selectedClipId = selectedClip?.id,
                                        allTracks = tracks,
                                        pxPerMs = pxPerMs,
                                        startOffsetDp = fixedPlayheadOffsetDp,
                                        onClipSelected = onClipSelected,
                                        onClipMoved = onClipMoved,
                                        onTransitionClicked = onTransitionClicked,
                                        onSeek = onSeek,
                                        onIconClick = onAddClipRequested,
                                        onAddClick = onAddClipRequested
                                    )
                                }
                            }

                            TimelineScope.AUDIO_SUBTIMELINE -> {
                                if (audioTracksAll.isNotEmpty()) {
                                    audioTracksAll.forEach { trk ->
                                        val audioClips = clips.filter { it.trackId == trk.id }
                                        TrackClipsRowWithIcon(
                                            icon = Icons.Default.Audiotrack,
                                            trackId = trk.id,
                                            trackType = "AUDIO",
                                            trackColor = StudioAccentPink,
                                            clips = audioClips,
                                            selectedClipId = selectedClip?.id,
                                            allTracks = tracks,
                                            pxPerMs = pxPerMs,
                                            startOffsetDp = fixedPlayheadOffsetDp,
                                            onClipSelected = onClipSelected,
                                            onClipMoved = onClipMoved,
                                            onTransitionClicked = onTransitionClicked,
                                            onSeek = onSeek,
                                            onIconClick = onAddSoundRequested,
                                            onAddClick = onAddSoundRequested
                                        )
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
                                        val textClips = clips.filter { it.trackId == trk.id || (trk == textTracksAll.first() && it.textContent != null) }
                                        TrackClipsRowWithIcon(
                                            icon = Icons.Default.Subtitles,
                                            trackId = trk.id,
                                            trackType = "TEXT",
                                            trackColor = StudioSecondaryTeal,
                                            clips = textClips,
                                            selectedClipId = selectedClip?.id,
                                            allTracks = tracks,
                                            pxPerMs = pxPerMs,
                                            startOffsetDp = fixedPlayheadOffsetDp,
                                            onClipSelected = onClipSelected,
                                            onClipMoved = onClipMoved,
                                            onTransitionClicked = onTransitionClicked,
                                            onSeek = onSeek,
                                            onIconClick = onAddTextRequested,
                                            onAddClick = onAddTextRequested
                                        )
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
                                        val stickerClips = clips.filter { it.trackId == trk.id || it.stickerIcon.isNotBlank() }
                                        TrackClipsRowWithIcon(
                                            icon = Icons.Default.AutoAwesome,
                                            trackId = trk.id,
                                            trackType = "STICKER",
                                            trackColor = Color(0xFFFFB74D),
                                            clips = stickerClips,
                                            selectedClipId = selectedClip?.id,
                                            allTracks = tracks,
                                            pxPerMs = pxPerMs,
                                            startOffsetDp = fixedPlayheadOffsetDp,
                                            onClipSelected = onClipSelected,
                                            onClipMoved = onClipMoved,
                                            onTransitionClicked = onTransitionClicked,
                                            onSeek = onSeek,
                                            onIconClick = onAddStickerRequested,
                                            onAddClick = onAddStickerRequested
                                        )
                                    }
                                } else {
                                    val stickerClips = clips.filter { it.stickerIcon.isNotBlank() }
                                    TrackClipsRowWithIcon(
                                        icon = Icons.Default.AutoAwesome,
                                        trackId = -999L,
                                        trackType = "STICKER",
                                        trackColor = Color(0xFFFFB74D),
                                        clips = stickerClips,
                                        selectedClipId = selectedClip?.id,
                                        allTracks = tracks,
                                        pxPerMs = pxPerMs,
                                        startOffsetDp = fixedPlayheadOffsetDp,
                                        onClipSelected = onClipSelected,
                                        onClipMoved = onClipMoved,
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

        // Fixed stationary playhead positioned at exactly 1/3 of the screen (completely motionless, timeline glides beneath)
        StaticFixedPlayhead(
            offsetDp = fixedPlayheadOffsetDp,
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
    allTracks: List<TimelineTrackEntity>,
    pxPerMs: Float,
    startOffsetDp: Dp,
    onClipSelected: (TimelineClipEntity) -> Unit,
    onClipMoved: (clipId: Long, newStartTimeMs: Long, newTrackId: Long) -> Unit,
    onTransitionClicked: (clipA: TimelineClipEntity, clipB: TimelineClipEntity) -> Unit,
    onSeek: (Long) -> Unit,
    onIconClick: () -> Unit,
    onAddClick: () -> Unit = onIconClick
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val sortedClips = remember(clips) { clips.sortedBy { it.startTimeMs } }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        // Track Icon Badge (Icon only, inside the scroll canvas so it scrolls naturally)
        Surface(
            modifier = Modifier
                .offset(x = 2.dp, y = 2.dp)
                .size(34.dp, 44.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable { onIconClick() },
            color = trackColor.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, trackColor.copy(alpha = 0.3f))
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
                .height(48.dp)
                .background(StudioPillBg.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                .border(0.5.dp, StudioCardHairline, RoundedCornerShape(10.dp))
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
                        fontWeight = FontWeight.SemiBold,
                        color = trackColor
                    )
                }
            }

            sortedClips.forEachIndexed { index, clip ->
                val startDp = (clip.startTimeMs * pxPerMs).dp
                val widthDp = (clip.durationMs * pxPerMs).dp.coerceAtLeast(36.dp)
                val isSelected = clip.id == selectedClipId

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
                        .zIndex(if (isDragging) 10f else if (isSelected) 5f else 1f)
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
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .testTag("clip_item_${clip.id}"),
                        color = if (isDragging) {
                            StudioElectricBlue
                        } else if (isSelected) {
                            trackColor
                        } else {
                            trackColor.copy(alpha = 0.85f)
                        },
                        border = BorderStroke(
                            if (isDragging || isSelected) 2.dp else 1.dp,
                            if (isDragging) StudioAmberGold else if (isSelected) StudioDarkCharcoal else Color.White.copy(alpha = 0.4f)
                        ),
                        shadowElevation = if (isDragging || isSelected) 4.dp else 0.dp
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 6.dp)
                                    .fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (trackType == "TEXT") clip.textContent ?: clip.title else clip.title,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f, fill = false)
                                )

                                if (clip.speedMultiplier != 1.0f) {
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "${clip.speedMultiplier}x",
                                            color = Color.White,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }

                            // Keyframe diamond markers (◆)
                            if (keyframes.isNotEmpty()) {
                                keyframes.forEach { kf ->
                                    val kfOffsetDp = (kf.timeOffsetMs * pxPerMs).dp
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .offset(x = kfOffsetDp - 4.dp, y = (-2).dp)
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(StudioAmberGold)
                                            .border(0.5.dp, Color.White, CircleShape)
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onSeek(clip.startTimeMs + kf.timeOffsetMs)
                                            }
                                    )
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
                            .offset(x = junctionOffsetDp, y = 13.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .zIndex(6f)
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
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
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
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
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

    Canvas(modifier = modifier) {
        val densityPxPerMs = pxPerMs.dp.toPx()
        val startOffsetPx = startOffsetDp.toPx()
        val totalWidthPx = startOffsetPx + (totalDurationMs * densityPxPerMs)

        // Draw horizontal baseline along bottom of ruler
        drawLine(
            color = StudioCardHairline,
            start = Offset(0f, size.height),
            end = Offset(totalWidthPx, size.height),
            strokeWidth = 1.dp.toPx()
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
                        style = androidx.compose.ui.text.TextStyle(
                            color = StudioElectricBlue,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    drawText(
                        textLayoutResult = textResult,
                        topLeft = Offset((xPx + 2.dp.toPx()).coerceAtMost(totalWidthPx - textResult.size.width), 1.dp.toPx())
                    )
                } else if (isHalfSecond) {
                    drawLine(
                        color = StudioTextDark.copy(alpha = 0.6f),
                        start = Offset(xPx, size.height - 13.dp.toPx()),
                        end = Offset(xPx, size.height),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    val textResult = textMeasurer.measure(
                        text = timeLabel,
                        style = androidx.compose.ui.text.TextStyle(
                            color = StudioTextDark.copy(alpha = 0.7f),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    drawText(
                        textLayoutResult = textResult,
                        topLeft = Offset((xPx + 2.dp.toPx()).coerceAtMost(totalWidthPx - textResult.size.width), 2.dp.toPx())
                    )
                } else {
                    // Fractional subdivisions: 1.10s, 1.20s, 1.30s, 1.40s, 1.60s, 1.70s, 1.80s, 1.90s
                    drawLine(
                        color = StudioTextMuted.copy(alpha = 0.35f),
                        start = Offset(xPx, size.height - 8.dp.toPx()),
                        end = Offset(xPx, size.height),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Draw text label on every 0.10s when zoomed in
                    if (pxPerMs >= 0.32f || (pxPerMs >= 0.22f && centis % 20 == 0)) {
                        val textResult = textMeasurer.measure(
                            text = timeLabel,
                            style = androidx.compose.ui.text.TextStyle(
                                color = StudioTextMuted,
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Medium
                            )
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
                        style = androidx.compose.ui.text.TextStyle(
                            color = StudioElectricBlue,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    drawText(
                        textLayoutResult = textResult,
                        topLeft = Offset((xPx + 3.dp.toPx()).coerceAtMost(totalWidthPx - textResult.size.width), 2.dp.toPx())
                    )
                } else if (isHalfSecond) {
                    drawLine(
                        color = StudioTextMuted.copy(alpha = 0.5f),
                        start = Offset(xPx, size.height - 9.dp.toPx()),
                        end = Offset(xPx, size.height),
                        strokeWidth = 1.2.dp.toPx()
                    )
                    val textResult = textMeasurer.measure(
                        text = timeLabel,
                        style = androidx.compose.ui.text.TextStyle(
                            color = StudioTextMuted,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Normal
                        )
                    )
                    drawText(
                        textLayoutResult = textResult,
                        topLeft = Offset((xPx + 2.dp.toPx()).coerceAtMost(totalWidthPx - textResult.size.width), 3.dp.toPx())
                    )
                } else {
                    drawLine(
                        color = StudioTextMuted.copy(alpha = 0.25f),
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
                        color = if (isMajor) StudioElectricBlue else StudioTextDark.copy(alpha = 0.5f),
                        start = Offset(xPx, size.height - (if (isMajor) 14.dp.toPx() else 10.dp.toPx())),
                        end = Offset(xPx, size.height),
                        strokeWidth = if (isMajor) 1.5.dp.toPx() else 1.dp.toPx()
                    )
                    if (isMajor || pxPerMs >= 0.06f) {
                        val textResult = textMeasurer.measure(
                            text = formatTimeMs(currentMs),
                            style = androidx.compose.ui.text.TextStyle(
                                color = if (isMajor) StudioElectricBlue else StudioTextDark,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        drawText(
                            textLayoutResult = textResult,
                            topLeft = Offset((xPx + 4.dp.toPx()).coerceAtMost(totalWidthPx - textResult.size.width), 2.dp.toPx())
                        )
                    }
                } else {
                    drawLine(
                        color = StudioTextMuted.copy(alpha = 0.2f),
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
    modifier: Modifier = Modifier
) {
    val playheadColor = StudioRosePink

    Box(
        modifier = modifier
            .offset(x = offsetDp - 5.dp)
            .width(10.dp)
            .zIndex(600f)
    ) {
        // 1. Small circular indicator dot at the top of the playhead ruler
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(10.dp),
            shape = CircleShape,
            color = playheadColor,
            border = BorderStroke(1.5.dp, Color.White),
            shadowElevation = 2.dp
        ) {}

        // 2. Aura glow behind the vertical line
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
                .width(4.dp)
                .fillMaxHeight()
                .background(playheadColor.copy(alpha = 0.25f))
        )

        // 3. Crisp straight vertical playhead line fixed at 1/3 of the timeline
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
                .width(1.5.dp)
                .fillMaxHeight()
                .background(playheadColor)
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
