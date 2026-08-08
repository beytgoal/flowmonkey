package com.example.ui.components

import androidx.compose.foundation.*
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import com.example.data.db.TimelineClipEntity
import com.example.data.db.TimelineTrackEntity
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineView(
    tracks: List<TimelineTrackEntity>,
    clips: List<TimelineClipEntity>,
    currentTimeMs: Long,
    totalDurationMs: Long = 15000L,
    activeColumnFilter: String = "ALL",
    onSeek: (Long) -> Unit,
    onClipSelected: (TimelineClipEntity) -> Unit,
    onClipMoved: (clipId: Long, newStartTimeMs: Long, newTrackId: Long) -> Unit = { _, _, _ -> },
    onAddClipRequested: () -> Unit = {},
    onAddOverlayTrackRequested: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val mainVideoTrack = tracks.find { it.trackType == "VIDEO" }

    // 1 ms = 0.08 dp scale for horizontal timeline canvas
    val pxPerMs = 0.08f
    val timelineWidthDp = (totalDurationMs * pxPerMs).dp.coerceAtLeast(360.dp)

    var showAddTrackMenu by remember { mutableStateOf(false) }

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
            .border(1.dp, StudioCardBorder, RoundedCornerShape(16.dp))
            .testTag("timeline_editor_container"),
        colors = CardDefaults.cardColors(containerColor = StudioCardBg)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {

            // Multitrack Action Toolbar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ViewTimeline,
                        contentDescription = "Timeline",
                        tint = StudioPrimaryViolet,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Timeline",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Unlimited Overlay Track Button
                    Box {
                        OutlinedButton(
                            onClick = { showAddTrackMenu = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioSecondaryTeal),
                            border = BorderStroke(1.dp, StudioSecondaryTeal),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp).testTag("add_overlay_track_button")
                        ) {
                            Text("+ Overlay", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        DropdownMenu(
                            expanded = showAddTrackMenu,
                            onDismissRequest = { showAddTrackMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Overlay Video PIP") },
                                onClick = {
                                    onAddOverlayTrackRequested("VIDEO")
                                    showAddTrackMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Subjudul") },
                                onClick = {
                                    onAddOverlayTrackRequested("TEXT")
                                    showAddTrackMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Audio") },
                                onClick = {
                                    onAddOverlayTrackRequested("AUDIO")
                                    showAddTrackMenu = false
                                }
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = onAddClipRequested,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = StudioPrimaryViolet.copy(alpha = 0.2f),
                            contentColor = StudioPrimaryViolet
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp).testTag("timeline_add_clip_button")
                    ) {
                        Text(text = "Tambah Klip", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val mainVideoTrack = tracks.find { it.trackType == "VIDEO" && it.trackIndex == 0 } ?: tracks.find { it.trackType == "VIDEO" }
            val overlayVideoTracks = tracks.filter { it.trackType == "VIDEO" && it.id != mainVideoTrack?.id }
            val textTracksAll = tracks.filter { it.trackType == "TEXT" }
            val stickerTracksAll = tracks.filter { it.trackType == "STICKER" }
            val audioTracksAll = tracks.filter { it.trackType == "AUDIO" }

            // Multitrack Container with Anchored Track Headers on Left & Synchronized Scrollable Canvas on Right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Fixed Left Track Controls Column (Anchored Width: 100.dp)
                Column(
                    modifier = Modifier.width(100.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Top Time Ruler Corner Anchor
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                            .background(StudioSurfaceDark, RoundedCornerShape(topStart = 8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = formatTimeMs(currentTimeMs),
                            fontSize = 11.sp,
                            color = StudioSecondaryTeal,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Determine visible tracks according to activeColumnFilter mode
                    val videoTracks = when (activeColumnFilter) {
                        "TEXT", "AUDIO", "STICKER" -> if (mainVideoTrack != null) listOf(mainVideoTrack) else emptyList()
                        else -> if (mainVideoTrack != null) listOf(mainVideoTrack) + overlayVideoTracks else emptyList()
                    }

                    val textTracks = when (activeColumnFilter) {
                        "TEXT", "ALL" -> textTracksAll
                        else -> emptyList()
                    }

                    val stickerTracks = when (activeColumnFilter) {
                        "STICKER", "ALL" -> if (stickerTracksAll.isNotEmpty()) stickerTracksAll else if (activeColumnFilter == "STICKER") listOf(
                            TimelineTrackEntity(id = -999L, projectId = mainVideoTrack?.projectId ?: 0L, trackType = "STICKER", trackName = "Track Stiker", trackIndex = 99)
                        ) else emptyList()
                        else -> emptyList()
                    }

                    val audioTracks = when (activeColumnFilter) {
                        "AUDIO", "ALL" -> audioTracksAll
                        else -> emptyList()
                    }

                    // Track Control Headers grouped by media column
                    if (videoTracks.isNotEmpty()) {
                        videoTracks.forEach { track ->
                            TrackHeaderCard(track = track)
                        }
                    }

                    if (textTracks.isNotEmpty()) {
                        textTracks.forEach { track ->
                            TrackHeaderCard(track = track)
                        }
                    }

                    if (stickerTracks.isNotEmpty()) {
                        stickerTracks.forEach { track ->
                            TrackHeaderCard(track = track)
                        }
                    }

                    if (audioTracks.isNotEmpty()) {
                        audioTracks.forEach { track ->
                            TrackHeaderCard(track = track)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Right Scrollable Multitrack Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(scrollState)
                        .pointerInput(totalDurationMs, pxPerMs) {
                            val densityPxPerMs = pxPerMs.dp.toPx()
                            detectTapGestures { offset ->
                                val targetMs = (offset.x / densityPxPerMs).toLong().coerceIn(0L, totalDurationMs)
                                onSeek(targetMs)
                            }
                        }
                        .pointerInput(totalDurationMs, pxPerMs) {
                            val densityPxPerMs = pxPerMs.dp.toPx()
                            detectDragGestures { change, _ ->
                                change.consume()
                                val targetMs = (change.position.x / densityPxPerMs).toLong().coerceIn(0L, totalDurationMs)
                                onSeek(targetMs)
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier.width(timelineWidthDp)
                    ) {
                        // High-Precision Time Ruler
                        TimeRulerCanvas(
                            totalDurationMs = totalDurationMs,
                            pxPerMs = pxPerMs,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp)
                                .background(Color.Black.copy(alpha = 0.5f))
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Track Rows for Clips grouped by media column according to activeColumnFilter
                        val videoTracksCanvas = when (activeColumnFilter) {
                            "TEXT", "AUDIO", "STICKER" -> if (mainVideoTrack != null) listOf(mainVideoTrack) else emptyList()
                            else -> if (mainVideoTrack != null) listOf(mainVideoTrack) + overlayVideoTracks else emptyList()
                        }

                        val textTracksCanvas = when (activeColumnFilter) {
                            "TEXT", "ALL" -> textTracksAll
                            else -> emptyList()
                        }

                        val stickerTracksCanvas = when (activeColumnFilter) {
                            "STICKER", "ALL" -> if (stickerTracksAll.isNotEmpty()) stickerTracksAll else if (activeColumnFilter == "STICKER") listOf(
                                TimelineTrackEntity(id = -999L, projectId = mainVideoTrack?.projectId ?: 0L, trackType = "STICKER", trackName = "Track Stiker", trackIndex = 99)
                            ) else emptyList()
                            else -> emptyList()
                        }

                        val audioTracksCanvas = when (activeColumnFilter) {
                            "AUDIO", "ALL" -> audioTracksAll
                            else -> emptyList()
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (videoTracksCanvas.isNotEmpty()) {
                                videoTracksCanvas.forEach { track ->
                                    val trackClips = clips.filter { it.trackId == track.id }
                                    TrackClipsRow(
                                        track = track,
                                        clips = trackClips,
                                        allTracks = tracks,
                                        pxPerMs = pxPerMs,
                                        onClipSelected = onClipSelected,
                                        onClipMoved = onClipMoved
                                    )
                                }
                            }

                            if (textTracksCanvas.isNotEmpty()) {
                                textTracksCanvas.forEach { track ->
                                    val trackClips = clips.filter { it.trackId == track.id }
                                    TrackClipsRow(
                                        track = track,
                                        clips = trackClips,
                                        allTracks = tracks,
                                        pxPerMs = pxPerMs,
                                        onClipSelected = onClipSelected,
                                        onClipMoved = onClipMoved
                                    )
                                }
                            }

                            if (stickerTracksCanvas.isNotEmpty()) {
                                stickerTracksCanvas.forEach { track ->
                                    val trackClips = clips.filter { it.trackId == track.id || (it.stickerIcon.isNotBlank() && track.id == -999L) }
                                    TrackClipsRow(
                                        track = track,
                                        clips = trackClips,
                                        allTracks = tracks,
                                        pxPerMs = pxPerMs,
                                        onClipSelected = onClipSelected,
                                        onClipMoved = onClipMoved
                                    )
                                }
                            }

                            if (audioTracksCanvas.isNotEmpty()) {
                                audioTracksCanvas.forEach { track ->
                                    val trackClips = clips.filter { it.trackId == track.id }
                                    TrackClipsRow(
                                        track = track,
                                        clips = trackClips,
                                        allTracks = tracks,
                                        pxPerMs = pxPerMs,
                                        onClipSelected = onClipSelected,
                                        onClipMoved = onClipMoved
                                    )
                                }
                            }
                        }
                    }

                    // Precision Playhead Red Line Overlay (0ms is aligned exactly at x = 0.dp)
                    val playheadOffsetDp = (currentTimeMs * pxPerMs).dp
                    PlayheadOverlay(
                        offsetDp = playheadOffsetDp,
                        currentTimeMs = currentTimeMs
                    )
                }
            }
        }
    }
}

@Composable
fun TrackHeaderCard(
    track: TimelineTrackEntity,
    onMuteToggled: (Boolean) -> Unit = {}
) {
    var isMuted by remember { mutableStateOf(false) }

    val trackColor = when (track.trackType) {
        "VIDEO" -> StudioPrimaryViolet
        "TEXT" -> StudioSecondaryTeal
        "AUDIO" -> StudioAccentPink
        else -> Color.Gray
    }

    val icon = when (track.trackType) {
        "VIDEO" -> Icons.Default.Movie
        "TEXT" -> Icons.Default.Subtitles
        "AUDIO" -> Icons.Default.Audiotrack
        else -> Icons.Default.Layers
    }

    Surface(
        modifier = Modifier
            .width(100.dp)
            .height(48.dp),
        color = trackColor.copy(alpha = 0.18f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, trackColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = trackColor,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                val cleanName = track.trackName.replace("Trek ", "").replace("Trek", "").trim()
                Text(
                    text = cleanName.ifBlank { track.trackType },
                    color = StudioTextPrimary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            IconButton(
                onClick = {
                    isMuted = !isMuted
                    onMuteToggled(isMuted)
                },
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = "Mute",
                    tint = if (isMuted) StudioAccentPink else StudioTextSecondary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
fun TrackClipsRow(
    track: TimelineTrackEntity,
    clips: List<TimelineClipEntity>,
    allTracks: List<TimelineTrackEntity>,
    pxPerMs: Float,
    onClipSelected: (TimelineClipEntity) -> Unit,
    onClipMoved: (clipId: Long, newStartTimeMs: Long, newTrackId: Long) -> Unit
) {
    val density = LocalDensity.current
    val trackColor = when (track.trackType) {
        "VIDEO" -> StudioPrimaryViolet
        "TEXT" -> StudioSecondaryTeal
        "AUDIO" -> StudioAccentPink
        else -> Color.Gray
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(StudioSurfaceLight.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
    ) {
        clips.forEach { clip ->
            val startDp = (clip.startTimeMs * pxPerMs).dp
            val widthDp = (clip.durationMs * pxPerMs).dp.coerceAtLeast(36.dp)

            var isDragging by remember { mutableStateOf(false) }
            var dragOffsetX by remember { mutableFloatStateOf(0f) }
            var dragOffsetY by remember { mutableFloatStateOf(0f) }

            Box(
                modifier = Modifier
                    .offset(x = startDp)
                    .offset { IntOffset(dragOffsetX.roundToInt(), dragOffsetY.roundToInt()) }
                    .width(widthDp)
                    .fillMaxHeight()
                    .padding(vertical = 2.dp, horizontal = 1.dp)
                    .zIndex(if (isDragging) 10f else 1f)
                    .pointerInput(clip.id) {
                        detectTapGestures(
                            onTap = { onClipSelected(clip) }
                        )
                    }
                    .pointerInput(clip.id, allTracks, pxPerMs) {
                        detectDragGestures(
                            onDragStart = {
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
                                if (isDragging) {
                                    val densityPxPerMs = pxPerMs.dp.toPx()
                                    val deltaMs = (dragOffsetX / densityPxPerMs).toLong()
                                    val newStartMs = (clip.startTimeMs + deltaMs).coerceAtLeast(0L)

                                    val trackHeightPx = with(density) { 54.dp.toPx() } // 48.dp row + 6.dp gap
                                    val indexShift = (dragOffsetY / trackHeightPx).roundToInt()

                                    // Filter target tracks strictly to the same track type (VIDEO, TEXT, AUDIO)
                                    val sameTypeTracks = allTracks.filter { it.trackType == track.trackType }
                                    val currentTrackIdx = sameTypeTracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
                                    val targetTrackIdx = (currentTrackIdx + indexShift).coerceIn(0, sameTypeTracks.lastIndex)
                                    val targetTrackId = sameTypeTracks.getOrNull(targetTrackIdx)?.id ?: track.id

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
                    color = if (isDragging) StudioSecondaryTeal.copy(alpha = 0.95f) else (if (track.trackType == "AUDIO") StudioAccentPink.copy(alpha = 0.85f) else trackColor.copy(alpha = 0.85f)),
                    border = BorderStroke(if (isDragging) 2.dp else 1.dp, if (isDragging) Color.Yellow else Color.White.copy(alpha = 0.4f)),
                    shadowElevation = if (isDragging) 8.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (track.trackType == "TEXT") clip.textContent ?: clip.title else clip.title,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            if (clip.hasKeyframe) {
                                Text("◇", color = StudioAccentAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
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
                }
            }
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
            color = Color.White.copy(alpha = 0.2f),
            start = Offset(0f, size.height),
            end = Offset(totalWidthPx, size.height),
            strokeWidth = 1.dp.toPx()
        )

        val stepMs = 1000L // 1 second steps
        var currentMs = 0L

        while (currentMs <= totalDurationMs) {
            val xPx = currentMs * densityPxPerMs
            val isMajor = currentMs % 5000L == 0L // Major tick every 5 seconds

            if (isMajor) {
                // Major tick line
                drawLine(
                    color = StudioSecondaryTeal,
                    start = Offset(xPx, size.height - 12.dp.toPx()),
                    end = Offset(xPx, size.height),
                    strokeWidth = 1.5.dp.toPx()
                )

                // Time label e.g. "00:05"
                val timeText = formatTimeMs(currentMs)
                val textResult = textMeasurer.measure(
                    text = timeText,
                    style = androidx.compose.ui.text.TextStyle(
                        color = StudioTextSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                drawText(
                    textLayoutResult = textResult,
                    topLeft = Offset((xPx + 4.dp.toPx()).coerceAtMost(totalWidthPx - textResult.size.width), 2.dp.toPx())
                )
            } else {
                // Minor tick line
                drawLine(
                    color = Color.White.copy(alpha = 0.3f),
                    start = Offset(xPx, size.height - 6.dp.toPx()),
                    end = Offset(xPx, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }

            currentMs += stepMs
        }
    }
}

@Composable
fun PlayheadOverlay(
    offsetDp: Dp,
    currentTimeMs: Long,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .offset(x = offsetDp)
            .width(2.dp)
            .fillMaxHeight()
    ) {
        // High-contrast background glow stroke behind red line
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .align(Alignment.Center)
                .background(Color.White.copy(alpha = 0.35f))
        )

        // Main Vibrant Red Playhead Line
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .align(Alignment.Center)
                .background(StudioAccentPink)
        )

        // Playhead Top Handle Badge with Live Time Readout
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-2).dp),
            shape = RoundedCornerShape(4.dp),
            color = StudioAccentPink,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = formatTimeMs(currentTimeMs),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SectionHeaderBadge(title: String, color: Color) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp),
        color = color.copy(alpha = 0.25f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}

@Composable
fun SectionDividerLine(title: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .height(2.dp)
                .weight(1f)
                .background(color.copy(alpha = 0.4f))
        )
        Text(
            text = " $title ",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = color.copy(alpha = 0.8f)
        )
        Box(
            modifier = Modifier
                .height(2.dp)
                .weight(1f)
                .background(color.copy(alpha = 0.4f))
        )
    }
}
