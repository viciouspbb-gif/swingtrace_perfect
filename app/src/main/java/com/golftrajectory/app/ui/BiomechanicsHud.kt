package com.golftrajectory.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.golftrajectory.app.logic.BiomechanicsFrame
import java.util.Locale

@Composable
fun BiomechanicsHud(frame: BiomechanicsFrame?, modifier: Modifier = Modifier) {
    if (frame == null) return

    Column(
        modifier = modifier
            .padding(16.dp)
            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
            .padding(16.dp)
            .width(260.dp)
    ) {
        Text(
            text = "KINEMATICS",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        // X-Factor ゲージ (表示範囲: 0〜90度, 理想: 30〜60度)
        MetricGaugeRow(
            label = "X-Factor",
            currentValue = frame.xFactorDegrees,
            displayMin = 0f,
            displayMax = 90f,
            targetMin = 30f,
            targetMax = 60f
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        // Spine Angle ゲージ (表示範囲: 0〜90度, 理想: 25〜50度)
        MetricGaugeRow(
            label = "Spine Angle",
            currentValue = frame.spineAngleDegrees,
            displayMin = 0f,
            displayMax = 90f,
            targetMin = 25f,
            targetMax = 50f
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        // COG Stability
        val stabilityText = if (frame.isStable) "STABLE" else "UNSTABLE"
        val stabilityColor = if (frame.isStable) Color.Green else Color.Red
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "COG", color = Color.LightGray, style = MaterialTheme.typography.bodyMedium)
            Text(text = stabilityText, color = stabilityColor, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun MetricGaugeRow(
    label: String,
    currentValue: Float,
    displayMin: Float,
    displayMax: Float,
    targetMin: Float,
    targetMax: Float
) {
    val valueFormatted = String.format(Locale.US, "%.1f°", currentValue)
    val clampValue = currentValue.coerceIn(displayMin, displayMax)
    val ratio = ((clampValue - displayMin) / (displayMax - displayMin)).coerceIn(0f, 1f)
    
    val inTarget = currentValue in targetMin..targetMax
    val barColor = if (inTarget) Color.Green else Color.Yellow

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = Color.LightGray, style = MaterialTheme.typography.bodyMedium)
            Text(text = valueFormatted, color = Color.White, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(modifier = Modifier.height(4.dp))
        
        // Custom Gauge Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.DarkGray)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio)
                    .height(6.dp)
                    .background(barColor)
            )
        }
    }
}
