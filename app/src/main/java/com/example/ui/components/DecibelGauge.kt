package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AcousticAmber
import com.example.ui.theme.AcousticCyan
import com.example.ui.theme.AcousticGreen
import com.example.ui.theme.AcousticRed
import com.example.ui.theme.AcousticYellow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DecibelGauge(
    currentDb: Float,
    peakDb: Float,
    weightingType: String = "dBA",
    modifier: Modifier = Modifier
) {
    // Smooth needle animation
    val animatedDb by animateFloatAsState(
        targetValue = currentDb.coerceIn(0f, 120f),
        animationSpec = tween(durationMillis = 80),
        label = "gauge_needle_anim"
    )

    // Gauge sweeps from 135 degrees to 405 degrees (total 270 degree sweep)
    val startAngle = 135f
    val sweepAngle = 270f
    val maxDb = 120f

    Box(
        modifier = modifier.size(270.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.minDimension / 2f) - 16f
            val arcSize = Size(radius * 2f, radius * 2f)
            val arcTopLeft = Offset(center.x - radius, center.y - radius)

            // 1. Background Arc Track
            drawArc(
                color = Color(0xFF1E293B),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = 18f, cap = StrokeCap.Round)
            )

            // 2. Multi-color Active Arc Brush (Safe Cyan -> Green -> Warning Amber -> Danger Red)
            val gaugeGradient = Brush.sweepGradient(
                0.0f to AcousticCyan,
                0.3f to AcousticGreen,
                0.55f to AcousticYellow,
                0.75f to AcousticAmber,
                1.0f to AcousticRed,
                center = center
            )

            val activeSweep = (animatedDb / maxDb) * sweepAngle
            if (activeSweep > 1f) {
                drawArc(
                    brush = gaugeGradient,
                    startAngle = startAngle,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = 18f, cap = StrokeCap.Round)
                )
            }

            // 3. Peak Marker Dot on rim
            val peakProgress = (peakDb.coerceIn(0f, maxDb) / maxDb)
            val peakAngleRad = Math.toRadians((startAngle + peakProgress * sweepAngle).toDouble())
            val peakDotX = center.x + radius * cos(peakAngleRad).toFloat()
            val peakDotY = center.y + radius * sin(peakAngleRad).toFloat()
            drawCircle(
                color = Color(0xFFFF5252),
                radius = 5.5f,
                center = Offset(peakDotX, peakDotY)
            )

            // 4. Tick Marks (every 10 dB)
            val totalTicks = 12
            for (i in 0..totalTicks) {
                val tickFraction = i.toFloat() / totalTicks
                val angleDeg = startAngle + tickFraction * sweepAngle
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val isMajor = i % 2 == 0

                val innerR = radius - (if (isMajor) 22f else 14f)
                val outerR = radius - 8f

                val startX = center.x + innerR * cos(angleRad).toFloat()
                val startY = center.y + innerR * sin(angleRad).toFloat()
                val endX = center.x + outerR * cos(angleRad).toFloat()
                val endY = center.y + outerR * sin(angleRad).toFloat()

                val tickColor = when {
                    i >= 10 -> AcousticRed.copy(alpha = if (isMajor) 0.9f else 0.5f)
                    i >= 8 -> AcousticAmber.copy(alpha = if (isMajor) 0.9f else 0.5f)
                    else -> Color.White.copy(alpha = if (isMajor) 0.8f else 0.35f)
                }

                drawLine(
                    color = tickColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = if (isMajor) 3.5f else 2f,
                    cap = StrokeCap.Round
                )
            }

            // 5. Dynamic Needle Pointer
            val needleFraction = animatedDb / maxDb
            val needleAngleDeg = startAngle + needleFraction * sweepAngle
            val needleAngleRad = Math.toRadians(needleAngleDeg.toDouble())
            val needleLen = radius - 30f

            val tipX = center.x + needleLen * cos(needleAngleRad).toFloat()
            val tipY = center.y + needleLen * sin(needleAngleRad).toFloat()

            // Needle base perpendicular
            val baseAngleRad = needleAngleRad + PI / 2.0
            val baseOffset = 8f
            val baseLeftX = center.x + baseOffset * cos(baseAngleRad).toFloat()
            val baseLeftY = center.y + baseOffset * sin(baseAngleRad).toFloat()
            val baseRightX = center.x - baseOffset * cos(baseAngleRad).toFloat()
            val baseRightY = center.y - baseOffset * sin(baseAngleRad).toFloat()

            val needlePath = Path().apply {
                moveTo(tipX, tipY)
                lineTo(baseLeftX, baseLeftY)
                lineTo(baseRightX, baseRightY)
                close()
            }

            // Glowing needle
            drawPath(
                path = needlePath,
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, AcousticCyan),
                    center = center,
                    radius = needleLen
                )
            )

            // Needle center pivot rings
            drawCircle(
                color = Color(0xFF0F172A),
                radius = 16f,
                center = center
            )
            drawCircle(
                color = AcousticCyan,
                radius = 9f,
                center = center
            )
            drawCircle(
                color = Color.White,
                radius = 4f,
                center = center
            )
        }

        // Center Digital dB Readout (positioned below the pivot)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 80.dp)
        ) {
            Text(
                text = "%.1f".format(currentDb),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = when {
                    currentDb >= 85f -> AcousticRed
                    currentDb >= 70f -> AcousticAmber
                    else -> AcousticCyan
                }
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = weightingType.uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp
                ),
                color = Color(0xFF94A3B8)
            )
        }
    }
}
