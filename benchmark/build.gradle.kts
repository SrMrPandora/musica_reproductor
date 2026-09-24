plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.reproductordeaudio.benchmark"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
        targetSdk = 35
        testInstrumentationRunner = "com.example.reproductordeaudio.benchmark.BenchmarkTestRunner"
        testInstrumentationRunnerArguments["androidx.benchmark.suppressWithBenchmarkRunner"] = "true"
        testInstrumentationRunnerArguments["grant-permissions"] = "false"
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "LOW-BATTERY,DEBUGGABLE,NOT-PROFILEABLE,EMULATOR"
    }

    buildTypes {
        create("benchmark") {
            isDebuggable = false
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.junit)
    implementation(libs.androidx.espresso.core)
}
