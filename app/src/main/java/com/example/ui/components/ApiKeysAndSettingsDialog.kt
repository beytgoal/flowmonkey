package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import com.example.data.models.HighfieldSettings
import com.example.data.models.MultiModelApiKeys
import com.example.data.models.SupportedAiModels
import com.example.data.models.UserProfile
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeysAndSettingsDialog(
    userProfile: UserProfile,
    apiKeys: MultiModelApiKeys,
    highfieldSettings: HighfieldSettings,
    onDismiss: () -> Unit,
    onSaveProfileAndKeys: (UserProfile, MultiModelApiKeys, HighfieldSettings) -> Unit
) {
    var geminiKey by remember { mutableStateOf(apiKeys.googleGeminiApiKey) }
    var openAiKey by remember { mutableStateOf(apiKeys.openAiSoraApiKey) }
    var claudeKey by remember { mutableStateOf(apiKeys.anthropicClaudeApiKey) }
    var kimiKey by remember { mutableStateOf(apiKeys.kimiAiDirectorApiKey) }
    var runwayKey by remember { mutableStateOf(apiKeys.runwayGen3ApiKey) }
    var lumaKey by remember { mutableStateOf(apiKeys.lumaDreamMachineApiKey) }

    var selectedEngine by remember { mutableStateOf(highfieldSettings.selectedEngine) }
    var fpsTarget by remember { mutableStateOf(highfieldSettings.fpsTarget) }
    var engineEnabled by remember { mutableStateOf(highfieldSettings.isEngineModeEnabled) }
    var raytracingEnabled by remember { mutableStateOf(highfieldSettings.raytracingSimulation) }

    var isGAuthLoggedIn by remember { mutableStateOf(userProfile.isGLoggedIn) }
    var currentUserName by remember { mutableStateOf(userProfile.userName) }
    var currentUserEmail by remember { mutableStateOf(userProfile.userEmail) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = StudioGlassWhite,
            border = BorderStroke(1.dp, StudioCardHairline),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
                .testTag("api_keys_settings_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxHeight(0.88f)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = StudioElectricBlue.copy(alpha = 0.12f),
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = StudioElectricBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Setelan Kunci API & AI",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = StudioTextDark
                            )
                            Text(
                                text = "Kredensial & Kualitas Generator",
                                fontSize = 11.sp,
                                color = StudioTextMuted
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = StudioTextDark.copy(alpha = 0.7f)
                        )
                    }
                }

                HorizontalDivider(color = StudioCardHairline, modifier = Modifier.padding(vertical = 12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // SECTION 1: Google Account
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = StudioPillBg,
                        border = BorderStroke(1.dp, StudioCardHairline),
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
                                        tint = StudioElectricBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Akun Pengguna Google",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StudioTextDark
                                    )
                                }

                                if (isGAuthLoggedIn) {
                                    FilledTonalButton(
                                        onClick = {
                                            isGAuthLoggedIn = false
                                            currentUserName = "Tamu (Mode Offline)"
                                            currentUserEmail = "tamu@flowmonkey.studio"
                                        },
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = StudioRosePink.copy(alpha = 0.12f),
                                            contentColor = StudioRosePink
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .height(32.dp)
                                            .testTag("google_oauth_logout_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Logout,
                                            contentDescription = "Logout",
                                            tint = StudioRosePink,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Logout",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = StudioRosePink
                                        )
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            isGAuthLoggedIn = true
                                            currentUserName = "Creator Google User"
                                            currentUserEmail = "cpktemon@gmail.com"
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = StudioElectricBlue,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .height(32.dp)
                                            .testTag("google_oauth_connect_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Login,
                                            contentDescription = "Connect Google",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Hubungkan Google",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isGAuthLoggedIn) 
                                    "Aplikasi terhubung dengan akun Google Anda via OAuth untuk sinkronisasi cloud dan fitur Gemini." 
                                    else "Hubungkan akun Google OAuth Anda untuk mengaktifkan sinkronisasi cloud dan Gemini AI.",
                                fontSize = 11.sp,
                                color = StudioTextMuted
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(StudioGlassWhite, RoundedCornerShape(12.dp))
                                    .border(1.dp, if (isGAuthLoggedIn) StudioEmeraldGreen.copy(alpha = 0.3f) else StudioCardHairline, RoundedCornerShape(12.dp))
                                    .padding(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (isGAuthLoggedIn) Icons.Default.CheckCircle else Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = if (isGAuthLoggedIn) StudioEmeraldGreen else StudioTextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = currentUserName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StudioTextDark
                                    )
                                    Text(
                                        text = currentUserEmail,
                                        fontSize = 10.sp,
                                        color = StudioTextMuted
                                    )
                                }
                            }
                        }
                    }

                    // SECTION 2: External Multi-Model API Keys
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = StudioPillBg,
                        border = BorderStroke(1.dp, StudioCardHairline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = null,
                                    tint = StudioAmberGold,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Kunci API Model Eksternal",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StudioTextDark
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Masukkan API key Anda untuk menggunakan generator video dan sutradara AI eksternal.",
                                fontSize = 11.sp,
                                color = StudioTextMuted
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Google Gemini API Key
                            OutlinedTextField(
                                value = geminiKey,
                                onValueChange = { geminiKey = it },
                                label = { Text("Google Gemini API Key", fontSize = 11.sp, color = StudioTextMuted) },
                                placeholder = { Text("AIzaSy...", fontSize = 11.sp, color = StudioTextMuted.copy(alpha = 0.5f)) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = StudioElectricBlue,
                                    unfocusedBorderColor = StudioCardHairline,
                                    focusedTextColor = StudioTextDark,
                                    unfocusedTextColor = StudioTextDark,
                                    focusedContainerColor = StudioCleanCanvas,
                                    unfocusedContainerColor = StudioCleanCanvas
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_gemini_api_key")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // OpenAI Sora API Key
                            OutlinedTextField(
                                value = openAiKey,
                                onValueChange = { openAiKey = it },
                                label = { Text("OpenAI Sora API Key", fontSize = 11.sp, color = StudioTextMuted) },
                                placeholder = { Text("sk-...", fontSize = 11.sp, color = StudioTextMuted.copy(alpha = 0.5f)) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = StudioElectricBlue,
                                    unfocusedBorderColor = StudioCardHairline,
                                    focusedTextColor = StudioTextDark,
                                    unfocusedTextColor = StudioTextDark,
                                    focusedContainerColor = StudioCleanCanvas,
                                    unfocusedContainerColor = StudioCleanCanvas
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_openai_api_key")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Anthropic Claude API Key
                            OutlinedTextField(
                                value = claudeKey,
                                onValueChange = { claudeKey = it },
                                label = { Text("Anthropic Claude API Key", fontSize = 11.sp, color = StudioTextMuted) },
                                placeholder = { Text("sk-ant-...", fontSize = 11.sp, color = StudioTextMuted.copy(alpha = 0.5f)) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = StudioElectricBlue,
                                    unfocusedBorderColor = StudioCardHairline,
                                    focusedTextColor = StudioTextDark,
                                    unfocusedTextColor = StudioTextDark,
                                    focusedContainerColor = StudioCleanCanvas,
                                    unfocusedContainerColor = StudioCleanCanvas
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_claude_api_key")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Kimi AI Director Key
                            OutlinedTextField(
                                value = kimiKey,
                                onValueChange = { kimiKey = it },
                                label = { Text("Kimi AI API Key", fontSize = 11.sp, color = StudioTextMuted) },
                                placeholder = { Text("kimi-...", fontSize = 11.sp, color = StudioTextMuted.copy(alpha = 0.5f)) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = StudioElectricBlue,
                                    unfocusedBorderColor = StudioCardHairline,
                                    focusedTextColor = StudioTextDark,
                                    unfocusedTextColor = StudioTextDark,
                                    focusedContainerColor = StudioCleanCanvas,
                                    unfocusedContainerColor = StudioCleanCanvas
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_kimi_api_key")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Runway & Luma
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = runwayKey,
                                    onValueChange = { runwayKey = it },
                                    label = { Text("Runway Gen-3", fontSize = 10.sp, color = StudioTextMuted) },
                                    placeholder = { Text("rw-...", fontSize = 10.sp, color = StudioTextMuted.copy(alpha = 0.5f)) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = StudioElectricBlue,
                                        unfocusedBorderColor = StudioCardHairline,
                                        focusedTextColor = StudioTextDark,
                                        unfocusedTextColor = StudioTextDark,
                                        focusedContainerColor = StudioCleanCanvas,
                                        unfocusedContainerColor = StudioCleanCanvas
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = lumaKey,
                                    onValueChange = { lumaKey = it },
                                    label = { Text("Luma Dream", fontSize = 10.sp, color = StudioTextMuted) },
                                    placeholder = { Text("luma-...", fontSize = 10.sp, color = StudioTextMuted.copy(alpha = 0.5f)) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = StudioElectricBlue,
                                        unfocusedBorderColor = StudioCardHairline,
                                        focusedTextColor = StudioTextDark,
                                        unfocusedTextColor = StudioTextDark,
                                        focusedContainerColor = StudioCleanCanvas,
                                        unfocusedContainerColor = StudioCleanCanvas
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // SECTION 3: Studio AI Workflow Quality
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = StudioPillBg,
                        border = BorderStroke(1.dp, StudioCardHairline),
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
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = StudioElectricBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Kualitas Alur Kerja AI",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StudioTextDark
                                    )
                                }

                                Switch(
                                    checked = engineEnabled,
                                    onCheckedChange = { engineEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = StudioElectricBlue
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Model Generator Utama:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StudioTextDark
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            SupportedAiModels.models.take(4).forEach { engine ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                ) {
                                    RadioButton(
                                        selected = selectedEngine == engine,
                                        onClick = { selectedEngine = engine },
                                        colors = RadioButtonDefaults.colors(selectedColor = StudioElectricBlue)
                                    )
                                    Text(
                                        text = engine,
                                        fontSize = 11.sp,
                                        color = StudioTextDark
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // FPS Target Selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Target Framerate:",
                                    fontSize = 11.sp,
                                    color = StudioTextDark,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(24, 30, 60).forEach { target ->
                                        FilterChip(
                                            selected = fpsTarget == target,
                                            onClick = { fpsTarget = target },
                                            label = { Text("${target} FPS", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = StudioDarkCTA,
                                                selectedLabelColor = Color.White,
                                                containerColor = StudioGlassWhite,
                                                labelColor = StudioTextDark
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Pencahayaan Sinematik AI",
                                    fontSize = 11.sp,
                                    color = StudioTextDark
                                )

                                Checkbox(
                                    checked = raytracingEnabled,
                                    onCheckedChange = { raytracingEnabled = it },
                                    colors = CheckboxDefaults.colors(checkedColor = StudioElectricBlue)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Save button (iOS Dark Pill CTA)
                Button(
                    onClick = {
                        val updatedProfile = userProfile.copy(
                            isGLoggedIn = isGAuthLoggedIn,
                            isLoggedIn = isGAuthLoggedIn,
                            userName = currentUserName,
                            userEmail = currentUserEmail,
                            isCustomGeminiKeyActive = geminiKey.isNotBlank()
                        )
                        val updatedKeys = apiKeys.copy(
                            googleGeminiApiKey = geminiKey,
                            openAiSoraApiKey = openAiKey,
                            anthropicClaudeApiKey = claudeKey,
                            kimiAiDirectorApiKey = kimiKey,
                            runwayGen3ApiKey = runwayKey,
                            lumaDreamMachineApiKey = lumaKey
                        )
                        val updatedSettings = highfieldSettings.copy(
                            isEngineModeEnabled = engineEnabled,
                            selectedEngine = selectedEngine,
                            fpsTarget = fpsTarget,
                            raytracingSimulation = raytracingEnabled
                        )
                        onSaveProfileAndKeys(updatedProfile, updatedKeys, updatedSettings)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioDarkCTA, contentColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_settings_button")
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "SIMPAN KUNCI API & SETELAN", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
