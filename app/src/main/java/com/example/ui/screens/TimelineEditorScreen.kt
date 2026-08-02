package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
    var selectedToolCategory by remember { mutableStateOf(0) } // 0: Dasar, 1: Visual & Efek, 2: Lanjutan & AI, 3: Audio & Teks

    // Dialog & Bottom Sheet visibility states
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
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        // Video Canvas Preview Top
        VideoPlayerView(
            aspectRatioStr = activeProject?.aspectRatio ?: "16:9",
            isPlaying = isPlaying,
            currentTimeMs = currentTimeMs,
            totalDurationMs = totalDurationMs,
            activeFilter = selectedClip?.filterName ?: "Cinematic Glow",
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

                // Aspect Ratio Selector Tag
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StudioSecondaryTeal.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, StudioSecondaryTeal)
                ) {
                    Text(
                        text = activeProject?.aspectRatio ?: "16:9",
                        color = StudioSecondaryTeal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        ScrollableTabRow(
            selectedTabIndex = selectedToolCategory,
            containerColor = Color.Transparent,
            contentColor = StudioPrimaryViolet,
            edgePadding = 0.dp
        ) {
            Tab(
                selected = selectedToolCategory == 0,
                onClick = { selectedToolCategory = 0 },
                text = { Text("Tools Dasar", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedToolCategory == 1,
                onClick = { selectedToolCategory = 1 },
                text = { Text("Visual & Efek", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedToolCategory == 2,
                onClick = { selectedToolCategory = 2 },
                text = { Text("Lanjutan & AI", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedToolCategory == 3,
                onClick = { selectedToolCategory = 3 },
                text = { Text("Audio & Teks", fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Category Specific Professional Toolbar
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.testTag("timeline_tools_bar")
        ) {
            when (selectedToolCategory) {
                0 -> {
                    // --- Tools Dasar Timeline ---
                    item {
                        ActionToolChip(
                            icon = Icons.Default.ContentCut,
                            label = "Split Bagi Klip",
                            onClick = {
                                selectedClip?.let {
                                    viewModel.splitClipAtCurrentTime(it, currentTimeMs)
                                }
                            },
                            enabled = selectedClip != null && currentTimeMs > (selectedClip?.startTimeMs ?: 0L) && currentTimeMs < (selectedClip?.endTimeMs ?: 0L),
                            color = StudioSecondaryTeal
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
                            enabled = selectedClip != null,
                            color = StudioAccentPink
                        )
                    }
                    item {
                        ActionToolChip(
                            icon = Icons.Default.Speed,
                            label = "Speed dan Curve",
                            onClick = { if (selectedClip != null) showSpeedSheet = true },
                            enabled = selectedClip != null
                        )
                    }
                    item {
                        ActionToolChip(
                            icon = Icons.Default.Animation,
                            label = "Animasi Studio",
                            onClick = { if (selectedClip != null) showAnimationSheet = true },
                            enabled = selectedClip != null
                        )
                    }
                    item {
                        ActionToolChip(
                            icon = Icons.Default.Crop,
                            label = "Crop dan Canvas",
                            onClick = { if (selectedClip != null) showCropRotateSheet = true },
                            enabled = selectedClip != null
                        )
                    }
                    item {
                        ActionToolChip(
                            icon = if (isProxyMode) Icons.Default.Speed else Icons.Default.HighQuality,
                            label = if (isProxyMode) "Proxy $proxyResolution ON" else "Proxy OFF 1080p",
                            onClick = { showProxyDialog = true },
                            color = if (isProxyMode) StudioSecondaryTeal else StudioAccentAmber
                        )
                    }
                }
                1 -> {
                    // --- Tools Visual dan Efek ---
                    item {
                        ActionToolChip(
                            icon = Icons.Default.FlipCameraAndroid,
                            label = "Edit Reverse dan Freeze",
                            onClick = { if (selectedClip != null) showEditVisualSheet = true },
                            enabled = selectedClip != null
                        )
                    }
                    item {
                        ActionToolChip(
                            icon = Icons.Default.AutoAwesome,
                            label = "Efek Video dan Body",
                            onClick = { if (selectedClip != null) showEffectsSheet = true },
                            enabled = selectedClip != null,
                            color = StudioAccentAmber
                        )
                    }
                    item {
                        ActionToolChip(
                            icon = Icons.Default.FilterVintage,
                            label = "Filter VFX Studio",
                            onClick = { if (selectedClip != null) showFilterSheet = true },
                            enabled = selectedClip != null
                        )
                    }
                    item {
                        ActionToolChip(
                            icon = Icons.Default.Tune,
                            label = "Adjust Color Grade",
                            onClick = { if (selectedClip != null) showAdjustSheet = true },
                            enabled = selectedClip != null,
                            color = StudioSecondaryTeal
                        )
                    }
                }
                2 -> {
                    // --- Tools Lanjutan dan AI ---
                    item {
                        ActionToolChip(
                            icon = Icons.Default.CenterFocusWeak,
                            label = "Cutout / Remove BG",
                            onClick = { if (selectedClip != null) showCutoutSheet = true },
                            enabled = selectedClip != null,
                            color = StudioSecondaryTeal
                        )
                    }
                    item {
                        ActionToolChip(
                            icon = Icons.Default.GridOn,
                            label = "Masking Shape",
                            onClick = { if (selectedClip != null) showMaskingSheet = true },
                            enabled = selectedClip != null
                        )
                    }
                    item {
                        ActionToolChip(
                            icon = Icons.Default.Transform,
                            label = "Keyframe Studio",
                            onClick = { if (selectedClip != null) showKeyframeSheet = true },
                            enabled = selectedClip != null,
                            color = if (selectedClip?.hasKeyframe == true) StudioAccentAmber else StudioPrimaryViolet
                        )
                    }
                    item {
                        ActionToolChip(
                            icon = Icons.Default.Security,
                            label = "Stabilize Video",
                            onClick = { if (selectedClip != null) showStabilizeSheet = true },
                            enabled = selectedClip != null
                        )
                    }
                }
                3 -> {
                    // --- Tools Audio, Teks & Stiker ---
                    item {
                        ActionToolChip(
                            icon = Icons.Default.Audiotrack,
                            label = "Audio Mixer & SFX",
                            onClick = { showAudioStudioSheet = true },
                            color = StudioSecondaryTeal
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
                            icon = Icons.Default.Subtitles,
                            label = "Teks Subjudul",
                            onClick = { showTextDialog = true }
                        )
                    }
                    item {
                        ActionToolChip(
                            icon = Icons.Default.EmojiEmotions,
                            label = "Stiker Hiasan",
                            onClick = { showStickersSheet = true }
                        )
                    }
                    item {
                        ActionToolChip(
                            icon = Icons.Default.AddPhotoAlternate,
                            label = "+ Media Galeri",
                            onClick = {
                                mediaPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                            color = StudioPrimaryViolet
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Multitrack Timeline Canvas
        TimelineView(
            tracks = tracks,
            clips = clips,
            currentTimeMs = currentTimeMs,
            totalDurationMs = totalDurationMs,
            onSeek = { viewModel.seekTo(it) },
            onClipSelected = { selectedClip = it },
            onAddOverlayTrackRequested = { trackType ->
                viewModel.addNewOverlayTrack(trackType)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Active Selected Clip Details & Status Card
        selectedClip?.let { clip ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StudioPrimaryViolet, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = StudioCardBg)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Movie, contentDescription = null, tint = StudioPrimaryViolet)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Klip Terpilih: ${clip.title}",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = { selectedClip = null }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Unselect", tint = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Durasi: ${clip.durationMs / 1000}s", color = StudioTextSecondary, fontSize = 12.sp)
                        Text("Kecepatan: ${clip.speedMultiplier}x (${clip.speedCurve})", color = StudioSecondaryTeal, fontSize = 12.sp)
                        Text("Volume: ${(clip.volume * 100).toInt()}%", color = StudioAccentAmber, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Active Effects Badges
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (clip.isReversed) {
                            item { StatusBadge("Reverse ON", StudioAccentPink) }
                        }
                        if (clip.isMirrored) {
                            item { StatusBadge("Mirror ON", StudioSecondaryTeal) }
                        }
                        if (clip.rotationDegrees > 0) {
                            item { StatusBadge("Rotasi: ${clip.rotationDegrees}°", StudioPrimaryViolet) }
                        }
                        if (clip.cutoutMode != "None") {
                            item { StatusBadge("Cutout: ${clip.cutoutMode}", StudioAccentAmber) }
                        }
                        if (clip.effectName != "None") {
                            item { StatusBadge("Efek: ${clip.effectName}", StudioPrimaryViolet) }
                        }
                        if (clip.hasKeyframe) {
                            item { StatusBadge("Keyframe Active", StudioAccentPink) }
                        }
                        if (clip.stabilizeLevel != "None") {
                            item { StatusBadge("Stabilize: ${clip.stabilizeLevel}", StudioSecondaryTeal) }
                        }
                        if (clip.vignette > 0f) {
                            item { StatusBadge("Vignette", StudioSecondaryTeal) }
                        }
                        if (clip.noiseReduction) {
                            item { StatusBadge("Denoise AI", StudioAccentAmber) }
                        }
                    }
                }
            }
        }
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
        val animsIn = listOf("None", "Fade In", "Slide Right", "Zoom In", "Bounce In", "Rotate Entrance")
        val animsOut = listOf("None", "Fade Out", "Slide Left", "Zoom Out", "Dissolve Out", "Glitch Exit")
        val animsCombo = listOf("None", "Spin & Zoom", "Glitch Bounce", "3D Flip", "Elastic Pop")

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
                Text("Animasi Keluar (Out):", color = StudioAccentPink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
        val videoEffects = listOf("None", "Retro VHS", "Cyber Glitch", "Light Leak", "Film Grain", "Neon Edge")
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

    // 6. Filter VFX Bottom Sheet
    if (showFilterSheet && selectedClip != null) {
        val filters = listOf("None", "Cinematic Glow", "Cyberpunk Neon", "Vintage Film", "Sunset Amber", "B&W Noir", "Moody Teal")
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
                Text("Filter Color VFX Studio", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))

                filters.forEach { filter ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.updateClipFilter(selectedClip!!, filter)
                                showFilterSheet = false
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedClip!!.filterName == filter, onClick = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(filter, color = Color.White, fontWeight = FontWeight.SemiBold)
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
        val cutoutModes = listOf("None", "Auto AI Cutout (Hapus BG)", "Chroma Key (Layar Hijau)", "Smart Portrait Segmentation")
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
                Text("Tambah Efek Suara (SFX):", color = StudioSecondaryTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

    // 13. Add Text Dialog
    if (showTextDialog) {
        var textInput by remember { mutableStateOf("SUBJUDUL FLOWMONKEY STUDIO") }
        AlertDialog(
            onDismissRequest = { showTextDialog = false },
            title = { Text("Tambah Teks Subjudul Studio", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("Teks Subjudul") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val projId = activeProject?.id ?: return@Button
                        val textTrack = tracks.find { it.trackType == "TEXT" }
                        if (textTrack != null) {
                            viewModel.addClip(
                                TimelineClipEntity(
                                    trackId = textTrack.id,
                                    projectId = projId,
                                    title = "Teks Subjudul",
                                    mediaUri = "",
                                    startTimeMs = currentTimeMs,
                                    endTimeMs = currentTimeMs + 3000L,
                                    durationMs = 3000L,
                                    textContent = textInput
                                )
                            )
                        }
                        showTextDialog = false
                    }
                ) {
                    Text("Tambah")
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
