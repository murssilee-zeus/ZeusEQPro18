package com.zeus.eqpro18

import androidx.compose.ui.graphics.Color

data class EqBand(
    val id: Int,
    var frequency: Float = 1000f,
    var gain: Float = 0f,
    var q: Float = 1.0f,
    var enabled: Boolean = true,
    var filterType: FilterType = FilterType.PEAK,
    val color: Color
) {
    enum class FilterType {
        LOW_SHELF, HIGH_SHELF, PEAK, LOW_PASS, HIGH_PASS, NOTCH, BAND_PASS
    }

    fun copyValuesFrom(other: EqBand) {
        frequency = other.frequency.coerceIn(1f, 30000f)
        gain = other.gain.coerceIn(-30f, 30f)
        q = other.q.coerceIn(0.1f, 40f)
        enabled = other.enabled
        filterType = other.filterType
    }
}

fun createDefaultBands(): List<EqBand> {
    val colors = listOf(
        Color(0xFFFF6B6B),
        Color(0xFFFF9F43),
        Color(0xFFFFEAA7),
        Color(0xFF55EFC4),
        Color(0xFF74B9FF),
        Color(0xFFA29BFE),
        Color(0xFFFD79A8),
        Color(0xFF00CEC9),
        Color(0xFFE17055),
        Color(0xFF6C5CE7)
    )

    val defaults = listOf(
        Triple(30f, 0f, 0.7f),
        Triple(80f, 0f, 1.2f),
        Triple(200f, 0f, 1.5f),
        Triple(500f, 0f, 1.0f),
        Triple(1000f, 0f, 1.0f),
        Triple(2000f, 0f, 1.2f),
        Triple(4000f, 0f, 1.5f),
        Triple(8000f, 0f, 1.0f),
        Triple(12000f, 0f, 0.7f),
        Triple(16000f, 0f, 0.8f)
    )

    return defaults.mapIndexed { index, (freq, gain, q) ->
        EqBand(
            id = index,
            frequency = freq,
            gain = gain,
            q = q,
            enabled = true,
            filterType = when (index) {
                0 -> EqBand.FilterType.LOW_SHELF
                9 -> EqBand.FilterType.HIGH_SHELF
                else -> EqBand.FilterType.PEAK
            },
            color = colors[index % colors.size]
        )
    }
}
