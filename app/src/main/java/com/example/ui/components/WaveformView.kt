package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AcousticCyan
import com.example.ui.theme.DarkCard

@Composable
fun WaveformView(
    waveformPoints: List<Float>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val minDb = 20f
            val maxDb = 110f

            // Reference Grid Lines at 40 dB, 60 dB, 80 dB, 100 dB
            val gridDbs = listOf(40f, 60f, 80f, 100f)
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

            for (db in gridDbs) {
                val yNorm = 1.0f - ((db - minDb) / (maxDb - minDb)).coerceIn(0f, 1f)
                val lineY = yNorm * height
                drawLine(
                    color = Color(0xFF334155),
                    start = Offset(0f, lineY),
                    end = Offset(width, lineY),
                    strokeWidth = 1f,
                    pathEffect = dashEffect
                )
            }

            if (waveformPoints.size >= 2) {
                val stepX = width / (waveformPoints.size - 1)
                val linePath = Path()
                val fillPath = Path()

                fillPath.moveTo(0f, height)

                waveformPoints.forEachIndexed { i, dbVal ->
                    val x = i * stepX
                    val yNorm = 1.0f - ((dbVal - minDb) / (maxDb - minDb)).coerceIn(0f, 1f)
                    val y = yNorm * height

                    if (i == 0) {
                        linePath.moveTo(x, y)
                        fillPath.lineTo(x, y)
                    } else {
                        // Smooth cubic bezier segment
                        val prevX = (i - 1) * stepX
                        val prevDb = waveformPoints[i - 1]
                        val prevY = (1.0f - ((prevDb - minDb) / (maxDb - minDb)).coerceIn(0f, 1f)) * height
                        val cx1 = prevX + (x - prevX) / 2f
                        val cy1 = prevY
                        val cx2 = prevX + (x - prevX) / 2f
                        val cy2 = y
                        linePath.cubicTo(cx1, cy1, cx2, cy2, x, y)
                        fillPath.cubicTo(cx1, cy1, cx2, cy2, x, y)
                    }
                }

                fillPath.lineTo(width, height)
                fillPath.close()

                // Draw translucent gradient underneath wave
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            AcousticCyan.copy(alpha = 0.35f),
                            AcousticCyan.copy(alpha = 0.03f)
                        ),
                        startY = 0f,
                        endY = height
                    )
                )

                // Draw glowing waveform line
                drawPath(
                    path = linePath,
                    color = AcousticCyan,
                    style = Stroke(width = 2.5f)
                )

                // Draw pulsing current dot at the latest sample
                val lastIdx = waveformPoints.size - 1
                val lastDb = waveformPoints[lastIdx]
                val lastX = width
                val lastY = (1.0f - ((lastDb - minDb) / (maxDb - minDb)).coerceIn(0f, 1f)) * height
                drawCircle(
                    color = AcousticCyan.copy(alpha = 0.4f),
                    radius = 8f,
                    center = Offset(lastX, lastY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.5f,
                    center = Offset(lastX, lastY)
                )
            }
        }

        // dB Axis labels
        Text(
            text = "100 dB",
            fontSize = 9.sp,
            color = Color(0xFF64748B),
            modifier = Modifier.align(Alignment.TopStart)
        )
        Text(
            text = "60 dB",
            fontSize = 9.sp,
            color = Color(0xFF64748B),
            modifier = Modifier.align(Alignment.CenterStart)
        )
        Text(
            text = "40 dB",
            fontSize = 9.sp,
            color = Color(0xFF64748B),
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}
