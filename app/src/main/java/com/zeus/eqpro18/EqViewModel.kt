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

    companion object {
        const val MAX_BANDS = 18
    }

    val bands = mutableStateListOf<EqBand>().apply {
        addAll(createDefaultBands())
    }

    var selectedBandIndex by mutableIntStateOf(4)
        private set

    var currentSection by mutableStateOf(EqSection.EQUALIZER)
        private set

    // Preamp
    var preamp by mutableFloatStateOf(-3.0f)

    // Limiter
    var limiterEnabled by mutableStateOf(true)
    var limiterThreshold by mutableFloatStateOf(-1.5f)
    var limiterAttack by mutableFloatStateOf(1.0f)
    var limiterRelease by mutableFloatStateOf(60f)
    var limiterRatio by mutableFloatStateOf(12f)
    var limiterPostGain by mutableFloatStateOf(0f)

    var crossoverFrequencies = mutableStateListOf(200f, 2000f, 8000f)

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

    /** Agrega una banda nueva (máximo 18) */
    fun addBand() {
        if (bands.size >= MAX_BANDS) return
        val newId = (bands.maxOfOrNull { it.id } ?: -1) + 1
        // Frecuencia por defecto: interpolar entre existentes o 1 kHz
        val newFreq = when {
            bands.isEmpty() -> 1000f
            else -> {
                val last = bands.last().frequency
                (last * 1.8f).coerceIn(1f, 30000f)
            }
        }
        bands.add(createNewBand(newId, newFreq))
        selectedBandIndex = bands.lastIndex
    }

    /** Quita la banda seleccionada (mínimo 1) */
    fun removeSelectedBand() {
        if (bands.size <= 1) return
        val idx = selectedBandIndex
        if (idx !in bands.indices) return
        bands.removeAt(idx)
        selectedBandIndex = idx.coerceIn(0, bands.lastIndex)
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
