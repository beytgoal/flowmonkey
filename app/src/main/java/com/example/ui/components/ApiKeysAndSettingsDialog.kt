package com.example.ui.components

import androidx.compose.foundation.background
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
    var highfieldEnabled by remember { mutableStateOf(highfieldSettings.isHighfieldModeEnabled) }
    var raytracingEnabled by remember { mutableStateOf(highfieldSettings.raytracingSimulation) }

    var isGAuthLoggedIn by remember { mutableStateOf(userProfile.isGLoggedIn) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF16161A),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
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
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Setelan API Keys & Studio Engine",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Divider(color = Color(0x1AFFFFFF), modifier = Modifier.padding(vertical = 12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // SECTION 1: Google Account & Firebase Auth
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0x0DFFFFFF),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF)),
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
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Google Account & Firebase Auth",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Surface(
                                    color = if (isGAuthLoggedIn) Color(0x2E10B981) else Color(0x2EEF4444),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (isGAuthLoggedIn) "TERTAUT" else "BELUM TERTAUT",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isGAuthLoggedIn) Color(0xFF10B981) else Color(0xFFEF4444),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Aplikasi terhubung dengan akun Google Anda untuk mengaktifkan API Key Gemini khusus pengguna.",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                                    .padding(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = userProfile.userName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = userProfile.userEmail,
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }

                    // SECTION 2: External API Keys
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0x0DFFFFFF),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "External Multi-Model API Keys",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Masukkan API key Anda untuk menggunakan generator video dan sutradara AI eksternal seperti Sora, Claude, Kimi, dan Runway.",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Google Gemini API Key
                            OutlinedTextField(
                                value = geminiKey,
                                onValueChange = { geminiKey = it },
                                label = { Text("Google Gemini API Key", fontSize = 11.sp) },
                                placeholder = { Text("AIzaSy...", fontSize = 11.sp, color = Color.Gray) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6366F1),
                                    unfocusedBorderColor = Color(0x33FFFFFF),
                                    focusedLabelColor = Color(0xFF818CF8),
                                    unfocusedLabelColor = Color.Gray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_gemini_api_key")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // OpenAI Sora API Key
                            OutlinedTextField(
                                value = openAiKey,
                                onValueChange = { openAiKey = it },
                                label = { Text("OpenAI ChatGPT / Sora API Key", fontSize = 11.sp) },
                                placeholder = { Text("sk-...", fontSize = 11.sp, color = Color.Gray) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF10B981),
                                    unfocusedBorderColor = Color(0x33FFFFFF),
                                    focusedLabelColor = Color(0xFF34D399),
                                    unfocusedLabelColor = Color.Gray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_openai_api_key")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Anthropic Claude API Key
                            OutlinedTextField(
                                value = claudeKey,
                                onValueChange = { claudeKey = it },
                                label = { Text("Anthropic Claude API Key Director AI", fontSize = 11.sp) },
                                placeholder = { Text("sk-ant-...", fontSize = 11.sp, color = Color.Gray) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFF59E0B),
                                    unfocusedBorderColor = Color(0x33FFFFFF),
                                    focusedLabelColor = Color(0xFFFBBF24),
                                    unfocusedLabelColor = Color.Gray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_claude_api_key")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Kimi AI Director Key
                            OutlinedTextField(
                                value = kimiKey,
                                onValueChange = { kimiKey = it },
                                label = { Text("Kimi AI Director API Key", fontSize = 11.sp) },
                                placeholder = { Text("kimi-...", fontSize = 11.sp, color = Color.Gray) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFEC4899),
                                    unfocusedBorderColor = Color(0x33FFFFFF),
                                    focusedLabelColor = Color(0xFFF472B6),
                                    unfocusedLabelColor = Color.Gray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_kimi_api_key")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Runway Gen-3 & Luma
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = runwayKey,
                                    onValueChange = { runwayKey = it },
                                    label = { Text("Runway Gen-3 Key", fontSize = 10.sp) },
                                    placeholder = { Text("rw-...", fontSize = 10.sp, color = Color.Gray) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF8B5CF6),
                                        unfocusedBorderColor = Color(0x33FFFFFF),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = lumaKey,
                                    onValueChange = { lumaKey = it },
                                    label = { Text("Luma Dream Key", fontSize = 10.sp) },
                                    placeholder = { Text("luma-...", fontSize = 10.sp, color = Color.Gray) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF06B6D4),
                                        unfocusedBorderColor = Color(0x33FFFFFF),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // SECTION 3: Studio AI Workflow Engine
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0x0DFFFFFF),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF)),
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
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Studio AI Workflow Quality",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Switch(
                                    checked = highfieldEnabled,
                                    onCheckedChange = { highfieldEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF6366F1)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Model Engine Utama:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.8f)
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
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF6366F1))
                                    )
                                    Text(
                                        text = engine,
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.9f)
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
                                    text = "Target Framerate FPS:",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(24, 30, 60).forEach { target ->
                                        FilterChip(
                                            selected = fpsTarget == target,
                                            onClick = { fpsTarget = target },
                                            label = { Text("${target} FPS", fontSize = 10.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF6366F1),
                                                selectedLabelColor = Color.White
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
                                    text = "Raytracing & Dynamic Lighting Engine",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )

                                Checkbox(
                                    checked = raytracingEnabled,
                                    onCheckedChange = { raytracingEnabled = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF6366F1))
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Save button
                Button(
                    onClick = {
                        val updatedProfile = userProfile.copy(
                            isGLoggedIn = true,
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
                            isHighfieldModeEnabled = highfieldEnabled,
                            selectedEngine = selectedEngine,
                            fpsTarget = fpsTarget,
                            raytracingSimulation = raytracingEnabled
                        )
                        onSaveProfileAndKeys(updatedProfile, updatedKeys, updatedSettings)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_settings_button")
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "SIMPAN SETELAN & API KEYS", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
