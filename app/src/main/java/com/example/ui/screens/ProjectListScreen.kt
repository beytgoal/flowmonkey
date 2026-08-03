package com.example.ui.screens

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.db.VideoProjectEntity
import com.example.ui.theme.*
import com.example.ui.viewmodels.MainTab
import com.example.ui.viewmodels.VideoStudioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    viewModel: VideoStudioViewModel,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.allProjects.collectAsState()
    val activeProjectId by viewModel.activeProjectId.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

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
                    Text("Daftar Proyek Video", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("${projects.size} proyek tersimpan", color = StudioTextSecondary, fontSize = 12.sp)
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

        projects.forEach { project ->
            val isActive = project.id == activeProjectId
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        1.dp,
                        if (isActive) StudioSecondaryTeal else StudioCardBorder,
                        RoundedCornerShape(14.dp)
                    )
                    .clickable {
                        viewModel.selectProject(project.id)
                        viewModel.selectTab(MainTab.TIMELINE_EDITOR)
                    }
                    .testTag("project_item_${project.id}"),
                colors = CardDefaults.cardColors(containerColor = StudioCardBg)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
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

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                project.title,
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

                    IconButton(onClick = { viewModel.deleteProject(project) }) {
                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete", tint = StudioAccentPink)
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
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
