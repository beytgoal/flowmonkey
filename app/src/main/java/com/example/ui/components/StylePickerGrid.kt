package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.VisualStyleOption
import com.example.data.models.VisualStylesRepository
import com.example.ui.theme.*

@Composable
fun StylePickerGrid(
    selectedStyleId: String,
    onStyleSelected: (VisualStyleOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = StudioVioletIndigo,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Gaya Visual AI • ${VisualStylesRepository.options.size} Pilihan",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudioTextDark
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .heightIn(max = 320.dp)
                .testTag("style_picker_grid")
        ) {
            items(VisualStylesRepository.options, key = { it.id }) { option ->
                val isSelected = option.name.contains(selectedStyleId, ignoreCase = true) ||
                        option.id.equals(selectedStyleId, ignoreCase = true)

                Surface(
                    onClick = { onStyleSelected(option) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) StudioPastelLavender else StudioCardWhite,
                    shadowElevation = if (isSelected) 2.dp else 1.dp,
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) option.accentColor else StudioCardHairline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("style_item_${option.id}")
                ) {
                    Box(
                        modifier = Modifier
                            .background(if (isSelected) StudioPastelLavender else StudioCardWhite)
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = StudioCardWhite,
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, option.accentColor)
                                ) {
                                    Text(
                                        text = option.badge,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = option.accentColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = option.accentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = option.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = StudioTextDark
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = option.description,
                                fontSize = 10.sp,
                                color = StudioTextMuted,
                                lineHeight = 13.sp,
                                maxLines = 3
                            )
                        }
                    }
                }
            }
        }
    }
}
