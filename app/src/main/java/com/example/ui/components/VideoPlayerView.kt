package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*

@Composable
fun VideoPlayerView(
    aspectRatioStr: String = "16:9",
    isPlaying: Boolean = false,
    currentTimeMs: Long = 0L,
    totalDurationMs: Long = 15000L,
    activeFilter: String = "None",
    thumbnailDrawableRes: Int? = null,
    isProxyMode: Boolean = true,
    proxyResolution: String = "360p Proxy",
    onToggleProxyMode: () -> Unit = {},
    onTogglePlay: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val aspectRatioFloat = when (aspectRatioStr) {
        "9:16" -> 9f / 16f
        "1:1" -> 1f
        else -> 16f / 9f
    }

    // Animated glow pulse for active playing
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatioFloat)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, StudioCardBorder, RoundedCornerShape(16.dp))
            .testTag("video_player_card"),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onTogglePlay() }
        ) {
            // Background preview graphic
            val imageRes = thumbnailDrawableRes ?: R.drawable.img_hero_banner_1785585794962
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = "Preview Video Clip",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Low-Res Proxy Preview Mode Downsampling Simulation Layer
            if (isProxyMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.08f))
                        .drawWithCache {
                            onDrawWithContent {
                                drawContent()
                                // Subtle pixelation grid overlay representing proxy low-res preview canvas
                                val step = 16f
                                for (x in 0..size.width.toInt() step step.toInt()) {
                                    drawLine(
                                        color = Color.Black.copy(alpha = 0.03f),
                                        start = androidx.compose.ui.geometry.Offset(x.toFloat(), 0f),
                                        end = androidx.compose.ui.geometry.Offset(x.toFloat(), size.height),
                                        strokeWidth = 1f
                                    )
                                }
                            }
                        }
                )
            }

            // Visual Filter Overlay simulation
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        when (activeFilter) {
                            "Cyberpunk Neon" -> Brush.verticalGradient(
                                listOf(Color(0x33FF00FF), Color(0x3300FFFF))
                            )
                            "Cinematic Glow" -> Brush.radialGradient(
                                listOf(Color(0x22FFD700), Color(0x66000000))
                            )
                            "Vintage Film" -> Brush.verticalGradient(
                                listOf(Color(0x33A0522D), Color(0x228B4513))
                            )
                            "Warm Sunset" -> Brush.horizontalGradient(
                                listOf(Color(0x33FF7F50), Color(0x33FFD700))
                            )
                            "B&W Noir" -> Brush.linearGradient(
                                listOf(Color(0x88000000), Color(0x88333333))
                            )
                            else -> Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xAA000000))
                            )
                        }
                    )
            )

            // Aspect Ratio badge & Veo watermark (Top Start)
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StudioPrimaryViolet)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Veo",
                            tint = StudioSecondaryTeal,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Veo 3 AI ($aspectRatioStr)",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (activeFilter != "None") {
                    Surface(
                        color = StudioPrimaryViolet.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Filter: $activeFilter",
                            color = Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Low-Resolution Proxy Preview Mode Toggle Badge (Top End)
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggleProxyMode() },
                color = if (isProxyMode) StudioSecondaryTeal.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.75f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isProxyMode) StudioSecondaryTeal else StudioPrimaryViolet
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isProxyMode) Icons.Default.Speed else Icons.Default.HighQuality,
                        contentDescription = "Proxy Mode",
                        tint = if (isProxyMode) Color.Black else StudioPrimaryViolet,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isProxyMode) "⚡ PROXY $proxyResolution" else "🎬 1080p FULL RES",
                        color = if (isProxyMode) Color.Black else Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Center Play/Pause Floating Icon Button
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(56.dp)
                    .clip(CircleShape)
                    .testTag("play_pause_button"),
                color = StudioPrimaryViolet.copy(alpha = if (isPlaying) 0.6f else 0.9f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Bottom Timestamp & Progress Indicator Bar
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                        )
                    )
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val formattedCurrent = formatTimeMs(currentTimeMs)
                    val formattedTotal = formatTimeMs(totalDurationMs)

                    Text(
                        text = "$formattedCurrent / $formattedTotal",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (isPlaying) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(StudioAccentPink.copy(alpha = pulseAlpha))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "LIVE PREVIEW",
                                color = StudioAccentPink,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                val progressFraction = (currentTimeMs.toFloat() / totalDurationMs.coerceAtLeast(1000L).toFloat())
                    .coerceIn(0f, 1f)

                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = StudioSecondaryTeal,
                    trackColor = Color.White.copy(alpha = 0.3f),
                )
            }
        }
    }
}

fun formatTimeMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
