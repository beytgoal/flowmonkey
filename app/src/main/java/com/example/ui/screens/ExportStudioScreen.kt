package com.example.ui.screens

import androidx.activity.compose.BackHandler
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

    var selectedPlatform by remember { mutableStateOf("TikTok 9:16") }
    var selectedResolution by remember { mutableStateOf("1080p") }
    var selectedFps by remember { mutableStateOf(30) }

    BackHandler(enabled = true) {
        viewModel.selectTab(com.example.ui.viewmodels.MainTab.TIMELINE_EDITOR)
    }

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
                tint = StudioElectricBlue,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Ekspor Video Media Sosial",
                    color = StudioTextDark,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Optimasi format, resolusi tinggi, & framerate",
                    color = StudioTextMuted,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Platform Target Cards Grid
        Text("Pilih Platform Target:", color = StudioTextDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            platforms.forEach { preset ->
                val isSelected = selectedPlatform == "${preset.name} ${preset.ratio}"
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            1.dp,
                            if (isSelected) StudioElectricBlue else StudioCardHairline,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { selectedPlatform = "${preset.name} ${preset.ratio}" }
                        .testTag("export_platform_${preset.name}"),
                    colors = CardDefaults.cardColors(containerColor = StudioGlassWhite)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = if (isSelected) StudioElectricBlue else StudioPillBg,
                                shape = CircleShape,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = preset.icon,
                                        contentDescription = preset.name,
                                        tint = if (isSelected) Color.White else StudioTextDark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(preset.name, color = StudioTextDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Rasio: ${preset.ratio} • Rekomendasi: ${preset.recommendedResolution}", color = StudioTextMuted, fontSize = 11.sp)
                            }
                        }

                        RadioButton(
                            selected = isSelected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = StudioElectricBlue)
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
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, StudioCardHairline, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = StudioGlassWhite)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Resolusi Video", color = StudioTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    listOf("720p HD", "1080p FHD", "4K Ultra HD").forEach { res ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedResolution = res }
                        ) {
                            RadioButton(
                                selected = selectedResolution == res,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = StudioElectricBlue)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(res, color = StudioTextDark, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // FPS Selection Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, StudioCardHairline, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = StudioGlassWhite)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Framerate (FPS)", color = StudioTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    listOf(24, 30, 60).forEach { fpsVal ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedFps = fpsVal }
                        ) {
                            RadioButton(
                                selected = selectedFps == fpsVal,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = StudioElectricBlue)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("$fpsVal FPS", color = StudioTextDark, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Export Render Action Button (Pill shaped iOS CTA)
        Button(
            onClick = { viewModel.startExport(selectedPlatform, selectedResolution, selectedFps) },
            enabled = !exportState.isExporting,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("start_export_video_button"),
            colors = ButtonDefaults.buttonColors(containerColor = StudioDarkCTA, contentColor = Color.White),
            shape = RoundedCornerShape(26.dp)
        ) {
            if (exportState.isExporting) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White)
                Spacer(modifier = Modifier.width(10.dp))
                Text("MERENDER FRAME... ${exportState.progressPercent}%", fontWeight = FontWeight.Bold, color = Color.White)
            } else {
                Text("EKSPOR VIDEO RESOLUSI TINGGI", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        // Live Render Progress Box
        if (exportState.isExporting) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, StudioElectricBlue, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = StudioGlassWhite)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Rendering Frame ${exportState.currentFrame} / ${exportState.totalFrames}", color = StudioTextDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("${exportState.progressPercent}%", color = StudioElectricBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { exportState.progressPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = StudioElectricBlue,
                        trackColor = StudioPillBg
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
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, StudioCardHairline, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = StudioGlassWhite)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ekspor Video Selesai!", color = StudioEmeraldGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Surface(
                            color = StudioPastelSky,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(selectedResolution, color = StudioElectricBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    VideoPlayerView(
                        aspectRatioStr = if (selectedPlatform.contains("9:16")) "9:16" else "16:9",
                        isPlaying = false,
                        totalDurationMs = 15000L
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { /* Share */ },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = StudioDarkCTA),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("Bagikan", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { /* Save */ },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, StudioElectricBlue),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioElectricBlue),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("Simpan MP4", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
