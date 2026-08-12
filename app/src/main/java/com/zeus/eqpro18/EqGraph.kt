package com.zeus.eqpro18

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.*

// ==========================================
// 1. GRAFICO DE EQUALIZADOR Y CROSSOVER
// ==========================================
@Composable
fun EqGraph(
    bands: List<EqBand>,
    selectedIndex: Int,
    spectrum: FloatArray,
    onBandSelected: (Int) -> Unit,
    onBandMoved: (Int, frequency: Float, gain: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val minFreq = 20f
    val maxFreq = 20000f
    val minGain = -30f
    val maxGain = 30f

    fun freqToX(freq: Float, width: Float): Float {
        val logMin = ln(minFreq)
        val logMax = ln(maxFreq)
        val logF = ln(freq.coerceIn(minFreq, maxFreq))
        return ((logF - logMin) / (logMax - logMin)) * width
    }

    fun xToFreq(x: Float, width: Float): Float {
        val logMin = ln(minFreq)
        val logMax = ln(maxFreq)
        val ratio = (x / width).coerceIn(0f, 1f)
        return exp(logMin + ratio * (logMax - logMin))
    }

    fun gainToY(gain: Float, height: Float): Float {
        val range = maxGain - minGain
        return height - ((gain - minGain) / range) * height
    }

    fun yToGain(y: Float, height: Float): Float {
        val range = maxGain - minGain
        return maxGain - (y / height) * range
    }

    val responsePoints = remember(bands) {
        val points = 300
        FloatArray(points) { i ->
            val freq = exp(ln(minFreq) + (i.toFloat() / (points - 1)) * (ln(maxFreq) - ln(minFreq)))
            var totalGain = 0f
            bands.filter { it.enabled }.forEach { band ->
                totalGain += calculateBandResponse(freq, band)
            }
            totalGain.coerceIn(minGain, maxGain)
        }
    }

    Box(modifier = modifier.background(Color(0xFF0F0F14)).padding(4.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(bands) {
                    detectTapGestures { offset ->
                        var closest = -1
                        var minDist = Float.MAX_VALUE
                        bands.forEachIndexed { idx, band ->
                            val bx = freqToX(band.frequency, size.width.toFloat())
                            val by = gainToY(band.gain, size.height.toFloat())
                            val dist = (offset.x - bx).pow(2) + (offset.y - by).pow(2)
                            if (dist < minDist && dist < 80f.pow(2)) {
                                minDist = dist
                                closest = idx
                            }
                        }
                        if (closest >= 0) onBandSelected(closest)
                    }
                }
                .pointerInput(selectedIndex) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val freq = xToFreq(change.position.x, size.width.toFloat())
                        val gain = yToGain(change.position.y, size.height.toFloat())
                        if (selectedIndex in bands.indices) {
                            onBandMoved(selectedIndex, freq, gain)
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val gridColor = Color(0xFF22222D)

            // Cuadrícula horizontal (dB)
            for (db in -30..30 step 10) {
                val y = gainToY(db.toFloat(), h)
                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            }

            // Guías de frecuencia logarítmicas
            listOf(20f, 100f, 1000f, 10000f, 20000f).forEach { f ->
                val x = freqToX(f, w)
                drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
            }

            val zeroY = gainToY(0f, h)
            drawLine(Color(0xFF38384A), Offset(0f, zeroY), Offset(w, zeroY), strokeWidth = 1.5f)

            // Espectro FFT
            if (spectrum.isNotEmpty()) {
                val spectrumPath = Path()
                spectrumPath.moveTo(0f, h)
                val step = w / (spectrum.size - 1).coerceAtLeast(1)
                spectrum.forEachIndexed { i, mag ->
                    val x = i * step
                    val y = h - mag * h * 0.85f
                    spectrumPath.lineTo(x, y)
                }
                spectrumPath.lineTo(w, h)
                spectrumPath.close()
                drawPath(spectrumPath, brush = Brush.verticalGradient(listOf(Color(0x3340A0C0), Color(0x0540A0C0))))
            }

            // Dibujado de curvas por banda
            bands.forEachIndexed { idx, band ->
                if (!band.enabled) return@forEachIndexed
                val path = Path()
                val fillPath = Path()
                val points = 200
                var first = true
                for (i in 0 until points) {
                    val freq = exp(ln(minFreq) + (i.toFloat() / (points - 1)) * (ln(maxFreq) - ln(minFreq)))
                    val g = calculateBandResponse(freq, band)
                    val x = freqToX(freq, w)
                    val y = gainToY(g, h)
                    if (first) {
                        path.moveTo(x, y)
                        fillPath.moveTo(x, zeroY)
                        fillPath.lineTo(x, y)
                        first = false
                    } else {
                        path.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }
                fillPath.lineTo(freqToX(maxFreq, w), zeroY)
                fillPath.close()
                val alpha = if (idx == selectedIndex) 0.30f else 0.12f
                drawPath(fillPath, band.color.copy(alpha = alpha))
                drawPath(path, band.color.copy(alpha = if (idx == selectedIndex) 1f else 0.6f),
                    style = Stroke(width = if (idx == selectedIndex) 3f else 1.8f, cap = StrokeCap.Round))
            }

            // Curva global combinada
            val combinedPath = Path()
            val n = responsePoints.size
            for (i in 0 until n) {
                val freq = exp(ln(minFreq) + (i.toFloat() / (n - 1)) * (ln(maxFreq) - ln(minFreq)))
                val x = freqToX(freq, w)
                val y = gainToY(responsePoints[i], h)
                if (i == 0) combinedPath.moveTo(x, y) else combinedPath.lineTo(x, y)
            }
            drawPath(combinedPath, Color(0xFFFFF3B0).copy(alpha = 0.95f), style = Stroke(width = 2.5f, cap = StrokeCap.Round))

            // Dibujado de marcadores en forma de triángulo estilo Crossover
            bands.forEachIndexed { idx, band ->
                val x = freqToX(band.frequency, w)
                val y = gainToY(band.gain, h)
                val isSelected = (idx == selectedIndex)
                
                val trianglePath = Path().apply {
                    val side = if (isSelected) 28f else 22f
                    moveTo(x, y - side / 2f)
                    lineTo(x - side / 2f, y + side / 2f)
                    lineTo(x + side / 2f, y + side / 2f)
                    close()
                }

                drawPath(trianglePath, color = if (band.enabled) band.color else Color.Gray)
                if (isSelected) {
                    drawPath(trianglePath, color = Color.White, style = Stroke(width = 2.5f))
                }
            }
        }
    }
}

// ==========================================
// 2. VÚMETRO Y PANEL DEL LIMITADOR (GR & CEILING)
// ==========================================
@Composable
fun LimiterMetersGraph(
    ceilingDb: Float,
    gainReductionDb: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFF141419))
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val barWidth = w * 0.35f
            val spacing = w * 0.1f

            // 1. Barra Ceiling (Nivel Techo)
            val ceilingNorm = ((ceilingDb + 30f) / 30f).coerceIn(0f, 1f)
            val ceilingBarH = ceilingNorm * h
            
            // Fondo barra
            drawRect(
                color = Color(0xFF22222A),
                topLeft = Offset(0f, 0f),
                size = Size(barWidth, h)
            )
            // Indicador activo Ceiling
            drawRect(
                color = Color(0xFF43A047),
                topLeft = Offset(0f, h - ceilingBarH),
                size = Size(barWidth, ceilingBarH)
            )

            // 2. Barra GR (Gain Reduction)
            val grNorm = (abs(gainReductionDb.coerceIn(-30f, 0f)) / 30f).coerceIn(0f, 1f)
            val grBarH = grNorm * h

            val grX = barWidth + spacing
            // Fondo barra GR
            drawRect(
                color = Color(0xFF22222A),
                topLeft = Offset(grX, 0f),
                size = Size(barWidth, h)
            )
            // Atenuación atajada por el limitador (cae desde arriba)
            drawRect(
                color = Color(0xFFE53935),
                topLeft = Offset(grX, 0f),
                size = Size(barWidth, grBarH)
            )
        }
    }
}

// ==========================================
// MATEMÁTICA DE RESPUESTA EN FRECUENCIA
// ==========================================
fun calculateBandResponse(freq: Float, band: EqBand): Float {
    if (!band.enabled || band.gain == 0f) return 0f
    val f0 = band.frequency
    val gainDb = band.gain
    val q = band.q.coerceAtLeast(0.1f)
    val w = freq / f0

    return when (band.filterType) {
        EqBand.FilterType.PEAK, EqBand.FilterType.BAND_PASS -> {
            val bw = 1f / q
            val factor = exp(-((ln(w)).pow(2)) / (2 * bw * bw))
            gainDb * factor
        }
        EqBand.FilterType.LOW_SHELF -> {
            if (freq < f0) gainDb * (1f - (freq / f0).pow(2)).coerceIn(0f, 1f) + gainDb * 0.1f
            else gainDb * 0.15f
        }
        EqBand.FilterType.HIGH_SHELF -> {
            if (freq > f0) gainDb * (1f - (f0 / freq).pow(2)).coerceIn(0f, 1f)
            else gainDb * 0.15f
        }
        EqBand.FilterType.LOW_PASS -> {
            val order = (q * 2).coerceIn(1f, 8f)
            -(20f * log10(1 + (freq / f0).pow(order)))
        }
        EqBand.FilterType.HIGH_PASS -> {
            val order = (q * 2).coerceIn(1f, 8f)
            -(20f * log10(1 + (f0 / freq).pow(order)))
        }
        EqBand.FilterType.NOTCH -> {
            val bw = 1f / q
            val factor = exp(-((ln(w)).pow(2)) / (2 * bw * bw))
            -gainDb.absoluteValue * factor * 2f
        }
    }.coerceIn(-30f, 30f)
}
