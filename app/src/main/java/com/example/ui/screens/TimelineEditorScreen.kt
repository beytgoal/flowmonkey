package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.ui.components.TimelineView
import com.example.ui.components.TransitionSelectionSheet
import com.example.ui.components.VideoPlayerView
import com.example.ui.components.formatTimeMs
import com.example.ui.theme.*
import com.example.ui.viewmodels.MainTab
import com.example.ui.viewmodels.VideoStudioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineEditorScreen(
    viewModel: VideoStudioViewModel,
    modifier: Modifier = Modifier
) {
    val activeProject by viewModel.activeProject.collectAsState()
    val tracks by viewModel.timelineTracks.collectAsState()
    val clips by viewModel.timelineClips.collectAsState()
    val currentTimeMs by viewModel.currentTimeMs.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()

    val isProxyMode by viewModel.isProxyModeEnabled.collectAsState()
    val proxyResolution by viewModel.proxyResolution.collectAsState()
    val autoTranscode by viewModel.autoTranscodeOnImport.collectAsState()
    val transcodingJobs by viewModel.transcodingJobs.collectAsState()

    var selectedClip by remember { mutableStateOf<TimelineClipEntity?>(null) }
    var timelineScope by remember { mutableStateOf(com.example.ui.components.TimelineScope.MAIN) }
    var selectedToolCategory by remember { mutableStateOf(0) } // 0: Dasar, 1: Visual/Style, 2: VFX/AI
    var isToolsExpanded by remember { mutableStateOf(true) }

    var showRatioSheet by remember { mutableStateOf(false) }
    var showCanvasBgSheet by remember { mutableStateOf(false) }
    var canvasBackgroundColor by remember { mutableStateOf(Color(0xFF0F0F1A)) }
    var canvasBlurRadius by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(selectedClip) {
        selectedClip?.let { clip ->
            val track = tracks.find { it.id == clip.trackId }
            when {
                clip.stickerIcon != "None" && clip.stickerIcon.isNotBlank() || track?.trackType == "STICKER" || track?.trackType == "OVERLAY" || clip.mediaUri.contains("photo") || clip.mediaUri.contains("image") -> {
                    timelineScope = com.example.ui.components.TimelineScope.STICKER_SUBTIMELINE
                }
                clip.textContent != null || track?.trackType == "TEXT" -> {
                    timelineScope = com.example.ui.components.TimelineScope.TEXT_SUBTIMELINE
                }
                clip.audioSfx != "None" && clip.audioSfx.isNotBlank() || track?.trackType == "AUDIO" || clip.isVoiceover -> {
                    timelineScope = com.example.ui.components.TimelineScope.AUDIO_SUBTIMELINE
                }
                else -> {
                    timelineScope = com.example.ui.components.TimelineScope.VIDEO_SUBTIMELINE
                }
            }
        }
    }

    // Dialog & Bottom Sheet visibility states
    var showAddClipSheet by remember { mutableStateOf(false) }
    var showAssetStoreSheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showAnimationSheet by remember { mutableStateOf(false) }
    var showCropRotateSheet by remember { mutableStateOf(false) }
    var showEditVisualSheet by remember { mutableStateOf(false) }
    var showEffectsSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showAdjustSheet by remember { mutableStateOf(false) }
    var showCutoutSheet by remember { mutableStateOf(false) }
    var showMaskingSheet by remember { mutableStateOf(false) }
    var showKeyframeSheet by remember { mutableStateOf(false) }
    var showStabilizeSheet by remember { mutableStateOf(false) }
    var showAudioStudioSheet by remember { mutableStateOf(false) }
    var showTextDialog by remember { mutableStateOf(false) }
    var showFontTypographySheet by remember { mutableStateOf(false) }
    var showStickersSheet by remember { mutableStateOf(false) }
    var showOverlaySheet by remember { mutableStateOf(false) }
    var showProxyDialog by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showTransitionSheet by remember { mutableStateOf(false) }
    var transitionClipA by remember { mutableStateOf<TimelineClipEntity?>(null) }
    var transitionClipB by remember { mutableStateOf<TimelineClipEntity?>(null) }

    val customLuts by viewModel.customLutList.collectAsState()

    // File picker launcher for Custom LUTs
    val lutFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val fileName = it.lastPathSegment?.substringAfterLast("/") ?: "Custom_LUT_${System.currentTimeMillis()}.cube"
            viewModel.importCustomLut(fileName, it.toString())
            selectedClip?.let { clip ->
                viewModel.updateClipFilter(clip, "LUT: $fileName")
            }
        }
    }

    // Media Gallery Picker Launcher for Main Video
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            viewModel.addMediaFromGallery(it.toString(), "Video Galeri")
        }
    }

    // Media Gallery Picker Launcher for Overlay Media (Photo / Image)
    val overlayPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            viewModel.addPhotoOverlay(it.toString(), "Overlay Foto")
        }
    }

    val totalDurationMs = (clips.maxOfOrNull { it.endTimeMs } ?: 15000L).coerceAtLeast(5000L)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {

        // Video Canvas Preview Top (Cleaned without active tags, supporting pinch zoom & pan)
        VideoPlayerView(
            aspectRatioStr = activeProject?.aspectRatio ?: "16:9",
            isPlaying = isPlaying,
            currentTimeMs = currentTimeMs,
            totalDurationMs = totalDurationMs,
            activeFilter = selectedClip?.filterName ?: "Cinematic Glow",
            activeAnimation = selectedClip?.animationIn?.takeIf { it != "None" }
                ?: selectedClip?.animationOut?.takeIf { it != "None" }
                ?: selectedClip?.animationCombo ?: "None",
            activeEffect = selectedClip?.effectName ?: "None",
            isProxyMode = isProxyMode,
            proxyResolution = proxyResolution,
            clips = clips,
            selectedClipId = selectedClip?.id,
            onAddOrUpdateKeyframe = { clip, timeOffsetMs, posX, posY, scale, rot, op ->
                viewModel.addOrUpdateKeyframePoint(clip, timeOffsetMs, posX, posY, scale, rot, op)
            },
            onRemoveKeyframe = { clip, timeOffsetMs ->
                viewModel.removeKeyframePoint(clip, timeOffsetMs)
            },
            onSeek = { viewModel.seekTo(it) },
            onToggleProxyMode = { showProxyDialog = true },
            onTogglePlay = { viewModel.togglePlayPause() }
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Time Duration & Adjustable Seek Slider (Directly below preview screen)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StudioCardBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = StudioCardBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val formattedCurrent = formatTimeMs(currentTimeMs)
                    val formattedTotal = formatTimeMs(totalDurationMs)

                    Text(
                        text = "$formattedCurrent / $formattedTotal",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Durasi Video",
                        color = StudioTextSecondary,
                        fontSize = 10.sp
                    )
                }

                Slider(
                    value = currentTimeMs.toFloat().coerceIn(0f, totalDurationMs.coerceAtLeast(1000L).toFloat()),
                    onValueChange = { viewModel.seekTo(it.toLong()) },
                    valueRange = 0f..totalDurationMs.coerceAtLeast(1000L).toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = StudioSecondaryTeal,
                        activeTrackColor = StudioSecondaryTeal,
                        inactiveTrackColor = StudioCardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .testTag("preview_time_scrubber")
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Playback & Undo/Redo & Aspect Ratio Control Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StudioCardBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = StudioCardBg)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Undo / Redo Actions
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.undoTimeline() },
                        enabled = canUndo,
                        modifier = Modifier.size(36.dp).testTag("undo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = "Undo",
                            tint = if (canUndo) Color.White else Color.Gray.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.redoTimeline() },
                        enabled = canRedo,
                        modifier = Modifier.size(36.dp).testTag("redo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Redo,
                            contentDescription = "Redo",
                            tint = if (canRedo) Color.White else Color.Gray.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Playback controls + Aspect Ratio Changer right beside Next button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.seekTo(0L) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.SkipPrevious, contentDescription = "Rewind", tint = Color.White, modifier = Modifier.size(20.dp))
                    }

                    FilledIconButton(
                        onClick = { viewModel.togglePlayPause() },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = StudioPrimaryViolet),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.seekTo(totalDurationMs) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Forward / Next", tint = Color.White, modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Functional Aspect Ratio Changer Button (Placed right beside Next button)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StudioSecondaryTeal.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, StudioSecondaryTeal),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showRatioSheet = true }
                            .testTag("preview_aspect_ratio_changer_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = "Ubah Rasio Layar",
                                tint = StudioSecondaryTeal,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = activeProject?.aspectRatio ?: "16:9",
                                color = StudioSecondaryTeal,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Context-Aware Adaptive Tools Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    timelineScope.themeColor.copy(alpha = 0.5f),
                    RoundedCornerShape(14.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = StudioCardBg)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                if (timelineScope == com.example.ui.components.TimelineScope.MAIN && selectedClip == null) {
                    // --- CLEAN INITIAL TIMELINE UTAMA TOOLBAR ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = StudioPrimaryViolet,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tools Timeline Utama",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Surface(
                            color = StudioPrimaryViolet.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "7 Aksi Dasar",
                                color = StudioPrimaryViolet,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 7 Foundational Actions on Timeline Utama
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.testTag("main_initial_tools_bar")
                    ) {
                        // 1. Tambah Klip Video
                        item {
                            MainToolButton(
                                icon = Icons.Default.Movie,
                                label = "+ Klip Video",
                                color = StudioPrimaryViolet,
                                onClick = { showAddClipSheet = true },
                                testTag = "main_tool_add_clip"
                            )
                        }

                        // 2. Split
                        item {
                            val activeVideoClip = clips.find {
                                it.startTimeMs <= currentTimeMs && it.endTimeMs > currentTimeMs &&
                                        tracks.find { t -> t.id == it.trackId }?.trackType == "VIDEO"
                            } ?: clips.firstOrNull { tracks.find { t -> t.id == it.trackId }?.trackType == "VIDEO" }

                            MainToolButton(
                                icon = Icons.Default.ContentCut,
                                label = "Split",
                                color = StudioSecondaryTeal,
                                enabled = activeVideoClip != null,
                                onClick = {
                                    activeVideoClip?.let {
                                        viewModel.splitClipAtCurrentTime(it, currentTimeMs)
                                    }
                                },
                                testTag = "main_tool_split"
                            )
                        }

                        // 3. Ukuran Rasio Kanvas
                        item {
                            MainToolButton(
                                icon = Icons.Default.AspectRatio,
                                label = "Ukuran Rasio",
                                color = StudioAccentAmber,
                                onClick = { showRatioSheet = true },
                                testTag = "main_tool_ratio"
                            )
                        }

                        // 4. Background Kanvas
                        item {
                            MainToolButton(
                                icon = Icons.Default.Wallpaper,
                                label = "Background",
                                color = Color(0xFF64B5F6),
                                onClick = { showCanvasBgSheet = true },
                                testTag = "main_tool_bg"
                            )
                        }

                        // 5. Add Sound / Musik
                        item {
                            MainToolButton(
                                icon = Icons.Default.MusicNote,
                                label = "+ Musik/Sound",
                                color = StudioAccentPink,
                                onClick = {
                                    timelineScope = com.example.ui.components.TimelineScope.AUDIO_SUBTIMELINE
                                    showAudioStudioSheet = true
                                },
                                testTag = "main_tool_add_sound"
                            )
                        }

                        // 6. Add Judul / Caption
                        item {
                            MainToolButton(
                                icon = Icons.Default.Title,
                                label = "+ Judul/Caption",
                                color = StudioSecondaryTeal,
                                onClick = {
                                    timelineScope = com.example.ui.components.TimelineScope.TEXT_SUBTIMELINE
                                    showTextDialog = true
                                },
                                testTag = "main_tool_add_text"
                            )
                        }

                        // 7. Add Stiker & Foto
                        item {
                            MainToolButton(
                                icon = Icons.Default.AutoAwesome,
                                label = "+ Stiker/Foto",
                                color = Color(0xFFFFB74D),
                                onClick = {
                                    timelineScope = com.example.ui.components.TimelineScope.STICKER_SUBTIMELINE
                                    showStickersSheet = true
                                },
                                testTag = "main_tool_add_sticker"
                            )
                        }
                    }
                } else {
                    // --- SUB-TIMELINE ADAPTIVE TOOLBAR WITH COMPACT ICON CONTROLS ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tombol KEMBALI di sebelah KIRI (Hanya Icon agar UI tidak penuh)
                        IconButton(
                            onClick = {
                                selectedClip = null
                                timelineScope = com.example.ui.components.TimelineScope.MAIN
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.4f))
                                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                .testTag("subtimeline_back_icon_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali ke Timeline Utama",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Judul Tools Sub-Timeline di Bagian TENGAH
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = when (timelineScope) {
                                    com.example.ui.components.TimelineScope.VIDEO_SUBTIMELINE -> Icons.Default.Movie
                                    com.example.ui.components.TimelineScope.AUDIO_SUBTIMELINE -> Icons.Default.Audiotrack
                                    com.example.ui.components.TimelineScope.TEXT_SUBTIMELINE -> Icons.Default.Title
                                    com.example.ui.components.TimelineScope.STICKER_SUBTIMELINE -> Icons.Default.AutoAwesome
                                    else -> Icons.Default.Tune
                                },
                                contentDescription = null,
                                tint = timelineScope.themeColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (timelineScope) {
                                    com.example.ui.components.TimelineScope.VIDEO_SUBTIMELINE -> "Tools Video & Overlay"
                                    com.example.ui.components.TimelineScope.AUDIO_SUBTIMELINE -> "Tools Musik & Audio"
                                    com.example.ui.components.TimelineScope.TEXT_SUBTIMELINE -> "Tools Tipografi & Caption"
                                    com.example.ui.components.TimelineScope.STICKER_SUBTIMELINE -> "Tools Stiker & Foto"
                                    else -> "Tools Aset"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            if (selectedClip != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = timelineScope.themeColor.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = selectedClip!!.title.take(12),
                                        color = timelineScope.themeColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        // Tombol +OVERLAY / AKSI di sebelah KANAN (Hanya Icon agar UI tidak penuh)
                        IconButton(
                            onClick = {
                                when (timelineScope) {
                                    com.example.ui.components.TimelineScope.VIDEO_SUBTIMELINE -> {
                                        showOverlaySheet = true
                                    }
                                    com.example.ui.components.TimelineScope.AUDIO_SUBTIMELINE -> {
                                        showAudioStudioSheet = true
                                    }
                                    com.example.ui.components.TimelineScope.TEXT_SUBTIMELINE -> {
                                        showTextDialog = true
                                    }
                                    com.example.ui.components.TimelineScope.STICKER_SUBTIMELINE -> {
                                        showStickersSheet = true
                                    }
                                    else -> {}
                                }
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(StudioSecondaryTeal.copy(alpha = 0.2f))
                                .border(1.dp, StudioSecondaryTeal, CircleShape)
                                .testTag("subtimeline_add_overlay_icon_button")
                        ) {
                            Icon(
                                imageVector = when (timelineScope) {
                                    com.example.ui.components.TimelineScope.VIDEO_SUBTIMELINE -> Icons.Default.Layers
                                    com.example.ui.components.TimelineScope.AUDIO_SUBTIMELINE -> Icons.Default.Add
                                    com.example.ui.components.TimelineScope.TEXT_SUBTIMELINE -> Icons.Default.Add
                                    com.example.ui.components.TimelineScope.STICKER_SUBTIMELINE -> Icons.Default.AddPhotoAlternate
                                    else -> Icons.Default.Add
                                },
                                contentDescription = "+ Overlay / Tambah Aset",
                                tint = StudioSecondaryTeal,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Sub-category Tabs for the active sub-timeline pillar
                    when (timelineScope) {
                        com.example.ui.components.TimelineScope.VIDEO_SUBTIMELINE -> {
                            ScrollableTabRow(
                                selectedTabIndex = selectedToolCategory.coerceIn(0, 2),
                                containerColor = Color.Transparent,
                                contentColor = StudioPrimaryViolet,
                                edgePadding = 0.dp
                            ) {
                                Tab(selected = selectedToolCategory == 0, onClick = { selectedToolCategory = 0 }, text = { Text("Dasar & Speed", fontWeight = FontWeight.Bold, fontSize = 11.sp) })
                                Tab(selected = selectedToolCategory == 1, onClick = { selectedToolCategory = 1 }, text = { Text("Warna & Filter", fontWeight = FontWeight.Bold, fontSize = 11.sp) })
                                Tab(selected = selectedToolCategory == 2, onClick = { selectedToolCategory = 2 }, text = { Text("VFX & AI", fontWeight = FontWeight.Bold, fontSize = 11.sp) })
                            }
                        }
                        com.example.ui.components.TimelineScope.AUDIO_SUBTIMELINE -> {
                            ScrollableTabRow(
                                selectedTabIndex = selectedToolCategory.coerceIn(0, 2),
                                containerColor = Color.Transparent,
                                contentColor = StudioAccentPink,
                                edgePadding = 0.dp
                            ) {
                                Tab(selected = selectedToolCategory == 0, onClick = { selectedToolCategory = 0 }, text = { Text("Mixer & Fade", fontWeight = FontWeight.Bold, fontSize = 11.sp) })
                                Tab(selected = selectedToolCategory == 1, onClick = { selectedToolCategory = 1 }, text = { Text("AI Denoise & Pitch", fontWeight = FontWeight.Bold, fontSize = 11.sp) })
                                Tab(selected = selectedToolCategory == 2, onClick = { selectedToolCategory = 2 }, text = { Text("Beat & Tempo", fontWeight = FontWeight.Bold, fontSize = 11.sp) })
                            }
                        }
                        com.example.ui.components.TimelineScope.TEXT_SUBTIMELINE -> {
                            ScrollableTabRow(
                                selectedTabIndex = selectedToolCategory.coerceIn(0, 2),
                                containerColor = Color.Transparent,
                                contentColor = StudioSecondaryTeal,
                                edgePadding = 0.dp
                            ) {
                                Tab(selected = selectedToolCategory == 0, onClick = { selectedToolCategory = 0 }, text = { Text("Teks & Font", fontWeight = FontWeight.Bold, fontSize = 11.sp) })
                                Tab(selected = selectedToolCategory == 1, onClick = { selectedToolCategory = 1 }, text = { Text("Gaya & Warna", fontWeight = FontWeight.Bold, fontSize = 11.sp) })
                                Tab(selected = selectedToolCategory == 2, onClick = { selectedToolCategory = 2 }, text = { Text("Animasi Teks", fontWeight = FontWeight.Bold, fontSize = 11.sp) })
                            }
                        }
                        com.example.ui.components.TimelineScope.STICKER_SUBTIMELINE -> {
                            ScrollableTabRow(
                                selectedTabIndex = selectedToolCategory.coerceIn(0, 2),
                                containerColor = Color.Transparent,
                                contentColor = Color(0xFFFFB74D),
                                edgePadding = 0.dp
                            ) {
                                Tab(selected = selectedToolCategory == 0, onClick = { selectedToolCategory = 0 }, text = { Text("Stiker & Emoji", fontWeight = FontWeight.Bold, fontSize = 11.sp) })
                                Tab(selected = selectedToolCategory == 1, onClick = { selectedToolCategory = 1 }, text = { Text("Overlay Foto", fontWeight = FontWeight.Bold, fontSize = 11.sp) })
                                Tab(selected = selectedToolCategory == 2, onClick = { selectedToolCategory = 2 }, text = { Text("Motion & Keyframe", fontWeight = FontWeight.Bold, fontSize = 11.sp) })
                            }
                        }
                        else -> {}
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Contextual Action Tool Chips for the current sub-timeline
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.testTag("timeline_tools_bar")
                    ) {
                        when (timelineScope) {
                            com.example.ui.components.TimelineScope.VIDEO_SUBTIMELINE -> {
                                when (selectedToolCategory.coerceIn(0, 2)) {
                                    0 -> {
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.ContentCut,
                                                label = "Split Bagi Klip",
                                                onClick = { selectedClip?.let { viewModel.splitClipAtCurrentTime(it, currentTimeMs) } },
                                                enabled = selectedClip != null && currentTimeMs > (selectedClip?.startTimeMs ?: 0L) && currentTimeMs < (selectedClip?.endTimeMs ?: 0L),
                                                color = StudioSecondaryTeal
                                            )
                                        }
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.Speed,
                                                label = "Speed Ramping Kurva Sentuh",
                                                onClick = { showSpeedSheet = true },
                                                color = StudioAccentAmber
                                            )
                                        }
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.Crop,
                                                label = "Crop & Rasio Kanvas",
                                                onClick = { showCropRotateSheet = true },
                                                color = StudioSecondaryTeal
                                            )
                                        }
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.VolumeUp,
                                                label = "Volume Video",
                                                onClick = { selectedClip?.let { viewModel.updateClipVolume(it, if (it.volume == 0f) 1f else 0f) } }
                                            )
                                        }
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.AddPhotoAlternate,
                                                label = "+ Tambah Overlay Foto/Video",
                                                onClick = { showOverlaySheet = true },
                                                color = Color(0xFFFFB74D)
                                            )
                                        }
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.FastRewind,
                                                label = "Reverse Video",
                                                onClick = { selectedClip?.let { viewModel.reverseClip(it) } }
                                            )
                                        }
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.Delete,
                                                label = "Hapus Klip",
                                                onClick = {
                                                    selectedClip?.let {
                                                        viewModel.deleteClip(it)
                                                        selectedClip = null
                                                    }
                                                },
                                                color = StudioAccentPink
                                            )
                                        }
                                    }
                                    1 -> {
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.FilterVintage,
                                                label = "Filter Sinematik & LUTs",
                                                onClick = { showFilterSheet = true },
                                                color = StudioPrimaryViolet
                                            )
                                        }
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.Tune,
                                                label = "Adjust Color Grade (HSL)",
                                                onClick = { showAdjustSheet = true },
                                                color = StudioSecondaryTeal
                                            )
                                        }
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.Opacity,
                                                label = "Opasitas & Blend Mode",
                                                onClick = { showOverlaySheet = true },
                                                color = Color(0xFFFFB74D)
                                            )
                                        }
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.FolderOpen,
                                                label = "Impor Custom LUT (.cube)",
                                                onClick = { lutFilePickerLauncher.launch("*/*") },
                                                color = StudioAccentAmber
                                            )
                                        }
                                    }
                                    2 -> {
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.AutoAwesome,
                                                label = "Efek Video & Body VFX",
                                                onClick = { showEffectsSheet = true },
                                                color = StudioPrimaryViolet
                                            )
                                        }
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.CenterFocusWeak,
                                                label = "AI Cutout & Chroma Key",
                                                onClick = { showCutoutSheet = true },
                                                color = StudioSecondaryTeal
                                            )
                                        }
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.GridOn,
                                                label = "Masking Shape Video",
                                                onClick = { showMaskingSheet = true },
                                                color = Color(0xFFFFB74D)
                                            )
                                        }
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.Transform,
                                                label = "Keyframe Studio Video",
                                                onClick = { showKeyframeSheet = true },
                                                color = StudioAccentAmber
                                            )
                                        }
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.SlowMotionVideo,
                                                label = "Animasi Transisi",
                                                onClick = { showAnimationSheet = true },
                                                color = StudioSecondaryTeal
                                            )
                                        }
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.Security,
                                                label = "Stabilisasi AI Gyro",
                                                onClick = { showStabilizeSheet = true }
                                            )
                                        }
                                    }
                                }
                            }
                            com.example.ui.components.TimelineScope.AUDIO_SUBTIMELINE -> {
                                when (selectedToolCategory.coerceIn(0, 2)) {
                                    0 -> {
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.VolumeUp,
                                                label = "Volume & Gain (0-200%)",
                                                onClick = { showAudioStudioSheet = true },
                                                color = StudioAccentPink
                                            )
                                        }
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.GraphicEq,
                                                label = "Fade In & Fade Out",
                                                onClick = { showAudioStudioSheet = true },
                                                color = StudioSecondaryTeal
                                            )
                                        }
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.ContentCut,
                                                label = "Split Audio",
                                                onClick = { selectedClip?.let { viewModel.splitClipAtCurrentTime(it, currentTimeMs) } },
                                                enabled = selectedClip != null && currentTimeMs > (selectedClip?.startTimeMs ?: 0L) && currentTimeMs < (selectedClip?.endTimeMs ?: 0L),
                                                color = StudioSecondaryTeal
                                            )
                                        }
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.Delete,
                                                label = "Hapus Audio",
                                                onClick = {
                                                    selectedClip?.let {
                                                        viewModel.deleteClip(it)
                                                        selectedClip = null
                                                    }
                                                },
                                                color = StudioAccentPink
                                            )
                                        }
                                    }
                                    1 -> {
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.SurroundSound,
                                                label = "AI Denoise Noise Cleaner",
                                                onClick = { showAudioStudioSheet = true },
                                                color = StudioSecondaryTeal
                                            )
                                        }
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.RecordVoiceOver,
                                                label = "Voice FX / Pitch Changer",
                                                onClick = { showAudioStudioSheet = true },
                                                color = StudioSecondaryTeal
                                            )
                                        }
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.Equalizer,
                                                label = "Equalizer Bass & Treble",
                                                onClick = { showAudioStudioSheet = true }
                                            )
                                        }
                                    }
                                    2 -> {
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.MusicNote,
                                                label = "Deteksi Beat Irama Sync",
                                                onClick = { showAudioStudioSheet = true },
                                                color = StudioAccentPink
                                            )
                                        }
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.Speed,
                                                label = "Kecepatan Tempo Musik",
                                                onClick = { showSpeedSheet = true }
                                            )
                                        }
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.LibraryMusic,
                                                label = "Buka Library Musik BGM",
                                                onClick = { showAudioStudioSheet = true },
                                                color = StudioSecondaryTeal
                                            )
                                        }
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.Mic,
                                                label = "Rekam Voiceover Mic",
                                                onClick = { showAudioStudioSheet = true },
                                                color = StudioAccentPink
                                            )
                                        }
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.FastRewind,
                                                label = "Reverse Audio",
                                                onClick = { selectedClip?.let { viewModel.reverseClip(it) } }
                                            )
                                        }
                                    }
                                }
                            }
                            com.example.ui.components.TimelineScope.TEXT_SUBTIMELINE -> {
                                when (selectedToolCategory.coerceIn(0, 2)) {
                                    0 -> {
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.Edit,
                                                label = "Edit Teks Subjudul",
                                                onClick = { showTextDialog = true },
                                                color = StudioSecondaryTeal
                                            )
                                        }
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.Add,
                                                label = "Tambah Subjudul Baru",
                                                onClick = { showTextDialog = true },
                                                color = StudioPrimaryViolet
                                            )
                                        }
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.RecordVoiceOver,
                                                label = "Auto Captions AI (Speech-to-Text)",
                                                onClick = { viewModel.autoTranscribeSubtitles() },
                                                color = StudioAccentAmber
                                            )
                                        }
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.VolumeUp,
                                                label = "Text-to-Speech AI Voice",
                                                onClick = {
                                                    selectedClip?.textContent?.let { text ->
                                                        viewModel.generateAiVoiceover(text, "Studio Neutral")
                                                    } ?: viewModel.generateAiVoiceover("Subjudul video studio otomatis", "Studio Neutral")
                                                },
                                                color = StudioAccentPink
                                            )
                                        }
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.ContentCut,
                                                label = "Split Subjudul",
                                                onClick = { selectedClip?.let { viewModel.splitClipAtCurrentTime(it, currentTimeMs) } },
                                                enabled = selectedClip != null && currentTimeMs > (selectedClip?.startTimeMs ?: 0L) && currentTimeMs < (selectedClip?.endTimeMs ?: 0L),
                                                color = StudioSecondaryTeal
                                            )
                                        }
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.Delete,
                                                label = "Hapus Subjudul",
                                                onClick = {
                                                    selectedClip?.let {
                                                        viewModel.deleteClip(it)
                                                        selectedClip = null
                                                    }
                                                },
                                                color = StudioAccentPink
                                            )
                                        }
                                    }
                                    1 -> {
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.FontDownload,
                                                label = "Pilihan Font Family",
                                                onClick = { showFontTypographySheet = true },
                                                color = StudioSecondaryTeal
                                            )
                                        }
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.FormatSize,
                                                label = "Ukuran Font & Spacing",
                                                onClick = { showFontTypographySheet = true },
                                                color = StudioAccentAmber
                                            )
                                        }
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.FormatAlignLeft,
                                                label = "Rata Posisi Teks",
                                                onClick = { showFontTypographySheet = true }
                                            )
                                        }
                                    }
                                    2 -> {
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.FormatPaint,
                                                label = "Warna Huruf & Gradasi",
                                                onClick = { showFontTypographySheet = true },
                                                color = StudioPrimaryViolet
                                            )
                                        }
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.BorderColor,
                                                label = "Stroke Outline & Shadow",
                                                onClick = { showFontTypographySheet = true }
                                            )
                                        }
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.Animation,
                                                label = "Animasi Teks (Typewriter/Pop/Fade)",
                                                onClick = { showAnimationSheet = true },
                                                color = StudioAccentPink
                                            )
                                        }
                                    }
                                }
                            }
                            com.example.ui.components.TimelineScope.STICKER_SUBTIMELINE -> {
                                when (selectedToolCategory.coerceIn(0, 2)) {
                                    0 -> {
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.EmojiEmotions,
                                                label = "Katalog Stiker FX & Emoji",
                                                onClick = { showStickersSheet = true },
                                                color = Color(0xFFFFB74D)
                                            )
                                        }
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.AddPhotoAlternate,
                                                label = "Tambah Foto Overlay PIP",
                                                onClick = {
                                                    overlayPhotoPickerLauncher.launch(
                                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                    )
                                                },
                                                color = StudioSecondaryTeal
                                            )
                                        }
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.RotateRight,
                                                label = "Transform & Rotasi 90°",
                                                onClick = {
                                                    selectedClip?.let { clip ->
                                                        val newRot = (clip.rotationDegrees + 90) % 360
                                                        viewModel.updateOverlayTransform(clip, newRot, clip.isMirrored, clip.cropRatio)
                                                    }
                                                },
                                                color = StudioAccentAmber
                                            )
                                        }
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.Delete,
                                                label = "Hapus Stiker / Overlay",
                                                onClick = {
                                                    selectedClip?.let {
                                                        viewModel.deleteClip(it)
                                                        selectedClip = null
                                                    }
                                                },
                                                color = StudioAccentPink
                                            )
                                        }
                                    }
                                    1 -> {
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.Opacity,
                                                label = "Opasitas Overlay (0-100%)",
                                                onClick = { showOverlaySheet = true },
                                                color = Color(0xFFFFB74D)
                                            )
                                        }
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.Layers,
                                                label = "Mode Blend Campuran",
                                                onClick = { showOverlaySheet = true },
                                                color = StudioSecondaryTeal
                                            )
                                        }
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.CenterFocusWeak,
                                                label = "AI Cutout Foto Stiker",
                                                onClick = { showCutoutSheet = true },
                                                color = StudioPrimaryViolet
                                            )
                                        }
                                    }
                                    2 -> {
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.Animation,
                                                label = "Animasi Stiker Masuk/Keluar",
                                                onClick = { showAnimationSheet = true },
                                                color = StudioAccentPink
                                            )
                                        }
                                        item {
                                            ActionToolChip(
                                                icon = Icons.Default.Timeline,
                                                label = "Keyframe Gerak Posisi",
                                                onClick = { showKeyframeSheet = true },
                                                color = StudioAccentAmber
                                            )
                                        }
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Hierarchical Multitrack Timeline Canvas
        TimelineView(
            tracks = tracks,
            clips = clips,
            currentTimeMs = currentTimeMs,
            totalDurationMs = totalDurationMs,
            timelineScope = timelineScope,
            selectedClip = selectedClip,
            onScopeChanged = { timelineScope = it },
            onSeek = { viewModel.seekTo(it) },
            onClipSelected = { selectedClip = it },
            onClipMoved = { clipId, newStartMs, newTrackId ->
                viewModel.moveClipPositionAndTrack(clipId, newStartMs, newTrackId)
            },
            onTransitionClicked = { clipA, clipB ->
                transitionClipA = clipA
                transitionClipB = clipB
                showTransitionSheet = true
            },
            onAddClipRequested = { showAddClipSheet = true },
            onAddOverlayTrackRequested = { trackType ->
                viewModel.addNewOverlayTrack(trackType)
            },
            onAddSoundRequested = { showAudioStudioSheet = true },
            onAddTextRequested = { showTextDialog = true },
            onAddStickerRequested = { showStickersSheet = true },
            modifier = Modifier.weight(1f)
        )
    }

    // --- TRANSITION SELECTION MODAL SHEET ---
    if (showTransitionSheet) {
        TransitionSelectionSheet(
            clipA = transitionClipA,
            clipB = transitionClipB,
            currentTransition = transitionClipA?.transitionType ?: "Fade",
            onApplyTransition = { transitionName, applyToAll ->
                if (applyToAll) {
                    viewModel.applyTransitionToAllClips(transitionName)
                } else {
                    transitionClipA?.let { viewModel.updateClipTransition(it, transitionName) }
                }
                showTransitionSheet = false
            },
            onDismiss = { showTransitionSheet = false }
        )
    }

    // --- CAPCUT BOTTOM SHEETS (SLIDE-UP FROM BOTTOM TO TOP) ---

    // 1. Interactive Speed Curve Bottom Sheet
    if (showSpeedSheet && selectedClip != null) {
        val speedSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val curves = listOf("Normal", "Hero", "Bullet Time", "Montage", "Fast Out", "Slow In")
        var selectedCurve by remember { mutableStateOf(selectedClip!!.speedCurve) }
        var currentSpeedMultiplier by remember { mutableStateOf(selectedClip!!.speedMultiplier) }

        ModalBottomSheet(
            onDismissRequest = { showSpeedSheet = false },
            sheetState = speedSheetState,
            containerColor = StudioSurfaceDark,
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = StudioSecondaryTeal)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Speed Ramping dan Kurva Kecepatan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    IconButton(onClick = { showSpeedSheet = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Interactive Custom Curve Canvas View
                com.example.ui.components.SpeedCurveCanvas(
                    curvePreset = selectedCurve,
                    durationMs = selectedClip!!.durationMs,
                    onCurveChanged = { curveName, avgSpeed, _ ->
                        currentSpeedMultiplier = avgSpeed
                        viewModel.updateClipSpeed(selectedClip!!, avgSpeed, curveName)
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Pilih Preset Kurva Kecepatan:", color = StudioSecondaryTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(curves) { curve ->
                        FilterChip(
                            selected = selectedCurve == curve,
                            onClick = {
                                selectedCurve = curve
                                viewModel.updateClipSpeed(selectedClip!!, currentSpeedMultiplier, curve)
                            },
                            label = { Text(curve, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { showSpeedSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = StudioPrimaryViolet)
                ) {
                    Text("Terapkan Kurva Kecepatan", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // 2. Animation Bottom Sheet
    if (showAnimationSheet && selectedClip != null) {
        val animsIn = listOf("None", "Kedip / Flash In", "Kedip Cepat Blink", "Kedip Disko Strobe", "Fade In", "Slide Right", "Zoom In", "Bounce In", "Rotate Entrance")
        val animsOut = listOf("None", "Kedip / Flash Out", "Kedip Cepat Blink", "Fade Out", "Slide Left", "Zoom Out", "Dissolve Out", "Glitch Exit")
        val animsCombo = listOf("None", "Kedip Kedip Strobe", "Flash Kedip Pulse", "Kedip Invert Blink", "Spin & Zoom", "Glitch Bounce", "3D Flip", "Elastic Pop")

        var currentIn by remember { mutableStateOf(selectedClip!!.animationIn) }
        var currentOut by remember { mutableStateOf(selectedClip!!.animationOut) }
        var currentCombo by remember { mutableStateOf(selectedClip!!.animationCombo) }

        ModalBottomSheet(
            onDismissRequest = { showAnimationSheet = false },
            containerColor = StudioCardBg,
            contentColor = StudioTextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .navigationBarsPadding()
            ) {
                Text("Animasi Studio In Out dan Combo", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = StudioTextPrimary)
                Spacer(modifier = Modifier.height(12.dp))

                Text("Animasi Masuk In:", color = StudioSecondaryTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(animsIn) { anim ->
                        FilterChip(
                            selected = currentIn == anim,
                            onClick = {
                                currentIn = anim
                                viewModel.updateClipAnimation(selectedClip!!, currentIn, currentOut, currentCombo)
                            },
                            label = { Text(anim) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Animasi Keluar Out:", color = StudioAccentPink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(animsOut) { anim ->
                        FilterChip(
                            selected = currentOut == anim,
                            onClick = {
                                currentOut = anim
                                viewModel.updateClipAnimation(selectedClip!!, currentIn, currentOut, currentCombo)
                            },
                            label = { Text(anim) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Animasi Combo:", color = StudioAccentAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(animsCombo) { anim ->
                        FilterChip(
                            selected = currentCombo == anim,
                            onClick = {
                                currentCombo = anim
                                viewModel.updateClipAnimation(selectedClip!!, currentIn, currentOut, currentCombo)
                            },
                            label = { Text(anim) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { showAnimationSheet = false }, modifier = Modifier.fillMaxWidth()) {
                    Text("Selesai")
                }
            }
        }
    }

    // 3. Crop & Canvas Bottom Sheet
    if (showCropRotateSheet && selectedClip != null) {
        val crops = listOf("16:9", "9:16", "1:1", "4:5", "3:4", "21:9", "Free")
        ModalBottomSheet(
            onDismissRequest = { showCropRotateSheet = false },
            containerColor = StudioSurfaceDark,
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .navigationBarsPadding()
            ) {
                Text("Crop, Canvas & Rotation Studio", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))

                Text("Rasio Aspect Canvas:", color = StudioTextSecondary, fontSize = 12.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(crops) { crop ->
                        FilterChip(
                            selected = selectedClip!!.cropRatio == crop,
                            onClick = {
                                viewModel.updateClipCropAndRotate(selectedClip!!, crop, 0)
                            },
                            label = { Text(crop) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { viewModel.updateClipCropAndRotate(selectedClip!!, selectedClip!!.cropRatio, 90) },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioPrimaryViolet)
                    ) {
                        Icon(imageVector = Icons.Default.RotateRight, contentDescription = "Rotate")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Rotasi +90°")
                    }

                    Button(
                        onClick = { viewModel.mirrorClip(selectedClip!!) },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioSecondaryTeal)
                    ) {
                        Icon(imageVector = Icons.Default.Flip, contentDescription = "Flip")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mirror Horizontal")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { showCropRotateSheet = false }, modifier = Modifier.fillMaxWidth()) {
                    Text("Tutup")
                }
            }
        }
    }

    // 4. Keyframe Bottom Sheet (Adjustable Keyframe Position/Scale/Rotation Engine)
    if (showKeyframeSheet && selectedClip != null) {
        var posX by remember { mutableStateOf(0f) }
        var posY by remember { mutableStateOf(0f) }
        var scale by remember { mutableStateOf(1.0f) }
        var rotation by remember { mutableStateOf(selectedClip!!.rotationDegrees.toFloat()) }
        var opacity by remember { mutableStateOf(1.0f) }
        var easeCurve by remember { mutableStateOf("Ease In Out") }

        ModalBottomSheet(
            onDismissRequest = { showKeyframeSheet = false },
            containerColor = StudioSurfaceDark,
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Transform, contentDescription = null, tint = StudioAccentAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Keyframe Interactive Studio", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Surface(
                        shape = CircleShape,
                        color = StudioAccentAmber.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, StudioAccentAmber)
                    ) {
                        Text("Timestamp: ${currentTimeMs}ms", color = StudioAccentAmber, fontSize = 10.sp, modifier = Modifier.padding(6.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Position X Offset: ${posX.toInt()}px", fontSize = 12.sp, color = Color.White)
                Slider(value = posX, onValueChange = { posX = it }, valueRange = -500f..500f)

                Text("Position Y Offset: ${posY.toInt()}px", fontSize = 12.sp, color = Color.White)
                Slider(value = posY, onValueChange = { posY = it }, valueRange = -500f..500f)

                Text("Scale Zoom: ${String.format("%.2f", scale)}x", fontSize = 12.sp, color = StudioSecondaryTeal)
                Slider(value = scale, onValueChange = { scale = it }, valueRange = 0.2f..3.0f)

                Text("Rotation Angle: ${rotation.toInt()}°", fontSize = 12.sp, color = StudioPrimaryViolet)
                Slider(value = rotation, onValueChange = { rotation = it }, valueRange = 0f..360f)

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.updateClipKeyframeTransform(
                                clip = selectedClip!!,
                                posX = posX,
                                posY = posY,
                                scale = scale,
                                rotation = rotation,
                                opacity = opacity,
                                ease = easeCurve
                            )
                            showKeyframeSheet = false
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = StudioAccentAmber)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Simpan Keyframe", fontWeight = FontWeight.Bold)
                    }

                    if (selectedClip!!.hasKeyframe) {
                        OutlinedButton(
                            onClick = {
                                viewModel.addOrRemoveKeyframe(selectedClip!!, "")
                                showKeyframeSheet = false
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioAccentPink)
                        ) {
                            Text("Hapus Keyframe")
                        }
                    }
                }
            }
        }
    }

    // 5. Video & Body Effects Bottom Sheet
    if (showEffectsSheet && selectedClip != null) {
        val videoEffects = listOf("None", "Pembalik Warna Invert FX", "Kedip Strobe Flash", "Retro VHS", "Cyber Glitch", "Light Leak", "Film Grain", "Neon Edge")
        val bodyEffects = listOf("None", "Aura Glow", "Cyber Eyes", "Lightning Wings", "Halo Ring")

        ModalBottomSheet(
            onDismissRequest = { showEffectsSheet = false },
            containerColor = StudioSurfaceDark,
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .navigationBarsPadding()
            ) {
                Text("Efek Video & AI Body Effects", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))

                Text("Efek Video Visual:", color = StudioSecondaryTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(videoEffects) { fx ->
                        FilterChip(
                            selected = selectedClip!!.effectName == fx,
                            onClick = { viewModel.updateClipEffects(selectedClip!!, fx, selectedClip!!.bodyEffectName) },
                            label = { Text(fx) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Efek Tubuh / Body FX AI:", color = StudioAccentAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(bodyEffects) { bfx ->
                        FilterChip(
                            selected = selectedClip!!.bodyEffectName == bfx,
                            onClick = { viewModel.updateClipEffects(selectedClip!!, selectedClip!!.effectName, bfx) },
                            label = { Text(bfx) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { showEffectsSheet = false }, modifier = Modifier.fillMaxWidth()) {
                    Text("Tutup")
                }
            }
        }
    }

    // 0. Tambah Klip Bottom Sheet (Support Gallery, AI Generator & Stock Samples)
    if (showAddClipSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddClipSheet = false },
            containerColor = StudioSurfaceDark,
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.VideoCall, contentDescription = null, tint = StudioPrimaryViolet)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tambah Klip ke Timeline", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                    IconButton(onClick = { showAddClipSheet = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Option 1: Dari Galeri HP
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showAddClipSheet = false
                            mediaPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        }
                        .border(1.dp, StudioPrimaryViolet, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = StudioCardBg)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = StudioPrimaryViolet.copy(alpha = 0.2f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, tint = StudioPrimaryViolet)
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Dari Galeri Device / HP", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                            Text("Pilih video atau foto lokal dari penyimpanan HP", color = StudioTextSecondary, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Option 2: AI Video Generator
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showAddClipSheet = false
                            viewModel.selectTab(MainTab.STUDIO_GENERATOR)
                        }
                        .border(1.dp, StudioSecondaryTeal, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = StudioCardBg)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = StudioSecondaryTeal.copy(alpha = 0.2f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = StudioSecondaryTeal)
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("AI Video Generator Studio", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                            Text("Buat klip video AI otomatis menggunakan text-to-video / image", color = StudioTextSecondary, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Pilih Klip Sampel Studio Instant:", color = StudioAccentAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                val sampleClips = listOf(
                    Pair("Cyberpunk Night City", "studio_sample_cyberpunk_night"),
                    Pair("Futuristic Drone Sunset", "studio_sample_drone_sunset"),
                    Pair("Anime Sky & Clouds", "studio_sample_anime_sky"),
                    Pair("Ocean Waves Sunset", "studio_sample_ocean_sunset"),
                    Pair("Matrix Particle Glow", "studio_sample_matrix_particle")
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sampleClips) { (title, uri) ->
                        OutlinedButton(
                            onClick = {
                                viewModel.addMediaFromGallery(uri, title)
                                showAddClipSheet = false
                            },
                            border = BorderStroke(1.dp, StudioAccentAmber.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(imageVector = Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(16.dp), tint = StudioAccentAmber)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(title, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Tool Asset Store Sheet (Overlay Assets & Stickers)
    if (showAssetStoreSheet) {
        var assetTab by remember { mutableStateOf(0) } // 0: Overlay FX Assets, 1: Stickers

        val overlayAssets = listOf(
            Pair("Light Leak Vintage FX", "Light Leak"),
            Pair("Film Grain 35mm Overlay", "Film Grain"),
            Pair("Atmospheric Rain & Dust", "Rain Dust"),
            Pair("Cyber Neon Frame Border", "Neon Frame"),
            Pair("VHS Glitch Scanlines", "VHS Glitch"),
            Pair("Fire Sparks & Smoke", "Fire Smoke"),
            Pair("Snow Particles Glow", "Snow Particles")
        )

        val stickerAssets = listOf(
            Pair("Lencana Subscribe & Like", "Badge Subscribe"),
            Pair("Lencana Verifikasi Centang Blue", "Verifikasi Centang"),
            Pair("Stiker Neon Arrow Panah", "Neon Arrow"),
            Pair("Grafis Flame Api", "Flame Api"),
            Pair("Cinema Clapboard", "Clapboard"),
            Pair("Lightning Flash", "Flash Lightning"),
            Pair("Star Burst Element", "Star Burst"),
            Pair("Boom Explosion", "Boom Explosion")
        )

        ModalBottomSheet(
            onDismissRequest = { showAssetStoreSheet = false },
            containerColor = StudioSurfaceDark,
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Category, contentDescription = null, tint = StudioAccentAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tool Asset & Overlay Library", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    IconButton(onClick = { showAssetStoreSheet = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TabRow(
                    selectedTabIndex = assetTab,
                    containerColor = Color.Transparent,
                    contentColor = StudioAccentAmber
                ) {
                    Tab(
                        selected = assetTab == 0,
                        onClick = { assetTab = 0 },
                        text = { Text("Overlay FX", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = assetTab == 1,
                        onClick = { assetTab = 1 },
                        text = { Text("Stiker", fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (assetTab == 0) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text("Pilih Asset Overlay PIP Picture in Picture:", color = StudioTextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        overlayAssets.forEach { (title, effectKey) ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        val projId = activeProject?.id ?: return@clickable
                                        viewModel.addNewOverlayTrack("VIDEO", "Overlay $title")
                                        viewModel.addClip(
                                            TimelineClipEntity(
                                                trackId = 0,
                                                projectId = projId,
                                                title = "Overlay: $title",
                                                mediaUri = "overlay_$effectKey",
                                                startTimeMs = currentTimeMs,
                                                endTimeMs = currentTimeMs + 5000L,
                                                durationMs = 5000L,
                                                effectName = effectKey
                                            )
                                        )
                                        showAssetStoreSheet = false
                                    },
                                colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                                border = BorderStroke(1.dp, StudioCardBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Layers, contentDescription = null, tint = StudioSecondaryTeal)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = StudioSecondaryTeal.copy(alpha = 0.2f)
                                    ) {
                                        Text("+ Tambah PIP", color = StudioSecondaryTeal, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text("Pilih Stiker & Motion Graphics Studio:", color = StudioTextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        stickerAssets.forEach { (title, stickerKey) ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        viewModel.addStickerClip(title)
                                        showAssetStoreSheet = false
                                    },
                                colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                                border = BorderStroke(1.dp, StudioCardBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.EmojiEmotions, contentDescription = null, tint = StudioAccentAmber)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = StudioAccentAmber.copy(alpha = 0.2f)
                                    ) {
                                        Text("+ Pasang Stiker", color = StudioAccentAmber, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Button(onClick = { showAssetStoreSheet = false }, modifier = Modifier.fillMaxWidth()) {
                    Text("Tutup Tool Asset")
                }
            }
        }
    }

    // 6. Filter Grading VFX & Custom LUTs Bottom Sheet
    if (showFilterSheet) {
        val targetClip = selectedClip ?: clips.firstOrNull()
        val presetFilters = listOf(
            "None",
            "Pembalik Warna Invert Colors",
            "Pembalik Warna RGB Negative",
            "Teal & Orange Hollywood",
            "Moody Dark Film",
            "Vintage 35mm Grain",
            "Cyberpunk Neon Glow",
            "Warm Sunset Amber",
            "HDR Ultra Clarity",
            "B&W Noir Dramatic"
        )
        var filterTab by remember { mutableStateOf(0) } // 0: Presets, 1: Custom LUTs

        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            containerColor = StudioSurfaceDark,
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.FilterVintage, contentDescription = null, tint = StudioSecondaryTeal)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Filter Color Grading & Custom LUTs", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    IconButton(onClick = { showFilterSheet = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TabRow(
                    selectedTabIndex = filterTab,
                    containerColor = Color.Transparent,
                    contentColor = StudioSecondaryTeal
                ) {
                    Tab(
                        selected = filterTab == 0,
                        onClick = { filterTab = 0 },
                        text = { Text("Preset Grading", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = filterTab == 1,
                        onClick = { filterTab = 1 },
                        text = { Text("Custom LUTs • ${customLuts.size}", fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (filterTab == 0) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        presetFilters.forEach { filter ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        targetClip?.let {
                                            viewModel.updateClipFilter(it, filter)
                                        }
                                        showFilterSheet = false
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = targetClip?.filterName == filter,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(selectedColor = StudioSecondaryTeal)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(filter, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                        }
                    }
                } else {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        // Import LUT Button
                        Button(
                            onClick = {
                                lutFilePickerLauncher.launch("*/*")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = StudioPrimaryViolet),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.FileUpload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Impor File LUT .cube / .3dl", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Daftar LUT Tersedia:", color = StudioTextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        customLuts.forEach { lut ->
                            val lutFilterName = "LUT: ${lut.name}"
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        targetClip?.let {
                                            viewModel.updateClipFilter(it, lutFilterName)
                                        }
                                        showFilterSheet = false
                                    },
                                colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                                border = BorderStroke(
                                    1.dp,
                                    if (targetClip?.filterName == lutFilterName) StudioSecondaryTeal else StudioCardBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.ColorLens, contentDescription = null, tint = StudioSecondaryTeal)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(lut.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(lut.description, color = StudioTextSecondary, fontSize = 11.sp)
                                        }
                                    }
                                    if (targetClip?.filterName == lutFilterName) {
                                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Active", tint = StudioSecondaryTeal)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { showFilterSheet = false }, modifier = Modifier.fillMaxWidth()) {
                    Text("Selesai")
                }
            }
        }
    }

    // 7. Color Adjust & Grading Bottom Sheet
    if (showAdjustSheet && selectedClip != null) {
        var brightness by remember { mutableStateOf(selectedClip!!.brightness) }
        var contrast by remember { mutableStateOf(selectedClip!!.contrast) }
        var saturation by remember { mutableStateOf(selectedClip!!.saturation) }
        var temperature by remember { mutableStateOf(selectedClip!!.temperature) }
        var vignette by remember { mutableStateOf(selectedClip!!.vignette) }

        ModalBottomSheet(
            onDismissRequest = { showAdjustSheet = false },
            containerColor = StudioSurfaceDark,
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
            ) {
                Text("Color Grading Studio", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))

                Text("Brightness: ${(brightness * 100).toInt()}%", fontSize = 12.sp, color = Color.White)
                Slider(value = brightness, onValueChange = { brightness = it; viewModel.updateClipAdjustments(selectedClip!!, brightness, contrast, saturation, temperature, vignette) }, valueRange = -0.5f..0.5f)

                Text("Contrast: ${(contrast * 100).toInt()}%", fontSize = 12.sp, color = Color.White)
                Slider(value = contrast, onValueChange = { contrast = it; viewModel.updateClipAdjustments(selectedClip!!, brightness, contrast, saturation, temperature, vignette) }, valueRange = 0.5f..1.5f)

                Text("Saturation: ${(saturation * 100).toInt()}%", fontSize = 12.sp, color = Color.White)
                Slider(value = saturation, onValueChange = { saturation = it; viewModel.updateClipAdjustments(selectedClip!!, brightness, contrast, saturation, temperature, vignette) }, valueRange = 0.0f..2.0f)

                Text("Vignette: ${(vignette * 100).toInt()}%", fontSize = 12.sp, color = StudioSecondaryTeal)
                Slider(value = vignette, onValueChange = { vignette = it; viewModel.updateClipAdjustments(selectedClip!!, brightness, contrast, saturation, temperature, vignette) }, valueRange = 0.0f..1.0f)

                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { showAdjustSheet = false }, modifier = Modifier.fillMaxWidth()) {
                    Text("Selesai")
                }
            }
        }
    }

    // 8. Cutout Bottom Sheet
    if (showCutoutSheet && selectedClip != null) {
        val cutoutModes = listOf("None", "Auto AI Cutout Hapus BG", "Chroma Key Layar Hijau", "Smart Portrait Segmentation")
        ModalBottomSheet(
            onDismissRequest = { showCutoutSheet = false },
            containerColor = StudioSurfaceDark,
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .navigationBarsPadding()
            ) {
                Text("Cutout & Smart Remove Background", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))

                cutoutModes.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.updateClipCutout(selectedClip!!, mode)
                                showCutoutSheet = false
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedClip!!.cutoutMode == mode, onClick = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(mode, color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { showCutoutSheet = false }, modifier = Modifier.fillMaxWidth()) {
                    Text("Tutup")
                }
            }
        }
    }

    // 9. Audio Studio Bottom Sheet
    if (showAudioStudioSheet) {
        val sfxList = listOf("Whoosh Transit", "Cinematic Impact", "SFX Pop", "Applause Crowd", "AI Voiceover", "Ambient Rain")
        var masterVol by remember { mutableStateOf(selectedClip?.volume ?: 1.0f) }
        var isDenoise by remember { mutableStateOf(selectedClip?.noiseReduction ?: false) }

        ModalBottomSheet(
            onDismissRequest = { showAudioStudioSheet = false },
            containerColor = StudioSurfaceDark,
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
            ) {
                Text("Audio Studio & SFX Mixer", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))

                selectedClip?.let { clip ->
                    Text("Volume Klip: ${(masterVol * 100).toInt()}%", fontSize = 12.sp, color = Color.White)
                    Slider(
                        value = masterVol,
                        onValueChange = {
                            masterVol = it
                            viewModel.updateClipAudioDetails(clip, masterVol, clip.audioFadeInSec, clip.audioFadeOutSec, clip.audioPitch, isDenoise, clip.vocalEnhance)
                        },
                        valueRange = 0.0f..2.0f
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Tambah Efek Suara SFX:", color = StudioSecondaryTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                sfxList.forEach { sfx ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.addAudioOrVoiceover(sfx, sfx, sfx == "AI Voiceover")
                                showAudioStudioSheet = false
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Audiotrack, contentDescription = sfx, tint = StudioSecondaryTeal)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(sfx, color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { showAudioStudioSheet = false }, modifier = Modifier.fillMaxWidth()) {
                    Text("Tutup")
                }
            }
        }
    }

    // 13. Add / Edit Text Subtitle Dialog
    if (showTextDialog) {
        var textInput by remember { mutableStateOf(selectedClip?.textContent ?: "SUBJUDUL VIDEO STUDIO") }
        AlertDialog(
            onDismissRequest = { showTextDialog = false },
            title = {
                Text(
                    if (selectedClip?.textContent != null) "Edit Teks Subjudul" else "Tambah Teks Subjudul Studio",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("Teks Subjudul") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val activeClip = selectedClip
                        if (activeClip != null && activeClip.textContent != null) {
                            viewModel.updateClipTextContent(activeClip, textInput)
                        } else {
                            val projId = activeProject?.id ?: return@Button
                            val textTrack = tracks.find { it.trackType == "TEXT" }
                            if (textTrack != null) {
                                viewModel.addClip(
                                    TimelineClipEntity(
                                        trackId = textTrack.id,
                                        projectId = projId,
                                        title = textInput.take(20),
                                        mediaUri = "",
                                        startTimeMs = currentTimeMs,
                                        endTimeMs = currentTimeMs + 3000L,
                                        durationMs = 3000L,
                                        textContent = textInput
                                    )
                                )
                            }
                        }
                        showTextDialog = false
                    }
                ) {
                    Text(if (selectedClip?.textContent != null) "Simpan" else "Tambah")
                }
            }
        )
    }

    // 14. Stickers Sheet
    if (showStickersSheet) {
        val stickers = listOf("Badge Subscribe", "Lencana Verifikasi", "Stiker Neon Arrow", "Elemen Grafis Star", "Garis Aksen Highlight", "Lencana Kualitas HD")
        AlertDialog(
            onDismissRequest = { showStickersSheet = false },
            title = { Text("Stiker dan Hiasan Animasi", fontWeight = FontWeight.Bold, color = StudioTextPrimary) },
            text = {
                Column {
                    stickers.forEach { sticker ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.addStickerClip(sticker)
                                    showStickersSheet = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = sticker, tint = StudioAccentAmber)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(sticker, color = StudioTextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStickersSheet = false }) { Text("Tutup") }
            }
        )
    }

    // 15. Low-Resolution Proxy Preview Mode Settings Dialog
    if (showProxyDialog) {
        val proxyResolutions = listOf(
            "360p Proxy" to "360p Proxy Super Mulus Beban GPU Minimal",
            "540p Proxy" to "540p Proxy Seimbang Performa dan Visual",
            "720p Proxy" to "720p HD Proxy Detail Lebih Tinggi",
            "1080p Original" to "1080p Full HD Original Proxy Nonaktif"
        )

        AlertDialog(
            onDismissRequest = { showProxyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = StudioSecondaryTeal)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Low-Resolution Proxy Preview", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = StudioTextPrimary)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Mode Proxy secara otomatis membuat pratinjau resolusi rendah untuk pengeditan timeline 60 FPS tanpa lag pada klip video. File asli tetap digunakan saat Export.",
                        color = StudioTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Pilih Quality Proxy Pratinjau:", color = StudioSecondaryTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    proxyResolutions.forEach { (resKey, resLabel) ->
                        val isSelected = (isProxyMode && proxyResolution.contains(resKey.take(4))) || (!isProxyMode && resKey.contains("1080p"))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.setProxyResolution(resKey)
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.setProxyResolution(resKey) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = resLabel,
                                color = if (isSelected) StudioTextPrimary else StudioTextSecondary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                        border = BorderStroke(1.dp, StudioSecondaryTeal.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Status Mode Timeline", color = StudioTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = if (isProxyMode) "Proxy Mode $proxyResolution" else "Original 1080p Quality",
                                        color = if (isProxyMode) StudioSecondaryTeal else StudioAccentAmber,
                                        fontSize = 11.sp
                                    )
                                }
                                Switch(
                                    checked = isProxyMode,
                                    onCheckedChange = { viewModel.toggleProxyMode(it) }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = StudioCardBorder)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Auto Transcode Impor", color = StudioTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "Konversi otomatis klip baru di latar belakang",
                                        color = StudioTextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                                Switch(
                                    checked = autoTranscode,
                                    onCheckedChange = { viewModel.toggleAutoTranscode(it) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Background Transcoder Jobs ${transcodingJobs.size}",
                            color = StudioSecondaryTeal,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { viewModel.transcodeAllClipsToProxy() }) {
                            Text("Transcode Semua", fontSize = 11.sp, color = StudioPrimaryViolet)
                        }
                    }

                    if (transcodingJobs.isEmpty()) {
                        Text(
                            text = "Belum ada tugas transcoding aktif. Impor file baru untuk diproses secara otomatis.",
                            color = StudioTextSecondary,
                            fontSize = 11.sp
                        )
                    } else {
                        transcodingJobs.take(3).forEach { job ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                                border = BorderStroke(1.dp, StudioCardBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(job.mediaTitle, color = StudioTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = if (job.isCompleted) "Proxy Ready" else "${job.progressPercent}%",
                                            color = if (job.isCompleted) StudioSecondaryTeal else StudioAccentAmber,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { job.progressPercent / 100f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = if (job.isCompleted) StudioSecondaryTeal else StudioAccentAmber,
                                        trackColor = StudioSurfaceLight
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showProxyDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioPrimaryViolet)
                ) {
                    Text("Selesai")
                }
            }
        )
    }

    // 16. Unified Overlay Studio Sheet (Photos & Stickers)
    if (showOverlaySheet) {
        val targetClip = selectedClip ?: clips.find { it.stickerIcon != "None" || it.mediaUri.contains("photo") || it.mediaUri.contains("image") }
        var currentOpacity by remember { mutableStateOf(targetClip?.opacity ?: 1.0f) }
        var currentBlendMode by remember { mutableStateOf(targetClip?.blendMode ?: "Normal") }
        val blendModes = listOf("Normal", "Screen", "Multiply", "Overlay", "Add", "Soft Light", "Darken")

        ModalBottomSheet(
            onDismissRequest = { showOverlaySheet = false },
            containerColor = StudioSurfaceDark,
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Layers, contentDescription = null, tint = Color(0xFFFFB74D))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pengaturan Overlay (Foto & Stiker)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    IconButton(onClick = { showOverlaySheet = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Opacity Slider
                Text("Opasitas & Transparansi: ${(currentOpacity * 100).toInt()}%", color = StudioSecondaryTeal, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = currentOpacity,
                    onValueChange = {
                        currentOpacity = it
                        targetClip?.let { clip ->
                            viewModel.updateOverlayBlend(clip, currentOpacity, currentBlendMode)
                        }
                    },
                    valueRange = 0.0f..1.0f
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Blend Modes
                Text("Mode Blend Campuran:", color = StudioAccentAmber, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(blendModes) { mode ->
                        FilterChip(
                            selected = currentBlendMode == mode,
                            onClick = {
                                currentBlendMode = mode
                                targetClip?.let { clip ->
                                    viewModel.updateOverlayBlend(clip, currentOpacity, mode)
                                }
                            },
                            label = { Text(mode, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFFB74D),
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Transform & Rotation
                Text("Transform & Orientasi:", color = StudioPrimaryViolet, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            targetClip?.let { clip ->
                                val newRot = (clip.rotationDegrees + 90) % 360
                                viewModel.updateOverlayTransform(clip, newRot, clip.isMirrored, clip.cropRatio)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = StudioCardBg),
                        border = BorderStroke(1.dp, StudioCardBorder)
                    ) {
                        Icon(Icons.Default.RotateRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Putar 90°", fontSize = 12.sp, color = Color.White)
                    }

                    Button(
                        onClick = {
                            targetClip?.let { clip ->
                                viewModel.updateOverlayTransform(clip, clip.rotationDegrees, !clip.isMirrored, clip.cropRatio)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = StudioCardBg),
                        border = BorderStroke(1.dp, StudioCardBorder)
                    ) {
                        Icon(Icons.Default.Flip, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Flip Mirror", fontSize = 12.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            showOverlaySheet = false
                            showCutoutSheet = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("AI Cutout / Chroma", fontSize = 11.sp, color = StudioSecondaryTeal)
                    }

                    OutlinedButton(
                        onClick = {
                            showOverlaySheet = false
                            showAnimationSheet = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Animasi Gerak", fontSize = 11.sp, color = Color(0xFFFFB74D))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = { showOverlaySheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = StudioPrimaryViolet)
                ) {
                    Text("Selesai")
                }
            }
        }
    }

    // 17. Font & Typography Studio Sheet
    if (showFontTypographySheet) {
        val targetClip = selectedClip ?: clips.find { it.textContent != null }
        val fontFamilies = listOf(
            "Inter", "Montserrat", "Bebas Neue", "Playfair Display",
            "Roboto Mono", "Oswald", "Cinzel", "Pacifico", "Poppins", "Outfit"
        )
        val textColors = listOf(
            Color.White to "Putih Bersih",
            Color(0xFFFFD54F) to "Kuning Emas",
            StudioSecondaryTeal to "Teal Neon",
            StudioAccentPink to "Pink Magenta",
            Color(0xFFFF5252) to "Merah Terang",
            StudioPrimaryViolet to "Violet Modern",
            Color(0xFF69F0AE) to "Hijau Mint"
        )
        var selectedFont by remember { mutableStateOf(targetClip?.fontFamily ?: "Inter") }
        var selectedFontSize by remember { mutableStateOf(targetClip?.fontSize ?: 24) }

        ModalBottomSheet(
            onDismissRequest = { showFontTypographySheet = false },
            containerColor = StudioSurfaceDark,
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.FontDownload, contentDescription = null, tint = StudioSecondaryTeal)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Font Family & Tipografi Subjudul", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    IconButton(onClick = { showFontTypographySheet = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Font Family Selection
                Text("Pilihan Font Family:", color = StudioSecondaryTeal, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(fontFamilies) { font ->
                        FilterChip(
                            selected = selectedFont == font,
                            onClick = {
                                selectedFont = font
                                targetClip?.let { clip ->
                                    viewModel.updateClipTypography(clip, font, selectedFontSize, clip.fontColor, clip.textAlignment)
                                }
                            },
                            label = { Text(font, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StudioSecondaryTeal,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Font Size Slider
                Text("Ukuran Huruf Font: ${selectedFontSize}sp", color = StudioPrimaryViolet, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = selectedFontSize.toFloat(),
                    onValueChange = {
                        selectedFontSize = it.toInt()
                        targetClip?.let { clip ->
                            viewModel.updateClipTypography(clip, selectedFont, selectedFontSize, clip.fontColor, clip.textAlignment)
                        }
                    },
                    valueRange = 12f..64f
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Text Color Selection
                Text("Pilihan Warna Teks:", color = StudioAccentAmber, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(textColors) { (color, name) ->
                        Surface(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable {
                                    targetClip?.let { clip ->
                                        val hexColor = "#" + Integer.toHexString(color.hashCode()).takeLast(6)
                                        viewModel.updateClipTypography(clip, selectedFont, selectedFontSize, hexColor, clip.textAlignment)
                                    }
                                },
                            color = color,
                            border = BorderStroke(2.dp, StudioCardBorder)
                        ) {}
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Alignment
                Text("Rata Posisi Teks:", color = StudioTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("LEFT" to Icons.Default.FormatAlignLeft, "CENTER" to Icons.Default.FormatAlignCenter, "RIGHT" to Icons.Default.FormatAlignRight).forEach { (align, icon) ->
                        OutlinedButton(
                            onClick = {
                                targetClip?.let { clip ->
                                    viewModel.updateClipTypography(clip, selectedFont, selectedFontSize, clip.fontColor, align)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(icon, contentDescription = align, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showFontTypographySheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = StudioSecondaryTeal)
                ) {
                    Text("Terapkan Tipografi")
                }
            }
        }
    }

    if (showExportSheet) {
        ModalBottomSheet(
            onDismissRequest = { showExportSheet = false },
            containerColor = StudioDarkBg,
            contentColor = Color.White,
            modifier = Modifier.testTag("export_studio_bottom_sheet")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
            ) {
                ExportStudioScreen(viewModel = viewModel)
            }
        }
    }

    // --- CANVAS ASPECT RATIO BOTTOM SHEET ---
    if (showRatioSheet) {
        ModalBottomSheet(
            onDismissRequest = { showRatioSheet = false },
            containerColor = StudioSurfaceDark,
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AspectRatio,
                        contentDescription = null,
                        tint = StudioAccentAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Ukuran Rasio Kanvas",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                val ratios = listOf(
                    Triple("16:9", "Landscape Cinema & YouTube", Icons.Default.CropLandscape),
                    Triple("9:16", "Portrait TikTok / Reels / Shorts", Icons.Default.CropPortrait),
                    Triple("1:1", "Square Instagram Feed", Icons.Default.CropSquare),
                    Triple("4:5", "Vertical Social Feed", Icons.Default.Crop54),
                    Triple("21:9", "Ultrawide Anamorphic", Icons.Default.Crop169)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ratios.forEach { (ratio, label, icon) ->
                        val isSelected = (activeProject?.aspectRatio ?: "16:9") == ratio
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    viewModel.updateProjectAspectRatio(ratio)
                                    showRatioSheet = false
                                },
                            color = if (isSelected) StudioPrimaryViolet.copy(alpha = 0.25f) else StudioCardBg,
                            border = BorderStroke(1.dp, if (isSelected) StudioPrimaryViolet else StudioCardBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) StudioPrimaryViolet else Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ratio,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (isSelected) StudioPrimaryViolet else Color.White
                                    )
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        color = StudioTextSecondary
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = StudioPrimaryViolet,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // --- CANVAS BACKGROUND BOTTOM SHEET ---
    if (showCanvasBgSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCanvasBgSheet = false },
            containerColor = StudioSurfaceDark,
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Wallpaper,
                        contentDescription = null,
                        tint = Color(0xFF64B5F6),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Background Kanvas Video",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Warna Solid Background:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudioSecondaryTeal
                )
                Spacer(modifier = Modifier.height(8.dp))

                val solidColors = listOf(
                    Color(0xFF000000) to "Black",
                    Color(0xFF0F172A) to "Slate",
                    Color(0xFF0F0F1A) to "Studio Dark",
                    Color(0xFF1E1035) to "Deep Purple",
                    Color(0xFF0A192F) to "Navy Blue",
                    Color(0xFF2D124D) to "Neon Violet",
                    Color(0xFF330C12) to "Burgundy",
                    Color(0xFFFFFFFF) to "Pure White"
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(solidColors) { (color, name) ->
                        val isSelected = canvasBackgroundColor == color
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { canvasBackgroundColor = color }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        if (isSelected) 3.dp else 1.dp,
                                        if (isSelected) StudioPrimaryViolet else StudioCardBorder,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = if (color == Color.White) Color.Black else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = name,
                                fontSize = 10.sp,
                                color = if (isSelected) StudioPrimaryViolet else StudioTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Canvas Blur Effect Simulation
                Text(
                    text = "Efek Blur Background Video (${canvasBlurRadius.toInt()}%):",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudioSecondaryTeal
                )
                Spacer(modifier = Modifier.height(6.dp))
                Slider(
                    value = canvasBlurRadius,
                    onValueChange = { canvasBlurRadius = it },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF64B5F6),
                        activeTrackColor = Color(0xFF64B5F6)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { showCanvasBgSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = StudioPrimaryViolet)
                ) {
                    Text("Terapkan Background", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun MainToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
    testTag: String = ""
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag),
        color = if (enabled) color.copy(alpha = 0.15f) else StudioSurfaceDark,
        border = BorderStroke(1.dp, if (enabled) color.copy(alpha = 0.6f) else StudioCardBorder),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) color else StudioTextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) Color.White else StudioTextSecondary
            )
        }
    }
}

@Composable
fun StatusBadge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, color)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun ActionToolChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    color: Color = StudioPrimaryViolet
) {
    Surface(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        color = if (enabled) color.copy(alpha = 0.2f) else StudioSurfaceDark,
        border = BorderStroke(1.dp, if (enabled) color else StudioCardBorder)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) color else StudioTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
