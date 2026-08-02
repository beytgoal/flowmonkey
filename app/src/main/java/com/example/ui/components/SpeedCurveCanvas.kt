package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

data class CurveNode(
    val id: Int,
    var normX: Float, // 0.0 to 1.0 (time ratio along clip)
    var normY: Float  // 0.0 to 1.0 (speed ratio: 0.0 = 0.2x, 0.5 = 1.0x, 1.0 = 5.0x)
) {
    fun speedMultiplier(): Float {
        // Map 0.0..1.0 to 0.2x..5.0x
        return 0.2f + normY * 4.8f
    }
}

@Composable
fun SpeedCurveCanvas(
    curvePreset: String,
    durationMs: Long,
    onCurveChanged: (String, Float, List<CurveNode>) -> Unit,
    modifier: Modifier = Modifier
) {
    // Generate initial nodes based on selected curve preset
    var nodes by remember(curvePreset) {
        mutableStateOf(getNodesForPreset(curvePreset))
    }

    var selectedNodeIndex by remember { mutableStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        // Top Info & Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = StudioSecondaryTeal.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StudioSecondaryTeal)
                ) {
                    Text(
                        text = "Speed Ramping dan Kurva Kecepatan",
                        color = StudioSecondaryTeal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            val currentSelectedSpeed = nodes.getOrNull(selectedNodeIndex)?.speedMultiplier() ?: 1.0f
            Text(
                text = "Point #${selectedNodeIndex + 1}: ${String.format("%.2f", currentSelectedSpeed)}x",
                color = StudioAccentAmber,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Interactive Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF14161C))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .testTag("speed_curve_canvas")
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .pointerInput(nodes) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val width = size.width
                            val height = size.height

                            if (nodes.isNotEmpty() && selectedNodeIndex in nodes.indices) {
                                val currentNode = nodes[selectedNodeIndex]
                                // Drag vertical to change speed (normY)
                                val deltaY = -dragAmount.y / height
                                val newNormY = (currentNode.normY + deltaY).coerceIn(0.0f, 1.0f)

                                // Drag horizontal for middle nodes (not first or last)
                                val deltaX = dragAmount.x / width
                                val newNormX = if (selectedNodeIndex == 0) 0.0f
                                else if (selectedNodeIndex == nodes.size - 1) 1.0f
                                else (currentNode.normX + deltaX).coerceIn(
                                    nodes[selectedNodeIndex - 1].normX + 0.05f,
                                    nodes[selectedNodeIndex + 1].normX - 0.05f
                                )

                                val updatedNodes = nodes.toMutableList()
                                updatedNodes[selectedNodeIndex] = currentNode.copy(normX = newNormX, normY = newNormY)
                                nodes = updatedNodes

                                val avgSpeed = updatedNodes.map { it.speedMultiplier() }.average().toFloat()
                                onCurveChanged("Custom Curve", avgSpeed, updatedNodes)
                            }
                        }
                    }
            ) {
                val w = size.width
                val h = size.height

                // Draw Horizontal Speed Gridlines (0.5x, 1.0x, 2.0x, 4.0x)
                val gridSpeeds = listOf(0.1f to "0.2x", 0.35f to "1.0x", 0.65f to "2.5x", 0.95f to "5.0x")
                gridSpeeds.forEach { (normY, label) ->
                    val y = h - (normY * h)
                    drawLine(
                        color = Color.White.copy(alpha = 0.1f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Smooth Cubic Bezier Path connecting all speed nodes
                if (nodes.size >= 2) {
                    val path = Path()
                    val points = nodes.map { Offset(it.normX * w, h - (it.normY * h)) }

                    path.moveTo(points.first().x, points.first().y)

                    for (i in 0 until points.size - 1) {
                        val p1 = points[i]
                        val p2 = points[i + 1]
                        val controlX1 = p1.x + (p2.x - p1.x) / 2f
                        val controlY1 = p1.y
                        val controlX2 = p1.x + (p2.x - p1.x) / 2f
                        val controlY2 = p2.y

                        path.cubicTo(controlX1, controlY1, controlX2, controlY2, p2.x, p2.y)
                    }

                    // Draw Gradient Fill under curve
                    val fillPath = Path().apply {
                        addPath(path)
                        lineTo(points.last().x, h)
                        lineTo(points.first().x, h)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                StudioPrimaryViolet.copy(alpha = 0.35f),
                                StudioSecondaryTeal.copy(alpha = 0.05f)
                            )
                        )
                    )

                    // Draw Glowing Curve Stroke
                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(
                            colors = listOf(StudioPrimaryViolet, StudioSecondaryTeal, StudioAccentPink)
                        ),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Draw Node Circles
                nodes.forEachIndexed { index, node ->
                    val cx = node.normX * w
                    val cy = h - (node.normY * h)
                    val isSelected = index == selectedNodeIndex

                    if (isSelected) {
                        drawCircle(
                            color = StudioAccentAmber.copy(alpha = 0.4f),
                            radius = 16.dp.toPx(),
                            center = Offset(cx, cy)
                        )
                    }

                    drawCircle(
                        color = if (isSelected) StudioAccentAmber else Color.White,
                        radius = 7.dp.toPx(),
                        center = Offset(cx, cy)
                    )

                    drawCircle(
                        color = StudioSurfaceDark,
                        radius = 4.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Node Selector & Fine Tuning Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                nodes.forEachIndexed { index, _ ->
                    FilterChip(
                        selected = selectedNodeIndex == index,
                        onClick = { selectedNodeIndex = index },
                        label = { Text("P${index + 1}", fontSize = 10.sp) },
                        modifier = Modifier.height(30.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = {
                        val updated = nodes.toMutableList()
                        val curr = updated[selectedNodeIndex]
                        updated[selectedNodeIndex] = curr.copy(normY = (curr.normY + 0.05f).coerceAtMost(1.0f))
                        nodes = updated
                        val avg = updated.map { it.speedMultiplier() }.average().toFloat()
                        onCurveChanged("Custom Curve", avg, updated)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(imageVector = Icons.Default.ArrowDropUp, contentDescription = "Speed Up", tint = StudioPrimaryViolet)
                }

                IconButton(
                    onClick = {
                        val updated = nodes.toMutableList()
                        val curr = updated[selectedNodeIndex]
                        updated[selectedNodeIndex] = curr.copy(normY = (curr.normY - 0.05f).coerceAtLeast(0.0f))
                        nodes = updated
                        val avg = updated.map { it.speedMultiplier() }.average().toFloat()
                        onCurveChanged("Custom Curve", avg, updated)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Speed Down", tint = StudioPrimaryViolet)
                }
            }
        }
    }
}

fun getNodesForPreset(preset: String): List<CurveNode> {
    return when (preset) {
        "Hero", "Hero Curve" -> listOf(
            CurveNode(1, 0.0f, 0.35f),  // 1.0x
            CurveNode(2, 0.25f, 0.85f), // 4.2x
            CurveNode(3, 0.55f, 0.15f), // 0.5x
            CurveNode(4, 0.8f, 0.6f),   // 2.8x
            CurveNode(5, 1.0f, 0.35f)   // 1.0x
        )
        "Bullet Time" -> listOf(
            CurveNode(1, 0.0f, 0.35f),  // 1.0x
            CurveNode(2, 0.2f, 0.02f),  // 0.2x Slow-mo
            CurveNode(3, 0.7f, 0.02f),  // 0.2x Slow-mo
            CurveNode(4, 0.9f, 0.8f),   // 4.0x Burst
            CurveNode(5, 1.0f, 0.35f)   // 1.0x
        )
        "Montage" -> listOf(
            CurveNode(1, 0.0f, 0.5f),   // 2.0x
            CurveNode(2, 0.25f, 0.1f),  // 0.5x
            CurveNode(3, 0.5f, 0.75f),  // 3.5x
            CurveNode(4, 0.75f, 0.15f), // 0.8x
            CurveNode(5, 1.0f, 0.6f)    // 2.8x
        )
        "Fast Out" -> listOf(
            CurveNode(1, 0.0f, 0.1f),   // 0.5x
            CurveNode(2, 0.4f, 0.25f),  // 1.0x
            CurveNode(3, 0.8f, 0.85f),  // 4.2x
            CurveNode(4, 1.0f, 0.95f)   // 5.0x
        )
        "Slow In" -> listOf(
            CurveNode(1, 0.0f, 0.95f),  // 5.0x
            CurveNode(2, 0.2f, 0.75f),  // 3.5x
            CurveNode(3, 0.6f, 0.25f),  // 1.0x
            CurveNode(4, 1.0f, 0.1f)    // 0.5x
        )
        else -> listOf( // Normal / Default
            CurveNode(1, 0.0f, 0.35f),  // 1.0x
            CurveNode(2, 0.33f, 0.35f),
            CurveNode(3, 0.66f, 0.35f),
            CurveNode(4, 1.0f, 0.35f)   // 1.0x
        )
    }
}
