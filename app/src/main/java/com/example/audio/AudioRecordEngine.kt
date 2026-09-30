package com.example.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

data class MeterData(
    val currentDb: Float = 30.0f,
    val peakDb: Float = 30.0f,
    val minDb: Float = 30.0f,
    val maxDb: Float = 30.0f,
    val avgDb: Float = 30.0f,
    val dominantFreqHz: Float = 0.0f,
    val dominantNote: String = "-",
    val dominantBand: String = "-",
    val bands: List<FrequencyBand> = emptyList(),
    val waveformPoints: List<Float> = emptyList(),
    val isRecordingAudio: Boolean = false,
    val isSessionActive: Boolean = false,
    val sessionDurationSec: Long = 0L
)

class AudioRecordEngine(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val TAG = "AudioRecordEngine"
    private val SAMPLE_RATE = 44100
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private val READ_CHUNK_SIZE = 1024

    private var audioRecord: AudioRecord? = null
    private var recordJob: Job? = null

    private val _meterData = MutableStateFlow(MeterData())
    val meterData: StateFlow<MeterData> = _meterData.asStateFlow()

    // Configurable parameters
    var calibrationOffset: Float = 0.0f
    var responseMode: String = "FAST" // "FAST" (125ms) or "SLOW" (1000ms)
    var weightingType: String = "dBA" // "dBA" or "dBZ"
    var baselineReference: Float = 94.0f // 0 dBFS reference for mobile mic

    // Running statistics
    private var minDbRecorded = 120.0f
    private var maxDbRecorded = 0.0f
    private var peakHoldDb = 0.0f
    private var peakHoldTimestamp = 0L

    // Leq Energy accumulator
    private var energySum = 0.0
    private var energyCount = 0L

    // Moving average state
    private var smoothedDb = 35.0f

    // Waveform history buffer (last 80 points)
    private val waveformBuffer = ArrayDeque<Float>(80)

    // Session recording state
    private var sessionStartTime = 0L
    private val sessionDbSamples = mutableListOf<Float>()
    var isSessionRecording: Boolean = false
        private set

    init {
        // Initialize waveform buffer with 80 quiet points
        repeat(80) { waveformBuffer.add(30f) }
    }

    @SuppressLint("MissingPermission")
    fun startListening() {
        if (recordJob?.isActive == true) return

        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = max(minBufferSize, READ_CHUNK_SIZE * 4)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed!")
                return
            }

            audioRecord?.startRecording()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting AudioRecord: ${e.message}")
            return
        }

        recordJob = scope.launch(Dispatchers.IO) {
            val audioBuffer = ShortArray(READ_CHUNK_SIZE)
            var lastUiUpdate = 0L
            val uiUpdateIntervalMs = 40L // ~25-30 FPS smooth UI updates

            while (isActive) {
                val record = audioRecord ?: break
                if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) break

                val readCount = record.read(audioBuffer, 0, READ_CHUNK_SIZE)
                if (readCount <= 0) continue

                // 1. Calculate RMS
                var sumSquares = 0.0
                for (i in 0 until readCount) {
                    val sample = audioBuffer[i].toDouble()
                    sumSquares += sample * sample
                }
                val rms = sqrt(sumSquares / readCount)

                // 2. Convert to dBFS and apply calibration
                val rawDbfs = if (rms > 0.0001) {
                    20.0 * log10(rms / 32767.0)
                } else {
                    -90.0
                }

                // Estimated dB SPL before weighting
                var instantDb = (rawDbfs.toFloat() + baselineReference + calibrationOffset).coerceIn(10.0f, 130.0f)

                // 3. FFT Analysis
                val isAWeight = weightingType.equals("dBA", ignoreCase = true)
                val (bands, dominantInfo) = FftCalculator.analyze(audioBuffer, isAWeight)

                // If dBA, adjust instantDb slightly by dominant frequency A-weighting
                if (isAWeight && dominantInfo.frequencyHz > 20f) {
                    val aFactor = FftCalculator.aWeightingFactor(dominantInfo.frequencyHz)
                    val aDbDelta = (20.0 * log10(aFactor.toDouble())).toFloat()
                    instantDb = (instantDb + aDbDelta * 0.4f).coerceIn(10.0f, 130.0f)
                }

                // 4. Time response smoothing (FAST 125ms vs SLOW 1000ms)
                // Chunk duration: 1024 / 44100 = ~23ms
                val dtSec = READ_CHUNK_SIZE.toFloat() / SAMPLE_RATE
                val tau = if (responseMode.equals("SLOW", ignoreCase = true)) 1.0f else 0.125f
                val alpha = (dtSec / (tau + dtSec)).coerceIn(0.05f, 0.95f)

                smoothedDb = alpha * instantDb + (1.0f - alpha) * smoothedDb

                // 5. Update Min, Max, Peak, Avg
                val now = SystemClock.elapsedRealtime()
                if (smoothedDb < minDbRecorded) minDbRecorded = smoothedDb
                if (smoothedDb > maxDbRecorded) maxDbRecorded = smoothedDb

                // Peak hold with 2 second decay
                if (instantDb >= peakHoldDb || (now - peakHoldTimestamp) > 2000L) {
                    peakHoldDb = instantDb
                    peakHoldTimestamp = now
                }

                // Leq equivalent continuous sound level
                energySum += 10.0.pow(smoothedDb.toDouble() / 10.0)
                energyCount++
                val avgDb = (10.0 * log10(energySum / energyCount.coerceAtLeast(1L))).toFloat()

                // Session recording accumulation
                var sessionDuration = 0L
                if (isSessionRecording) {
                    sessionDuration = (System.currentTimeMillis() - sessionStartTime) / 1000L
                    synchronized(sessionDbSamples) {
                        sessionDbSamples.add(smoothedDb)
                    }
                }

                // 6. Push to UI at controlled rate
                if (now - lastUiUpdate >= uiUpdateIntervalMs) {
                    lastUiUpdate = now

                    // Waveform roll
                    if (waveformBuffer.size >= 80) waveformBuffer.removeFirst()
                    waveformBuffer.add(smoothedDb)
                    val wavePoints = waveformBuffer.toList()

                    _meterData.value = MeterData(
                        currentDb = smoothedDb,
                        peakDb = peakHoldDb,
                        minDb = minDbRecorded,
                        maxDb = maxDbRecorded,
                        avgDb = avgDb,
                        dominantFreqHz = dominantInfo.frequencyHz,
                        dominantNote = dominantInfo.noteName,
                        dominantBand = dominantInfo.bandCategory,
                        bands = bands,
                        waveformPoints = wavePoints,
                        isRecordingAudio = true,
                        isSessionActive = isSessionRecording,
                        sessionDurationSec = sessionDuration
                    )
                }
            }
        }
    }

    fun stopListening() {
        recordJob?.cancel()
        recordJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord: ${e.message}")
        }
        audioRecord = null
        _meterData.value = _meterData.value.copy(isRecordingAudio = false)
    }

    fun resetStatistics() {
        minDbRecorded = smoothedDb
        maxDbRecorded = smoothedDb
        peakHoldDb = smoothedDb
        energySum = 10.0.pow(smoothedDb.toDouble() / 10.0)
        energyCount = 1L
        _meterData.value = _meterData.value.copy(
            minDb = minDbRecorded,
            maxDb = maxDbRecorded,
            peakDb = peakHoldDb,
            avgDb = smoothedDb
        )
    }

    fun startSessionRecording() {
        sessionStartTime = System.currentTimeMillis()
        synchronized(sessionDbSamples) {
            sessionDbSamples.clear()
        }
        isSessionRecording = true
        _meterData.value = _meterData.value.copy(isSessionActive = true, sessionDurationSec = 0L)
    }

    data class SessionResult(
        val durationSeconds: Long,
        val minDb: Float,
        val maxDb: Float,
        val avgDb: Float,
        val peakDb: Float,
        val dominantFreqHz: Int,
        val samplesJson: String
    )

    fun stopSessionRecording(): SessionResult {
        isSessionRecording = false
        val duration = max(1L, (System.currentTimeMillis() - sessionStartTime) / 1000L)

        val samplesSnapshot: List<Float>
        synchronized(sessionDbSamples) {
            samplesSnapshot = sessionDbSamples.toList()
        }

        val minVal = samplesSnapshot.minOrNull() ?: smoothedDb
        val maxVal = samplesSnapshot.maxOrNull() ?: smoothedDb
        val avgVal = if (samplesSnapshot.isNotEmpty()) samplesSnapshot.average().toFloat() else smoothedDb
        val peakVal = maxVal

        // Subsample down to max 60 points for compact JSON storage
        val downsampled = if (samplesSnapshot.size > 60) {
            val step = samplesSnapshot.size / 60.0
            (0 until 60).map { i ->
                val idx = (i * step).toInt().coerceIn(0, samplesSnapshot.size - 1)
                samplesSnapshot[idx]
            }
        } else {
            samplesSnapshot
        }
        val samplesStr = downsampled.joinToString(separator = ",") { "%.1f".format(it) }

        _meterData.value = _meterData.value.copy(isSessionActive = false, sessionDurationSec = 0L)

        return SessionResult(
            durationSeconds = duration,
            minDb = minVal,
            maxDb = maxVal,
            avgDb = avgVal,
            peakDb = peakVal,
            dominantFreqHz = _meterData.value.dominantFreqHz.toInt(),
            samplesJson = samplesStr
        )
    }
}
