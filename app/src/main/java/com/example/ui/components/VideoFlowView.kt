package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.StoryboardSceneEntity
import com.example.ui.theme.*

data class VideoFlowNode(
    val stepIndex: Int,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val statusText: String,
    val isCompleted: Boolean = true
)

@Composable
fun VideoFlowView(
    scenes: List<StoryboardSceneEntity>,
    modifier: Modifier = Modifier
) {
    val flowNodes = listOf(
        VideoFlowNode(
            stepIndex = 1,
            title = "Naskah & Konsep",
            subtitle = "Ide Cerita / Audio Prompt",
            icon = Icons.Default.Lightbulb,
            statusText = "Siap"
        ),
        VideoFlowNode(
            stepIndex = 2,
            title = "Sutradara AI",
            subtitle = "Gemini 3.1 Pro Thinking",
            icon = Icons.Default.Psychology,
            statusText = "HIGH Level"
        ),
        VideoFlowNode(
            stepIndex = 3,
            title = "Storyboard (${scenes.size} Adegan)",
            subtitle = scenes.firstOrNull()?.title ?: "Skenario Adegan",
            icon = Icons.Default.ViewCarousel,
            statusText = "Terstruktur"
        ),
        VideoFlowNode(
            stepIndex = 4,
            title = "Veo 3 AI Generator",
            subtitle = "veo-3.1-fast-generate",
            icon = Icons.Default.AutoAwesome,
            statusText = "Klip HD"
        ),
        VideoFlowNode(
            stepIndex = 5,
            title = "Penggabung Timeline",
            subtitle = "Transisi & Subjudul",
            icon = Icons.Default.MovieFilter,
            statusText = "Multitrack"
        ),
        VideoFlowNode(
            stepIndex = 6,
            title = "Ekspor Sosial Media",
            subtitle = "TikTok / Reels 1080p",
            icon = Icons.Default.IosShare,
            statusText = "Siap Rilis"
        )
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, StudioCardBorder, RoundedCornerShape(16.dp))
            .testTag("videoflow_canvas"),
        colors = CardDefaults.cardColors(containerColor = StudioSurfaceDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccountTree,
                    contentDescription = "VideoFlow",
                    tint = StudioSecondaryTeal,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Alur Kerja VideoFlow AI",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Peta visual terintegrasi dari konsep AI hingga klip tereksplorasi.",
                color = StudioTextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(flowNodes) { index, node ->
                    Row(verticalAlignment = Alignment.CenterVertically) {

                        // Node Card
                        Surface(
                            modifier = Modifier
                                .width(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    1.dp,
                                    if (node.isCompleted) StudioPrimaryViolet else StudioCardBorder,
                                    RoundedCornerShape(12.dp)
                                ),
                            color = StudioCardBg
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = StudioPrimaryViolet.copy(alpha = 0.2f),
                                        shape = CircleShape,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = node.icon,
                                                contentDescription = node.title,
                                                tint = StudioSecondaryTeal,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Surface(
                                        color = StudioSecondaryTeal.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = node.statusText,
                                            color = StudioSecondaryTeal,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "${node.stepIndex}. ${node.title}",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = node.subtitle,
                                    color = StudioTextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }

                        // Arrow Connector to next node
                        if (index < flowNodes.size - 1) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Connector",
                                tint = StudioPrimaryViolet,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                }
            }
        }
    }
}
