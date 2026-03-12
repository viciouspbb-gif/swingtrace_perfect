package com.golftrajectory.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.golftrajectory.app.logic.BiomechanicsFrame

@Composable
fun KinematicsGraph(
    history: List<BiomechanicsFrame>,
    modifier: Modifier = Modifier
) {
    if (history.isEmpty()) return

    Column(
        modifier = modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .padding(16.dp)
            .width(300.dp)
            .height(120.dp)
    ) {
        Text(
            text = "KINEMATICS HISTORY",
            color = Color.White,
            style = MaterialTheme.typography.titleSmall
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        ) {
            drawKinematicsGraph(history, size.width, size.height)
        }
    }
}

private fun DrawScope.drawKinematicsGraph(
    history: List<BiomechanicsFrame>,
    canvasWidth: Float,
    canvasHeight: Float
) {
    // Background
    drawRect(color = Color.Black.copy(alpha = 0.8f))
    
    // Grid lines
    drawGrid(canvasWidth, canvasHeight)
    
    // Draw X-Factor line (green)
    drawMetricLine(
        history = history,
        getValue = { it.xFactorDegrees },
        color = Color.Green,
        canvasWidth = canvasWidth,
        canvasHeight = canvasHeight,
        minValue = 0f,
        maxValue = 90f
    )
    
    // Draw Spine Angle line (cyan)
    drawMetricLine(
        history = history,
        getValue = { it.spineAngleDegrees },
        color = Color.Cyan,
        canvasWidth = canvasWidth,
        canvasHeight = canvasHeight,
        minValue = 0f,
        maxValue = 90f
    )
}

private fun DrawScope.drawGrid(width: Float, height: Float) {
    val gridColor = Color.Gray.copy(alpha = 0.3f)
    
    // Horizontal lines
    for (i in 0..4) {
        val y = (height / 4) * i
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 1f
        )
    }
    
    // Vertical lines
    for (i in 0..4) {
        val x = (width / 4) * i
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = 1f
        )
    }
}

private fun DrawScope.drawMetricLine(
    history: List<BiomechanicsFrame>,
    getValue: (BiomechanicsFrame) -> Float,
    color: Color,
    canvasWidth: Float,
    canvasHeight: Float,
    minValue: Float,
    maxValue: Float
) {
    if (history.size < 2) return
    
    val path = Path()
    val xStep = canvasWidth / (history.size - 1)
    
    history.forEachIndexed { index, frame ->
        val value = getValue(frame).coerceIn(minValue, maxValue)
        val normalizedValue = (value - minValue) / (maxValue - minValue)
        val x = index * xStep
        val y = canvasHeight - (normalizedValue * canvasHeight)
        
        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    
    drawPath(
        path = path,
        color = color,
        style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 2f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    )
}
