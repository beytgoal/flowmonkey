package com.example.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.Color
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
    MAIN("Timeline Utama", "Komposisi Penuh Multitrack", StudioPrimaryViolet),
    VIDEO_SUBTIMELINE("Sub-Timeline Video & Overlay", "Video Utama & Layer PIP / Foto", StudioPrimaryViolet),
    AUDIO_SUBTIMELINE("Sub-Timeline Musik & Audio", "Musik BGM, Voiceover AI, & SFX", StudioAccentPink),
    TEXT_SUBTIMELINE("Sub-Timeline Tipografi & Caption", "Teks Subjudul, Judul, & Auto-Captions", StudioSecondaryTeal),
    STICKER_SUBTIMELINE("Sub-Timeline Stiker & Overlay Foto", "Stiker Grafis, Emoji, & Badge Foto", Color(0xFFFFB74D))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineView(
    tracks: List<TimelineTrackEntity>,
    clips: List<TimelineClipEntity>,
    currentTimeMs: Long,
    totalDurationMs: Long = 15000L,
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

    // Dynamic horizontal timeline canvas scale (0.02f = Overview / Zoom Out, 0.08f = Normal 1s, 0.45f = 0.1s Ultra-Precision)
    var pxPerMs by remember { mutableFloatStateOf(0.08f) }
    val timelineWidthDp = (totalDurationMs * pxPerMs).dp.coerceAtLeast(360.dp)

    val mainVideoTrack = tracks.find { it.trackType == "VIDEO" && it.trackIndex == 0 } ?: tracks.find { it.trackType == "VIDEO" }
    val overlayVideoTracks = tracks.filter { it.trackType == "VIDEO" && it.id != mainVideoTrack?.id }
    val textTracksAll = tracks.filter { it.trackType == "TEXT" }
    val stickerTracksAll = tracks.filter { it.trackType == "STICKER" }
    val audioTracksAll = tracks.filter { it.trackType == "AUDIO" }

    // Auto-scroll timeline smoothly to follow playhead when playing or seeking
    LaunchedEffect(currentTimeMs) {
        val playheadPx = with(density) { (currentTimeMs * pxPerMs).dp.toPx() }
        val visibleStartPx = scrollState.value.toFloat()
        val visibleEndPx = visibleStartPx + scrollState.viewportSize.toFloat()

        if (visibleEndPx > 0f) {
            if (playheadPx > visibleEndPx - 80f) {
                scrollState.animateScrollTo((playheadPx - 80f).toInt().coerceAtLeast(0))
            } else if (playheadPx < visibleStartPx) {
                scrollState.animateScrollTo((playheadPx - 20f).toInt().coerceAtLeast(0))
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, timelineScope.themeColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
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
                                    val newPx = (pxPerMs * factor).coerceIn(0.02f, 0.45f)
                                    if (newPx != pxPerMs) {
                                        pxPerMs = newPx
                                    }
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .testTag("timeline_editor_container"),
        colors = CardDefaults.cardColors(containerColor = StudioCardBg)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {

            // --- TIMELINE HEADER (NO BUTTONS, MULTI-TOUCH PINCH TO ZOOM) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Scope Badge & Return Affordance
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = timelineScope.themeColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, timelineScope.themeColor.copy(alpha = 0.5f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = timelineScope.title,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (timelineScope != TimelineScope.MAIN) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onScopeChanged(TimelineScope.MAIN) },
                            color = StudioSurfaceDark,
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.3f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Kembali ke Timeline Utama",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Utama",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Dynamic Scale Indicator (Pinch Gesture Feedback)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    when {
                        pxPerMs >= 0.25f -> {
                            Surface(
                                color = StudioSecondaryTeal.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.8.dp, StudioSecondaryTeal)
                            ) {
                                Text(
                                    text = "🎯 Skala 0.1s Presisi",
                                    color = StudioSecondaryTeal,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        pxPerMs <= 0.04f -> {
                            Surface(
                                color = StudioPrimaryViolet.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.8.dp, StudioPrimaryViolet)
                            ) {
                                Text(
                                    text = "🌐 Tampilan Penuh",
                                    color = StudioPrimaryViolet,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        else -> {
                            Surface(
                                color = StudioSurfaceDark,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
                            ) {
                                Text(
                                    text = "🤏 Cubit 2 Jari Zoom",
                                    color = StudioTextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Multitrack Container with Anchored Track Headers on Left & Synchronized Scrollable Canvas on Right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Fixed Left Track Controls Column (Anchored Width: 105.dp)
                Column(
                    modifier = Modifier.width(105.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Top Time Ruler Corner Anchor
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .background(StudioSurfaceDark, RoundedCornerShape(topStart = 8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = formatTimeMsPrecise(currentTimeMs, showTenths = pxPerMs >= 0.18f),
                            fontSize = 11.sp,
                            color = StudioSecondaryTeal,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Render Left Track Headers according to timelineScope
                    when (timelineScope) {
                        TimelineScope.MAIN -> {
                            // 1. Video Utama Track Header
                            TrackHeaderCard(
                                title = "1. Video Utama",
                                subtitle = if (overlayVideoTracks.isNotEmpty()) "+${overlayVideoTracks.size} Overlay" else "Main Track",
                                trackType = "VIDEO",
                                color = StudioPrimaryViolet,
                                icon = Icons.Default.Movie,
                                isSelected = selectedClip?.let { it.trackId == mainVideoTrack?.id } == true,
                                onClick = { onScopeChanged(TimelineScope.VIDEO_SUBTIMELINE) }
                            )

                            // 2. Sound / Musik Track Header
                            TrackHeaderCard(
                                title = "2. Sound / Musik",
                                subtitle = if (audioTracksAll.size > 1) "${audioTracksAll.size} Trek Audio" else "BGM & Voice",
                                trackType = "AUDIO",
                                color = StudioAccentPink,
                                icon = Icons.Default.Audiotrack,
                                isSelected = selectedClip?.let { clip -> audioTracksAll.any { it.id == clip.trackId } } == true,
                                onClick = { onScopeChanged(TimelineScope.AUDIO_SUBTIMELINE) }
                            )

                            // 3. Tipografi & Caption Track Header
                            TrackHeaderCard(
                                title = "3. Teks & Caption",
                                subtitle = if (textTracksAll.size > 1) "${textTracksAll.size} Trek Teks" else "Subjudul",
                                trackType = "TEXT",
                                color = StudioSecondaryTeal,
                                icon = Icons.Default.Subtitles,
                                isSelected = selectedClip?.let { clip -> textTracksAll.any { it.id == clip.trackId } || clip.textContent != null } == true,
                                onClick = { onScopeChanged(TimelineScope.TEXT_SUBTIMELINE) }
                            )

                            // 4. Stiker & Overlay Foto Track Header
                            TrackHeaderCard(
                                title = "4. Stiker & Foto",
                                subtitle = "Grafis FX",
                                trackType = "STICKER",
                                color = Color(0xFFFFB74D),
                                icon = Icons.Default.AutoAwesome,
                                isSelected = selectedClip?.let { clip -> stickerTracksAll.any { it.id == clip.trackId } || clip.stickerIcon.isNotBlank() } == true,
                                onClick = { onScopeChanged(TimelineScope.STICKER_SUBTIMELINE) }
                            )
                        }

                        TimelineScope.VIDEO_SUBTIMELINE -> {
                            // Main Video Track Header
                            if (mainVideoTrack != null) {
                                TrackHeaderCard(
                                    title = "Video Utama",
                                    subtitle = "Track #1",
                                    trackType = "VIDEO",
                                    color = StudioPrimaryViolet,
                                    icon = Icons.Default.Movie,
                                    isSelected = selectedClip?.trackId == mainVideoTrack.id,
                                    onClick = {}
                                )
                            }
                            // Overlay Video & Photo Tracks Headers
                            overlayVideoTracks.forEachIndexed { idx, trk ->
                                TrackHeaderCard(
                                    title = "Overlay #${idx + 1}",
                                    subtitle = "PIP Video / Foto",
                                    trackType = "VIDEO",
                                    color = StudioSecondaryTeal,
                                    icon = Icons.Default.Layers,
                                    isSelected = selectedClip?.trackId == trk.id,
                                    onClick = {}
                                )
                            }
                        }

                        TimelineScope.AUDIO_SUBTIMELINE -> {
                            if (audioTracksAll.isNotEmpty()) {
                                audioTracksAll.forEachIndexed { idx, trk ->
                                    TrackHeaderCard(
                                        title = if (idx == 0) "Musik BGM" else if (idx == 1) "Voiceover AI" else "SFX #${idx + 1}",
                                        subtitle = "Audio Track",
                                        trackType = "AUDIO",
                                        color = StudioAccentPink,
                                        icon = Icons.Default.Audiotrack,
                                        isSelected = selectedClip?.trackId == trk.id,
                                        onClick = {}
                                    )
                                }
                            } else {
                                TrackHeaderCard(
                                    title = "Musik Utama",
                                    subtitle = "Belum ada audio",
                                    trackType = "AUDIO",
                                    color = StudioAccentPink,
                                    icon = Icons.Default.Audiotrack,
                                    isSelected = false,
                                    onClick = onAddSoundRequested
                                )
                            }
                        }

                        TimelineScope.TEXT_SUBTIMELINE -> {
                            if (textTracksAll.isNotEmpty()) {
                                textTracksAll.forEachIndexed { idx, trk ->
                                    TrackHeaderCard(
                                        title = if (idx == 0) "Subjudul Utama" else if (idx == 1) "Judul Heading" else "Auto-Caption #${idx + 1}",
                                        subtitle = "Tipografi",
                                        trackType = "TEXT",
                                        color = StudioSecondaryTeal,
                                        icon = Icons.Default.Subtitles,
                                        isSelected = selectedClip?.trackId == trk.id,
                                        onClick = {}
                                    )
                                }
                            } else {
                                TrackHeaderCard(
                                    title = "Subjudul Teks",
                                    subtitle = "Belum ada teks",
                                    trackType = "TEXT",
                                    color = StudioSecondaryTeal,
                                    icon = Icons.Default.Subtitles,
                                    isSelected = false,
                                    onClick = onAddTextRequested
                                )
                            }
                        }

                        TimelineScope.STICKER_SUBTIMELINE -> {
                            if (stickerTracksAll.isNotEmpty()) {
                                stickerTracksAll.forEachIndexed { idx, trk ->
                                    TrackHeaderCard(
                                        title = "Stiker #${idx + 1}",
                                        subtitle = "Badge FX",
                                        trackType = "STICKER",
                                        color = Color(0xFFFFB74D),
                                        icon = Icons.Default.AutoAwesome,
                                        isSelected = selectedClip?.trackId == trk.id,
                                        onClick = {}
                                    )
                                }
                            } else {
                                TrackHeaderCard(
                                    title = "Stiker & Foto",
                                    subtitle = "Belum ada stiker",
                                    trackType = "STICKER",
                                    color = Color(0xFFFFB74D),
                                    icon = Icons.Default.AutoAwesome,
                                    isSelected = false,
                                    onClick = onAddStickerRequested
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Right Scrollable Multitrack Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(scrollState)
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
                                                val newPx = (pxPerMs * factor).coerceIn(0.02f, 0.45f)
                                                if (newPx != pxPerMs) {
                                                    pxPerMs = newPx
                                                }
                                            }
                                        }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                        .pointerInput(totalDurationMs, pxPerMs) {
                            val densityPxPerMs = pxPerMs.dp.toPx()
                            detectTapGestures { offset ->
                                val targetMs = (offset.x / densityPxPerMs).toLong().coerceIn(0L, totalDurationMs)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSeek(targetMs)
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier.width(timelineWidthDp)
                    ) {
                        // High-Precision Adaptive Time Ruler
                        TimeRulerCanvas(
                            totalDurationMs = totalDurationMs,
                            pxPerMs = pxPerMs,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                                .background(Color.Black.copy(alpha = 0.5f))
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Multitrack Rows according to timelineScope
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            when (timelineScope) {
                                TimelineScope.MAIN -> {
                                    // Row 1: Main Video Track Clips
                                    val mainVideoClips = if (mainVideoTrack != null) clips.filter { it.trackId == mainVideoTrack.id } else emptyList()
                                    TrackClipsRow(
                                        trackId = mainVideoTrack?.id ?: 0L,
                                        trackType = "VIDEO",
                                        trackColor = StudioPrimaryViolet,
                                        clips = mainVideoClips,
                                        selectedClipId = selectedClip?.id,
                                        currentTimeMs = currentTimeMs,
                                        allTracks = tracks,
                                        pxPerMs = pxPerMs,
                                        onClipSelected = { clip ->
                                            onClipSelected(clip)
                                            onScopeChanged(TimelineScope.VIDEO_SUBTIMELINE)
                                        },
                                        onClipMoved = onClipMoved,
                                        onTransitionClicked = onTransitionClicked,
                                        onSeek = onSeek
                                    )

                                    // Row 2: Sound & Audio Track Clips
                                    val audioClips = clips.filter { clip ->
                                        audioTracksAll.any { it.id == clip.trackId } || clip.audioSfx != "None" || clip.isVoiceover
                                    }
                                    TrackClipsRow(
                                        trackId = audioTracksAll.firstOrNull()?.id ?: -10L,
                                        trackType = "AUDIO",
                                        trackColor = StudioAccentPink,
                                        clips = audioClips,
                                        selectedClipId = selectedClip?.id,
                                        currentTimeMs = currentTimeMs,
                                        allTracks = tracks,
                                        pxPerMs = pxPerMs,
                                        onClipSelected = { clip ->
                                            onClipSelected(clip)
                                            onScopeChanged(TimelineScope.AUDIO_SUBTIMELINE)
                                        },
                                        onClipMoved = onClipMoved,
                                        onTransitionClicked = onTransitionClicked,
                                        onSeek = onSeek
                                    )

                                    // Row 3: Typography & Text Track Clips
                                    val textClips = clips.filter { clip ->
                                        textTracksAll.any { it.id == clip.trackId } || clip.textContent != null
                                    }
                                    TrackClipsRow(
                                        trackId = textTracksAll.firstOrNull()?.id ?: -20L,
                                        trackType = "TEXT",
                                        trackColor = StudioSecondaryTeal,
                                        clips = textClips,
                                        selectedClipId = selectedClip?.id,
                                        currentTimeMs = currentTimeMs,
                                        allTracks = tracks,
                                        pxPerMs = pxPerMs,
                                        onClipSelected = { clip ->
                                            onClipSelected(clip)
                                            onScopeChanged(TimelineScope.TEXT_SUBTIMELINE)
                                        },
                                        onClipMoved = onClipMoved,
                                        onTransitionClicked = onTransitionClicked,
                                        onSeek = onSeek
                                    )

                                    // Row 4: Sticker & Photo Overlay Clips
                                    val stickerClips = clips.filter { clip ->
                                        stickerTracksAll.any { it.id == clip.trackId } || clip.stickerIcon.isNotBlank()
                                    }
                                    TrackClipsRow(
                                        trackId = stickerTracksAll.firstOrNull()?.id ?: -30L,
                                        trackType = "STICKER",
                                        trackColor = Color(0xFFFFB74D),
                                        clips = stickerClips,
                                        selectedClipId = selectedClip?.id,
                                        currentTimeMs = currentTimeMs,
                                        allTracks = tracks,
                                        pxPerMs = pxPerMs,
                                        onClipSelected = { clip ->
                                            onClipSelected(clip)
                                            onScopeChanged(TimelineScope.STICKER_SUBTIMELINE)
                                        },
                                        onClipMoved = onClipMoved,
                                        onTransitionClicked = onTransitionClicked,
                                        onSeek = onSeek
                                    )
                                }

                                TimelineScope.VIDEO_SUBTIMELINE -> {
                                    // Main Video Track Row
                                    if (mainVideoTrack != null) {
                                        val mainVideoClips = clips.filter { it.trackId == mainVideoTrack.id }
                                        TrackClipsRow(
                                            trackId = mainVideoTrack.id,
                                            trackType = "VIDEO",
                                            trackColor = StudioPrimaryViolet,
                                            clips = mainVideoClips,
                                            selectedClipId = selectedClip?.id,
                                            currentTimeMs = currentTimeMs,
                                            allTracks = tracks,
                                            pxPerMs = pxPerMs,
                                            onClipSelected = onClipSelected,
                                            onClipMoved = onClipMoved,
                                            onTransitionClicked = onTransitionClicked,
                                            onSeek = onSeek
                                        )
                                    }

                                    // Overlay Video & Photo Rows
                                    overlayVideoTracks.forEach { trk ->
                                        val overlayClips = clips.filter { it.trackId == trk.id }
                                        TrackClipsRow(
                                            trackId = trk.id,
                                            trackType = "VIDEO",
                                            trackColor = StudioSecondaryTeal,
                                            clips = overlayClips,
                                            selectedClipId = selectedClip?.id,
                                            currentTimeMs = currentTimeMs,
                                            allTracks = tracks,
                                            pxPerMs = pxPerMs,
                                            onClipSelected = onClipSelected,
                                            onClipMoved = onClipMoved,
                                            onTransitionClicked = onTransitionClicked,
                                            onSeek = onSeek
                                        )
                                    }
                                }

                                TimelineScope.AUDIO_SUBTIMELINE -> {
                                    if (audioTracksAll.isNotEmpty()) {
                                        audioTracksAll.forEach { trk ->
                                            val audioClips = clips.filter { it.trackId == trk.id }
                                            TrackClipsRow(
                                                trackId = trk.id,
                                                trackType = "AUDIO",
                                                trackColor = StudioAccentPink,
                                                clips = audioClips,
                                                selectedClipId = selectedClip?.id,
                                                currentTimeMs = currentTimeMs,
                                                allTracks = tracks,
                                                pxPerMs = pxPerMs,
                                                onClipSelected = onClipSelected,
                                                onClipMoved = onClipMoved,
                                                onTransitionClicked = onTransitionClicked,
                                                onSeek = onSeek
                                            )
                                        }
                                    } else {
                                        EmptyTrackPlaceholder(
                                            title = "+ Tambah Musik atau Voiceover AI",
                                            color = StudioAccentPink,
                                            onClick = onAddSoundRequested
                                        )
                                    }
                                }

                                TimelineScope.TEXT_SUBTIMELINE -> {
                                    if (textTracksAll.isNotEmpty()) {
                                        textTracksAll.forEach { trk ->
                                            val textClips = clips.filter { it.trackId == trk.id || (trk == textTracksAll.first() && it.textContent != null) }
                                            TrackClipsRow(
                                                trackId = trk.id,
                                                trackType = "TEXT",
                                                trackColor = StudioSecondaryTeal,
                                                clips = textClips,
                                                selectedClipId = selectedClip?.id,
                                                currentTimeMs = currentTimeMs,
                                                allTracks = tracks,
                                                pxPerMs = pxPerMs,
                                                onClipSelected = onClipSelected,
                                                onClipMoved = onClipMoved,
                                                onTransitionClicked = onTransitionClicked,
                                                onSeek = onSeek
                                            )
                                        }
                                    } else {
                                        EmptyTrackPlaceholder(
                                            title = "+ Tambah Judul atau Subjudul Caption",
                                            color = StudioSecondaryTeal,
                                            onClick = onAddTextRequested
                                        )
                                    }
                                }

                                TimelineScope.STICKER_SUBTIMELINE -> {
                                    if (stickerTracksAll.isNotEmpty()) {
                                        stickerTracksAll.forEach { trk ->
                                            val stickerClips = clips.filter { it.trackId == trk.id || it.stickerIcon.isNotBlank() }
                                            TrackClipsRow(
                                                trackId = trk.id,
                                                trackType = "STICKER",
                                                trackColor = Color(0xFFFFB74D),
                                                clips = stickerClips,
                                                selectedClipId = selectedClip?.id,
                                                currentTimeMs = currentTimeMs,
                                                allTracks = tracks,
                                                pxPerMs = pxPerMs,
                                                onClipSelected = onClipSelected,
                                                onClipMoved = onClipMoved,
                                                onTransitionClicked = onTransitionClicked,
                                                onSeek = onSeek
                                            )
                                        }
                                    } else {
                                        val stickerClips = clips.filter { it.stickerIcon.isNotBlank() }
                                        TrackClipsRow(
                                            trackId = -999L,
                                            trackType = "STICKER",
                                            trackColor = Color(0xFFFFB74D),
                                            clips = stickerClips,
                                            selectedClipId = selectedClip?.id,
                                            currentTimeMs = currentTimeMs,
                                            allTracks = tracks,
                                            pxPerMs = pxPerMs,
                                            onClipSelected = onClipSelected,
                                            onClipMoved = onClipMoved,
                                            onTransitionClicked = onTransitionClicked,
                                            onSeek = onSeek
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Precision Playhead Red Line Overlay with tactile dragging
                    val playheadOffsetDp = (currentTimeMs * pxPerMs).dp
                    PlayheadOverlay(
                        offsetDp = playheadOffsetDp,
                        currentTimeMs = currentTimeMs,
                        pxPerMs = pxPerMs,
                        totalDurationMs = totalDurationMs,
                        onSeek = { newMs ->
                            val tick = newMs / 200L
                            if (tick != lastHapticTick) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                lastHapticTick = tick
                            }
                            onSeek(newMs)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TrackHeaderCard(
    title: String,
    subtitle: String,
    trackType: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .width(105.dp)
            .height(48.dp)
            .clickable { onClick() },
        color = if (isSelected) color.copy(alpha = 0.35f) else color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) color else color.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    color = color.copy(alpha = 0.9f),
                    fontSize = 8.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun TrackClipsRow(
    trackId: Long,
    trackType: String,
    trackColor: Color,
    clips: List<TimelineClipEntity>,
    selectedClipId: Long?,
    currentTimeMs: Long,
    allTracks: List<TimelineTrackEntity>,
    pxPerMs: Float,
    onClipSelected: (TimelineClipEntity) -> Unit,
    onClipMoved: (clipId: Long, newStartTimeMs: Long, newTrackId: Long) -> Unit,
    onTransitionClicked: (clipA: TimelineClipEntity, clipB: TimelineClipEntity) -> Unit,
    onSeek: (Long) -> Unit
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val sortedClips = remember(clips) { clips.sortedBy { it.startTimeMs } }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(StudioSurfaceLight.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
    ) {
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
                        detectDragGestures(
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
                        .clip(RoundedCornerShape(6.dp))
                        .testTag("clip_item_${clip.id}"),
                    color = if (isDragging) {
                        StudioSecondaryTeal.copy(alpha = 0.95f)
                    } else if (isSelected) {
                        trackColor.copy(alpha = 1f)
                    } else {
                        trackColor.copy(alpha = 0.75f)
                    },
                    border = BorderStroke(
                        if (isDragging || isSelected) 2.dp else 1.dp,
                        if (isDragging) Color.Yellow else if (isSelected) Color.White else Color.White.copy(alpha = 0.35f)
                    ),
                    shadowElevation = if (isDragging || isSelected) 6.dp else 0.dp
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
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                if (clip.speedMultiplier != 1.0f) {
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "${clip.speedMultiplier}x",
                                            color = StudioSecondaryTeal,
                                            fontSize = 8.sp,
                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // --- KEYFRAME DIAMOND MARKERS (◆) ON TIMELINE CLIP ---
                        if (keyframes.isNotEmpty()) {
                            keyframes.forEach { kf ->
                                val kfOffsetDp = (kf.timeOffsetMs * pxPerMs).dp
                                val isKfActive = Math.abs(currentTimeMs - (clip.startTimeMs + kf.timeOffsetMs)) < 50L
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .offset(x = kfOffsetDp - 4.dp, y = (-2).dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isKfActive) Color.White else StudioAccentAmber)
                                        .border(0.5.dp, Color.Black, CircleShape)
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

            // --- TRANSITION JUNCTION BUTTON BETWEEN TWO ADJACENT CLIPS ---
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
                    color = if (clip.transitionType.isNotBlank() && clip.transitionType != "None") StudioSecondaryTeal else StudioCardBg,
                    border = BorderStroke(1.dp, if (clip.transitionType.isNotBlank() && clip.transitionType != "None") StudioSecondaryTeal else Color.White.copy(alpha = 0.6f)),
                    shadowElevation = 3.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CompareArrows,
                            contentDescription = "Transisi ${clip.transitionType}",
                            tint = if (clip.transitionType.isNotBlank() && clip.transitionType != "None") Color.Black else Color.White,
                            modifier = Modifier.size(12.dp)
                        )
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
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
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
    totalDurationMs: Long,
    pxPerMs: Float,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        val densityPxPerMs = pxPerMs.dp.toPx()
        val totalWidthPx = totalDurationMs * densityPxPerMs

        // Draw horizontal baseline along bottom of ruler
        drawLine(
            color = Color.White.copy(alpha = 0.25f),
            start = Offset(0f, size.height),
            end = Offset(totalWidthPx, size.height),
            strokeWidth = 1.dp.toPx()
        )

        val isUltraZoom = pxPerMs >= 0.25f          // 0.1s (100ms) resolution mode
        val isMediumZoom = pxPerMs in 0.10f..0.249f // 0.5s resolution mode

        val stepMs: Long = when {
            isUltraZoom -> 100L   // Subdivide every 0.1s (100ms)
            isMediumZoom -> 500L  // Subdivide every 0.5s (500ms)
            else -> 1000L         // Subdivide every 1.0s (1000ms)
        }

        var currentMs = 0L
        while (currentMs <= totalDurationMs) {
            val xPx = currentMs * densityPxPerMs

            if (isUltraZoom) {
                // --- ULTRA ZOOM MODE (0.1s per tick) ---
                val isSecond = currentMs % 1000L == 0L
                val isHalfSecond = currentMs % 500L == 0L

                if (isSecond) {
                    // Full Second Major Tick (1.0s, 2.0s, etc.)
                    drawLine(
                        color = StudioSecondaryTeal,
                        start = Offset(xPx, size.height - 18.dp.toPx()),
                        end = Offset(xPx, size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                    val textResult = textMeasurer.measure(
                        text = formatTimeMsPrecise(currentMs, showTenths = true),
                        style = androidx.compose.ui.text.TextStyle(
                            color = StudioSecondaryTeal,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    drawText(
                        textLayoutResult = textResult,
                        topLeft = Offset((xPx + 3.dp.toPx()).coerceAtMost(totalWidthPx - textResult.size.width), 1.dp.toPx())
                    )
                } else if (isHalfSecond) {
                    // Half-Second Medium Tick (0.5s)
                    drawLine(
                        color = Color.White.copy(alpha = 0.85f),
                        start = Offset(xPx, size.height - 12.dp.toPx()),
                        end = Offset(xPx, size.height),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    val textResult = textMeasurer.measure(
                        text = ".5s",
                        style = androidx.compose.ui.text.TextStyle(
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    drawText(
                        textLayoutResult = textResult,
                        topLeft = Offset((xPx + 2.dp.toPx()).coerceAtMost(totalWidthPx - textResult.size.width), 3.dp.toPx())
                    )
                } else {
                    // 0.1s Minor Graduation Tick (.1, .2, .3, .4, .6, .7, .8, .9)
                    drawLine(
                        color = Color.White.copy(alpha = 0.4f),
                        start = Offset(xPx, size.height - 7.dp.toPx()),
                        end = Offset(xPx, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                    val tenths = (currentMs % 1000L) / 100L
                    if (tenths % 2L == 0L) {
                        val textResult = textMeasurer.measure(
                            text = ".${tenths}",
                            style = androidx.compose.ui.text.TextStyle(
                                color = StudioTextSecondary,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Normal
                            )
                        )
                        drawText(
                            textLayoutResult = textResult,
                            topLeft = Offset((xPx + 2.dp.toPx()).coerceAtMost(totalWidthPx - textResult.size.width), 6.dp.toPx())
                        )
                    }
                }
            } else if (isMediumZoom) {
                // --- MEDIUM ZOOM MODE (0.5s / 1.0s) ---
                val isSecond = currentMs % 1000L == 0L
                if (isSecond) {
                    drawLine(
                        color = StudioSecondaryTeal,
                        start = Offset(xPx, size.height - 14.dp.toPx()),
                        end = Offset(xPx, size.height),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    val textResult = textMeasurer.measure(
                        text = formatTimeMs(currentMs),
                        style = androidx.compose.ui.text.TextStyle(
                            color = StudioSecondaryTeal,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    drawText(
                        textLayoutResult = textResult,
                        topLeft = Offset((xPx + 3.dp.toPx()).coerceAtMost(totalWidthPx - textResult.size.width), 2.dp.toPx())
                    )
                } else {
                    drawLine(
                        color = Color.White.copy(alpha = 0.5f),
                        start = Offset(xPx, size.height - 8.dp.toPx()),
                        end = Offset(xPx, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            } else {
                // --- NORMAL / OVERVIEW ZOOM MODE ---
                val isMajor = currentMs % 5000L == 0L
                if (isMajor) {
                    drawLine(
                        color = StudioSecondaryTeal,
                        start = Offset(xPx, size.height - 14.dp.toPx()),
                        end = Offset(xPx, size.height),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    val textResult = textMeasurer.measure(
                        text = formatTimeMs(currentMs),
                        style = androidx.compose.ui.text.TextStyle(
                            color = StudioSecondaryTeal,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    drawText(
                        textLayoutResult = textResult,
                        topLeft = Offset((xPx + 4.dp.toPx()).coerceAtMost(totalWidthPx - textResult.size.width), 2.dp.toPx())
                    )
                } else {
                    drawLine(
                        color = Color.White.copy(alpha = 0.3f),
                        start = Offset(xPx, size.height - 6.dp.toPx()),
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
fun PlayheadOverlay(
    offsetDp: Dp,
    currentTimeMs: Long,
    pxPerMs: Float,
    totalDurationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val showTenths = pxPerMs >= 0.18f

    // 44dp wide touch hitbox centered directly over the needle at offsetDp
    Box(
        modifier = modifier
            .offset(x = offsetDp - 22.dp)
            .width(44.dp)
            .fillMaxHeight()
            .zIndex(20f)
            .pointerInput(pxPerMs, totalDurationMs) {
                val densityPxPerMs = pxPerMs.dp.toPx()
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val deltaMs = (dragAmount.x / densityPxPerMs).toLong()
                    onSeek((currentTimeMs + deltaMs).coerceIn(0L, totalDurationMs))
                }
            }
    ) {
        // 1. Vertical Glow / Aura Line (3dp wide, centered)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp)
                .width(3.dp)
                .fillMaxHeight()
                .background(StudioSecondaryTeal.copy(alpha = 0.45f))
        )

        // 2. Main Crisp White Playhead Needle (1.5dp wide, centered)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp)
                .width(1.5.dp)
                .fillMaxHeight()
                .background(Color.White)
        )

        // 3. Top Playhead Head Pointer / Handle Badge (Centered at top)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-2).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = StudioSecondaryTeal,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.8f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = formatTimeMsPrecise(currentTimeMs, showTenths = showTenths),
                        color = Color.Black,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Downward Pointer Triangle Tip
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = StudioSecondaryTeal,
                modifier = Modifier
                    .size(14.dp)
                    .offset(y = (-4).dp)
            )
        }
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
