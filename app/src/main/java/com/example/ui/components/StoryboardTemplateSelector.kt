package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import com.example.ui.theme.*

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
            color = StudioCardWhite,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, StudioCardHairline),
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
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
                        Surface(
                            color = StudioPastelLavender,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.5.dp, StudioVioletIndigo),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ViewCarousel,
                                    contentDescription = null,
                                    tint = StudioVioletIndigo,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Template Storyboard AI",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudioTextDark
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = StudioTextDark
                        )
                    }
                }

                Text(
                    text = "Pilih preset penceritaan multi-genre yang siap disesuaikan dengan kebutuhan Anda",
                    fontSize = 12.sp,
                    color = StudioTextMuted,
                    modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
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
                            color = if (isSelected) StudioVioletIndigo else StudioPillBg,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) StudioVioletIndigo else StudioCardHairline
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = genre,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isSelected) Color.White else StudioTextDark,
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
                            color = StudioCleanCanvas,
                            border = BorderStroke(1.dp, StudioCardHairline),
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
                                        color = StudioTextDark
                                    )

                                    Surface(
                                        color = StudioPastelLavender,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, StudioVioletIndigo)
                                    ) {
                                        Text(
                                            text = template.targetPlatform,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = StudioVioletIndigo,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = template.description,
                                    fontSize = 11.sp,
                                    color = StudioTextMuted,
                                    lineHeight = 15.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${template.scenes.size} Adegan",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StudioEmeraldGreen
                                    )
                                    Text(
                                        text = template.recommendedAspectRatio,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StudioAmberGold
                                    )
                                    Text(
                                        text = template.defaultStyle,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StudioRosePink
                                    )
                                }

                                HorizontalDivider(
                                    color = StudioCardHairline,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )

                                // Scene preview pills
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    template.scenes.forEach { sc ->
                                        Text(
                                            text = "• ${sc.title} — ${sc.durationSeconds}s — ${sc.cameraMovement}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = StudioTextDark
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        onSelectTemplate(template)
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = StudioDarkCharcoal,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("apply_template_${template.id}")
                                ) {
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
