package com.example.ui.screens

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

    val userProjects = remember(projects) { projects.filter { !it.isTemplate } }
    val templateProjects = remember(projects) { projects.filter { it.isTemplate } }

    var selectedSubTab by remember { mutableIntStateOf(0) } // 0: Proyek, 1: Template
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedTemplateForMediaReplace by remember { mutableStateOf<VideoProjectEntity?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

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
                        tint = StudioPrimaryViolet,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Daftar Proyek & Template", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (selectedSubTab == 0) "${userProjects.size} proyek aktif" else "${templateProjects.size} template siap pakai",
                            color = StudioTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = StudioPrimaryViolet,
                    contentColor = Color.White,
                    modifier = Modifier.size(44.dp).testTag("fab_new_project")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "New Project")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sub Tab Navigation (Proyek vs Template)
            TabRow(
                selectedTabIndex = selectedSubTab,
                containerColor = StudioSurfaceDark,
                contentColor = Color.White,
                divider = {},
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, StudioCardBorder, RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Proyek (${userProjects.size})", fontWeight = FontWeight.Bold)
                        }
                    },
                    selectedContentColor = StudioSecondaryTeal,
                    unselectedContentColor = StudioTextSecondary
                )
                Tab(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Template (${templateProjects.size})", fontWeight = FontWeight.Bold)
                        }
                    },
                    selectedContentColor = StudioPrimaryViolet,
                    unselectedContentColor = StudioTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedSubTab == 0) {
                // TAB 0: PROYEK AKTIF
                if (userProjects.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.VideoCall,
                                contentDescription = null,
                                tint = StudioTextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Belum ada proyek aktif", color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Tarik proyek ke kiri untuk menjadikannya Template!", color = StudioTextSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { showCreateDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = StudioPrimaryViolet)
                            ) {
                                Text("+ Buat Proyek Baru")
                            }
                        }
                    }
                } else {
                    Text(
                        text = "💡 Petunjuk: Tarik (swipe) proyek ke kiri untuk membuka ikon Buat Template & Hapus",
                        fontSize = 11.sp,
                        color = StudioSecondaryTeal,
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
            } else {
                // TAB 1: TEMPLATE (Full Tools & Placeholder Media)
                if (templateProjects.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                        border = BorderStroke(1.dp, StudioPrimaryViolet.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = StudioPrimaryViolet,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Belum Ada Template Kustom", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Buka tab 'Proyek', lalu tarik (swipe) proyek ke kiri dan tekan ikon Buat Template untuk menyimpan preset full tools + placeholder!",
                                color = StudioTextSecondary,
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
                    Icon(imageVector = Icons.Default.PermMedia, contentDescription = null, tint = StudioSecondaryTeal)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Masukan Media Baru dari Galeri", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Template: ${template.title}",
                        fontSize = 12.sp,
                        color = StudioPrimaryViolet,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Ganti placeholder media di bawah dengan video/musik baru dari galeri device Anda:",
                        fontSize = 12.sp,
                        color = StudioTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = sampleMediaName,
                        onValueChange = { sampleMediaName = it },
                        label = { Text("File Video Utama / Galeri") },
                        leadingIcon = { Icon(imageVector = Icons.Default.VideoFile, contentDescription = null) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = sampleAudioName,
                        onValueChange = { sampleAudioName = it },
                        label = { Text("File Musik Audio / Galeri") },
                        leadingIcon = { Icon(imageVector = Icons.Default.AudioFile, contentDescription = null) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
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
                    colors = ButtonDefaults.buttonColors(containerColor = StudioSecondaryTeal)
                ) {
                    Text("Terapkan Media & Buat Proyek")
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
            title = { Text("Buat Proyek Video Baru", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("Judul Proyek") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Rasio Layar:", fontSize = 12.sp, color = StudioTextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("16:9", "9:16", "1:1").forEach { ratio ->
                            FilterChip(
                                selected = selectedRatio == ratio,
                                onClick = { selectedRatio = ratio },
                                label = { Text(ratio) }
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
                    colors = ButtonDefaults.buttonColors(containerColor = StudioPrimaryViolet)
                ) {
                    Text("Buat Proyek")
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
            .clip(RoundedCornerShape(14.dp))
            .background(StudioSurfaceDark)
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
                    .background(StudioSecondaryTeal)
                    .testTag("action_make_template_${project.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.BookmarkAdd,
                    contentDescription = "Buat Template",
                    tint = Color.Black
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
                    .background(StudioAccentPink)
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
                    if (isActive) StudioSecondaryTeal else StudioCardBorder,
                    RoundedCornerShape(14.dp)
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
            colors = CardDefaults.cardColors(containerColor = StudioCardBg)
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
                        .clip(RoundedCornerShape(10.dp))
                        .background(StudioSurfaceDark)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_style_cinematic_1785585807861),
                        contentDescription = "Project Preview",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = project.title,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        if (isActive) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Active Project",
                                tint = StudioSecondaryTeal,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Rasio: ${project.aspectRatio}", color = StudioTextSecondary, fontSize = 11.sp)
                        Text("Gaya: ${project.visualStyle}", color = StudioPrimaryViolet, fontSize = 11.sp)
                    }
                }

                // Drag indicator hint
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Swipe left",
                    tint = StudioTextSecondary.copy(alpha = 0.6f),
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
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, StudioPrimaryViolet.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .testTag("template_item_${template.id}"),
        colors = CardDefaults.cardColors(containerColor = StudioCardBg)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = StudioPrimaryViolet.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = StudioPrimaryViolet,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("FULL TOOLS TEMPLATE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = StudioPrimaryViolet)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rasio: ${template.aspectRatio}", color = StudioTextSecondary, fontSize = 11.sp)
                }

                IconButton(
                    onClick = onDeleteTemplate,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete Template", tint = StudioAccentPink, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = template.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = template.description.ifBlank { "Template preset timeline lengkap dengan filter, speed curve, subjudul & audio placeholder." },
                color = StudioTextSecondary,
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
                        color = StudioSurfaceDark,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(0.5.dp, StudioCardBorder)
                    ) {
                        Text(
                            text = "✓ $toolTag",
                            color = StudioSecondaryTeal,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                    colors = ButtonDefaults.buttonColors(containerColor = StudioPrimaryViolet),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gunakan Template", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onReplaceMedia,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioSecondaryTeal),
                    border = BorderStroke(1.dp, StudioSecondaryTeal),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
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
