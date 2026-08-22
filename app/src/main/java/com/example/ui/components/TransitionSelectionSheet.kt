package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.TimelineClipEntity
import com.example.ui.theme.*

data class TransitionEffectItem(
    val name: String,
    val category: String,
    val icon: ImageVector,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransitionSelectionSheet(
    clipA: TimelineClipEntity?,
    clipB: TimelineClipEntity?,
    currentTransition: String = "Fade",
    onApplyTransition: (transitionName: String, applyToAll: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTransition by remember { mutableStateOf(currentTransition) }
    var selectedCategory by remember { mutableStateOf("Semua") }
    var transitionDurationSec by remember { mutableFloatStateOf(0.5f) }

    val categories = listOf("Semua", "Fade & Dissolve", "Slide & Push", "Wipe & Iris", "Zoom & Motion", "Glitch & VFX")

    val transitionsList = remember {
        listOf(
            TransitionEffectItem("None", "Fade & Dissolve", Icons.Default.Block, "Tanpa Transisi (Cut Langsung)"),
            TransitionEffectItem("Cross Dissolve", "Fade & Dissolve", Icons.Default.BlurOn, "Peleburan Gambar Mulus Antar Klip"),
            TransitionEffectItem("Fade to Black", "Fade & Dissolve", Icons.Default.Brightness1, "Fade Gelap Sinematik ke Hitam"),
            TransitionEffectItem("Dip to White", "Fade & Dissolve", Icons.Default.LightMode, "Flash Putih Elegan Terang"),
            TransitionEffectItem("Blur Fade", "Fade & Dissolve", Icons.Default.Grain, "Transisi Buram Lembut"),
            TransitionEffectItem("Slide Left", "Slide & Push", Icons.Default.ArrowBack, "Geser Masuk ke Kiri"),
            TransitionEffectItem("Slide Right", "Slide & Push", Icons.Default.ArrowForward, "Geser Masuk ke Kanan"),
            TransitionEffectItem("Slide Up", "Slide & Push", Icons.Default.ArrowUpward, "Geser Masuk ke Atas"),
            TransitionEffectItem("Push Left", "Slide & Push", Icons.Default.CompareArrows, "Dorong Klip Lama ke Kiri"),
            TransitionEffectItem("Push Right", "Slide & Push", Icons.Default.SwapHoriz, "Dorong Klip Lama ke Kanan"),
            TransitionEffectItem("Clockwise Wipe", "Wipe & Iris", Icons.Default.RotateRight, "Sapuan Jarum Jam Melingkar"),
            TransitionEffectItem("Linear Wipe", "Wipe & Iris", Icons.Default.ViewColumn, "Sapuan Garis Diagonal"),
            TransitionEffectItem("Iris Circle Wipe", "Wipe & Iris", Icons.Default.Adjust, "Bukaan Lingkaran Fokus Iris"),
            TransitionEffectItem("Zoom In Sweep", "Zoom & Motion", Icons.Default.ZoomIn, "Zoom Cepat Menembus Frame"),
            TransitionEffectItem("Zoom Out", "Zoom & Motion", Icons.Default.ZoomOut, "Tarik Mundur Zoom Out"),
            TransitionEffectItem("Whip Pan Left", "Zoom & Motion", Icons.Default.FastForward, "Gerakan Kamera Cepat Whip Pan"),
            TransitionEffectItem("3D Spin Flip", "Zoom & Motion", Icons.Default.FlipCameraAndroid, "Putaran Tiga Dimensi Flip"),
            TransitionEffectItem("Digital Glitch", "Glitch & VFX", Icons.Default.Bolt, "Distorsi Sinyal Digital Glitch"),
            TransitionEffectItem("Film Burn", "Glitch & VFX", Icons.Default.LocalFireDepartment, "Bocoran Cahaya Film Seluloid"),
            TransitionEffectItem("RGB Split Strobe", "Glitch & VFX", Icons.Default.FlashOn, "Pemisahan Spektrum Warna RGB")
        )
    }

    val filteredTransitions = remember(selectedCategory) {
        if (selectedCategory == "Semua") transitionsList
        else transitionsList.filter { it.category == selectedCategory }
    }

    // Infinite animation for live preview of selected transition
    val transitionAnim = rememberInfiniteTransition(label = "transition_preview")
    val progress by transitionAnim.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((transitionDurationSec * 1600).toInt().coerceAtLeast(800), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "trans_progress"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StudioCardWhite,
        contentColor = StudioTextDark,
        modifier = Modifier.testTag("transition_selection_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = StudioPastelMint,
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CompareArrows,
                                contentDescription = null,
                                tint = StudioSecondaryTeal,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Tool Pemilih Transisi Klip",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudioTextDark
                        )
                        val clipATitle = clipA?.title?.take(14) ?: "Klip A"
                        val clipBTitle = clipB?.title?.take(14) ?: "Klip B"
                        Text(
                            text = "$clipATitle ➔ $clipBTitle",
                            fontSize = 11.sp,
                            color = StudioTextMuted
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup", tint = StudioTextMuted)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Animated Live Preview Canvas
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = StudioDarkCharcoal),
                border = BorderStroke(1.dp, StudioCardHairline)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Clip A Background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF1A237E), Color(0xFF00695C))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = clipA?.title?.take(16) ?: "Klip A",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // Clip B Layer with active animated transition transformation
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                when (selectedTransition) {
                                    "Cross Dissolve" -> {
                                        alpha = progress
                                    }
                                    "Fade to Black" -> {
                                        alpha = if (progress < 0.5f) 0f else (progress - 0.5f) * 2f
                                    }
                                    "Dip to White" -> {
                                        alpha = if (progress < 0.5f) 0f else (progress - 0.5f) * 2f
                                    }
                                    "Blur Fade" -> {
                                        alpha = progress
                                    }
                                    "Slide Left" -> {
                                        translationX = (1f - progress) * size.width
                                    }
                                    "Slide Right" -> {
                                        translationX = -(1f - progress) * size.width
                                    }
                                    "Slide Up" -> {
                                        translationY = (1f - progress) * size.height
                                    }
                                    "Push Left" -> {
                                        translationX = (1f - progress) * size.width
                                    }
                                    "Push Right" -> {
                                        translationX = -(1f - progress) * size.width
                                    }
                                    "Zoom In Sweep" -> {
                                        scaleX = 0.3f + progress * 0.7f
                                        scaleY = 0.3f + progress * 0.7f
                                        alpha = progress
                                    }
                                    "Zoom Out" -> {
                                        scaleX = 2.0f - progress * 1.0f
                                        scaleY = 2.0f - progress * 1.0f
                                        alpha = progress
                                    }
                                    "3D Spin Flip" -> {
                                        rotationY = (1f - progress) * 180f
                                        alpha = if (progress > 0.5f) 1f else 0f
                                    }
                                    "Whip Pan Left" -> {
                                        translationX = (1f - progress) * size.width * 1.5f
                                    }
                                    "Digital Glitch", "RGB Split Strobe", "Film Burn" -> {
                                        alpha = progress
                                    }
                                    "None" -> {
                                        alpha = if (progress >= 0.5f) 1f else 0f
                                    }
                                    else -> {
                                        alpha = progress
                                    }
                                }
                            }
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF880E4F), Color(0xFFE65100))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = clipB?.title?.take(16) ?: "Klip B",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // Overlay Effects during transition
                    if (selectedTransition == "Fade to Black" && progress in 0.35f..0.65f) {
                        val blackAlpha = 1f - (Math.abs(progress - 0.5f) * 6.6f).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = blackAlpha))
                        )
                    } else if (selectedTransition == "Dip to White" && progress in 0.35f..0.65f) {
                        val whiteAlpha = 1f - (Math.abs(progress - 0.5f) * 6.6f).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = whiteAlpha))
                        )
                    }

                    // Active transition name pill
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = StudioDarkCharcoal,
                        border = BorderStroke(1.dp, StudioElectricBlue)
                    ) {
                        Text(
                            text = "Efek: $selectedTransition",
                            color = StudioPastelSky,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Duration Adjustment
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Durasi Transisi: ${String.format("%.1f", transitionDurationSec)}s",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudioTextDark
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(0.3f, 0.5f, 0.8f, 1.0f, 1.5f).forEach { dur ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (transitionDurationSec == dur) StudioElectricBlue else StudioPillBg,
                            border = BorderStroke(1.dp, if (transitionDurationSec == dur) StudioElectricBlue else StudioCardHairline),
                            modifier = Modifier.clickable { transitionDurationSec = dur }
                        ) {
                            Text(
                                text = "${dur}s",
                                color = if (transitionDurationSec == dur) Color.White else StudioTextDark,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Category Filter Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StudioElectricBlue,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Transitions Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
            ) {
                items(filteredTransitions) { item ->
                    val isSelected = selectedTransition == item.name
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .clickable { selectedTransition = item.name }
                            .testTag("transition_item_${item.name}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) StudioPastelSky else StudioPillBg
                        ),
                        border = BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) StudioElectricBlue else StudioCardHairline
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.name,
                                tint = if (isSelected) StudioElectricBlue else StudioTextDark,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.name,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isSelected) StudioElectricBlue else StudioTextDark,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Apply Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onApplyTransition(selectedTransition, true)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioElectricBlue),
                    border = BorderStroke(1.dp, StudioElectricBlue)
                ) {
                    Text("Terapkan ke Semua", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        onApplyTransition(selectedTransition, false)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = StudioDarkCTA)
                ) {
                    Text("Terapkan", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
