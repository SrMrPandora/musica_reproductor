package com.example.reproductordeaudio.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import okio.Buffer
import java.io.ByteArrayOutputStream

class EmbeddedArtworkFetcher(
    private val context: Context,
    private val uri: Uri
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val pictureBytes = retriever.embeddedPicture
            if (pictureBytes != null && pictureBytes.isNotEmpty()) {
                val buffer = Buffer().write(pictureBytes)
                return SourceResult(
                    source = ImageSource(buffer, context),
                    mimeType = getMimeType(pictureBytes),
                    dataSource = DataSource.DISK
                )
            }
        } catch (e: Exception) {
            // Ignore
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val bitmap = context.contentResolver.loadThumbnail(uri, Size(512, 512), null)
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                val bytes = stream.toByteArray()
                if (bytes.isNotEmpty()) {
                    return SourceResult(
                        source = ImageSource(Buffer().write(bytes), context),
                        mimeType = "image/jpeg",
                        dataSource = DataSource.DISK
                    )
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        return null
    }

    private fun getMimeType(bytes: ByteArray): String {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        return options.outMimeType ?: "image/jpeg"
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            return if (data.scheme == "content" && data.path?.contains("/audio/") == true) {
                EmbeddedArtworkFetcher(context, data)
            } else {
                null
            }
        }
    }
}
