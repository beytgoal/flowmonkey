package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.draw.shadow
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
import kotlin.math.hypot

data class CurveNode(
    val id: Int,
    var normX: Float, // 0.0 to 1.0 (time ratio along clip)
    var normY: Float  // 0.0 to 1.0 (speed ratio: 0.0 = 0.2x, 0.35 = 1.0x, 1.0 = 5.0x)
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
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioCardWhite, RoundedCornerShape(16.dp))
            .border(1.dp, StudioCardHairline, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        // Top Info & Speed Indicator Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = StudioSecondaryTeal,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Speed Ramping Kurva Sentuh",
                    color = StudioTextDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            val currentNode = nodes.getOrNull(selectedNodeIndex)
            val currentSelectedSpeed = currentNode?.speedMultiplier() ?: 1.0f

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when {
                    currentSelectedSpeed > 1.5f -> StudioPastelAmber
                    currentSelectedSpeed < 0.8f -> StudioPastelMint
                    else -> StudioPastelLavender
                },
                border = BorderStroke(
                    1.5.dp,
                    if (currentSelectedSpeed > 1.5f) StudioAccentAmber else StudioSecondaryTeal
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (currentSelectedSpeed > 1.0f) Icons.Default.FastForward else Icons.Default.SlowMotionVideo,
                        contentDescription = null,
                        tint = if (currentSelectedSpeed > 1.5f) StudioAccentAmber else StudioSecondaryTeal,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Titik #${selectedNodeIndex + 1}: ${String.format("%.2f", currentSelectedSpeed)}x",
                        color = StudioTextDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Sentuh & geser langsung titik pada kurva untuk mengubah kecepatan (atas/bawah) & timing (kiri/kanan).",
            color = StudioTextSecondary,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Direct Touch & Drag Interactive Speed Graph Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0F1117))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                .testTag("speed_curve_canvas")
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    // Tap to select node or create new point on curve
                    .pointerInput(nodes) {
                        detectTapGestures { offset ->
                            val w = size.width
                            val h = size.height
                            val touchRadiusPx = 36.dp.toPx()

                            // Find closest node
                            var closestIndex = -1
                            var minDistance = Float.MAX_VALUE

                            nodes.forEachIndexed { index, node ->
                                val nodeX = node.normX * w
                                val nodeY = h - (node.normY * h)
                                val dist = hypot(offset.x - nodeX, offset.y - nodeY)
                                if (dist < minDistance) {
                                    minDistance = dist
                                    closestIndex = index
                                }
                            }

                            if (closestIndex != -1 && minDistance <= touchRadiusPx) {
                                selectedNodeIndex = closestIndex
                            } else {
                                // Add new node at tap position along curve
                                val normX = (offset.x / w).coerceIn(0.05f, 0.95f)
                                val normY = (1.0f - (offset.y / h)).coerceIn(0.0f, 1.0f)
                                val newId = (nodes.maxOfOrNull { it.id } ?: 0) + 1
                                val newNode = CurveNode(newId, normX, normY)
                                val updated = (nodes + newNode).sortedBy { it.normX }
                                nodes = updated
                                selectedNodeIndex = updated.indexOf(newNode).coerceAtLeast(0)

                                val avg = SpeedCurveInterpolator.calculateAverageSpeed(updated)
                                onCurveChanged("Custom Curve", avg, updated)
                            }
                        }
                    }
                    // Direct Drag Gestures: Touch and drag ANY node directly
                    .pointerInput(nodes) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                isDragging = true
                                dragPosition = offset
                                val w = size.width
                                val h = size.height
                                val touchRadiusPx = 48.dp.toPx()

                                // Select nearest node on touch down
                                var closestIndex = -1
                                var minDistance = Float.MAX_VALUE
                                nodes.forEachIndexed { index, node ->
                                    val nodeX = node.normX * w
                                    val nodeY = h - (node.normY * h)
                                    val dist = hypot(offset.x - nodeX, offset.y - nodeY)
                                    if (dist < minDistance) {
                                        minDistance = dist
                                        closestIndex = index
                                    }
                                }
                                if (closestIndex != -1 && minDistance <= touchRadiusPx) {
                                    selectedNodeIndex = closestIndex
                                }
                            },
                            onDragEnd = { isDragging = false },
                            onDragCancel = { isDragging = false },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragPosition = change.position
                                val w = size.width
                                val h = size.height

                                if (nodes.isNotEmpty() && selectedNodeIndex in nodes.indices) {
                                    val currentNode = nodes[selectedNodeIndex]

                                    // Vertical drag -> Speed (0.0 to 1.0)
                                    val deltaY = -dragAmount.y / h
                                    val newNormY = (currentNode.normY + deltaY).coerceIn(0.0f, 1.0f)

                                    // Horizontal drag -> Time position (constrained between neighbors)
                                    val deltaX = dragAmount.x / w
                                    val newNormX = when (selectedNodeIndex) {
                                        0 -> 0.0f
                                        nodes.size - 1 -> 1.0f
                                        else -> (currentNode.normX + deltaX).coerceIn(
                                            nodes[selectedNodeIndex - 1].normX + 0.03f,
                                            nodes[selectedNodeIndex + 1].normX - 0.03f
                                        )
                                    }

                                    val updatedNodes = nodes.toMutableList()
                                    updatedNodes[selectedNodeIndex] = currentNode.copy(normX = newNormX, normY = newNormY)
                                    nodes = updatedNodes

                                    val avgSpeed = SpeedCurveInterpolator.calculateAverageSpeed(updatedNodes)
                                    onCurveChanged("Custom Curve", avgSpeed, updatedNodes)
                                }
                            }
                        )
                    }
            ) {
                val w = size.width
                val h = size.height

                // Draw Horizontal Speed Gridlines (0.2x, 0.5x, 1.0x, 2.5x, 5.0x)
                val gridSpeeds = listOf(
                    0.0f to "0.2x (Slow)",
                    0.166f to "0.5x",
                    0.354f to "1.0x (Normal)",
                    0.687f to "2.5x",
                    1.0f to "5.0x (Fast)"
                )

                gridSpeeds.forEach { (normY, label) ->
                    val y = h - (normY * h)
                    val isBaseLine = normY in 0.34f..0.37f
                    drawLine(
                        color = if (isBaseLine) StudioSecondaryTeal.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = if (isBaseLine) 1.5.dp.toPx() else 1.dp.toPx()
                    )
                }

                // Ultra Smooth Monotone Hermite / Catmull-Rom Bezier Curve Path
                if (nodes.size >= 2) {
                    val path = Path()
                    val sortedNodes = nodes.sortedBy { it.normX }
                    val points = sortedNodes.map { Offset(it.normX * w, h - (it.normY * h)) }

                    path.moveTo(points.first().x, points.first().y)

                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val cx1 = p0.x + (p1.x - p0.x) * 0.5f
                        val cy1 = p0.y
                        val cx2 = p0.x + (p1.x - p0.x) * 0.5f
                        val cy2 = p1.y
                        path.cubicTo(cx1, cy1, cx2, cy2, p1.x, p1.y)
                    }

                    // Gradient Fill under curve for modern visual depth
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
                                StudioSecondaryTeal.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )

                    // Glowing Neon Stroke
                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                StudioSecondaryTeal,
                                StudioPrimaryViolet,
                                StudioAccentAmber,
                                StudioAccentPink
                            )
                        ),
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Draw Direct Touch Interactive Node Circles
                nodes.forEachIndexed { index, node ->
                    val cx = node.normX * w
                    val cy = h - (node.normY * h)
                    val isSelected = index == selectedNodeIndex

                    if (isSelected) {
                        // Outer Pulsing Glow
                        drawCircle(
                            color = StudioAccentAmber.copy(alpha = 0.35f),
                            radius = 20.dp.toPx(),
                            center = Offset(cx, cy)
                        )
                        drawCircle(
                            color = StudioSecondaryTeal.copy(alpha = 0.6f),
                            radius = 12.dp.toPx(),
                            center = Offset(cx, cy)
                        )
                    }

                    // Solid Node Disc
                    drawCircle(
                        color = if (isSelected) StudioAccentAmber else Color.White,
                        radius = if (isSelected) 8.dp.toPx() else 6.dp.toPx(),
                        center = Offset(cx, cy)
                    )

                    // Center Core Hole
                    drawCircle(
                        color = Color(0xFF0F1117),
                        radius = if (isSelected) 4.dp.toPx() else 3.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                }
            }

            // Real-Time Floating Magnifier Tooltip when dragging
            if (isDragging && selectedNodeIndex in nodes.indices) {
                val currentNode = nodes[selectedNodeIndex]
                val currentSpeed = currentNode.speedMultiplier()
                val isSlow = currentSpeed < 0.9f

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSlow) StudioSecondaryTeal else StudioAccentAmber
                    ),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                        .shadow(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSlow) Icons.Default.SlowMotionVideo else Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = if (isSlow) StudioSecondaryTeal else StudioAccentAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${String.format("%.2f", currentSpeed)}x Speed • Waktu ${(currentNode.normX * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Preset Speed Curves Bar
        Text("Pilih Preset Kurva Cepat:", color = StudioTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))

        val presetList = listOf(
            "Hero" to "Hero (Fast-Slow-Fast)",
            "Montage" to "Montage Beat",
            "Bullet Time" to "Bullet Time Slow-Mo",
            "Fast Out" to "Akselerasi Cepat",
            "Slow In" to "Deselerasi Halus",
            "Normal" to "Reset Normal 1.0x"
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(presetList) { (key, label) ->
                FilterChip(
                    selected = curvePreset == key,
                    onClick = {
                        val newNodes = getNodesForPreset(key)
                        nodes = newNodes
                        selectedNodeIndex = 0
                        val avg = SpeedCurveInterpolator.calculateAverageSpeed(newNodes)
                        onCurveChanged(key, avg, newNodes)
                    },
                    label = { Text(label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StudioPrimaryViolet,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Action Buttons: Add Node, Remove Node, Reset
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Add node button
                OutlinedButton(
                    onClick = {
                        if (nodes.size < 8) {
                            val middleIndex = (selectedNodeIndex.coerceAtLeast(0) + 1).coerceAtMost(nodes.size - 1)
                            val prevX = nodes[selectedNodeIndex].normX
                            val nextX = nodes.getOrNull(selectedNodeIndex + 1)?.normX ?: 1.0f
                            val newX = (prevX + nextX) / 2f
                            val newY = nodes[selectedNodeIndex].normY
                            val newId = (nodes.maxOfOrNull { it.id } ?: 0) + 1
                            val newNode = CurveNode(newId, newX, newY)
                            val updated = (nodes + newNode).sortedBy { it.normX }
                            nodes = updated
                            selectedNodeIndex = updated.indexOf(newNode)
                            val avg = SpeedCurveInterpolator.calculateAverageSpeed(updated)
                            onCurveChanged("Custom Curve", avg, updated)
                        }
                    },
                    enabled = nodes.size < 8,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Titik", fontSize = 11.sp)
                }

                // Delete selected node (only middle nodes)
                if (selectedNodeIndex > 0 && selectedNodeIndex < nodes.size - 1 && nodes.size > 2) {
                    OutlinedButton(
                        onClick = {
                            val updated = nodes.toMutableList()
                            updated.removeAt(selectedNodeIndex)
                            nodes = updated
                            selectedNodeIndex = (selectedNodeIndex - 1).coerceAtLeast(0)
                            val avg = SpeedCurveInterpolator.calculateAverageSpeed(updated)
                            onCurveChanged("Custom Curve", avg, updated)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioAccentPink),
                        border = BorderStroke(1.5.dp, StudioAccentPink),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hapus Titik", fontSize = 11.sp)
                    }
                }
            }

            TextButton(
                onClick = {
                    val defaultNodes = getNodesForPreset("Normal")
                    nodes = defaultNodes
                    selectedNodeIndex = 0
                    onCurveChanged("Normal", 1.0f, defaultNodes)
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = StudioTextSecondary)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset 1.0x", fontSize = 11.sp, color = StudioTextSecondary)
            }
        }
    }
}

fun getNodesForPreset(preset: String): List<CurveNode> {
    return when (preset) {
        "Hero", "Hero Curve" -> listOf(
            CurveNode(1, 0.0f, 0.354f),  // 1.0x
            CurveNode(2, 0.22f, 0.85f),  // 4.2x Fast burst
            CurveNode(3, 0.55f, 0.06f),  // 0.4x Ultra slow motion
            CurveNode(4, 0.78f, 0.65f),  // 3.2x Dynamic punch
            CurveNode(5, 1.0f, 0.354f)   // 1.0x
        )
        "Bullet Time" -> listOf(
            CurveNode(1, 0.0f, 0.354f),  // 1.0x
            CurveNode(2, 0.20f, 0.02f),  // 0.25x Ultra Slow-mo
            CurveNode(3, 0.70f, 0.02f),  // 0.25x Ultra Slow-mo
            CurveNode(4, 0.88f, 0.82f),  // 4.0x Speed burst
            CurveNode(5, 1.0f, 0.354f)   // 1.0x
        )
        "Montage" -> listOf(
            CurveNode(1, 0.0f, 0.55f),   // 2.8x
            CurveNode(2, 0.25f, 0.08f),  // 0.5x
            CurveNode(3, 0.50f, 0.75f),  // 3.8x
            CurveNode(4, 0.75f, 0.10f),  // 0.6x
            CurveNode(5, 1.0f, 0.60f)    // 3.0x
        )
        "Fast Out" -> listOf(
            CurveNode(1, 0.0f, 0.08f),   // 0.5x
            CurveNode(2, 0.40f, 0.25f),  // 1.0x
            CurveNode(3, 0.75f, 0.80f),  // 4.0x
            CurveNode(4, 1.0f, 0.95f)    // 5.0x
        )
        "Slow In" -> listOf(
            CurveNode(1, 0.0f, 0.95f),   // 5.0x
            CurveNode(2, 0.25f, 0.75f),  // 3.8x
            CurveNode(3, 0.60f, 0.25f),  // 1.0x
            CurveNode(4, 1.0f, 0.05f)    // 0.35x
        )
        else -> listOf( // Normal 1.0x linear
            CurveNode(1, 0.0f, 0.354f),  // 1.0x
            CurveNode(2, 0.33f, 0.354f), // 1.0x
            CurveNode(3, 0.66f, 0.354f), // 1.0x
            CurveNode(4, 1.0f, 0.354f)   // 1.0x
        )
    }
}
