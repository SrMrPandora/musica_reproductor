package com.example.reproductordeaudio.benchmark

import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

@OptIn(ExperimentalMetricApi::class)
@RunWith(AndroidJUnit4::class)
class PlayerScreenBenchmark {

    val macrobenchmarkRule = MacrobenchmarkRule()

    @get:Rule
    val rule: TestRule = TestRule { base, description ->
        object : Statement() {
            override fun evaluate() {
                try {
                    val field = MacrobenchmarkRule::class.java.getDeclaredField("currentDescription")
                    field.isAccessible = true
                    field.set(macrobenchmarkRule, description)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                base.evaluate()
            }
        }
    }

    @Test
    fun playerScreenTransition() {
        macrobenchmarkRule.measureRepeated(
            packageName = "com.example.reproductordeaudio",
            metrics = listOf(
                TraceSectionMetric("blur_render"),
                TraceSectionMetric("cover_decode"),
                TraceSectionMetric("visualizer_compose")
            ),
            iterations = 5,
            startupMode = StartupMode.WARM
        ) {
            pressHome()
            device.executeShellCommand("am start -n com.example.reproductordeaudio/com.example.reproductordeaudio.MainActivity")

            val songItem = device.wait(Until.findObject(By.res("song_item")), 4000L)
                ?: device.findObject(By.textContains("canción"))

            songItem?.click()
            device.waitForIdle()
        }
    }
}
