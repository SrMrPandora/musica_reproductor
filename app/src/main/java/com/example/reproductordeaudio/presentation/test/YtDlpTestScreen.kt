package com.example.reproductordeaudio.presentation.test

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import coil.compose.AsyncImage
import com.example.reproductordeaudio.data.settings.SettingsDataStore
import com.example.reproductordeaudio.presentation.home.HomeViewModel
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YtDlpTestScreen(
    viewModel: HomeViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dataStore = remember { SettingsDataStore(context) }

    var urlText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var videoInfo by remember { mutableStateOf<VideoInfo?>(null) }
    var versionInfo by remember { mutableStateOf("") }
    var errorLogs by remember { mutableStateOf("") }

    val mainScrollState = rememberScrollState()
    val logScrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("yt-dlp Test Prototype") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(mainScrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // URL Input
            OutlinedTextField(
                value = urlText,
                onValueChange = { urlText = it },
                label = { Text("URL de Video / Audio") },
                placeholder = { Text("https://www.youtube.com/watch?v=...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Buttons Row
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (urlText.isNotBlank()) {
                                scope.launch {
                                    isLoading = true
                                    statusMessage = "Obteniendo información..."
                                    videoInfo = null
                                    try {
                                        val info = withContext(Dispatchers.IO) {
                                            YoutubeDL.getInstance().getInfo(urlText.trim())
                                        }
                                        videoInfo = info
                                        statusMessage = "Información obtenida correctamente."
                                    } catch (e: Exception) {
                                        val sw = StringWriter()
                                        e.printStackTrace(PrintWriter(sw))
                                        errorLogs += "\n[Error Info] ${e.localizedMessage ?: e.message}\n$sw"
                                        statusMessage = "Error al obtener info: ${e.message}"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            } else {
                                statusMessage = "Por favor ingrese una URL válida."
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Obtener info")
                    }

                    Button(
                        onClick = {
                            if (urlText.isNotBlank()) {
                                scope.launch {
                                    isLoading = true
                                    statusMessage = "Iniciando descarga a carpeta temporal..."
                                    downloadProgress = 0f
                                    var customFolderWarning = ""
                                    try {
                                        val tempDir = File(context.cacheDir, "yt_downloads")
                                        if (!tempDir.exists()) tempDir.mkdirs()

                                        val outputTemplate = File(tempDir, "%(title)s.%(ext)s").absolutePath

                                        val request = YoutubeDLRequest(urlText.trim()).apply {
                                            addOption("-f", "bestaudio/best")
                                            addOption("-x")
                                            addOption("--audio-format", "mp3")
                                            addOption("--add-metadata")
                                            addOption("--embed-thumbnail")
                                            addOption("-o", outputTemplate)
                                        }

                                        val finalPath = withContext(Dispatchers.IO) {
                                            YoutubeDL.getInstance().execute(request, null) { progress, etaInSeconds, line ->
                                                scope.launch(Dispatchers.Main) {
                                                    downloadProgress = progress
                                                    statusMessage = "Descargando: ${progress.toInt()}% (ETA: ${etaInSeconds}s)\n$line"
                                                }
                                            }

                                            val downloadedFile = tempDir.listFiles()?.firstOrNull {
                                                it.isFile && it.length() > 0 && (it.extension == "mp3" || it.extension == "m4a" || it.extension == "ogg" || it.extension == "wav")
                                            } ?: throw Exception("No se encontró el archivo de audio descargado en la carpeta temporal.")

                                            val fileName = downloadedFile.name
                                            val mimeType = getMimeTypeFromExtension(downloadedFile.extension)

                                            val customUriString = dataStore.downloadFolderUri.firstOrNull()

                                            val (actionName, savedPath) = if (!customUriString.isNullOrEmpty()) {
                                                val treeUri = Uri.parse(customUriString)
                                                val hasPermission = context.contentResolver.persistedUriPermissions.any {
                                                    it.uri == treeUri && it.isWritePermission
                                                }
                                                val documentDir = if (hasPermission) DocumentFile.fromTreeUri(context, treeUri) else null

                                                if (documentDir != null && documentDir.exists() && documentDir.canWrite()) {
                                                    val existingDoc = documentDir.findFile(fileName)
                                                    val (action, targetDoc) = if (existingDoc != null && existingDoc.exists()) {
                                                        "Reemplazada" to existingDoc
                                                    } else {
                                                        "Guardada" to (documentDir.createFile(mimeType, fileName)
                                                            ?: throw Exception("No se pudo crear el archivo en la carpeta seleccionada."))
                                                    }

                                                    val outputStream = context.contentResolver.openOutputStream(targetDoc.uri, "wt")
                                                        ?: throw Exception("No se pudo abrir el flujo de salida en la carpeta seleccionada.")
                                                    outputStream.use { output ->
                                                        downloadedFile.inputStream().use { input ->
                                                            input.copyTo(output)
                                                        }
                                                    }

                                                    val documentId = try { DocumentsContract.getTreeDocumentId(treeUri) } catch (_: Exception) { "" }
                                                    val targetFileName = targetDoc.name ?: fileName
                                                    val path = if (treeUri.authority == "com.android.externalstorage.documents" && documentId.startsWith("primary:")) {
                                                        val relativePath = documentId.substringAfter("primary:").trim('/')
                                                        val fullDirPath = if (relativePath.isBlank()) "/storage/emulated/0" else "/storage/emulated/0/$relativePath"
                                                        val physicalFilePath = "$fullDirPath/$targetFileName"

                                                        MediaScannerConnection.scanFile(context, arrayOf(physicalFilePath), arrayOf(mimeType)) { _, _ ->
                                                            viewModel.syncLibrary()
                                                        }
                                                        "Carpeta personalizada ($physicalFilePath)"
                                                    } else {
                                                        "Carpeta personalizada SAF ($targetFileName)"
                                                    }
                                                    action to path
                                                } else {
                                                    customFolderWarning = "\n[Aviso]: Permiso o carpeta personalizada no disponible; se usó carpeta pública Music/ por defecto."
                                                    saveToDefaultMusicFolder(context, downloadedFile, fileName, mimeType)
                                                }
                                            } else {
                                                saveToDefaultMusicFolder(context, downloadedFile, fileName, mimeType)
                                            }

                                            downloadedFile.delete()

                                            var mediaStoreWarning = ""
                                            val verifyQuery = context.contentResolver.query(
                                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                                arrayOf(MediaStore.Audio.Media._ID),
                                                "${MediaStore.Audio.Media.DISPLAY_NAME} = ?",
                                                arrayOf(fileName),
                                                null
                                            )
                                            val isVerified = verifyQuery?.use { it.moveToFirst() } ?: false
                                            if (!isVerified) {
                                                mediaStoreWarning = "\n[Aviso]: El archivo no figura registrado en el MediaStore del sistema aún."
                                            }

                                            "$actionName en: $savedPath$mediaStoreWarning"
                                        }

                                        viewModel.syncLibrary()
                                        statusMessage = "¡Audio procesado correctamente!\n$finalPath$customFolderWarning\nBiblioteca sincronizada."
                                    } catch (e: Exception) {
                                        val sw = StringWriter()
                                        e.printStackTrace(PrintWriter(sw))
                                        errorLogs += "\n[Error Descarga] ${e.localizedMessage ?: e.message}\n$sw"
                                        statusMessage = "Error en descarga: ${e.message}"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            } else {
                                statusMessage = "Por favor ingrese una URL válida."
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Descargar audio")
                    }
                }

                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            try {
                                val currentVersion = YoutubeDL.getInstance().version(context)
                                versionInfo = "Versión actual: $currentVersion"
                                statusMessage = "Actualizando yt-dlp desde canal STABLE..."

                                val updateStatus = withContext(Dispatchers.IO) {
                                    YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel.STABLE)
                                }

                                val newVersion = YoutubeDL.getInstance().version(context)
                                versionInfo = "Versión anterior: $currentVersion -> Nueva versión: $newVersion"
                                statusMessage = "Estado de actualización: $updateStatus"
                            } catch (e: Exception) {
                                val sw = StringWriter()
                                e.printStackTrace(PrintWriter(sw))
                                errorLogs += "\n[Error Actualizar] ${e.localizedMessage ?: e.message}\n$sw"
                                statusMessage = "Error al actualizar: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Actualizar yt-dlp")
                }
            }

            // Progress / Status
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (downloadProgress > 0f) {
                Column {
                    Text("Progreso: ${downloadProgress.toInt()}%", style = MaterialTheme.typography.bodySmall)
                    LinearProgressIndicator(
                        progress = { downloadProgress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (statusMessage.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = statusMessage,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (versionInfo.isNotBlank()) {
                Text(
                    text = versionInfo,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // VideoInfo Display
            videoInfo?.let { info ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = info.title ?: "Sin título",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Canal/Uploader: ${info.uploader ?: "Desconocido"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Duración: ${info.duration} segundos",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        info.thumbnail?.let { thumbUrl ->
                            Text(
                                text = "Thumbnail URL: $thumbUrl",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            AsyncImage(
                                model = thumbUrl,
                                contentDescription = "Thumbnail",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Formatos de Audio:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        val audioFormats = info.formats?.filter { format ->
                            format.vcodec == "none" || (format.acodec != null && format.acodec != "none")
                        } ?: emptyList()

                        if (audioFormats.isEmpty()) {
                            Text(
                                text = "No se encontraron formatos de audio dedicados.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            audioFormats.forEach { format ->
                                Text(
                                    text = "Format ID: ${format.formatId ?: "N/A"} | Bitrate (abr): ${format.abr} kbps | Ext: ${format.ext ?: "N/A"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            // Error log area
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Logs y Errores de depuración",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (errorLogs.isNotEmpty()) {
                        Button(onClick = { errorLogs = "" }) {
                            Text("Limpiar logs")
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color.Black, shape = RoundedCornerShape(8.dp))
                        .padding(8.dp)
                        .verticalScroll(logScrollState)
                ) {
                    Text(
                        text = if (errorLogs.isBlank()) "Sin errores." else errorLogs.trim(),
                        color = if (errorLogs.isBlank()) Color.Gray else Color.Green,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

private fun saveToDefaultMusicFolder(
    context: Context,
    downloadedFile: File,
    fileName: String,
    mimeType: String
): Pair<String, String> {
    val actionName: String
    val savedPath: String

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val resolver = context.contentResolver
        val projection = arrayOf(MediaStore.Audio.Media._ID)
        val selection = "${MediaStore.Audio.Media.DISPLAY_NAME} = ? AND ${MediaStore.Audio.Media.RELATIVE_PATH} = ?"
        val selectionArgs = arrayOf(fileName, "Music/")

        val existingUri = resolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
            } else null
        }

        if (existingUri != null) {
            actionName = "Reemplazada"
            val pendingValues = ContentValues().apply {
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            resolver.update(existingUri, pendingValues, null, null)

            val outputStream = resolver.openOutputStream(existingUri, "wt")
                ?: throw Exception("No se pudo abrir el flujo de salida para sobrescribir en MediaStore.")
            outputStream.use { output ->
                downloadedFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }

            val updateValues = ContentValues().apply {
                put(MediaStore.Audio.Media.IS_PENDING, 0)
                put(MediaStore.Audio.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)
            }
            resolver.update(existingUri, updateValues, null, null)
            savedPath = "Carpeta pública Music/ ($fileName)"
        } else {
            actionName = "Guardada"
            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
                put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }

            val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw Exception("Error al registrar entrada en MediaStore.")

            val outputStream = resolver.openOutputStream(uri)
                ?: throw Exception("Error al abrir el flujo de salida en MediaStore.")
            outputStream.use { output ->
                downloadedFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }

            contentValues.clear()
            contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
            savedPath = "Carpeta pública Music/ ($fileName)"
        }
    } else {
        val publicMusicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        if (!publicMusicDir.exists()) publicMusicDir.mkdirs()
        val targetFile = File(publicMusicDir, fileName)
        actionName = if (targetFile.exists()) "Reemplazada" else "Guardada"
        downloadedFile.copyTo(targetFile, overwrite = true)
        savedPath = targetFile.absolutePath

        MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), arrayOf(mimeType), null)
    }

    return actionName to savedPath
}

private fun getMimeTypeFromExtension(ext: String): String {
    return when (ext.lowercase()) {
        "mp3" -> "audio/mpeg"
        "m4a" -> "audio/mp4"
        "ogg" -> "audio/ogg"
        "wav" -> "audio/x-wav"
        "flac" -> "audio/flac"
        "aac" -> "audio/aac"
        else -> "audio/mpeg"
    }
}
