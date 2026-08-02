package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.ViewCarousel
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
import com.example.data.models.StoryboardTemplate
import com.example.data.models.StoryboardTemplatesRepository

@Composable
fun StoryboardTemplateSelectorDialog(
    onDismiss: () -> Unit,
    onSelectTemplate: (StoryboardTemplate) -> Unit
) {
    var selectedGenreFilter by remember { mutableStateOf("Semua") }
    val genres = listOf("Semua", "E-Commerce & Produk", "Social Media", "Film & Game", "Pendidikan & Explainer", "Musik & Seni", "Gaya Hidup & Travel")

    val filteredTemplates = remember(selectedGenreFilter) {
        if (selectedGenreFilter == "Semua") {
            StoryboardTemplatesRepository.templates
        } else {
            StoryboardTemplatesRepository.templates.filter { it.genre == selectedGenreFilter }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF16161A),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("storyboard_template_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxHeight(0.85f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ViewCarousel,
                            contentDescription = null,
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Template Storyboard AI",
                            fontSize = 17.sp,
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

                Text(
                    text = "Pilih preset penceritaan multi-genre yang siap disesuaikan dengan kebutuhan Anda",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                // Genre Filter Chips
                ScrollableTabRow(
                    selectedTabIndex = genres.indexOf(selectedGenreFilter).coerceAtLeast(0),
                    containerColor = Color.Transparent,
                    edgePadding = 0.dp,
                    indicator = {},
                    divider = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    genres.forEach { genre ->
                        val isSelected = genre == selectedGenreFilter
                        Surface(
                            onClick = { selectedGenreFilter = genre },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) Color(0xFF6366F1) else Color(0x1AFFFFFF),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF6366F1) else Color(0x26FFFFFF)
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = genre,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredTemplates, key = { it.id }) { template ->
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
                                    Text(
                                        text = template.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )

                                    Surface(
                                        color = Color(0x2E6366F1),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = template.targetPlatform,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF818CF8),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = template.description,
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.6f),
                                    lineHeight = 15.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🎬 ${template.scenes.size} Adegan",
                                        fontSize = 10.sp,
                                        color = Color(0xFF10B981)
                                    )
                                    Text(
                                        text = "📐 ${template.recommendedAspectRatio}",
                                        fontSize = 10.sp,
                                        color = Color(0xFFF59E0B)
                                    )
                                    Text(
                                        text = "🎨 ${template.defaultStyle}",
                                        fontSize = 10.sp,
                                        color = Color(0xFFEC4899)
                                    )
                                }

                                Divider(
                                    color = Color(0x1AFFFFFF),
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )

                                // Scene preview pills
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    template.scenes.forEach { sc ->
                                        Text(
                                            text = "• ${sc.title} (${sc.durationSeconds}s) — ${sc.cameraMovement}",
                                            fontSize = 10.sp,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        onSelectTemplate(template)
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("apply_template_${template.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "GUNAKAN TEMPLATE INI",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
