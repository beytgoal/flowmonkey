package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VideoSettings
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

    val userProfile by viewModel.userProfileState.collectAsState()
    val apiKeys by viewModel.apiKeysState.collectAsState()
    val highfieldSettings by viewModel.highfieldSettingsState.collectAsState()

    var showApiSettingsDialog by remember { mutableStateOf(false) }

    val qualityModes = listOf(
        "360p Proxy" to "360p (Cepat & Ringan)",
        "540p Proxy" to "540p (Seimbang)",
        "720p Proxy" to "720p HD (Kualitas Tinggi)",
        "1080p Original" to "1080p Asli (Kualitas Penuh)"
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
            .background(StudioCleanCanvas)
            .padding(16.dp)
            .testTag("settings_screen_container")
    ) {
        // Section Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = StudioPastelSky,
                shape = CircleShape,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.VideoSettings,
                        contentDescription = null,
                        tint = StudioElectricBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Pengaturan",
                    color = StudioTextDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = (-0.4).sp
                )
                Text(
                    text = "Kelola akun pengguna, API key, dan performa pemutaran.",
                    color = StudioTextMuted,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // User Account & API Key Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StudioGlassWhite),
                    border = BorderStroke(1.dp, StudioCardHairline),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    color = StudioPastelSky,
                                    shape = CircleShape,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.AccountCircle,
                                            contentDescription = null,
                                            tint = StudioElectricBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = userProfile.userName,
                                        color = StudioTextDark,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = userProfile.userEmail,
                                        color = StudioTextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            if (userProfile.isGLoggedIn) {
                                FilledTonalButton(
                                    onClick = {
                                        viewModel.userProfileState.value = userProfile.copy(
                                            isGLoggedIn = false,
                                            isLoggedIn = false,
                                            userName = "Tamu (Mode Offline)",
                                            userEmail = "tamu@flowmonkey.studio"
                                        )
                                    },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = StudioPastelRose,
                                        contentColor = StudioRosePink
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Logout", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StudioRosePink)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        viewModel.userProfileState.value = userProfile.copy(
                                            isGLoggedIn = true,
                                            isLoggedIn = true,
                                            userName = "Creator Google User",
                                            userEmail = "cpktemon@gmail.com"
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = StudioElectricBlue,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Connect Google", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = StudioPillBg,
                            border = BorderStroke(1.dp, StudioCardHairline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Status Kunci API AI:",
                                        fontSize = 11.sp,
                                        color = StudioTextMuted
                                    )
                                    Text(
                                        text = if (apiKeys.googleGeminiApiKey.isNotBlank()) "Kustom Terpasang" else "Standar Studio",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (apiKeys.googleGeminiApiKey.isNotBlank()) StudioEmeraldGreen else StudioElectricBlue
                                    )
                                }

                                Button(
                                    onClick = { showApiSettingsDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = StudioDarkCTA),
                                    shape = RoundedCornerShape(18.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Kelola Kunci", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Preview Performance Mode
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StudioGlassWhite),
                    border = BorderStroke(1.dp, StudioCardHairline),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Surface(
                                    color = StudioPastelMint,
                                    shape = CircleShape,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Speed,
                                            contentDescription = null,
                                            tint = StudioEmeraldGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Akselerasi Pratinjau Lancar",
                                        color = StudioTextDark,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Optimalkan pemutaran timeline agar selalu responsif.",
                                        color = StudioTextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Switch(
                                checked = isProxyMode,
                                onCheckedChange = { viewModel.toggleProxyMode(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = StudioElectricBlue
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Otomatisasi Media Baru",
                                    color = StudioTextDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Optimalkan media secara instan saat ditambahkan.",
                                    color = StudioTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = autoTranscode,
                                onCheckedChange = { viewModel.toggleAutoTranscode(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = StudioElectricBlue
                                )
                            )
                        }
                    }
                }
            }

            // Quality Target Resolution Selection
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StudioGlassWhite),
                    border = BorderStroke(1.dp, StudioCardHairline),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Resolusi Kualitas Pratinjau",
                            color = StudioTextDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Pilih tingkat detail pemutaran saat mengedit di timeline.",
                            color = StudioTextMuted,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        qualityModes.forEach { pair ->
                            val resKey = pair.first
                            val resLabel = pair.second
                            val isSelected = (isProxyMode && proxyResolution.contains(resKey.take(4))) || (!isProxyMode && resKey.contains("1080p"))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.setProxyResolution(resKey) }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.setProxyResolution(resKey) },
                                    colors = RadioButtonDefaults.colors(selectedColor = StudioElectricBlue)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = resLabel,
                                    color = if (isSelected) StudioTextDark else StudioTextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
