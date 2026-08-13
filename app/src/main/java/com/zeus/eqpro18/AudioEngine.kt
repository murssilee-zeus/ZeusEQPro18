package com.zeus.eqpro18

import android.content.Context
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Visualizer
import android.util.Log
import kotlin.math.*

class AudioEngine(private val context: Context) {

    companion object {
        private const val TAG = "ZeusAudioEngine"
        private const val MAX_BANDS = 18
        private const val CHANNEL_COUNT = 2
        private const val MAX_SAFE_GAIN = 6f
    }

    private var dynamicsProcessing: DynamicsProcessing? = null
    private var visualizer: Visualizer? = null
    private var audioSessionId: Int = 0
    private var isEnabled = false

    private var bands: List<EqBand> = createDefaultBands()
    private var preGain = -3f
    private var limiterEnabled = true
    private var limiterThreshold = -1.5f
    private var limiterAttack = 1f
    private var limiterRelease = 60f
    private var limiterRatio = 12f
    private var limiterPostGain = 0f

    @Volatile
    var spectrumData: FloatArray = FloatArray(128) { 0f }
        private set

    fun initialize(sessionId: Int = 0): Boolean {
        release()
        audioSessionId = sessionId

        return try {
            val config = DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                CHANNEL_COUNT,
                true,
                MAX_BANDS,
                true,
                MAX_BANDS,
                true,
                MAX_BANDS,
                true
            ).build()

            dynamicsProcessing
