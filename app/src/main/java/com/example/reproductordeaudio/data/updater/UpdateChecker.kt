package com.example.reproductordeaudio.data.updater

import com.example.reproductordeaudio.domain.updater.AppUpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {
    private const val GITHUB_API_URL = "https://api.github.com/repos/SrMrPandora/musica_reproductor/releases/latest"

    suspend fun checkForUpdate(currentVersionName: String): AppUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

            if (connection.responseCode != 200) return@withContext null

            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseText)

            val tagName = json.optString("tag_name", "").removePrefix("v").removePrefix("V").trim()
            val releaseNotes = json.optString("body", "")

            if (tagName.isBlank()) return@withContext null

            if (isVersionGreater(tagName, currentVersionName)) {
                val assets = json.optJSONArray("assets") ?: return@withContext null
                var downloadUrl: String? = null
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        val urlStr = asset.optString("browser_download_url", "")
                        if (urlStr.isNotBlank()) {
                            downloadUrl = urlStr
                            break
                        }
                    }
                }
                if (downloadUrl != null) {
                    return@withContext AppUpdateInfo(
                        version = tagName,
                        downloadUrl = downloadUrl,
                        releaseNotes = releaseNotes
                    )
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    fun isVersionGreater(remote: String, local: String): Boolean {
        val remoteParts = remote.split(".").mapNotNull { it.takeWhile { char -> char.isDigit() }.toIntOrNull() }
        val localParts = local.split(".").mapNotNull { it.takeWhile { char -> char.isDigit() }.toIntOrNull() }

        val maxLength = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxLength) {
            val remoteSeg = remoteParts.getOrElse(i) { 0 }
            val localSeg = localParts.getOrElse(i) { 0 }
            if (remoteSeg > localSeg) return true
            if (remoteSeg < localSeg) return false
        }
        return false
    }
}
