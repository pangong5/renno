package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.FrequencyBand
import com.example.ui.theme.AcousticAmber
import com.example.ui.theme.AcousticCyan
import com.example.ui.theme.AcousticGreen
import com.example.ui.theme.AcousticRed
import com.example.ui.theme.AcousticTeal
import com.example.ui.theme.AcousticYellow
import com.example.ui.theme.DarkCard

@Composable
fun SpectrumVisualizer(
    bands: List<FrequencyBand>,
    dominantFreqHz: Float,
    dominantNote: String,
    dominantBand: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
            .padding(14.dp)
    ) {
        // Header with Dominant Frequency Callout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Spektrum Frekuensi",
                    tint = AcousticCyan,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text(
                    text = "Spektrum Real-Time (FFT)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White
                )
            }

            // Dominant note pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = AcousticAmber,
                    modifier = Modifier.padding(end = 4.dp).width(14.dp).height(14.dp)
                )
                Text(
                    text = if (dominantFreqHz > 20f) "%.0f Hz • %s".format(dominantFreqHz, dominantNote) else "-",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = AcousticAmber
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Spectrum Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val bandCount = bands.size.coerceAtLeast(1)
                val barSpacing = 3f
                val totalSpacing = barSpacing * (bandCount - 1)
                val barWidth = ((canvasWidth - totalSpacing) / bandCount).coerceAtLeast(2f)

                // Background horizontal guide lines
                val guideLines = 4
                for (g in 1..guideLines) {
                    val gy = (g.toFloat() / guideLines) * canvasHeight
                    drawLine(
                        color = Color(0xFF1E293B),
                        start = Offset(0f, gy),
                        end = Offset(canvasWidth, gy),
                        strokeWidth = 1f
                    )
                }

                // Draw bars
                bands.forEachIndexed { i, band ->
                    val x = i * (barWidth + barSpacing)
                    val mag = band.magnitudeNorm.coerceIn(0.04f, 1.0f)
                    val barHeight = mag * canvasHeight
                    val topY = canvasHeight - barHeight

                    // Dynamic color based on frequency spectrum range
                    val barBrush = when {
                        i < bandCount * 0.25f -> Brush.verticalGradient(
                            listOf(AcousticCyan, Color(0xFF0284C7)),
                            startY = topY,
                            endY = canvasHeight
                        )
                        i < bandCount * 0.55f -> Brush.verticalGradient(
                            listOf(AcousticTeal, AcousticGreen),
                            startY = topY,
                            endY = canvasHeight
                        )
                        i < bandCount * 0.8f -> Brush.verticalGradient(
                            listOf(AcousticYellow, AcousticAmber),
                            startY = topY,
                            endY = canvasHeight
                        )
                        else -> Brush.verticalGradient(
                            listOf(AcousticRed, AcousticAmber),
                            startY = topY,
                            endY = canvasHeight
                        )
                    }

                    // Rounded bar
                    drawRoundRect(
                        brush = barBrush,
                        topLeft = Offset(x, topY),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(4f, 4f)
                    )

                    // Peak dot indicator
                    drawCircle(
                        color = Color.White.copy(alpha = 0.9f),
                        radius = (barWidth / 2f).coerceAtMost(3.5f),
                        center = Offset(x + barWidth / 2f, (topY - 4f).coerceAtLeast(3f))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Frequency Band Labels (Bass, Mid, Treble)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("20 Hz (Bass)", fontSize = 10.sp, color = Color(0xFF64748B))
            Text("500 Hz (Mid)", fontSize = 10.sp, color = Color(0xFF64748B))
            Text("4 kHz (Treble)", fontSize = 10.sp, color = Color(0xFF64748B))
            Text("20 kHz", fontSize = 10.sp, color = Color(0xFF64748B))
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Kategori Frekuensi Terkuat: $dominantBand",
            fontSize = 11.sp,
            color = Color(0xFF94A3B8),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}
