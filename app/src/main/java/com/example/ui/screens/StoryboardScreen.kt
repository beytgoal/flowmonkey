package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.StoryboardSceneEntity
import com.example.ui.components.VideoFlowView
import com.example.ui.theme.*
import com.example.ui.viewmodels.VideoStudioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryboardScreen(
    viewModel: VideoStudioViewModel,
    modifier: Modifier = Modifier
) {
    val activeProject by viewModel.activeProject.collectAsState()
    val scenes by viewModel.storyboardScenes.collectAsState()
    val directorState by viewModel.directorGenState.collectAsState()

    var conceptInput by remember { mutableStateOf("Peluncuran mobil listrik futuristik di tengah kota neon") }
    var selectedViewMode by remember { mutableStateOf(0) } // 0: Scenes List, 1: VideoFlow Graph
    var showTemplateDialog by remember { mutableStateOf(false) }
    var sceneToDelete by remember { mutableStateOf<StoryboardSceneEntity?>(null) }

    val cameraMovements = listOf("Pan Right", "Zoom In", "Orbit", "Drone Overhead", "Handheld")

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        // Director AI High Thinking Mode Banner Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StudioCardHairline, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = StudioGlassWhite)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = StudioPastelSky,
                            shape = CircleShape,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = "Director AI",
                                    tint = StudioElectricBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Sutradara AI High Thinking Mode",
                                color = StudioTextDark,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Kreativitas Mendalam & Alur Cerita Cerdas",
                                color = StudioEmeraldGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Surface(
                        color = StudioPastelRose,
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Thinking Mode Active",
                            tint = StudioRosePink,
                            modifier = Modifier.padding(8.dp).size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = conceptInput,
                    onValueChange = { conceptInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(95.dp)
                        .testTag("director_concept_input"),
                    label = { Text("Konsep Cerita / Iklan", color = StudioTextMuted, fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StudioElectricBlue,
                        unfocusedBorderColor = StudioCardHairline,
                        focusedTextColor = StudioTextDark,
                        unfocusedTextColor = StudioTextDark,
                        focusedContainerColor = StudioCleanCanvas,
                        unfocusedContainerColor = StudioCleanCanvas
                    ),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.generateDirectorStoryboard(
                                concept = conceptInput,
                                style = activeProject?.visualStyle ?: "Cinematic 8K"
                            )
                        },
                        enabled = !directorState.isGenerating && conceptInput.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("generate_storyboard_thinking_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = StudioDarkCTA),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        if (directorState.isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                        } else {
                            Text(text = "Rancang AI", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = { showTemplateDialog = true },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("open_template_selector_button"),
                        border = BorderStroke(1.dp, StudioElectricBlue),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioElectricBlue),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(text = "Template", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Toggle View: Scene Cards vs VideoFlow Graph
        TabRow(
            selectedTabIndex = selectedViewMode,
            containerColor = StudioGlassWhite,
            contentColor = StudioTextDark,
            divider = {},
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, StudioCardHairline, RoundedCornerShape(16.dp))
                .testTag("storyboard_tab_row")
        ) {
            Tab(
                selected = selectedViewMode == 0,
                onClick = { selectedViewMode = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.ViewList, contentDescription = "List", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kartu Adegan (${scenes.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                selectedContentColor = StudioElectricBlue,
                unselectedContentColor = StudioTextMuted
            )
            Tab(
                selected = selectedViewMode == 1,
                onClick = { selectedViewMode = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AccountTree, contentDescription = "Flow", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Visual VideoFlow", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                selectedContentColor = StudioElectricBlue,
                unselectedContentColor = StudioTextMuted
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedViewMode == 1) {
            VideoFlowView(scenes = scenes)
        } else {
            // Scene Cards List
            scenes.forEachIndexed { index, scene ->
                StoryboardSceneCard(
                    scene = scene,
                    cameraMovements = cameraMovements,
                    onUpdateScene = { updated -> viewModel.updateScene(updated) },
                    onDeleteScene = { toDelete -> sceneToDelete = toDelete }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Combine and send scenes to Timeline Button
            Button(
                onClick = { viewModel.applyStoryboardToTimeline() },
                enabled = scenes.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("apply_storyboard_timeline_button"),
                colors = ButtonDefaults.buttonColors(containerColor = StudioDarkCTA, contentColor = Color.White),
                shape = RoundedCornerShape(26.dp)
            ) {
                Text(text = "PENGGABUNGAN OTOMATIS & TRANSISI SMART", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }

    // Dialog Konfirmasi Hapus Adegan Storyboard
    sceneToDelete?.let { scene ->
        AlertDialog(
            onDismissRequest = { sceneToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, tint = StudioAccentPink)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hapus Adegan?", fontWeight = FontWeight.Bold, color = StudioTextDark)
                }
            },
            text = {
                Text(
                    "Apakah Anda yakin ingin menghapus adegan ${scene.sceneIndex + 1} ('${scene.title}') dari storyboard?",
                    color = StudioTextDark,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteScene(scene)
                        sceneToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioAccentPink),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Hapus", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { sceneToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Storyboard Template Selector Dialog
    if (showTemplateDialog) {
        com.example.ui.components.StoryboardTemplateSelectorDialog(
            onDismiss = { showTemplateDialog = false },
            onSelectTemplate = { selectedTemplate ->
                viewModel.applyTemplateToActiveProject(selectedTemplate)
            }
        )
    }
}

@Composable
fun StoryboardSceneCard(
    scene: StoryboardSceneEntity,
    cameraMovements: List<String>,
    onUpdateScene: (StoryboardSceneEntity) -> Unit,
    onDeleteScene: (StoryboardSceneEntity) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, StudioCardHairline, RoundedCornerShape(18.dp))
            .testTag("storyboard_scene_card_${scene.id}"),
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
                        color = StudioDarkCTA,
                        shape = CircleShape,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${scene.sceneIndex + 1}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = scene.title,
                        color = StudioTextDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row {
                    IconButton(onClick = { isExpanded = !isExpanded }) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand",
                            tint = StudioTextMuted
                        )
                    }
                    IconButton(onClick = { onDeleteScene(scene) }) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = StudioRosePink
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Naskah: ${scene.scriptText}",
                color = StudioTextMuted,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = StudioPastelSky,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Kamera: ${scene.cameraMovement}",
                        color = StudioElectricBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = StudioPastelMint,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Durasi: ${scene.durationSeconds}s",
                        color = StudioEmeraldGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = StudioCardHairline)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Prompt Visual Video AI:", color = StudioTextDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = scene.visualPrompt,
                        onValueChange = { onUpdateScene(scene.copy(visualPrompt = it)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StudioElectricBlue,
                            unfocusedBorderColor = StudioCardHairline,
                            focusedTextColor = StudioTextDark,
                            unfocusedTextColor = StudioTextDark,
                            focusedContainerColor = StudioCleanCanvas,
                            unfocusedContainerColor = StudioCleanCanvas
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    }
}
