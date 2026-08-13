package com.zeus.eqpro18

import android.content.Context
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Visualizer
import android.util.Log
import kotlin.math.*

/**
 * Real audio processing engine using Android's native DynamicsProcessing API.
 * Supports parametric EQ bands, multiband processing and limiter.
 */
class AudioEngine(private val context: Context) {

    companion object {
        private const val TAG = "ZeusAudioEngine"
        private const val MAX_BANDS = 10
        private const val CHANNEL_COUNT = 2 // Stereo
    }

    private var dynamicsProcessing: DynamicsProcessing? = null
    private var visualizer: Visualizer? = null
    private var audioSessionId: Int = 0
    private var isEnabled = false

    // Current state
    private var bands: List<EqBand> = createDefaultBands()
    private var preGain = 0f
    private var limiterEnabled = true
    private var limiterThreshold = -1.0f
    private var limiterAttack = 1f
    private var limiterRelease = 60f
    private var limiterRatio = 10f
    private var limiterPostGain = 0f

    // Spectrum data for visualization (0..1)
    @Volatile
    var spectrumData: FloatArray = FloatArray(128) { 0f }
        private set

    fun initialize(sessionId: Int = 0): Boolean {
        release()
        audioSessionId = sessionId

        return try {
            // Create DynamicsProcessing with configuration
            val config = DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                CHANNEL_COUNT,
                true,   // preEqInUse
                MAX_BANDS,
                true,   // mbcInUse (multiband compressor)
                MAX_BANDS,
                true,   // postEqInUse
                MAX_BANDS,
                true    // limiterInUse
            ).build()

            dynamicsProcessing = DynamicsProcessing(0, audioSessionId, config).apply {
                enabled = true
            }

            // Prefer global session when possible (session 0)
            applyAllBands()
            applyLimiter()

            // Visualizer for spectrum (uses same session)
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
            // Smooth and normalize
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
        // Also set channel-based gains if needed
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
            val freq = band.frequency.coerceIn(1f, 20000f) // DP has practical limits
            val gain = if (band.enabled) band.gain.coerceIn(-30f, 30f) else 0f
            val q = band.q.coerceIn(0.1f, 40f)

            // Pre-EQ stage (parametric)
            val preEqBand = DynamicsProcessing.EqBand(true, freq, gain)
            // Note: DynamicsProcessing EqBand doesn't expose Q directly in the simple constructor.
            // We use the frequency and gain. For more control we can use MBC stages.

            dp.setPreEqBandAllChannelsTo(index, preEqBand)

            // Also configure as MBC band for more dynamics control if desired
            val mbcBand = DynamicsProcessing.MbcBand(
                true,           // enabled
                freq,           // cutoffFrequency
                3.0f,           // attackTime
                80.0f,          // releaseTime
                1.0f,           // ratio
                -50f,           // threshold
                0f,             // kneeWidth
                0f,             // noiseGateThreshold
                0f,             // expanderRatio
                gain            // preGain (we use this for EQ-like boost/cut)
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
                true,                   // linked
                1,                      // link group
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

    /**
     * Attempts to attach to the current music/media audio session.
     * Falls back to session 0 (global) when possible.
     */
    fun attachToMediaSession(): Boolean {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        // Try to get active sessions is complex; for many devices session 0 works for global effects
        return initialize(0)
    }
}
