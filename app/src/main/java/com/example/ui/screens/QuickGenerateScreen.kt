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

        // Hero Banner Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp)
                .clip(RoundedCornerShape(22.dp))
                .border(1.dp, StudioCardHairline, RoundedCornerShape(22.dp)),
            colors = CardDefaults.cardColors(containerColor = StudioGlassWhite)
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
                                listOf(Color.Black.copy(alpha = 0.82f), Color.Black.copy(alpha = 0.35f))
                            )
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = StudioElectricBlue,
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "STUDIO AI PRO",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Kualitas Sinematik",
                                color = StudioEmeraldGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Ubah Teks & Gambar Jadi Video Sinematik",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.3).sp
                        )
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
                    activeContainerColor = StudioDarkCTA,
                    activeContentColor = Color.White,
                    inactiveContainerColor = StudioGlassWhite,
                    inactiveContentColor = StudioTextMuted
                )
            ) {
                Text("Teks Ke Video", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            SegmentedButton(
                selected = mode == GeneratorMode.IMAGE_TO_VIDEO,
                onClick = { viewModel.generatorMode.value = GeneratorMode.IMAGE_TO_VIDEO },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = StudioDarkCTA,
                    activeContentColor = Color.White,
                    inactiveContainerColor = StudioGlassWhite,
                    inactiveContentColor = StudioTextMuted
                )
            ) {
                Text("Gambar Ke Video", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Image Picker Area (if Image to Video mode)
        if (mode == GeneratorMode.IMAGE_TO_VIDEO) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StudioCardHairline, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = StudioGlassWhite)
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
                                .clip(RoundedCornerShape(16.dp))
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
                                .height(64.dp)
                                .testTag("upload_image_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioElectricBlue),
                            border = BorderStroke(1.dp, StudioElectricBlue.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Pilih Foto Dari Galeri", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.analyzeSelectedImage() },
                        enabled = selectedBitmap != null && !imageAnalysisState.isGenerating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("analyze_image_gemini_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = StudioElectricBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            text = if (imageAnalysisState.isGenerating) "Menganalisis Gambar AI..." else "Analisis Gambar Untuk Prompt Video",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
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
                .border(1.dp, StudioCardHairline, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = StudioGlassWhite)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Deskripsi Visual Prompt Video",
                        color = StudioTextDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Audio Mic Transcriber button
                    IconButton(
                        onClick = { showMicDialog = true },
                        modifier = Modifier
                            .background(StudioPastelRose.copy(alpha = 0.6f), CircleShape)
                            .testTag("mic_transcribe_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Transcribe Audio",
                            tint = StudioRosePink,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = prompt,
                    onValueChange = { viewModel.promptText.value = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp)
                        .testTag("prompt_text_field"),
                    placeholder = { Text("Ketik instruksi gerakan video AI di sini...", color = StudioTextSubtle, fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StudioElectricBlue,
                        unfocusedBorderColor = StudioCardHairline,
                        focusedTextColor = StudioTextDark,
                        unfocusedTextColor = StudioTextDark,
                        focusedContainerColor = StudioCleanCanvas,
                        unfocusedContainerColor = StudioCleanCanvas
                    ),
                    shape = RoundedCornerShape(16.dp)
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
                    tint = StudioElectricBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Gaya Visual Studio",
                    color = StudioTextDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Surface(
                color = StudioElectricBlue.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = style,
                    color = StudioElectricBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.testTag("visual_style_list")
        ) {
            items(styleItems) { item ->
                val isSelected = style.contains(item.first, ignoreCase = true) || item.first.contains(style, ignoreCase = true)
                Card(
                    modifier = Modifier
                        .width(134.dp)
                        .height(84.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            2.dp,
                            if (isSelected) StudioElectricBlue else Color.Transparent,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { viewModel.selectedStyle.value = item.first }
                        .testTag("style_item_${item.first}"),
                    colors = CardDefaults.cardColors(containerColor = StudioGlassWhite)
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
                                    if (isSelected) StudioElectricBlue.copy(alpha = 0.65f)
                                    else Color.Black.copy(alpha = 0.45f)
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
                    .border(1.dp, StudioCardHairline, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = StudioGlassWhite)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Rasio Layar",
                        color = StudioTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StudioPillBg, RoundedCornerShape(12.dp))
                            .border(1.dp, StudioCardHairline, RoundedCornerShape(12.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("16:9", "9:16", "1:1").forEach { ratio ->
                            val isSelected = aspectRatio == ratio
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(30.dp)
                                    .background(
                                        if (isSelected) StudioDarkCTA else Color.Transparent,
                                        RoundedCornerShape(9.dp)
                                    )
                                    .clickable { viewModel.selectedAspectRatio.value = ratio },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = ratio,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else StudioTextDark,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            // Duration Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, StudioCardHairline, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = StudioGlassWhite)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Durasi Klip",
                        color = StudioTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StudioPillBg, RoundedCornerShape(12.dp))
                            .border(1.dp, StudioCardHairline, RoundedCornerShape(12.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(3, 5, 10).forEach { dur ->
                            val isSelected = duration == dur
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(30.dp)
                                    .background(
                                        if (isSelected) StudioDarkCTA else Color.Transparent,
                                        RoundedCornerShape(9.dp)
                                    )
                                    .clickable { viewModel.selectedDuration.value = dur },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${dur}s",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else StudioTextDark,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Main Generate Button (iOS Large Pill CTA)
        Button(
            onClick = { viewModel.generateVideoClip() },
            enabled = !clipGenState.isGenerating && prompt.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("generate_clip_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = StudioDarkCTA,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(26.dp)
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
                Text(text = "GENERATE KLIP VIDEO AI", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, letterSpacing = 0.3.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Video Result Preview Player
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pratinjau Klip Ter-generate",
                color = StudioTextDark,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Surface(
                color = StudioElectricBlue.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "Rasio $aspectRatio",
                    color = StudioElectricBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

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
                    Text("Transkripsi Suara AI Studio", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
}
