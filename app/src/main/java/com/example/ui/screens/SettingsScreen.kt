package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
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
import com.example.ui.components.ApiKeysAndSettingsDialog
import com.example.ui.theme.*
import com.example.ui.viewmodels.VideoStudioViewModel

@Composable
fun SettingsScreen(
    viewModel: VideoStudioViewModel,
    modifier: Modifier = Modifier
) {
    val isProxyMode by viewModel.isProxyModeEnabled.collectAsState()
    val proxyResolution by viewModel.proxyResolution.collectAsState()
    val autoTranscode by viewModel.autoTranscodeOnImport.collectAsState()
    val transcodingJobs by viewModel.transcodingJobs.collectAsState()

    val userProfile by viewModel.userProfileState.collectAsState()
    val apiKeys by viewModel.apiKeysState.collectAsState()
    val highfieldSettings by viewModel.highfieldSettingsState.collectAsState()

    var showApiSettingsDialog by remember { mutableStateOf(false) }

    val qualityModes = listOf(
        "360p Proxy" to "360p Proxy",
        "540p Proxy" to "540p Proxy",
        "720p Proxy" to "720p HD Proxy",
        "1080p Original" to "1080p Original"
    )

    if (showApiSettingsDialog) {
        ApiKeysAndSettingsDialog(
            userProfile = userProfile,
            apiKeys = apiKeys,
            highfieldSettings = highfieldSettings,
            onDismiss = { showApiSettingsDialog = false },
            onSaveProfileAndKeys = { updatedProfile, updatedKeys, updatedSettings ->
                viewModel.userProfileState.value = updatedProfile
                viewModel.apiKeysState.value = updatedKeys
                viewModel.highfieldSettingsState.value = updatedSettings
                com.example.data.api.ApiClient.setUserApiKey(updatedKeys.googleGeminiApiKey)
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioDarkBg)
            .padding(14.dp)
            .testTag("settings_screen_container")
    ) {
        // Section Header Title
        Text(
            text = "Pengaturan Studio",
            color = StudioTextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            letterSpacing = (-0.3).sp
        )
        Text(
            text = "Kelola akun Google OAuth, API key terarah pengguna, dan performa proxy.",
            color = StudioTextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Google OAuth & Direct User API Key Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                    border = BorderStroke(1.dp, StudioPrimaryViolet.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = StudioSecondaryTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Akun Pengguna & Google OAuth",
                                    color = StudioTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Surface(
                                color = if (userProfile.isGLoggedIn) Color(0x2E10B981) else Color(0x2EEF4444),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = if (userProfile.isGLoggedIn) "TERAUTENTIKASI" else "BELUM LOGIN",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (userProfile.isGLoggedIn) Color(0xFF10B981) else Color(0xFFEF4444),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "API Key Gemini dan model generator diarahkan langsung dari akun terotentikasi pengguna (${userProfile.userEmail}).",
                            color = StudioTextSecondary,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(StudioSurfaceDark, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "API Key Pengguna Aktif:",
                                    fontSize = 10.sp,
                                    color = StudioTextSecondary
                                )
                                Text(
                                    text = if (apiKeys.googleGeminiApiKey.isNotBlank()) "Kustom Terpasang (${apiKeys.googleGeminiApiKey.take(8)}...)" else "Akun Pengguna Auto-Directed (OAuth)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (apiKeys.googleGeminiApiKey.isNotBlank()) StudioSecondaryTeal else Color(0xFF818CF8)
                                )
                            }

                            Button(
                                onClick = { showApiSettingsDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = StudioPrimaryViolet),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Kelola API", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Quality Mode Switch Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                    border = BorderStroke(1.dp, StudioCardBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Mode Proxy Pratinjau",
                                color = StudioTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Gunakan proxy ringan agar timeline tetap lancar.",
                                color = StudioTextSecondary,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Switch(
                            checked = isProxyMode,
                            onCheckedChange = { viewModel.toggleProxyMode(it) }
                        )
                    }
                }
            }

            // Auto Transcode Switch Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                    border = BorderStroke(1.dp, StudioCardBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto Transcode Impor",
                                color = StudioTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Otomatis konversi klip baru saat diimpor.",
                                color = StudioTextSecondary,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Switch(
                            checked = autoTranscode,
                            onCheckedChange = { viewModel.toggleAutoTranscode(it) }
                        )
                    }
                }
            }

            // Target Resolution Selection Section
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                    border = BorderStroke(1.dp, StudioCardBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Target Resolusi Proxy",
                            color = StudioTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        qualityModes.forEach { pair ->
                            val resKey = pair.first
                            val resLabel = pair.second
                            val isSelected = (isProxyMode && proxyResolution.contains(resKey.take(4))) || (!isProxyMode && resKey.contains("1080p"))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setProxyResolution(resKey) }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
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
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // Native Engine Architecture & APK Size Optimization Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                    border = BorderStroke(1.dp, StudioSecondaryTeal.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = StudioSecondaryTeal,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Mesin Multimedia Native Teroptimasi",
                                    color = StudioTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                color = StudioSecondaryTeal.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Hemat ~119MB APK",
                                    color = StudioSecondaryTeal,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // FFmpeg Trimmed Metric
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("• FFmpeg Codec Pruned (H.264/HEVC/AAC)", color = StudioTextSecondary, fontSize = 11.sp)
                            Text("Hemat ~42 MB", color = StudioAccentAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        // GStreamer gst-full Metric
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("• GStreamer \"gst-full\" Monolithic", color = StudioTextSecondary, fontSize = 11.sp)
                            Text("Hemat ~45 MB", color = StudioAccentAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        // MediaPipe Quantized Metric
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("• MediaPipe INT8 Quantized (No OpenCV)", color = StudioTextSecondary, fontSize = 11.sp)
                            Text("Hemat ~32 MB", color = StudioAccentAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        // OpenCV Vision Engine Metric
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("• OpenCV Vision (Optical Flow & HSL)", color = StudioTextSecondary, fontSize = 11.sp)
                            Text("Aktif (Zero-Copy)", color = StudioSecondaryTeal, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        // Stripped Debug Symbols Metric
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("• Native Symbol Stripped (-Wl,--strip-all)", color = StudioTextSecondary, fontSize = 11.sp)
                            Text("Hemat ~50% Size", color = StudioPrimaryViolet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        // NDK Shared Memory Metric
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("• NDK Shared Memory Buffer (Zero-Copy)", color = StudioTextSecondary, fontSize = 11.sp)
                            Text("Alokasi 0-Copy RAM", color = StudioSecondaryTeal, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Transcoding Jobs Queue
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Status Transcoder Latar Belakang",
                        color = StudioTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { viewModel.transcodeAllClipsToProxy() }) {
                        Text("Transcode Semua", fontSize = 11.sp, color = StudioPrimaryViolet)
                    }
                }
            }

            if (transcodingJobs.isEmpty()) {
                item {
                    Surface(
                        color = StudioCardBg,
                        border = BorderStroke(1.dp, StudioCardBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Tidak ada tugas transcode aktif.",
                            color = StudioTextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            } else {
                items(transcodingJobs) { job ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                        border = BorderStroke(1.dp, StudioCardBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = job.mediaTitle,
                                    color = StudioTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = if (job.isCompleted) "Selesai" else "${job.progressPercent}%",
                                    color = if (job.isCompleted) StudioSecondaryTeal else StudioAccentAmber,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { job.progressPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = if (job.isCompleted) StudioSecondaryTeal else StudioAccentAmber,
                                trackColor = StudioSurfaceDark
                            )
                        }
                    }
                }
            }
        }
    }
}
