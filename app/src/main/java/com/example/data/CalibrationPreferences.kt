package com.example.data

import android.content.Context
import android.content.SharedPreferences

class CalibrationPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("sound_meter_prefs", Context.MODE_PRIVATE)

    var calibrationOffset: Float
        get() = prefs.getFloat(KEY_CALIBRATION_OFFSET, 0.0f)
        set(value) = prefs.edit().putFloat(KEY_CALIBRATION_OFFSET, value).apply()

    var responseMode: String // "FAST" (125ms) or "SLOW" (1000ms)
        get() = prefs.getString(KEY_RESPONSE_MODE, "FAST") ?: "FAST"
        set(value) = prefs.edit().putString(KEY_RESPONSE_MODE, value).apply()

    var weightingType: String // "dBA" (A-weighting) or "dBZ" (flat)
        get() = prefs.getString(KEY_WEIGHTING_TYPE, "dBA") ?: "dBA"
        set(value) = prefs.edit().putString(KEY_WEIGHTING_TYPE, value).apply()

    var warningThresholdDb: Float
        get() = prefs.getFloat(KEY_WARNING_THRESHOLD, 85.0f)
        set(value) = prefs.edit().putFloat(KEY_WARNING_THRESHOLD, value).apply()

    var enableVibrateWarning: Boolean
        get() = prefs.getBoolean(KEY_ENABLE_VIBRATE, true)
        set(value) = prefs.edit().putBoolean(KEY_ENABLE_VIBRATE, value).apply()

    var keepScreenOn: Boolean
        get() = prefs.getBoolean(KEY_KEEP_SCREEN_ON, true)
        set(value) = prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, value).apply()

    companion object {
        private const val KEY_CALIBRATION_OFFSET = "key_calibration_offset"
        private const val KEY_RESPONSE_MODE = "key_response_mode"
        private const val KEY_WEIGHTING_TYPE = "key_weighting_type"
        private const val KEY_WARNING_THRESHOLD = "key_warning_threshold"
        private const val KEY_ENABLE_VIBRATE = "key_enable_vibrate"
        private const val KEY_KEEP_SCREEN_ON = "key_keep_screen_on"
    }
}
