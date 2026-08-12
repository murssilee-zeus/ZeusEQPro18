package com.zeus.eqpro18

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

enum class EqSection {
    EQUALIZER, CROSSOVER, LIMITER
}

class EqViewModel : ViewModel() {

    val bands = mutableStateListOf<EqBand>().apply {
        addAll(createDefaultBands())
    }

    var selectedBandIndex by mutableIntStateOf(4)
        private set

    var currentSection by mutableStateOf(EqSection.EQUALIZER)
        private set

    var limiterEnabled by mutableStateOf(true)
    var limiterThreshold by mutableFloatStateOf(-1.0f)
    var limiterAttack by mutableFloatStateOf(1.0f)
    var limiterRelease by mutableFloatStateOf(60f)
    var limiterRatio by mutableFloatStateOf(10f)
    var limiterPostGain by mutableFloatStateOf(0f)

    var crossoverFrequencies = mutableStateListOf(200f, 2000f, 8000f)

    var masterGain by mutableFloatStateOf(0f)

    var isEngineRunning by mutableStateOf(false)

    var spectrum by mutableStateOf(FloatArray(128) { 0f })

    fun selectBand(index: Int) {
        if (index in bands.indices) selectedBandIndex = index
    }

    fun updateSelectedBand(
        frequency: Float? = null,
        gain: Float? = null,
        q: Float? = null,
        enabled: Boolean? = null,
        filterType: EqBand.FilterType? = null
    ) {
        val idx = selectedBandIndex
        if (idx !in bands.indices) return
        val current = bands[idx]
        bands[idx] = current.copy(
            frequency = frequency?.coerceIn(1f, 30000f) ?: current.frequency,
            gain = gain?.coerceIn(-30f, 30f) ?: current.gain,
            q = q?.coerceIn(0.1f, 40f) ?: current.q,
            enabled = enabled ?: current.enabled,
            filterType = filterType ?: current.filterType
        )
    }

    fun nextSection() {
        currentSection = when (currentSection) {
            EqSection.EQUALIZER -> EqSection.CROSSOVER
            EqSection.CROSSOVER -> EqSection.LIMITER
            EqSection.LIMITER -> EqSection.EQUALIZER
        }
    }

    fun previousSection() {
        currentSection = when (currentSection) {
            EqSection.EQUALIZER -> EqSection.LIMITER
            EqSection.CROSSOVER -> EqSection.EQUALIZER
            EqSection.LIMITER -> EqSection.CROSSOVER
        }
    }

    fun sectionTitle(): String = when (currentSection) {
        EqSection.EQUALIZER -> "Equalizer Pro18"
        EqSection.CROSSOVER -> "Crossover Multiband"
        EqSection.LIMITER -> "Limiter"
    }

    fun selectedBand(): EqBand? = bands.getOrNull(selectedBandIndex)
}
