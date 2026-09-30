package com.example.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class FrequencyBand(
    val label: String,
    val centerFreqHz: Float,
    val magnitudeNorm: Float // 0.0 to 1.0 for visualizer bar
)

data class DominantFrequencyInfo(
    val frequencyHz: Float,
    val noteName: String,
    val bandCategory: String // e.g., "Bass", "Mid", "Treble"
)

object FftCalculator {
    private const val SAMPLE_RATE = 44100
    private const val FFT_SIZE = 1024

    // Precomputed Hann window
    private val hannWindow = FloatArray(FFT_SIZE) { i ->
        (0.5 * (1.0 - cos(2.0 * PI * i / (FFT_SIZE - 1)))).toFloat()
    }

    // 32 Logarithmic frequency band boundaries from ~20 Hz to ~20,000 Hz
    private val bandEdges = floatArrayOf(
        20f, 25f, 31.5f, 40f, 50f, 63f, 80f, 100f, 125f, 160f,
        200f, 250f, 315f, 400f, 500f, 630f, 800f, 1000f, 1250f, 1600f,
        2000f, 2500f, 3150f, 4000f, 5000f, 6300f, 8000f, 10000f, 12500f, 16000f, 20000f
    )

    private val bandLabels = listOf(
        "25", "31", "40", "50", "63", "80", "100", "125", "160",
        "200", "250", "315", "400", "500", "630", "800", "1k", "1.2k", "1.6k",
        "2k", "2.5k", "3.1k", "4k", "5k", "6.3k", "8k", "10k", "12.5k", "16k", "20k"
    )

    /**
     * Compute FFT and return normalized magnitude bands (0.0 to 1.0) and dominant frequency.
     */
    fun analyze(audioBuffer: ShortArray, isAWeighted: Boolean): Pair<List<FrequencyBand>, DominantFrequencyInfo> {
        val n = FFT_SIZE
        val real = FloatArray(n)
        val imag = FloatArray(n)

        val copyLen = minOf(audioBuffer.size, n)
        for (i in 0 until copyLen) {
            // Apply Hann window and normalize 16-bit PCM to [-1.0, 1.0]
            real[i] = (audioBuffer[i] / 32768.0f) * hannWindow[i]
            imag[i] = 0.0f
        }
        for (i in copyLen until n) {
            real[i] = 0.0f
            imag[i] = 0.0f
        }

        // In-place Cooley-Tukey radix-2 FFT
        fft(real, imag)

        // Calculate power spectrum for positive frequencies (bins 0 to n/2)
        val halfN = n / 2
        val magnitudes = FloatArray(halfN)
        val binWidth = SAMPLE_RATE.toFloat() / n

        var maxMag = 0.0001f
        var peakBin = 1

        for (k in 1 until halfN) {
            val freq = k * binWidth
            var mag = sqrt(real[k] * real[k] + imag[k] * imag[k])

            if (isAWeighted) {
                val weightGain = aWeightingFactor(freq)
                mag *= weightGain
            }

            magnitudes[k] = mag
            // Look for peak in audible range (50 Hz to 12 kHz)
            if (freq in 50.0f..12000.0f && mag > maxMag) {
                maxMag = mag
                peakBin = k
            }
        }

        // Parabolic interpolation around peak for precise sub-bin frequency
        val dominantFreqHz = if (peakBin in 2 until halfN - 1) {
            val alpha = magnitudes[peakBin - 1]
            val beta = magnitudes[peakBin]
            val gamma = magnitudes[peakBin + 1]
            val delta = 0.5f * (alpha - gamma) / (alpha - 2f * beta + gamma + 1e-9f)
            (peakBin + delta) * binWidth
        } else {
            peakBin * binWidth
        }

        // Group into logarithmic bands
        val bands = mutableListOf<FrequencyBand>()
        for (i in 0 until bandLabels.size.coerceAtMost(bandEdges.size - 1)) {
            val lowF = bandEdges[i]
            val highF = bandEdges[i + 1]
            val centerF = sqrt(lowF * highF)

            val startBin = (lowF / binWidth).toInt().coerceIn(1, halfN - 1)
            val endBin = (highF / binWidth).toInt().coerceIn(startBin, halfN - 1)

            var bandSum = 0.0f
            var binCount = 0
            for (b in startBin..endBin) {
                bandSum += magnitudes[b]
                binCount++
            }
            val avgMag = if (binCount > 0) bandSum / binCount else 0.0f

            // Convert to dB scale relative to reference: -80 dB to 0 dB mapped to [0.0, 1.0]
            val dbVal = if (avgMag > 1e-6f) {
                20.0f * log10(avgMag)
            } else {
                -80.0f
            }
            val normalized = ((dbVal + 70.0f) / 60.0f).coerceIn(0.02f, 1.0f)

            bands.add(
                FrequencyBand(
                    label = bandLabels[i],
                    centerFreqHz = centerF,
                    magnitudeNorm = normalized
                )
            )
        }

        val dominantInfo = DominantFrequencyInfo(
            frequencyHz = dominantFreqHz.coerceIn(20f, 20000f),
            noteName = frequencyToMusicalNote(dominantFreqHz),
            bandCategory = classifyFrequencyBand(dominantFreqHz)
        )

        return Pair(bands, dominantInfo)
    }

    /**
     * Standard A-weighting relative curve factor (IEC 61672:2003)
     */
    fun aWeightingFactor(freqHz: Float): Float {
        if (freqHz <= 10f) return 0.01f
        val f2 = freqHz * freqHz
        val num = 12194f * 12194f * f2 * f2
        val den = (f2 + 20.6f * 20.6f) *
                sqrt((f2 + 107.7f * 107.7f) * (f2 + 737.9f * 737.9f)) *
                (f2 + 12194f * 12194f)
        val ra = (num / den).toDouble()
        val dbA = 20.0 * log10(ra) + 2.0
        // Convert dB gain to linear amplitude factor
        return Math.pow(10.0, dbA / 20.0).toFloat().coerceIn(0.01f, 2.5f)
    }

    /**
     * Map frequency in Hz to closest Musical Note (e.g. 440 Hz -> "A4", 261.63 Hz -> "C4")
     */
    fun frequencyToMusicalNote(frequencyHz: Float): String {
        if (frequencyHz < 20f || frequencyHz > 15000f) return "-"
        val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        val midiNumber = 69 + 12.0 * (ln(frequencyHz.toDouble() / 440.0) / ln(2.0))
        val roundedMidi = midiNumber.roundToInt()
        val noteIndex = ((roundedMidi % 12) + 12) % 12
        val octave = (roundedMidi / 12) - 1
        return "${noteNames[noteIndex]}$octave"
    }

    private fun classifyFrequencyBand(freqHz: Float): String {
        return when {
            freqHz < 60f -> "Sub-Bass (<60 Hz)"
            freqHz < 250f -> "Bass (60 - 250 Hz)"
            freqHz < 500f -> "Low Mid (250 - 500 Hz)"
            freqHz < 2000f -> "Mid (500 - 2k Hz)"
            freqHz < 4000f -> "High Mid (2k - 4k Hz)"
            freqHz < 6000f -> "Presence (4k - 6k Hz)"
            else -> "Brilliance (>6k Hz)"
        }
    }

    /**
     * Bit-reversal and in-place Cooley-Tukey radix-2 FFT
     */
    private fun fft(real: FloatArray, imag: FloatArray) {
        val n = real.size
        var j = 0
        for (i in 0 until n - 1) {
            if (i < j) {
                val tempR = real[i]
                real[i] = real[j]
                real[j] = tempR

                val tempI = imag[i]
                imag[i] = imag[j]
                imag[j] = tempI
            }
            var k = n shr 1
            while (k <= j) {
                j -= k
                k = k shr 1
            }
            j += k
        }

        var len = 2
        while (len <= n) {
            val halfLen = len shr 1
            val angle = (-2.0 * PI / len).toFloat()
            val wStepR = cos(angle.toDouble()).toFloat()
            val wStepI = sin(angle.toDouble()).toFloat()

            var i = 0
            while (i < n) {
                var wR = 1.0f
                var wI = 0.0f
                for (k in 0 until halfLen) {
                    val uR = real[i + k]
                    val uI = imag[i + k]
                    val vR = real[i + k + halfLen] * wR - imag[i + k + halfLen] * wI
                    val vI = real[i + k + halfLen] * wI + imag[i + k + halfLen] * wR

                    real[i + k] = uR + vR
                    imag[i + k] = uI + vI
                    real[i + k + halfLen] = uR - vR
                    imag[i + k + halfLen] = uI - vI

                    val nextWR = wR * wStepR - wI * wStepI
                    val nextWI = wR * wStepI + wI * wStepR
                    wR = nextWR
                    wI = nextWI
                }
                i += len
            }
            len = len shl 1
        }
    }
}
