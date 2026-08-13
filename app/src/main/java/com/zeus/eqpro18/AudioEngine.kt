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
    private var enabledFlag = false

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

            dynamicsProcessing = DynamicsProcessing(0, audioSessionId, config).apply {
                enabled = true
            }

            applyAllBands()
            applyPreGain()
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
                    }, Visualizer.getMaxCaptureRate() / 4, false, true)
                    enabled = true
                }
            } catch (e: Exception) {
                Log.w(TAG, "Visualizer no disponible: ${e.message}")
            }

            enabledFlag = true
            Log.i(TAG, "DynamicsProcessing OK (session=$audioSessionId)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar DynamicsProcessing: ${e.message}", e)
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
            spectrumData[i] = spectrumData[i] * 0.85f + normalized * 0.15f
        }
    }

    fun setBands(newBands: List<EqBand>) {
        bands = newBands
        applyAllBands()
        applyPreGain()
    }

    fun updateBand(band: EqBand) {
        val idx = bands.indexOfFirst { it.id == band.id }
        if (idx >= 0) {
            bands = bands.toMutableList().also { it[idx] = band }
            applyBand(band, idx)
            applyPreGain()
        }
    }

    private fun estimatePeakBoost(): Float {
        var maxBoost = 0f
        bands.filter { it.enabled && it.gain > 0f }.forEach { band ->
            maxBoost = max(maxBoost, band.gain)
        }
        return maxBoost
    }

    private fun applyAllBands() {
        val dp = dynamicsProcessing ?: return
        for (i in 0 until MAX_BANDS) {
            if (i >= bands.size) {
                try {
                    val empty = DynamicsProcessing.EqBand(false, 1000f, 0f)
                    dp.setPreEqBandAllChannelsTo(i, empty)
                } catch (_: Exception) {}
            }
        }
        bands.forEachIndexed { index, band ->
            applyBand(band, index)
        }
    }

    private fun applyBand(band: EqBand, index: Int) {
        val dp = dynamicsProcessing ?: return
        if (index >= MAX_BANDS) return

        try {
            val freq = band.frequency.coerceIn(20f, 20000f)
            val isBypass = band.filterType == EqBand.FilterType.BYPASS
            val gain = if (band.enabled && !isBypass) band.gain.coerceIn(-30f, 30f) else 0f

            val preEqBand = DynamicsProcessing.EqBand(band.enabled && !isBypass, freq, gain)
            dp.setPreEqBandAllChannelsTo(index, preEqBand)

            val mbcBand = DynamicsProcessing.MbcBand(
                band.enabled && !isBypass,
                freq,
                5.0f,
                100.0f,
                1.0f,
                -60f,
                0f,
                0f,
                1f,
                gain,
                0f
            )
            dp.setMbcBandAllChannelsTo(index, mbcBand)
        } catch (e: Exception) {
            Log.e(TAG, "Error band $index: ${e.message}")
        }
    }

    fun setPreGain(gainDb: Float) {
        preGain = gainDb.coerceIn(-30f, 12f)
        applyPreGain()
    }

    fun getPreGain(): Float = preGain

    private fun applyPreGain() {
        val dp = dynamicsProcessing ?: return
        try {
            val peakBoost = estimatePeakBoost()
            val safePre = if (peakBoost > 3f) {
                (preGain - (peakBoost - 3f) * 0.5f).coerceIn(-30f, 6f)
            } else {
                preGain
            }
            dp.setInputGainAllChannelsTo(safePre.coerceIn(-30f, MAX_SAFE_GAIN))
        } catch (e: Exception) {
            Log.w(TAG, "setInputGain: ${e.message}")
        }
    }

    fun setLimiter(
        enabled: Boolean,
        threshold: Float = -1.5f,
        attack: Float = 1f,
        release: Float = 60f,
        ratio: Float = 12f,
        postGain: Float = 0f
    ) {
        limiterEnabled = enabled
        limiterThreshold = threshold.coerceIn(-30f, 0f)
        limiterAttack = attack.coerceIn(0.1f, 50f)
        limiterRelease = release.coerceIn(1f, 500f)
        limiterRatio = ratio.coerceIn(1f, 20f)
        limiterPostGain = postGain.coerceIn(-12f, 12f)
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
            Log.e(TAG, "Limiter error: ${e.message}")
        }
    }

    fun setEnabled(enabled: Boolean) {
        enabledFlag = enabled
        dynamicsProcessing?.enabled = enabled
    }

    fun isEnabled(): Boolean = enabledFlag && (dynamicsProcessing?.enabled == true)

    fun getAudioSessionId(): Int = audioSessionId

    fun release() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
            visualizer = null
            dynamicsProcessing?.enabled = false
            dynamicsProcessing?.release()
            dynamicsProcessing = null
        } catch (e: Exception) {
            Log.w(TAG, "Release: ${e.message}")
        }
        enabledFlag = false
    }

    fun attachToMediaSession(): Boolean {
        return initialize(0)
    }
}
