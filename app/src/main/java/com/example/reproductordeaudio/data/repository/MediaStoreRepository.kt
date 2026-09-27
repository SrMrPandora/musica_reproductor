package com.example.reproductordeaudio.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.reproductordeaudio.data.local.db.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreRepository(private val context: Context) {

    suspend fun fetchAudioFiles(): List<SongEntity> = withContext(Dispatchers.IO) {
        val songList = mutableListOf<SongEntity>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.DATA
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Audio.Media.RELATIVE_PATH)
            }
        }.toTypedArray()

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 10000"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val relativePathColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
            } else -1

            while (cursor.moveToNext()) {
                val filePath = cursor.getString(dataColumn) ?: ""
                val relativePath = if (relativePathColumn != -1) cursor.getString(relativePathColumn) ?: "" else ""
                val title = cursor.getString(titleColumn) ?: "Unknown Title"

                val isWhatsAppVoiceNote = filePath.contains("WhatsApp Voice Notes", ignoreCase = true) ||
                        relativePath.contains("WhatsApp Voice Notes", ignoreCase = true) ||
                        filePath.contains("WhatsApp Audio", ignoreCase = true)

                val isOpusFormat = filePath.endsWith(".opus", ignoreCase = true) ||
                        title.endsWith(".opus", ignoreCase = true)

                if (isWhatsAppVoiceNote || isOpusFormat) {
                    continue
                }

                val id = cursor.getLong(idColumn)
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val album = cursor.getString(albumColumn)
                val albumId = cursor.getLong(albumIdColumn)
                val duration = cursor.getLong(durationColumn)
                val dateModified = cursor.getLong(dateModifiedColumn)

                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                val artworkUriStr = contentUri.toString()

                songList.add(
                    SongEntity(
                        mediaStoreId = id,
                        title = title,
                        artist = artist,
                        album = album,
                        uri = contentUri.toString(),
                        artworkUri = artworkUriStr,
                        duration = duration,
                        dateModified = dateModified
                    )
                )
            }
        }

        songList
    }

    suspend fun deleteAudioFilePhysical(songUri: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(songUri)
            val rows = context.contentResolver.delete(uri, null, null)
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
