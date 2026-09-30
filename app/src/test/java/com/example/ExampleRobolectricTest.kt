package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.FftCalculator
import com.example.viewmodel.MeterViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Decibel Meter", appName)
    }

    @Test
    fun `test musical note conversion`() {
        // Standard A4 concert pitch = 440 Hz
        val noteA4 = FftCalculator.frequencyToMusicalNote(440.0f)
        assertEquals("A4", noteA4)

        // Middle C = ~261.63 Hz
        val noteC4 = FftCalculator.frequencyToMusicalNote(261.63f)
        assertEquals("C4", noteC4)
    }

    @Test
    fun `test noise classification`() {
        val quiet = MeterViewModel.classifyNoiseLevel(25.0f)
        assertEquals("Sangat Tenang", quiet.levelName)

        val normal = MeterViewModel.classifyNoiseLevel(55.0f)
        assertEquals("Sedang / Wajar", normal.levelName)

        val noisy = MeterViewModel.classifyNoiseLevel(75.0f)
        assertEquals("Bising / Ramai", noisy.levelName)

        val dangerous = MeterViewModel.classifyNoiseLevel(105.0f)
        assertEquals("Berbahaya / Ekstrem", dangerous.levelName)
    }

    @Test
    fun `test safe exposure calculation`() {
        assertEquals("8 Jam (OSHA)", MeterViewModel.getSafeExposureHours(85.0f))
        assertEquals("4 Jam", MeterViewModel.getSafeExposureHours(88.0f))
        assertEquals("15 Menit", MeterViewModel.getSafeExposureHours(100.0f))
    }

    @Test
    fun `test fft analysis output structure`() {
        val testBuffer = ShortArray(1024) { i ->
            // Sine wave at 440 Hz
            (10000 * Math.sin(2.0 * Math.PI * 440.0 * i / 44100.0)).toInt().toShort()
        }
        val (bands, dominantInfo) = FftCalculator.analyze(testBuffer, isAWeighted = false)

        assertTrue(bands.isNotEmpty())
        assertTrue(dominantInfo.frequencyHz in 400.0f..480.0f)
    }
}
