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
import com.example.ui.components.VideoPlayerView
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
    var activeColumnMode by remember { mutableStateOf("ALL") } // "ALL", "TEXT", "AUDIO", "STICKER", "VIDEO"
    var selectedToolCategory by remember { mutableStateOf(0) } // 0: Utama, 1: VFX & Gaya, 2: Lanjutan & AI
    var isToolsExpanded by remember { mutableStateOf(false) } // Default hidden to give maximum timeline space

    LaunchedEffect(selectedClip) {
        selectedClip?.let { clip ->
            val track = tracks.find { it.id == clip.trackId }
            when {
                clip.stickerIcon.isNotBlank() || track?.trackType == "STICKER" -> activeColumnMode = "STICKER"
                clip.textContent != null || track?.trackType == "TEXT" -> activeColumnMode = "TEXT"
                track?.trackType == "AUDIO" -> activeColumnMode = "AUDIO"
                else -> activeColumnMode = "VIDEO"
            }
            isToolsExpanded = true
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
    var showStickersSheet by remember { mutableStateOf(false) }
    var showProxyDialog by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }

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

    // Media Gallery Picker Launcher
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            viewModel.addMediaFromGallery(it.toString(), "Media Galeri")
        }
    }

    val totalDurationMs = (clips.maxOfOrNull { it.endTimeMs } ?: 15000L).coerceAtLeast(5000L)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // Video Canvas Preview Top
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
            onToggleProxyMode = { showProxyDialog = true },
            onTogglePlay = { viewModel.togglePlayPause() }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Playback & Undo/Redo & Aspect Ratio Quick Control Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StudioCardBorder, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = StudioCardBg)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Undo / Redo Actions
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.undoTimeline() },
                        enabled = canUndo,
                        modifier = Modifier.testTag("undo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = "Undo",
                            tint = if (canUndo) Color.White else Color.Gray.copy(alpha = 0.4f)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.redoTimeline() },
                        enabled = canRedo,
                        modifier = Modifier.testTag("redo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Redo,
                            contentDescription = "Redo",
                            tint = if (canRedo) Color.White else Color.Gray.copy(alpha = 0.4f)
                        )
                    }
                }

                // Playback controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.seekTo(0L) }) {
                        Icon(imageVector = Icons.Default.SkipPrevious, contentDescription = "Rewind", tint = Color.White)
                    }

                    FilledIconButton(
                        onClick = { viewModel.togglePlayPause() },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = StudioPrimaryViolet)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White
                        )
                    }

                    IconButton(onClick = { viewModel.seekTo(totalDurationMs) }) {
                        Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Forward", tint = Color.White)
                    }
                }

                // Aspect Ratio Tag
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StudioSecondaryTeal.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, StudioSecondaryTeal)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = "Rasio Layar",
                            tint = StudioSecondaryTeal,
                            modifier = Modifier.size(14.dp)
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

        Spacer(modifier = Modifier.height(8.dp))

        // Column Switcher & Top-to-Bottom Dropdown Tools Menu Container
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    when (activeColumnMode) {
                        "VIDEO" -> StudioPrimaryViolet
                        "TEXT" -> StudioSecondaryTeal
                        "AUDIO" -> StudioAccentPink
                        "STICKER" -> Color(0xFFFFB74D)
                        else -> StudioCardBorder
                    },
                    RoundedCornerShape(14.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = StudioCardBg)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Column Selection Filter Chips (Utama/All, Subjudul, Musik, Stiker, Video)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        item {
                            FilterChip(
                                selected = activeColumnMode == "ALL",
                                onClick = {
                                    activeColumnMode = "ALL"
                                    isToolsExpanded = true
                                },
                                label = { Text("Utama", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = activeColumnMode == "TEXT",
                                onClick = {
                                    activeColumnMode = "TEXT"
                                    isToolsExpanded = true
                                },
                                label = { Text("Subjudul", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StudioSecondaryTeal,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = activeColumnMode == "AUDIO",
                                onClick = {
                                    activeColumnMode = "AUDIO"
                                    isToolsExpanded = true
                                },
                                label = { Text("Musik", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StudioAccentPink,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = activeColumnMode == "STICKER",
                                onClick = {
                                    activeColumnMode = "STICKER"
                                    isToolsExpanded = true
                                },
                                label = { Text("Stiker", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFFB74D),
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = activeColumnMode == "VIDEO",
                                onClick = {
                                    activeColumnMode = "VIDEO"
                                    isToolsExpanded = true
                                },
                                label = { Text("Video PIP", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StudioPrimaryViolet,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (selectedClip != null) {
                            IconButton(
                                onClick = { selectedClip = null },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Unselect Clip", tint = Color.LightGray)
                            }
                        }

                        // Toggle Tools Panel Visibility
                        IconButton(
                            onClick = { isToolsExpanded = !isToolsExpanded },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("toggle_tools_expansion_button")
                        ) {
                            Icon(
                                imageVector = if (isToolsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.Tune,
                                contentDescription = if (isToolsExpanded) "Sembunyikan Tools" else "Tampilkan Tools",
                                tint = StudioPrimaryViolet
                            )
                        }
                    }
                }

                // Top-to-Bottom Dropdown Tools Container (Hidden initially for spacious timeline)
                AnimatedVisibility(
                    visible = isToolsExpanded,
                    enter = expandVertically(expandFrom = Alignment.Top) + fadeIn() + slideInVertically { -it / 2 },
                    exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(6.dp))

                        // Sub-category Tabs for active column
                        when (activeColumnMode) {
                            "VIDEO" -> {
                                ScrollableTabRow(
                                    selectedTabIndex = selectedToolCategory.coerceIn(0, 2),
                                    containerColor = Color.Transparent,
                                    contentColor = StudioPrimaryViolet,
                                    edgePadding = 0.dp
                                ) {
                                    Tab(selected = selectedToolCategory == 0, onClick = { selectedToolCategory = 0 }, text = { Text("Dasar Video", fontWeight = FontWeight.Bold, fontSize = 11.sp) })
                                    Tab(selected = selectedToolCategory == 1, onClick = { selectedToolCategory = 1 }, text = { Text("Visual & VFX", fontWeight = FontWeight.Bold, fontSize = 11.sp) })
                                    Tab(selected = selectedToolCategory == 2, onClick = { selectedToolCategory = 2 }, text = { Text("Cutout & AI", fontWeight = FontWeight.Bold, fontSize = 11.sp) })
                                }
                            }
                            "TEXT" -> {
                                ScrollableTabRow(
                                    selectedTabIndex = selectedToolCategory.coerceIn(0, 2),
                                    containerColor = Color.Transparent,
                                    contentColor = StudioSecondaryTeal,
                                    edgePadding = 0.dp
                                ) {
                                    Tab(selected = selectedToolCategory == 0, onClick = { selectedToolCategory = 0 }, text = { Text("Edit Subjudul", fontWeight = FontWeight.Bold, fontSize = 11.sp) })
                                    Tab(selected = selectedToolCategory == 1, onClick = { selectedToolCategory = 1 }, text = { Text("Gaya & Font", fontWeight = FontWeight.Bold, fontSize = 11.sp) })
                                    Tab(selected = selectedToolCategory == 2, onClick = { selectedToolCategory = 2 }, text = { Text("Animasi Teks", fontWeight = FontWeight.Bold, fontSize = 11.sp) })
                                }
                            }
                            "AUDIO" -> {
                                ScrollableTabRow(
                                    selectedTabIndex = selectedToolCategory.coerceIn(0, 2),
                                    containerColor = Color.Transparent,
                                    contentColor = StudioAccentPink,
                                    edgePadding = 0.dp
                                ) {
                                    Tab(selected = selectedToolCategory == 0, onClick = { selectedToolCategory = 0 }, text = { Text("Kontrol Audio", fontWeight = FontWeight.Bold, fontSize = 11.sp) })
                                    Tab(selected = selectedToolCategory == 1, onClick = { selectedToolCategory = 1 }, text = { Text("Denoise & Speed", fontWeight = FontWeight.Bold, fontSize = 11.sp) })
                                    Tab(selected = selectedToolCategory == 2, onClick = { selectedToolCategory = 2 }, text = { Text("SFX Studio", fontWeight = FontWeight.Bold, fontSize = 11.sp) })
                                }
                            }
                            "STICKER" -> {
                                ScrollableTabRow(
                                    selectedTabIndex = selectedToolCategory.coerceIn(0, 1),
                                    containerColor = Color.Transparent,
                                    contentColor = Color(0xFFFFB74D),
                                    edgePadding = 0.dp
                                ) {
                                    Tab(selected = selectedToolCategory == 0, onClick = { selectedToolCategory = 0 }, text = { Text("Pilih Stiker", fontWeight = FontWeight.Bold, fontSize = 11.sp) })
                                    Tab(selected = selectedToolCategory == 1, onClick = { selectedToolCategory = 1 }, text = { Text("Animasi & FX", fontWeight = FontWeight.Bold, fontSize = 11.sp) })
                                }
                            }
                            else -> { // "ALL"
                                ScrollableTabRow(
                                    selectedTabIndex = selectedToolCategory.coerceIn(0, 2),
                                    containerColor = Color.Transparent,
                                    contentColor = StudioPrimaryViolet,
                                    edgePadding = 0.dp
                                ) {
                                    Tab(selected = selectedToolCategory == 0, onClick = { selectedToolCategory = 0 }, text = { Text("Tools Dasar", fontWeight = FontWeight.Bold, fontSize = 11.sp) })
                                    Tab(selected = selectedToolCategory == 1, onClick = { selectedToolCategory = 1 }, text = { Text("Visual & Efek", fontWeight = FontWeight.Bold, fontSize = 11.sp) })
                                    Tab(selected = selectedToolCategory == 2, onClick = { selectedToolCategory = 2 }, text = { Text("Audio & Teks", fontWeight = FontWeight.Bold, fontSize = 11.sp) })
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Dynamic Action Tools Bar
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.testTag("timeline_tools_bar")
                        ) {
                            when (activeColumnMode) {
                                "VIDEO" -> {
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
                                                    label = "Kecepatan & Curve",
                                                    onClick = { showSpeedSheet = true }
                                                )
                                            }
                                            item {
                                                ActionToolChip(
                                                    icon = Icons.Default.Crop,
                                                    label = "Crop dan Canvas",
                                                    onClick = { showCropRotateSheet = true }
                                                )
                                            }
                                            item {
                                                ActionToolChip(
                                                    icon = Icons.Default.FlipCameraAndroid,
                                                    label = "Reverse / Freeze",
                                                    onClick = { showEditVisualSheet = true }
                                                )
                                            }
                                            item {
                                                ActionToolChip(
                                                    icon = Icons.Default.Add,
                                                    label = "Tambah Video",
                                                    onClick = { showAddClipSheet = true },
                                                    color = StudioPrimaryViolet
                                                )
                                            }
                                            if (selectedClip != null) {
                                                item {
                                                    ActionToolChip(
                                                        icon = Icons.Default.Delete,
                                                        label = "Hapus Klip Video",
                                                        onClick = {
                                                            selectedClip?.let { viewModel.deleteClip(it) }
                                                            selectedClip = null
                                                        },
                                                        color = StudioAccentPink
                                                    )
                                                }
                                            }
                                        }
                                        1 -> {
                                            item {
                                                ActionToolChip(
                                                    icon = Icons.Default.Animation,
                                                    label = "Animasi Kedip / Studio",
                                                    onClick = { showAnimationSheet = true },
                                                    color = StudioPrimaryViolet
                                                )
                                            }
                                            item {
                                                ActionToolChip(
                                                    icon = Icons.Default.FilterVintage,
                                                    label = "Filter & Custom LUTs",
                                                    onClick = { showFilterSheet = true },
                                                    color = StudioSecondaryTeal
                                                )
                                            }
                                            item {
                                                ActionToolChip(
                                                    icon = Icons.Default.InvertColors,
                                                    label = "Pembalik Warna Invert",
                                                    onClick = {
                                                        selectedClip?.let { clip ->
                                                            viewModel.updateClipFilter(clip, "Pembalik Warna Invert Colors")
                                                        }
                                                    },
                                                    color = StudioAccentAmber
                                                )
                                            }
                                            item {
                                                ActionToolChip(
                                                    icon = Icons.Default.AutoAwesome,
                                                    label = "Efek Video & Body",
                                                    onClick = { showEffectsSheet = true },
                                                    color = StudioAccentAmber
                                                )
                                            }
                                            item {
                                                ActionToolChip(
                                                    icon = Icons.Default.Tune,
                                                    label = "Adjust Color Grade",
                                                    onClick = { showAdjustSheet = true },
                                                    color = StudioSecondaryTeal
                                                )
                                            }
                                        }
                                        2 -> {
                                            item {
                                                ActionToolChip(
                                                    icon = Icons.Default.CenterFocusWeak,
                                                    label = "Cutout / Remove BG",
                                                    onClick = { showCutoutSheet = true },
                                                    color = StudioSecondaryTeal
                                                )
                                            }
                                            item {
                                                ActionToolChip(
                                                    icon = Icons.Default.GridOn,
                                                    label = "Masking Shape",
                                                    onClick = { showMaskingSheet = true }
                                                )
                                            }
                                            item {
                                                ActionToolChip(
                                                    icon = Icons.Default.Transform,
                                                    label = "Keyframe Studio",
                                                    onClick = { showKeyframeSheet = true },
                                                    color = if (selectedClip?.hasKeyframe == true) StudioAccentAmber else StudioPrimaryViolet
                                                )
                                            }
                                            item {
                                                ActionToolChip(
                                                    icon = Icons.Default.Security,
                                                    label = "Stabilize Video",
                                                    onClick = { showStabilizeSheet = true }
                                                )
                                            }
                                        }
                                    }
                                }
                                "TEXT" -> {
                                    when (selectedToolCategory.coerceIn(0, 2)) {
                                        0 -> {
                                            item {
                                                ActionToolChip(
                                                    icon = Icons.Default.Edit,
                                                    label = if (selectedClip != null) "Edit Teks Subjudul" else "Tambah Teks Baru",
                                                    onClick = { showTextDialog = true },
                                                    color = StudioSecondaryTeal
                                                )
                                            }
                                            item {
                                                ActionToolChip(
                                                    icon = Icons.Default.ContentCut,
                                                    label = "Split Subjudul",
                                                    onClick = { selectedClip?.let { viewModel.splitClipAtCurrentTime(it, currentTimeMs) } },
                                                    enabled = selectedClip != null && currentTimeMs > (selectedClip?.startTimeMs ?: 0L) && currentTimeMs < (selectedClip?.endTimeMs ?: 0L)
                                                )
                                            }
                                            item {
                                                ActionToolChip(
                                                    icon = Icons.Default.RecordVoiceOver,
                                                    label = "Auto Captions AI",
                                                    onClick = { viewModel.autoTranscribeSubtitles() },
                                                    color = StudioAccentAmber
                                                )
                                            }
                                            item {
                                                ActionToolChip(
                                                    icon = Icons.Default.Add,
                                                    label = "Tambah Subjudul Studio",
                                                    onClick = { showTextDialog = true },
                                                    color = StudioPrimaryViolet
                                                )
                                            }
                                            if (selectedClip != null) {
                                                item {
                                                    ActionToolChip(
                                                        icon = Icons.Default.Delete,
                                                        label = "Hapus Subjudul",
                                                        onClick = {
                                                            selectedClip?.let { viewModel.deleteClip(it) }
                                                            selectedClip = null
                                                        },
                                                        color = StudioAccentPink
                                                    )
                                                }
                                            }
                                        }
                                        1 -> {
                                            item {
                                                ActionToolChip(
                                                    icon = Icons.Default.FormatPaint,
                                                    label = "Style & Warna Teks",
                                                    onClick = { showTextDialog = true },
                                                    color = StudioSecondaryTeal
                                                )
                                            }
                                            item {
                                                ActionToolChip(
                                                    icon = Icons.Default.ChatBubble,
                                                    label = "Gelembung Subjudul",
                                                    onClick = { showStickersSheet = true }
                                                )
                                            }
                                            item {
                                                ActionToolChip(
                                                    icon = Icons.Default.EmojiEmotions,
                                                    label = "Stiker Hiasan",
                                                    onClick = { showStickersSheet = true }
                                                )
                                            }
                                        }
                                        2 -> {
                                            item {
                                                ActionToolChip(
                                                    icon = Icons.Default.Animation,
                                                    label = "Animasi Teks Kedip",
                                                    onClick = { showAnimationSheet = true },
                                                    color = StudioPrimaryViolet
                                                )
                                            }
                                            item {
                                                ActionToolChip(
                                                    icon = Icons.Default.Transform,
                                                    label = "Keyframe Posisi Teks",
                                                    onClick = { showKeyframeSheet = true }
                                                )
                                            }
                                        }
                                    }
                                }
                                "AUDIO" -> {
                                    when (selectedToolCategory.coerceIn(0, 2)) {
                                        0 -> {
                                            item {
                                                ActionToolChip(
                                                    icon = Icons.Default.VolumeUp,
                                                    label = "Volume & Fade Mixer",
                                                    onClick = { showAudioStudioSheet = true },
                                                    color = StudioSecondaryTeal
                                                )
                                            }
                                            item {
                                                ActionToolChip(
                                                    icon = Icons.Default.ContentCut,
                                                    label = "Split Audio",
                                                    onClick = { selectedClip?.let { viewModel.splitClipAtCurrentTime(it, currentTimeMs) } },
                                                    enabled = selectedClip != null && currentTimeMs > (selectedClip?.startTimeMs ?: 0L) && currentTimeMs < (selectedClip?.endTimeMs ?: 0L)
                                                )
                                            }
                                            item {
                                                ActionToolChip(
                                                    icon = Icons.Default.Add,
                                                    label = "Tambah Audio / SFX",
                                                    onClick = { showAudioStudioSheet = true },
                                                    color = StudioPrimaryViolet
                                                )
                                            }
                                            if (selectedClip != null) {
                                                item {
                                                    ActionToolChip(
                                                        icon = Icons.Default.Delete,
                                                        label = "Hapus Musik",
                                                        onClick = {
                                                            selectedClip?.let { viewModel.deleteClip(it) }
                                                            selectedClip = null
                                                        },
                                                        color = StudioAccentPink
                                                    )
                                                }
                                            }
                                        }
                                        1 -> {
                                            item {
                                                ActionToolChip(
                                                    icon = Icons.Default.SurroundSound,
                                                    label = "Denoise AI",
                                                    onClick = { showAudioStudioSheet = true },
                                                    color = StudioAccentAmber
                                                )
                                            }
                                            item {
                                                ActionToolChip(
                                                    icon = Icons.Default.Speed,
                                                    label = "Kecepatan Audio",
                                                    onClick = { showSpeedSheet = true }
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
                                        2 -> {
                                            item {
                                                ActionToolChip(
                                                    icon = Icons.Default.Audiotrack,
                                                    label = "Audio Mixer & SFX",
                                                    onClick = { showAudioStudioSheet = true },
                                                    color = StudioSecondaryTeal
                                                )
                                            }
                                        }
                                    }
                                }
                                "STICKER" -> {
                                    when (selectedToolCategory.coerceIn(0, 1)) {
                                        0 -> {
                                            item {
                                                ActionToolChip(
                                                    icon = Icons.Default.EmojiEmotions,
                                                    label = "Katalog Stiker & Emoji",
                                                    onClick = { showStickersSheet = true },
                                                    color = Color(0xFFFFB74D)
                                                )
                                            }
                                            item {
                                                ActionToolChip(
                                                    icon = Icons.Default.Add,
                                                    label = "Tambah Stiker FX",
                                                    onClick = { showStickersSheet = true },
                                                    color = StudioPrimaryViolet
                                                )
                                            }
                                            if (selectedClip != null) {
                                                item {
                                                    ActionToolChip(
                                                        icon = Icons.Default.Delete,
                                                        label = "Hapus Stiker",
                                                        onClick = {
                                                            selectedClip?.let { viewModel.deleteClip(it) }
                                                            selectedClip = null
                                                        },
                                                        color = StudioAccentPink
                                                    )
                                                }
                                            }
                                        }
                                        1 -> {
                                            item {
                                                ActionToolChip(
                                                    icon = Icons.Default.Animation,
                                                    label = "Animasi Stiker",
                                                    onClick = { showAnimationSheet = true },
                                                    color = StudioSecondaryTeal
                                                )
                                            }
                                        }
                                    }
                                }
                                else -> { // "ALL"
                                    item {
                                        ActionToolChip(
                                            icon = Icons.Default.Add,
                                            label = "Tambah Video",
                                            onClick = { showAddClipSheet = true },
                                            color = StudioPrimaryViolet
                                        )
                                    }
                                    item {
                                        ActionToolChip(
                                            icon = Icons.Default.Subtitles,
                                            label = "Tambah Subjudul",
                                            onClick = { showTextDialog = true },
                                            color = StudioSecondaryTeal
                                        )
                                    }
                                    item {
                                        ActionToolChip(
                                            icon = Icons.Default.Audiotrack,
                                            label = "Audio Mixer",
                                            onClick = { showAudioStudioSheet = true },
                                            color = StudioAccentPink
                                        )
                                    }
                                    item {
                                        ActionToolChip(
                                            icon = Icons.Default.Speed,
                                            label = "Kecepatan & Curve",
                                            onClick = { showSpeedSheet = true }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Multitrack Timeline Canvas
        TimelineView(
            tracks = tracks,
            clips = clips,
            currentTimeMs = currentTimeMs,
            totalDurationMs = totalDurationMs,
            activeColumnFilter = activeColumnMode,
            onSeek = { viewModel.seekTo(it) },
            onClipSelected = { selectedClip = it },
            onClipMoved = { clipId, newStartMs, newTrackId ->
                viewModel.moveClipPositionAndTrack(clipId, newStartMs, newTrackId)
            },
            onAddClipRequested = { showAddClipSheet = true },
            onAddOverlayTrackRequested = { trackType ->
                viewModel.addNewOverlayTrack(trackType)
            },
            modifier = Modifier.weight(1f)
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
