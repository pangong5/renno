package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioRecordEngine
import com.example.audio.MeterData
import com.example.data.AppDatabase
import com.example.data.CalibrationPreferences
import com.example.data.SoundMeasurementRecord
import com.example.data.SoundRecordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScreenTab {
    METER, SPECTRUM, HISTORY, CALIBRATION
}

class MeterViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = CalibrationPreferences(application)
    private val database = AppDatabase.getDatabase(application)
    private val repository = SoundRecordRepository(database.soundRecordDao())
    val audioEngine = AudioRecordEngine(application, viewModelScope)

    // Current navigation tab
    private val _currentTab = MutableStateFlow(ScreenTab.METER)
    val currentTab: StateFlow<ScreenTab> = _currentTab.asStateFlow()

    // Meter data state flow
    val meterData: StateFlow<MeterData> = audioEngine.meterData

    // Calibration & settings state
    private val _calibrationOffset = MutableStateFlow(preferences.calibrationOffset)
    val calibrationOffset: StateFlow<Float> = _calibrationOffset.asStateFlow()

    private val _responseMode = MutableStateFlow(preferences.responseMode)
    val responseMode: StateFlow<String> = _responseMode.asStateFlow()

    private val _weightingType = MutableStateFlow(preferences.weightingType)
    val weightingType: StateFlow<String> = _weightingType.asStateFlow()

    private val _warningThreshold = MutableStateFlow(preferences.warningThresholdDb)
    val warningThreshold: StateFlow<Float> = _warningThreshold.asStateFlow()

    private val _enableVibrate = MutableStateFlow(preferences.enableVibrateWarning)
    val enableVibrate: StateFlow<Boolean> = _enableVibrate.asStateFlow()

    // History filter tag
    private val _selectedFilterTag = MutableStateFlow("Semua")
    val selectedFilterTag: StateFlow<String> = _selectedFilterTag.asStateFlow()

    // Saved records flow
    val allRecords: StateFlow<List<SoundMeasurementRecord>> = repository.allRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recordCount: StateFlow<Int> = repository.recordCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val maxDbEver: StateFlow<Float?> = repository.maxDbEver
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Active session save dialog state
    private val _pendingSessionResult = MutableStateFlow<AudioRecordEngine.SessionResult?>(null)
    val pendingSessionResult: StateFlow<AudioRecordEngine.SessionResult?> = _pendingSessionResult.asStateFlow()

    // Selected record for details preview
    private val _selectedRecordDetail = MutableStateFlow<SoundMeasurementRecord?>(null)
    val selectedRecordDetail: StateFlow<SoundMeasurementRecord?> = _selectedRecordDetail.asStateFlow()

    private var lastVibrateTime = 0L

    init {
        // Sync preferences with audio engine
        audioEngine.calibrationOffset = preferences.calibrationOffset
        audioEngine.responseMode = preferences.responseMode
        audioEngine.weightingType = preferences.weightingType

        // Observe meter data for threshold vibration alerts
        viewModelScope.launch {
            meterData.collect { data ->
                checkThresholdWarning(data.currentDb)
            }
        }
    }

    fun setTab(tab: ScreenTab) {
        _currentTab.value = tab
    }

    fun startListening() {
        audioEngine.startListening()
    }

    fun stopListening() {
        audioEngine.stopListening()
    }

    fun resetStatistics() {
        audioEngine.resetStatistics()
    }

    fun setCalibrationOffset(offset: Float) {
        val clamped = (Math.round(offset * 10.0f) / 10.0f).coerceIn(-30.0f, 30.0f)
        _calibrationOffset.value = clamped
        preferences.calibrationOffset = clamped
        audioEngine.calibrationOffset = clamped
    }

    fun calibrateToTarget(targetDb: Float) {
        // Calculate offset difference from current reading before calibration
        val currentDb = meterData.value.currentDb
        val currentOffset = _calibrationOffset.value
        val rawBase = currentDb - currentOffset
        val newOffset = targetDb - rawBase
        setCalibrationOffset(newOffset)
    }

    fun applyPresetCalibration(presetName: String) {
        when (presetName) {
            "default" -> setCalibrationOffset(0.0f)
            "quiet_room" -> calibrateToTarget(30.0f)
            "normal_conversation" -> calibrateToTarget(60.0f)
            "traffic" -> calibrateToTarget(75.0f)
        }
    }

    fun setResponseMode(mode: String) {
        _responseMode.value = mode
        preferences.responseMode = mode
        audioEngine.responseMode = mode
    }

    fun setWeightingType(weighting: String) {
        _weightingType.value = weighting
        preferences.weightingType = weighting
        audioEngine.weightingType = weighting
    }

    fun setWarningThreshold(threshold: Float) {
        _warningThreshold.value = threshold
        preferences.warningThresholdDb = threshold
    }

    fun setEnableVibrate(enable: Boolean) {
        _enableVibrate.value = enable
        preferences.enableVibrateWarning = enable
    }

    fun setFilterTag(tag: String) {
        _selectedFilterTag.value = tag
    }

    fun startSessionRecording() {
        audioEngine.startSessionRecording()
    }

    fun stopSessionRecording() {
        val result = audioEngine.stopSessionRecording()
        _pendingSessionResult.value = result
    }

    fun dismissSessionDialog() {
        _pendingSessionResult.value = null
    }

    fun saveSessionRecord(title: String, locationTag: String, notes: String) {
        val result = _pendingSessionResult.value ?: return
        val risk = classifyNoiseLevel(result.avgDb).levelName

        viewModelScope.launch {
            repository.insert(
                SoundMeasurementRecord(
                    title = title.ifBlank { "Pengukuran $locationTag" },
                    locationTag = locationTag,
                    timestamp = System.currentTimeMillis(),
                    durationSeconds = result.durationSeconds,
                    minDb = result.minDb,
                    maxDb = result.maxDb,
                    avgDb = result.avgDb,
                    peakDb = result.peakDb,
                    dominantFreqHz = result.dominantFreqHz,
                    weightingType = _weightingType.value,
                    riskLevel = risk,
                    notes = notes,
                    samplesJson = result.samplesJson
                )
            )
            _pendingSessionResult.value = null
        }
    }

    fun showRecordDetail(record: SoundMeasurementRecord?) {
        _selectedRecordDetail.value = record
    }

    fun deleteRecord(record: SoundMeasurementRecord) {
        viewModelScope.launch {
            repository.delete(record)
            if (_selectedRecordDetail.value?.id == record.id) {
                _selectedRecordDetail.value = null
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
            _selectedRecordDetail.value = null
        }
    }

    private fun checkThresholdWarning(currentDb: Float) {
        if (!_enableVibrate.value) return
        val threshold = _warningThreshold.value
        val now = System.currentTimeMillis()
        if (currentDb >= threshold && (now - lastVibrateTime) > 3000L) {
            lastVibrateTime = now
            triggerHapticAlert()
        }
    }

    private fun triggerHapticAlert() {
        try {
            val app = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(150)
            }
        } catch (_: Exception) {}
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.stopListening()
    }

    companion object {
        data class NoiseClassification(
            val levelName: String,
            val description: String,
            val safeDuration: String,
            val colorHex: Long
        )

        fun classifyNoiseLevel(db: Float): NoiseClassification {
            return when {
                db < 30.0f -> NoiseClassification(
                    levelName = "Sangat Tenang",
                    description = "Bisikan, dedaunan bergeser, studio kedap suara",
                    safeDuration = "Aman tanpa batas (>24 jam)",
                    colorHex = 0xFF00E5FF
                )
                db < 50.0f -> NoiseClassification(
                    levelName = "Tenang",
                    description = "Perpustakaan sepi, kamar tidur di malam hari",
                    safeDuration = "Aman tanpa batas (>24 jam)",
                    colorHex = 0xFF10B981
                )
                db < 70.0f -> NoiseClassification(
                    levelName = "Sedang / Wajar",
                    description = "Percakapan biasa, kantor tenang, AC rumah",
                    safeDuration = "Aman (Kenyamanan normal)",
                    colorHex = 0xFF38BDF8
                )
                db < 85.0f -> NoiseClassification(
                    levelName = "Bising / Ramai",
                    description = "Lalu lintas kota, restoran ramai, vacuum cleaner",
                    safeDuration = "Batas aman harian (Mulai mengganggu)",
                    colorHex = 0xFFF59E0B
                )
                db < 100.0f -> NoiseClassification(
                    levelName = "Sangat Bising",
                    description = "Pemotong rumput, blender, pabrik, lalu lintas padat",
                    safeDuration = "Batas aman 15 mnt - 2 jam (Gunakan pelindung telinga)",
                    colorHex = 0xFFF97316
                )
                else -> NoiseClassification(
                    levelName = "Berbahaya / Ekstrem",
                    description = "Sirene dekat, konser musik rock, mesin bor, jet",
                    safeDuration = "< 1 - 5 menit (Risiko kerusakan pendengaran permanen!)",
                    colorHex = 0xFFEF4444
                )
            }
        }

        fun getSafeExposureHours(db: Float): String {
            return when {
                db <= 80f -> "> 24 Jam"
                db <= 83f -> "12 - 16 Jam"
                db <= 85f -> "8 Jam (OSHA)"
                db <= 88f -> "4 Jam"
                db <= 91f -> "2 Jam"
                db <= 94f -> "1 Jam"
                db <= 97f -> "30 Menit"
                db <= 100f -> "15 Menit"
                db <= 105f -> "5 Menit"
                db <= 110f -> "< 1.5 Menit"
                else -> "Bahaya Langsung"
            }
        }
    }
}
