package com.example.reproductordeaudio.domain.updater

data class AppUpdateInfo(
    val version: String,
    val downloadUrl: String,
    val releaseNotes: String = ""
)
