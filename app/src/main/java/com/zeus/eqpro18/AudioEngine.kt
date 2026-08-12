package com.zeus.eqpro18

import android.content.Context
import android.media.AudioManager
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Visualizer
import android.util.Log
import kotlin.math.*

class AudioEngine(private val context: Context) {

    companion object {
        private const val TAG = "ZeusAudioEngine"
        private const val MAX_BANDS = 10
        private const val CHANNEL_COUNT = 2
    }

    private var dynamicsProcessing: DynamicsProcessing? = null
    private var visualizer: Visualizer? = null
    private var audioSessionId: Int = 0
    private var isEnabled = false

    private var bands: List<EqBand> = createDefaultBands()
    private var preGain = 0f
    private var limiterEnabled = true
    private var limiterThreshold = -1.0f
    private var limiterAttack = 1f
    private var limiterRelease = 60f
    private var limiterRatio = 10f
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

            dynamicsProcessing = DynamicsProcessing(0, audioSessionId, config).apply {
                enabled = true
            }

            applyAllBands()
            applyLimiter()

            try {
                visualizer = Visualizer(audioSessionId).apply {
                    captureSize = Visualizer.getCaptureSizeRange()[1]
                    setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) {}

                        override fun onFftDataCapture(
                            visualizer: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) {
                            if (fft == null) return
                            processFft(fft)
                        }
                    }, Visualizer.getMaxCaptureRate() / 2, false, true)
                    enabled = true
                }
            } catch (e: Exception) {
                Log.w(TAG, "Visualizer not available: ${e.message}")
            }

            isEnabled = true
            Log.i(TAG, "DynamicsProcessing initialized successfully (session=$audioSessionId)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize DynamicsProcessing: ${e.message}", e)
            false
        }
    }

    private fun processFft(fft: ByteArray) {
        val n = min(spectrumData.size, fft.size / 2)
        for (i in 0 until n) {
            val real = fft[i * 2].toInt()
            val imag = fft[i * 2 + 1].toInt()
            val magnitude = sqrt((real * real + imag * imag).toFloat())
            val db = 20f * log10(magnitude + 1f)
            val normalized = ((db + 40f) / 80f).coerceIn(0f, 1f)
            spectrumData[i] = spectrumData[i] * 0.7f + normalized * 0.3f
        }
    }

    fun setBands(newBands: List<EqBand>) {
        bands = newBands
        applyAllBands()
    }

    fun updateBand(band: EqBand) {
        val idx = bands.indexOfFirst { it.id == band.id }
        if (idx >= 0) {
            bands = bands.toMutableList().also { it[idx] = band }
            applyBand(band, idx)
        }
    }

    private fun applyAllBands() {
        val dp = dynamicsProcessing ?: return
        bands.forEachIndexed { index, band ->
            applyBand(band, index)
        }
        try {
            for (ch in 0 until CHANNEL_COUNT) {
                dp.setInputGainByChannelIndex(ch, preGain)
            }
        } catch (e: Exception) {
            Log.w(TAG, "setInputGain failed: ${e.message}")
        }
    }

    private fun applyBand(band: EqBand, index: Int) {
        val dp = dynamicsProcessing ?: return
        if (index >= MAX_BANDS) return

        try {
            val freq = band.frequency.coerceIn(1f, 20000f)
            val gain = if (band.enabled) band.gain.coerceIn(-30f, 30f) else 0f

            val preEqBand = DynamicsProcessing.EqBand(true, freq, gain)
            dp.setPreEqBandAllChannelsTo(index, preEqBand)

            val mbcBand = DynamicsProcessing.MbcBand(
                true,
                freq,
                3.0f,
                80.0f,
                1.0f,
                -50f,
                0f,
                0f,
                0f,
                gain
            )
            dp.setMbcBandAllChannelsTo(index, mbcBand)

        } catch (e: Exception) {
            Log.e(TAG, "Error applying band $index: ${e.message}")
        }
    }

    fun setPreGain(gainDb: Float) {
        preGain = gainDb.coerceIn(-30f, 30f)
        dynamicsProcessing?.let { dp ->
            for (ch in 0 until CHANNEL_COUNT) {
                try {
                    dp.setInputGainByChannelIndex(ch, preGain)
                } catch (_: Exception) {}
            }
        }
    }

    fun setLimiter(
        enabled: Boolean,
        threshold: Float = -1.0f,
        attack: Float = 1f,
        release: Float = 60f,
        ratio: Float = 10f,
        postGain: Float = 0f
    ) {
        limiterEnabled = enabled
        limiterThreshold = threshold
        limiterAttack = attack
        limiterRelease = release
        limiterRatio = ratio
        limiterPostGain = postGain
        applyLimiter()
    }

    private fun applyLimiter() {
        val dp = dynamicsProcessing ?: return
        try {
            val limiter = DynamicsProcessing.Limiter(
                limiterEnabled,
                true,
                1,
                limiterAttack,
                limiterRelease,
                limiterRatio,
                limiterThreshold,
                limiterPostGain
            )
            dp.setLimiterAllChannelsTo(limiter)
        } catch (e: Exception) {
            Log.e(TAG, "Error applying limiter: ${e.message}")
        }
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        dynamicsProcessing?.enabled = enabled
    }

    fun isEnabled(): Boolean = isEnabled && dynamicsProcessing?.enabled == true

    fun getAudioSessionId(): Int = audioSessionId

    fun release() {
        try {
            visualizer?.release()
            visualizer = null
            dynamicsProcessing?.release()
            dynamicsProcessing = null
        } catch (e: Exception) {
            Log.w(TAG, "Release error: ${e.message}")
        }
        isEnabled = false
    }

    fun attachToMediaSession(): Boolean {
        return initialize(0)
    }
}
