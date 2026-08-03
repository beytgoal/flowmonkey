package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.VideoPlayerView
import com.example.ui.theme.*
import com.example.ui.viewmodels.GeneratorMode
import com.example.ui.viewmodels.VideoStudioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickGenerateScreen(
    viewModel: VideoStudioViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mode by viewModel.generatorMode.collectAsState()
    val prompt by viewModel.promptText.collectAsState()
    val style by viewModel.selectedStyle.collectAsState()
    val aspectRatio by viewModel.selectedAspectRatio.collectAsState()
    val duration by viewModel.selectedDuration.collectAsState()
    val selectedBitmap by viewModel.selectedImageBitmap.collectAsState()
    val clipGenState by viewModel.clipGenState.collectAsState()
    val imageAnalysisState by viewModel.imageAnalysisState.collectAsState()
    val currentTimeMs by viewModel.currentTimeMs.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    val userProfile by viewModel.userProfileState.collectAsState()
    val apiKeys by viewModel.apiKeysState.collectAsState()
    val highfieldSettings by viewModel.highfieldSettingsState.collectAsState()

    var showMicDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                viewModel.selectedImageBitmap.value = bitmap
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        // Hero Banner Header with Highfield Status & Settings Button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = StudioCardBg)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_banner_1785585794962),
                    contentDescription = "Studio Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(Color.Black.copy(alpha = 0.9f), Color.Black.copy(alpha = 0.3f))
                            )
                        )
                        .padding(14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Color(0xFF6366F1),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "HIGHFIELD AI",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = highfieldSettings.selectedEngine.take(18) + "...",
                                    color = StudioSecondaryTeal,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ubah Teks & Gambar Jadi Video Ultra High-End",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Settings Button
                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier
                                .background(Color(0x33FFFFFF), CircleShape)
                                .testTag("open_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "API Keys & Settings",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mode Switcher: Text to Video vs Image to Video
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("mode_switcher_row")
        ) {
            SegmentedButton(
                selected = mode == GeneratorMode.TEXT_TO_VIDEO,
                onClick = { viewModel.generatorMode.value = GeneratorMode.TEXT_TO_VIDEO },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = StudioPrimaryViolet,
                    activeContentColor = Color.White,
                    inactiveContainerColor = StudioCardBg,
                    inactiveContentColor = StudioTextSecondary
                ),
                icon = {
                    Icon(imageVector = Icons.Default.TextFields, contentDescription = "Text to Video")
                }
            ) {
                Text("Teks Ke Video", fontWeight = FontWeight.Bold)
            }

            SegmentedButton(
                selected = mode == GeneratorMode.IMAGE_TO_VIDEO,
                onClick = { viewModel.generatorMode.value = GeneratorMode.IMAGE_TO_VIDEO },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = StudioPrimaryViolet,
                    activeContentColor = Color.White,
                    inactiveContainerColor = StudioCardBg,
                    inactiveContentColor = StudioTextSecondary
                ),
                icon = {
                    Icon(imageVector = Icons.Default.Image, contentDescription = "Image to Video")
                }
            ) {
                Text("Gambar Ke Video", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Image Picker Area (if Image to Video mode)
        if (mode == GeneratorMode.IMAGE_TO_VIDEO) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StudioCardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = StudioCardBg)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (selectedBitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            androidx.compose.foundation.Image(
                                bitmap = selectedBitmap!!.asImageBitmap(),
                                contentDescription = "Selected Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .testTag("upload_image_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioSecondaryTeal),
                            border = BorderStroke(1.dp, StudioSecondaryTeal)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Pick Image",
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Pilih Foto Dari Galeri", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.analyzeSelectedImage() },
                        enabled = selectedBitmap != null && !imageAnalysisState.isGenerating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("analyze_image_gemini_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = StudioSecondaryTeal, contentColor = Color.Black)
                    ) {
                        Icon(imageVector = Icons.Default.DocumentScanner, contentDescription = "Analyze")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (imageAnalysisState.isGenerating) "Menganalisis Gambar (Gemini 3.1 Pro)..." else "Analisis Gambar Untuk Prompt Video",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Text Prompt Input Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StudioCardBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = StudioCardBg)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Deskripsi Visual Prompt Video",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Audio Mic Transcriber button
                    IconButton(
                        onClick = { showMicDialog = true },
                        modifier = Modifier.testTag("mic_transcribe_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Transcribe Audio",
                            tint = StudioAccentPink
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = prompt,
                    onValueChange = { viewModel.promptText.value = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("prompt_text_field"),
                    placeholder = { Text("Ketik instruksi gerakan video AI di sini...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StudioPrimaryViolet,
                        unfocusedBorderColor = StudioCardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Visual Style Selector (Restored to horizontal cards with thumbnail images)
        val styleItems = listOf(
            Pair("Cinematic", R.drawable.img_style_cinematic_1785585807861),
            Pair("Anime", R.drawable.img_style_anime_1785585820860),
            Pair("Cyberpunk", R.drawable.img_style_cyberpunk_1785585839466),
            Pair("Photorealistic", R.drawable.img_hero_banner_1785585794962),
            Pair("3D Render", R.drawable.img_app_icon_1785585782754),
            Pair("Retro VHS", R.drawable.img_style_cinematic_1785585807861),
            Pair("Vintage Film", R.drawable.img_style_anime_1785585820860)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = StudioPrimaryViolet,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Gaya Visual Studio",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = style,
                color = StudioSecondaryTeal,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.testTag("visual_style_list")
        ) {
            items(styleItems) { item ->
                val isSelected = style.contains(item.first, ignoreCase = true) || item.first.contains(style, ignoreCase = true)
                Card(
                    modifier = Modifier
                        .width(130.dp)
                        .height(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            2.dp,
                            if (isSelected) StudioPrimaryViolet else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { viewModel.selectedStyle.value = item.first }
                        .testTag("style_item_${item.first}"),
                    colors = CardDefaults.cardColors(containerColor = StudioSurfaceDark)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = item.second),
                            contentDescription = item.first,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (isSelected) StudioPrimaryViolet.copy(alpha = 0.65f)
                                    else Color.Black.copy(alpha = 0.55f)
                                )
                                .padding(8.dp),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Text(
                                text = item.first,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Aspect Ratio & Duration Settings Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Aspect Ratio Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, StudioCardBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = StudioCardBg)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Rasio Layar",
                        color = StudioTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("16:9", "9:16", "1:1").forEach { ratio ->
                            FilterChip(
                                selected = aspectRatio == ratio,
                                onClick = { viewModel.selectedAspectRatio.value = ratio },
                                label = { Text(ratio, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StudioPrimaryViolet,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // Duration Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, StudioCardBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = StudioCardBg)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Durasi Klip",
                        color = StudioTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(3, 5, 10).forEach { dur ->
                            FilterChip(
                                selected = duration == dur,
                                onClick = { viewModel.selectedDuration.value = dur },
                                label = { Text("${dur}s", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StudioSecondaryTeal,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Main Generate Button
        Button(
            onClick = { viewModel.generateVideoClip() },
            enabled = !clipGenState.isGenerating && prompt.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("generate_veo_clip_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = StudioPrimaryViolet,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (clipGenState.isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = clipGenState.progressMessage, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            } else {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Generate")
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "GENERATE KLIP VEO 3", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Video Result Preview Player
        Text(
            text = "Pratinjau Klip Ter-generate",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        VideoPlayerView(
            aspectRatioStr = aspectRatio,
            isPlaying = isPlaying,
            currentTimeMs = currentTimeMs,
            totalDurationMs = duration * 1000L,
            activeFilter = style,
            onTogglePlay = { viewModel.togglePlayPause() }
        )
    }

    // Audio Mic Transcriber Dialog
    if (showMicDialog) {
        AlertDialog(
            onDismissRequest = { showMicDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Mic, contentDescription = "Mic", tint = StudioAccentPink)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Transkripsi Suara Gemini Flash", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                ) {
                    Surface(
                        color = StudioAccentPink.copy(alpha = 0.2f),
                        shape = CircleShape,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Recording",
                                tint = StudioAccentPink,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Bicara untuk menginput instruksi klip video...", fontSize = 13.sp, color = StudioTextSecondary)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.processVoiceInput(ByteArray(10)) { transcribedText ->
                            viewModel.promptText.value = transcribedText
                        }
                        showMicDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioAccentPink)
                ) {
                    Text("Selesai & Transkrip")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMicDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Settings & API Keys Dialog
    if (showSettingsDialog) {
        com.example.ui.components.ApiKeysAndSettingsDialog(
            userProfile = userProfile,
            apiKeys = apiKeys,
            highfieldSettings = highfieldSettings,
            onDismiss = { showSettingsDialog = false },
            onSaveProfileAndKeys = { updatedProfile, updatedKeys, updatedSettings ->
                viewModel.userProfileState.value = updatedProfile
                viewModel.apiKeysState.value = updatedKeys
                viewModel.highfieldSettingsState.value = updatedSettings
            }
        )
    }
}
