package com.example.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onSeek: (Long) -> Unit,
    onClipSelected: (TimelineClipEntity) -> Unit,
    onAddClipRequested: () -> Unit = {},
    onAddOverlayTrackRequested: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // 1 ms = 0.08 dp scale for horizontal timeline
    val pxPerMs = 0.08f
    val totalWidthDp = (totalDurationMs * pxPerMs).dp.coerceAtLeast(360.dp)

    var showAddTrackMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, StudioCardBorder, RoundedCornerShape(16.dp))
            .testTag("timeline_editor_container"),
        colors = CardDefaults.cardColors(containerColor = StudioCardBg)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // Multitrack Header and Track Creator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                                text = { Text("Trek Overlay Video PIP") },
                                onClick = {
                                    onAddOverlayTrackRequested("VIDEO")
                                    showAddTrackMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Trek Subjudul") },
                                onClick = {
                                    onAddOverlayTrackRequested("TEXT")
                                    showAddTrackMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Trek Audio") },
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

            // Multitrack Virtualized Scrollable Workspace
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .horizontalScroll(scrollState)
            ) {
                Column(
                    modifier = Modifier.width(totalWidthDp)
                ) {

                    // Timeline Time Ruler Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .background(Color.Black.copy(alpha = 0.4f)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val stepCount = (totalDurationMs / 5000L).toInt() + 1
                        for (i in 0..stepCount) {
                            val stepTime = i * 5000L
                            Text(
                                text = formatTimeMs(stepTime),
                                color = StudioTextSecondary,
                                fontSize = 10.sp,
                                modifier = Modifier
                                    .padding(start = if (i == 0) 4.dp else 0.dp)
                                    .width((5000L * pxPerMs).dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Virtualized Lazy Track rendering for 60FPS high performance
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(tracks, key = { it.id }) { track ->
                            val trackClips = clips.filter { it.trackId == track.id }
                            MultitrackTrackRow(
                                track = track,
                                clips = trackClips,
                                pxPerMs = pxPerMs,
                                onClipSelected = onClipSelected
                            )
                        }
                    }
                }

                // Playhead Red Line Overlay
                val playheadOffsetDp = (currentTimeMs * pxPerMs).dp
                Box(
                    modifier = Modifier
                        .offset(x = playheadOffsetDp)
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(StudioAccentPink)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(StudioAccentPink)
                            .align(Alignment.TopCenter)
                    )
                }
            }
        }
    }
}

@Composable
fun MultitrackTrackRow(
    track: TimelineTrackEntity,
    clips: List<TimelineClipEntity>,
    pxPerMs: Float,
    onClipSelected: (TimelineClipEntity) -> Unit
) {
    var isMuted by remember { mutableStateOf(false) }

    val trackColor = when (track.trackType) {
        "VIDEO" -> StudioPrimaryViolet
        "TEXT" -> StudioSecondaryTeal
        "AUDIO" -> StudioAccentPink
        else -> Color.Gray
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(StudioSurfaceLight, RoundedCornerShape(8.dp))
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track Control Label Bar
        Surface(
            modifier = Modifier
                .width(100.dp)
                .fillMaxHeight()
                .padding(end = 4.dp),
            color = trackColor.copy(alpha = 0.15f),
            shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp),
            border = BorderStroke(1.dp, trackColor.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = track.trackName,
                    color = StudioTextPrimary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { isMuted = !isMuted },
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

        // Track Clips Canvas Container
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            clips.forEach { clip ->
                val startDp = (clip.startTimeMs * pxPerMs).dp
                val widthDp = (clip.durationMs * pxPerMs).dp.coerceAtLeast(40.dp)

                Surface(
                    modifier = Modifier
                        .offset(x = startDp)
                        .width(widthDp)
                        .fillMaxHeight()
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onClipSelected(clip) }
                        .testTag("clip_item_${clip.id}"),
                    color = if (isMuted) trackColor.copy(alpha = 0.3f) else trackColor.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
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
                            maxLines = 1
                        )

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
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
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
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
