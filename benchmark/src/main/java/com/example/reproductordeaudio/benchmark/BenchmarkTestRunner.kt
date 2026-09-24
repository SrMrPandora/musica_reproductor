package com.example.reproductordeaudio.benchmark

import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner

class BenchmarkTestRunner : AndroidJUnitRunner() {
    override fun onCreate(arguments: Bundle) {
        arguments.putString("androidx.benchmark.suppressWithBenchmarkRunner", "true")
        super.onCreate(arguments)
    }
}
