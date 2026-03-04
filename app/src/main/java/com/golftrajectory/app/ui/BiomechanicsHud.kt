package com.golftrajectory.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .padding(16.dp)
            .width(240.dp)
    ) {
        Text(
            text = "Biomechanics (Real-time)",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        MetricRow("X-Factor", String.format(Locale.US, "%.1f°", frame.xFactorDegrees))
        MetricRow("Spine Angle", String.format(Locale.US, "%.1f°", frame.spineAngleDegrees))
        
        val stabilityText = if (frame.isStable) "STABLE" else "UNSTABLE"
        val stabilityColor = if (frame.isStable) Color.Green else Color.Red
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "COG", color = Color.LightGray, style = MaterialTheme.typography.bodyMedium)
            Text(text = stabilityText, color = stabilityColor, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.LightGray, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, color = Color.White, style = MaterialTheme.typography.bodyLarge)
    }
}
