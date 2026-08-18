package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.db.VideoProjectEntity
import com.example.data.models.LocalMediaAsset
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.example.ui.theme.*
import com.example.ui.viewmodels.MainTab
import com.example.ui.viewmodels.VideoStudioViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    viewModel: VideoStudioViewModel,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.allProjects.collectAsState()
    val activeProjectId by viewModel.activeProjectId.collectAsState()
    val mediaAssets by viewModel.localMediaAssets.collectAsState()

    val userProjects = remember(projects) { projects.filter { !it.isTemplate } }
    val templateProjects = remember(projects) { projects.filter { it.isTemplate } }

    var selectedSubTab by remember { mutableIntStateOf(0) } // 0: Proyek, 1: Template, 2: Pustaka Media
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedTemplateForMediaReplace by remember { mutableStateOf<VideoProjectEntity?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(enabled = showCreateDialog || selectedTemplateForMediaReplace != null || selectedSubTab != 0) {
        when {
            showCreateDialog -> showCreateDialog = false
            selectedTemplateForMediaReplace != null -> selectedTemplateForMediaReplace = null
            selectedSubTab != 0 -> selectedSubTab = 0
        }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            snackbarMessage = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header Title & FAB
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = StudioElectricBlue,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Proyek, Template & Media", color = StudioTextDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = when (selectedSubTab) {
                                0 -> "${userProjects.size} proyek aktif"
                                1 -> "${templateProjects.size} template siap pakai"
                                else -> "${mediaAssets.size} aset media di pustaka"
                            },
                            color = StudioTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = StudioElectricBlue,
                    contentColor = Color.White,
                    modifier = Modifier.size(42.dp).testTag("fab_new_project"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "New Project", modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sub Tab Navigation (Proyek vs Template vs Pustaka Media)
            TabRow(
                selectedTabIndex = selectedSubTab,
                containerColor = StudioGlassWhite,
                contentColor = StudioTextDark,
                divider = {},
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, StudioCardHairline, RoundedCornerShape(16.dp))
            ) {
                Tab(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Proyek (${userProjects.size})", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    },
                    selectedContentColor = StudioElectricBlue,
                    unselectedContentColor = StudioTextMuted
                )
                Tab(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Template (${templateProjects.size})", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    },
                    selectedContentColor = StudioElectricBlue,
                    unselectedContentColor = StudioTextMuted
                )
                Tab(
                    selected = selectedSubTab == 2,
                    onClick = { selectedSubTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.PermMedia, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pustaka (${mediaAssets.size})", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    },
                    selectedContentColor = StudioElectricBlue,
                    unselectedContentColor = StudioTextMuted
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedSubTab == 0) {
                // TAB 0: PROYEK AKTIF
                if (userProjects.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = StudioGlassWhite),
                        border = BorderStroke(1.dp, StudioCardHairline),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoCall,
                                contentDescription = null,
                                tint = StudioElectricBlue,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Belum ada proyek aktif", color = StudioTextDark, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Tarik proyek ke kiri untuk menjadikannya Template!", color = StudioTextMuted, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showCreateDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = StudioDarkCTA),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Text("+ Buat Proyek Baru", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Petunjuk: Tarik (swipe) proyek ke kiri untuk opsi Template & Hapus",
                        fontSize = 11.sp,
                        color = StudioTextMuted,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    userProjects.forEach { project ->
                        val isActive = project.id == activeProjectId
                        SwipeableProjectCard(
                            project = project,
                            isActive = isActive,
                            onSelectProject = {
                                viewModel.selectProject(project.id)
                                viewModel.selectTab(MainTab.TIMELINE_EDITOR)
                            },
                            onCreateTemplate = {
                                viewModel.createTemplateFromProject(project) {
                                    snackbarMessage = "Berhasil membuat Template dari '${project.title}'!"
                                }
                            },
                            onDeleteProject = {
                                viewModel.deleteProject(project)
                                snackbarMessage = "Proyek '${project.title}' dihapus."
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            } else if (selectedSubTab == 1) {
                // TAB 1: TEMPLATE (Full Tools & Placeholder Media)
                if (templateProjects.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = StudioGlassWhite),
                        border = BorderStroke(1.dp, StudioCardHairline),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = StudioElectricBlue,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Belum Ada Template Kustom", color = StudioTextDark, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Buka tab 'Proyek', lalu tarik (swipe) proyek ke kiri dan tekan ikon Buat Template untuk menyimpan preset full tools + placeholder!",
                                color = StudioTextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }

                templateProjects.forEach { template ->
                    TemplateItemCard(
                        template = template,
                        onUseTemplate = {
                            viewModel.createProjectFromTemplate(template)
                            viewModel.selectTab(MainTab.TIMELINE_EDITOR)
                            snackbarMessage = "Proyek baru dari Template '${template.title}' siap diedit!"
                        },
                        onReplaceMedia = {
                            selectedTemplateForMediaReplace = template
                        },
                        onDeleteTemplate = {
                            viewModel.deleteProject(template)
                            snackbarMessage = "Template '${template.title}' dihapus."
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            } else {
                // TAB 2: PUSTAKA MEDIA LOKAL
                LocalMediaLibraryView(
                    viewModel = viewModel,
                    onShowSnackbar = { snackbarMessage = it }
                )
            }
        }
    }

    // Dialog Input / Replace Placeholder Media for Template
    selectedTemplateForMediaReplace?.let { template ->
        var sampleMediaName by remember { mutableStateOf("my_custom_vlog_video.mp4") }
        var sampleAudioName by remember { mutableStateOf("my_custom_music.mp3") }

        AlertDialog(
            onDismissRequest = { selectedTemplateForMediaReplace = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.PermMedia, contentDescription = null, tint = StudioElectricBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Masukan Media Baru dari Galeri", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Template: ${template.title}",
                        fontSize = 12.sp,
                        color = StudioElectricBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Ganti placeholder media di bawah dengan video/musik baru dari galeri device Anda:",
                        fontSize = 12.sp,
                        color = StudioTextMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = sampleMediaName,
                        onValueChange = { sampleMediaName = it },
                        label = { Text("File Video Utama / Galeri") },
                        leadingIcon = { Icon(imageVector = Icons.Default.VideoFile, contentDescription = null) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = StudioTextDark,
                            unfocusedTextColor = StudioTextDark,
                            focusedBorderColor = StudioElectricBlue,
                            unfocusedBorderColor = StudioCardHairline
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = sampleAudioName,
                        onValueChange = { sampleAudioName = it },
                        label = { Text("File Musik Audio / Galeri") },
                        leadingIcon = { Icon(imageVector = Icons.Default.AudioFile, contentDescription = null) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = StudioTextDark,
                            unfocusedTextColor = StudioTextDark,
                            focusedBorderColor = StudioElectricBlue,
                            unfocusedBorderColor = StudioCardHairline
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val customTitle = "Proyek (${sampleMediaName.substringBefore(".")})"
                        viewModel.createProjectFromTemplate(template, customTitle)
                        selectedTemplateForMediaReplace = null
                        snackbarMessage = "Media galeri berhasil dimasukkan ke template!"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioDarkCTA),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Terapkan & Buat", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTemplateForMediaReplace = null }) { Text("Batal") }
            }
        )
    }

    // Create New Project Dialog
    if (showCreateDialog) {
        var titleInput by remember { mutableStateOf("Iklan Media Sosial Baru") }
        var selectedRatio by remember { mutableStateOf("16:9") }
        var selectedStyle by remember { mutableStateOf("Cinematic") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Buat Proyek Video Baru", fontWeight = FontWeight.Bold, color = StudioTextDark) },
            text = {
                Column {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("Judul Proyek") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = StudioTextDark,
                            unfocusedTextColor = StudioTextDark,
                            focusedBorderColor = StudioElectricBlue,
                            unfocusedBorderColor = StudioCardHairline
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Rasio Layar:", fontSize = 12.sp, color = StudioTextMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("16:9", "9:16", "1:1").forEach { ratio ->
                            FilterChip(
                                selected = selectedRatio == ratio,
                                onClick = { selectedRatio = ratio },
                                label = { Text(ratio) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StudioDarkCTA,
                                    selectedLabelColor = Color.White,
                                    containerColor = StudioPillBg,
                                    labelColor = StudioTextDark
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createNewProject(titleInput, selectedRatio, selectedStyle)
                        showCreateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioDarkCTA),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Buat Proyek", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Batal") }
            }
        )
    }
}

/**
 * Swipeable Project Card
 * Dragging left reveals Action Buttons containing STRICTLY ONLY ICONS (Template & Delete).
 */
@Composable
fun SwipeableProjectCard(
    project: VideoProjectEntity,
    isActive: Boolean,
    onSelectProject: () -> Unit,
    onCreateTemplate: () -> Unit,
    onDeleteProject: () -> Unit
) {
    val density = LocalDensity.current
    val maxRevealPx = with(density) { 120.dp.toPx() } // Revealing 2 action icon buttons (60dp each)

    var offsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(targetValue = offsetX, label = "swipe_offset")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(StudioPillBg)
    ) {
        // Revealed Right Action Buttons Layer (CONTAINS STRICTLY ONLY ICONS)
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(120.dp)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Button 1: Buat Template (STRICTLY ONLY ICON)
            IconButton(
                onClick = {
                    offsetX = 0f
                    onCreateTemplate()
                },
                modifier = Modifier
                    .width(60.dp)
                    .fillMaxHeight()
                    .background(StudioElectricBlue)
                    .testTag("action_make_template_${project.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.BookmarkAdd,
                    contentDescription = "Buat Template",
                    tint = Color.White
                )
            }

            // Button 2: Hapus Proyek (STRICTLY ONLY ICON)
            IconButton(
                onClick = {
                    offsetX = 0f
                    onDeleteProject()
                },
                modifier = Modifier
                    .width(60.dp)
                    .fillMaxHeight()
                    .background(StudioRosePink)
                    .testTag("action_delete_project_${project.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Hapus Proyek",
                    tint = Color.White
                )
            }
        }

        // Foreground Swipeable Project Card
        Card(
            modifier = Modifier
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .fillMaxSize()
                .border(
                    1.dp,
                    if (isActive) StudioElectricBlue else StudioCardHairline,
                    RoundedCornerShape(18.dp)
                )
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            offsetX = if (offsetX < -maxRevealPx / 2f) -maxRevealPx else 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val newX = offsetX + dragAmount
                            offsetX = newX.coerceIn(-maxRevealPx, 0f)
                        }
                    )
                }
                .clickable { onSelectProject() }
                .testTag("project_item_${project.id}"),
            colors = CardDefaults.cardColors(containerColor = StudioGlassWhite)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(StudioCleanCanvas)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_style_cinematic_1785585807861),
                        contentDescription = "Project Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = project.title,
                            color = StudioTextDark,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        if (isActive) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Active Project",
                                tint = StudioElectricBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Rasio: ${project.aspectRatio}", color = StudioTextMuted, fontSize = 11.sp)
                        Text("Gaya: ${project.visualStyle}", color = StudioElectricBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Drag indicator hint
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Swipe left",
                    tint = StudioTextMuted.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Template Item Card with Full Tools Indicator and Placeholder Replacer
 */
@Composable
fun TemplateItemCard(
    template: VideoProjectEntity,
    onUseTemplate: () -> Unit,
    onReplaceMedia: () -> Unit,
    onDeleteTemplate: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, StudioCardHairline, RoundedCornerShape(18.dp))
            .testTag("template_item_${template.id}"),
        colors = CardDefaults.cardColors(containerColor = StudioGlassWhite)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = StudioElectricBlue.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = StudioElectricBlue,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("FULL TOOLS TEMPLATE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = StudioElectricBlue)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rasio: ${template.aspectRatio}", color = StudioTextMuted, fontSize = 11.sp)
                }

                IconButton(
                    onClick = onDeleteTemplate,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete Template", tint = StudioRosePink, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = template.title,
                color = StudioTextDark,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = template.description.ifBlank { "Template preset timeline lengkap dengan filter, speed curve, subjudul & audio placeholder." },
                color = StudioTextMuted,
                fontSize = 12.sp,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Pre-configured tools badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                listOf("Multi-Track", "Speed Curve 2.0x", "Keyframe FX", "Subjudul Teks", "Audio Mix").forEach { toolTag ->
                    Surface(
                        color = StudioPillBg,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.5.dp, StudioCardHairline)
                    ) {
                        Text(
                            text = toolTag,
                            color = StudioTextDark,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Actions: Use Template or Input Custom Media from Gallery
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onUseTemplate,
                    colors = ButtonDefaults.buttonColors(containerColor = StudioDarkCTA),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gunakan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onReplaceMedia,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioElectricBlue),
                    border = BorderStroke(1.dp, StudioElectricBlue.copy(alpha = 0.6f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.PermMedia, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Media Galeri", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Pustaka Media Lokal View Component
 * Filter categorized assets (Video, Audio, Gambar) with Drag & Drop or Direct Insertion to Active Timeline.
 */
@Composable
fun LocalMediaLibraryView(
    viewModel: VideoStudioViewModel,
    onShowSnackbar: (String) -> Unit
) {
    val mediaAssets by viewModel.localMediaAssets.collectAsState()
    var selectedCategory by remember { mutableStateOf("Semua") }
    var searchQuery by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }

    val categories = listOf("Semua", "Video", "Audio", "Gambar & Grafis", "AI Generated")

    val filteredAssets = remember(mediaAssets, selectedCategory, searchQuery) {
        mediaAssets.filter { asset ->
            val matchesCategory = when (selectedCategory) {
                "Video" -> asset.category.equals("VIDEO", ignoreCase = true)
                "Audio" -> asset.category.equals("AUDIO", ignoreCase = true)
                "Gambar & Grafis" -> asset.category.equals("IMAGE", ignoreCase = true) || asset.category.equals("GRAPHIC", ignoreCase = true)
                "AI Generated" -> asset.isAiGenerated
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() || asset.title.contains(searchQuery, ignoreCase = true) || asset.tags.any { it.contains(searchQuery, ignoreCase = true) }
            matchesCategory && matchesSearch
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Compact Search & Action Bar (Clean layout without space-wasting header banner)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari aset video, musik, SFX, stiker...", fontSize = 12.sp, color = StudioTextSubtle) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = StudioTextMuted, modifier = Modifier.size(16.dp)) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = StudioTextMuted, modifier = Modifier.size(14.dp))
                        }
                    }
                } else null,
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = StudioCleanCanvas,
                    unfocusedContainerColor = StudioCleanCanvas,
                    focusedBorderColor = StudioElectricBlue,
                    unfocusedBorderColor = StudioCardHairline,
                    focusedTextColor = StudioTextDark,
                    unfocusedTextColor = StudioTextDark
                ),
                shape = RoundedCornerShape(14.dp)
            )

            Button(
                onClick = { showImportDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = StudioDarkCTA),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("button_import_media")
            ) {
                Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("+ Impor", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Category Filter Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = when (cat) {
                        "Video" -> { { Icon(imageVector = Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(14.dp)) } }
                        "Audio" -> { { Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(14.dp)) } }
                        "Gambar & Grafis" -> { { Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(14.dp)) } }
                        "AI Generated" -> { { Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp)) } }
                        else -> null
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StudioDarkCTA,
                        selectedLabelColor = Color.White,
                        containerColor = StudioGlassWhite,
                        labelColor = StudioTextDark
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (filteredAssets.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(containerColor = StudioGlassWhite),
                border = BorderStroke(1.dp, StudioCardHairline),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(imageVector = Icons.Default.PermMedia, contentDescription = null, tint = StudioElectricBlue, modifier = Modifier.size(44.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Aset Media Tidak Ditemukan", color = StudioTextDark, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Tekan '+ Impor' untuk menambah media dari galeri device.", color = StudioTextMuted, fontSize = 12.sp)
                }
            }
        } else {
            filteredAssets.forEach { asset ->
                MediaAssetCard(
                    asset = asset,
                    onAddToTimeline = {
                        viewModel.insertAssetToActiveTimeline(asset, jumpToTimeline = false)
                        onShowSnackbar("Aset '${asset.title}' dimasukkan ke timeline proyek aktif!")
                    },
                    onDragAndInsertToTimeline = {
                        viewModel.insertAssetToActiveTimeline(asset, jumpToTimeline = true)
                        onShowSnackbar("Menyisipkan '${asset.title}' & Membuka Editor Timeline...")
                    },
                    onDuplicateAsset = {
                        viewModel.duplicateMediaAsset(asset)
                        onShowSnackbar("Aset '${asset.title}' berhasil diduplikat (0 memory/storage overhead).")
                    },
                    onDeleteAsset = {
                        viewModel.deleteMediaAsset(asset.id)
                        onShowSnackbar("Aset '${asset.title}' dihapus dari pustaka.")
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    // Dialog Import Media Baru
    if (showImportDialog) {
        var newTitle by remember { mutableStateOf("") }
        var selectedCategory by remember { mutableStateOf("VIDEO") }
        var newUri by remember { mutableStateOf("") }
        var durationInput by remember { mutableStateOf("00:05") }

        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, tint = StudioElectricBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Impor Media Baru", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Nama / Judul Aset") },
                        placeholder = { Text("Contoh: Video Cinematic Sunset") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = StudioTextDark,
                            unfocusedTextColor = StudioTextDark,
                            focusedBorderColor = StudioElectricBlue,
                            unfocusedBorderColor = StudioCardHairline
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Kategori Media:", fontSize = 12.sp, color = StudioTextMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("VIDEO", "AUDIO", "IMAGE").forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(if (cat == "IMAGE") "GAMBAR" else cat, fontSize = 11.sp) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newUri,
                        onValueChange = { newUri = it },
                        label = { Text("URI / Path File Media") },
                        placeholder = { Text("storage/emulated/0/Movies/clip.mp4") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = StudioTextDark,
                            unfocusedTextColor = StudioTextDark,
                            focusedBorderColor = StudioElectricBlue,
                            unfocusedBorderColor = StudioCardHairline
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = durationInput,
                        onValueChange = { durationInput = it },
                        label = { Text("Durasi (mm:ss)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = StudioTextDark,
                            unfocusedTextColor = StudioTextDark,
                            focusedBorderColor = StudioElectricBlue,
                            unfocusedBorderColor = StudioCardHairline
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val titleFinal = newTitle.ifBlank { "Media Impor ${System.currentTimeMillis() % 1000}" }
                        val uriFinal = newUri.ifBlank { "imported_media_${System.currentTimeMillis()}" }
                        viewModel.addMediaAsset(
                            LocalMediaAsset(
                                title = titleFinal,
                                category = selectedCategory,
                                uri = uriFinal,
                                durationText = durationInput,
                                isAiGenerated = false,
                                dateAdded = "Baru saja"
                            )
                        )
                        showImportDialog = false
                        onShowSnackbar("Aset '${titleFinal}' berhasil diimpor ke pustaka!")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioDarkCTA),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Impor ke Pustaka", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Batal") }
            }
        )
    }
}

/**
 * Card Asset Item di Pustaka Media
 * Fitur: Preview kustom, Badge Kategori & AI, Tombol Tambah & Sisipkan (Drag/Drop trigger to Timeline).
 */
@Composable
fun MediaAssetCard(
    asset: LocalMediaAsset,
    onAddToTimeline: () -> Unit,
    onDragAndInsertToTimeline: () -> Unit,
    onDuplicateAsset: () -> Unit,
    onDeleteAsset: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, StudioCardHairline, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = StudioGlassWhite)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Asset Thumbnail Placeholder / Category Icon
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when (asset.category.uppercase()) {
                            "VIDEO" -> StudioElectricBlue.copy(alpha = 0.15f)
                            "AUDIO" -> StudioEmeraldGreen.copy(alpha = 0.15f)
                            else -> StudioRosePink.copy(alpha = 0.15f)
                        }
                    )
                    .border(
                        1.dp,
                        when (asset.category.uppercase()) {
                            "VIDEO" -> StudioElectricBlue.copy(alpha = 0.3f)
                            "AUDIO" -> StudioEmeraldGreen.copy(alpha = 0.3f)
                            else -> StudioRosePink.copy(alpha = 0.3f)
                        },
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = when (asset.category.uppercase()) {
                            "VIDEO" -> Icons.Default.Movie
                            "AUDIO" -> Icons.Default.MusicNote
                            else -> Icons.Default.Image
                        },
                        contentDescription = null,
                        tint = when (asset.category.uppercase()) {
                            "VIDEO" -> StudioElectricBlue
                            "AUDIO" -> StudioEmeraldGreen
                            else -> StudioRosePink
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = asset.durationText,
                        color = StudioTextDark,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = asset.title,
                    color = StudioTextDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Category badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(StudioPillBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(asset.category, fontSize = 10.sp, color = StudioTextDark, fontWeight = FontWeight.Bold)
                    }

                    if (asset.isAiGenerated) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(StudioElectricBlue.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = StudioElectricBlue, modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("AI Generated", fontSize = 10.sp, color = StudioElectricBlue, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text(asset.resolutionOrType, fontSize = 10.sp, color = StudioTextMuted)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action buttons row: Add to Timeline / Duplicate (Salin) / Drag to Editor / Delete
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Button 1: Quick Add to Timeline
                    Button(
                        onClick = onAddToTimeline,
                        colors = ButtonDefaults.buttonColors(containerColor = StudioDarkCTA),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Tambah", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Button 2: Salin (Duplikat Aset)
                    OutlinedButton(
                        onClick = onDuplicateAsset,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, StudioElectricBlue.copy(alpha = 0.5f)),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = StudioElectricBlue, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Salin", fontSize = 11.sp, color = StudioElectricBlue, fontWeight = FontWeight.Bold)
                    }

                    // Button 3: Drag & Insert to Timeline (Switches to Editor)
                    OutlinedButton(
                        onClick = onDragAndInsertToTimeline,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, StudioEmeraldGreen.copy(alpha = 0.5f)),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DragHandle, contentDescription = null, tint = StudioEmeraldGreen, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Editor", fontSize = 11.sp, color = StudioEmeraldGreen, fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                        onClick = onDeleteAsset,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete Asset", tint = StudioRosePink, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
