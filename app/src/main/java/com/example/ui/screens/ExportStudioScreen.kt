package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.example.ui.components.VideoPlayerView
import com.example.ui.theme.*
import com.example.ui.viewmodels.VideoStudioViewModel

data class PlatformPreset(
    val name: String,
    val ratio: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val recommendedResolution: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportStudioScreen(
    viewModel: VideoStudioViewModel,
    modifier: Modifier = Modifier
) {
    val exportState by viewModel.exportState.collectAsState()
    val activeProject by viewModel.activeProject.collectAsState()

    var selectedPlatform by remember { mutableStateOf("TikTok (9:16)") }
    var selectedResolution by remember { mutableStateOf("1080p") }
    var selectedFps by remember { mutableStateOf(30) }

    val platforms = listOf(
        PlatformPreset("TikTok", "9:16", Icons.Default.MusicNote, "1080p Full HD"),
        PlatformPreset("Instagram Reels", "9:16", Icons.Default.CameraAlt, "1080p Full HD"),
        PlatformPreset("YouTube", "16:9", Icons.Default.PlayCircle, "4K Ultra HD"),
        PlatformPreset("YouTube Shorts", "9:16", Icons.Default.ElectricBolt, "1080p Full HD"),
        PlatformPreset("Twitter / X", "16:9", Icons.Default.Share, "1080p Full HD")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.IosShare,
                contentDescription = "Export Studio",
                tint = StudioSecondaryTeal,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Ekspor Video Media Sosial",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Optimasi format, resolusi tinggi, & framerate",
                    color = StudioTextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Platform Target Cards Grid
        Text("Pilih Platform Target:", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            platforms.forEach { preset ->
                val isSelected = selectedPlatform == "${preset.name} (${preset.ratio})"
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            1.dp,
                            if (isSelected) StudioPrimaryViolet else StudioCardBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedPlatform = "${preset.name} (${preset.ratio})" }
                        .testTag("export_platform_${preset.name}"),
                    colors = CardDefaults.cardColors(containerColor = StudioCardBg)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = if (isSelected) StudioPrimaryViolet else StudioSurfaceDark,
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = preset.icon,
                                        contentDescription = preset.name,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(preset.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Rasio: ${preset.ratio} • Rekomendasi: ${preset.recommendedResolution}", color = StudioTextSecondary, fontSize = 11.sp)
                            }
                        }

                        RadioButton(
                            selected = isSelected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = StudioPrimaryViolet)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Settings Row: Resolution & FPS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Resolution Selection Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, StudioCardBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = StudioCardBg)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Resolusi Video", color = StudioTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    listOf("720p HD", "1080p FHD", "4K Ultra HD").forEach { res ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedResolution = res }
                        ) {
                            RadioButton(selected = selectedResolution == res, onClick = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(res, color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }

            // FPS Selection Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, StudioCardBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = StudioCardBg)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Framerate (FPS)", color = StudioTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    listOf(24, 30, 60).forEach { fpsVal ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedFps = fpsVal }
                        ) {
                            RadioButton(selected = selectedFps == fpsVal, onClick = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("$fpsVal FPS", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Export Render Action Button
        Button(
            onClick = { viewModel.startExport(selectedPlatform, selectedResolution, selectedFps) },
            enabled = !exportState.isExporting,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("start_export_video_button"),
            colors = ButtonDefaults.buttonColors(containerColor = StudioSecondaryTeal, contentColor = Color.Black),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (exportState.isExporting) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.Black)
                Spacer(modifier = Modifier.width(10.dp))
                Text("MENGAMBIL & MERENDER FRAME... (${exportState.progressPercent}%)", fontWeight = FontWeight.Bold)
            } else {
                Icon(imageVector = Icons.Default.Download, contentDescription = "Export")
                Spacer(modifier = Modifier.width(8.dp))
                Text("EKSPOR VIDEO BERKUALITAS TINGGI", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        // Live Render Progress Box
        if (exportState.isExporting) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StudioSecondaryTeal, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = StudioSurfaceDark)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Rendering Frame ${exportState.currentFrame} / ${exportState.totalFrames}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("${exportState.progressPercent}%", color = StudioSecondaryTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { exportState.progressPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = StudioSecondaryTeal,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                }
            }
        }

        // Output Result Video Player Card
        if (exportState.exportedVideoUri != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StudioPrimaryViolet, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = StudioCardBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Hasil Ekspor Video Berhasil!", color = StudioSecondaryTeal, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Surface(
                            color = StudioPrimaryViolet.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(selectedResolution, color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    VideoPlayerView(
                        aspectRatioStr = if (selectedPlatform.contains("9:16")) "9:16" else "16:9",
                        isPlaying = false,
                        totalDurationMs = 15000L
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { /* Share */ },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = StudioPrimaryViolet)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Bagikan")
                        }

                        OutlinedButton(
                            onClick = { /* Save */ },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, StudioSecondaryTeal),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioSecondaryTeal)
                        ) {
                            Icon(imageVector = Icons.Default.SaveAlt, contentDescription = "Save", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simpan MP4")
                        }
                    }
                }
            }
        }
    }
}
