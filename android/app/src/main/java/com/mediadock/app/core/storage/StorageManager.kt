package com.mediadock.app.core.storage

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.mediadock.app.core.common.FormattingUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Creates an output destination stream for a downloaded media file.
     * Returns a Pair of the target Uri and the open OutputStream.
     */
    fun createMediaOutputStream(
        filename: String,
        mediaType: String, // "video" | "audio" | "image" | "gif" | "other"
        mimeType: String,
        customDirUri: String? = null
    ): Pair<Uri, OutputStream> {
        val sanitized = FormattingUtils.sanitizeFilename(filename)

        // 1. SAF Custom Folder branch if user configured a directory
        if (!customDirUri.isNullOrBlank()) {
            val treeUri = Uri.parse(customDirUri)
            val docDir = DocumentFile.fromTreeUri(context, treeUri)
            if (docDir != null && docDir.canWrite()) {
                val newFile = docDir.createFile(mimeType, sanitized)
                if (newFile != null) {
                    val out = context.contentResolver.openOutputStream(newFile.uri)
                        ?: throw IllegalStateException("Cannot open OutputStream for SAF document")
                    return Pair(newFile.uri, out)
                }
            }
        }

        // 2. MediaStore Scoped Storage branch (Standard public collections)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, sanitized)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.IS_PENDING, 1)

                val relativePath = when (mediaType) {
                    "video" -> Environment.DIRECTORY_MOVIES + "/MediaDock"
                    "audio" -> Environment.DIRECTORY_MUSIC + "/MediaDock"
                    "image", "gif" -> Environment.DIRECTORY_PICTURES + "/MediaDock"
                    else -> Environment.DIRECTORY_DOWNLOADS + "/MediaDock"
                }
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            }

            val collectionUri = when (mediaType) {
                "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                "image", "gif" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                else -> MediaStore.Downloads.EXTERNAL_CONTENT_URI
            }

            val itemUri = context.contentResolver.insert(collectionUri, contentValues)
                ?: throw IllegalStateException("Failed to create MediaStore entry")
            val stream = context.contentResolver.openOutputStream(itemUri)
                ?: throw IllegalStateException("Failed to open MediaStore OutputStream")

            return Pair(itemUri, stream)
        } else {
            // Android 8.0 - 9.0 legacy public storage fallback
            val subDir = when (mediaType) {
                "video" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                "audio" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                "image", "gif" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                else -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            }
            val appDir = File(subDir, "MediaDock").apply { mkdirs() }
            val destFile = getNonCollidingFile(appDir, sanitized)
            val stream = destFile.outputStream()
            val uri = Uri.fromFile(destFile)
            return Pair(uri, stream)
        }
    }

    /**
     * Finalizes MediaStore write by setting IS_PENDING = 0
     */
    fun finishMediaStoreEntry(itemUri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && itemUri.authority == MediaStore.AUTHORITY) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            context.contentResolver.update(itemUri, values, null, null)
        }
    }

    /** Called only for the incomplete output created by the current transfer. */
    fun discardIncomplete(uri: Uri) {
        runCatching { if (uri.scheme == "file") File(requireNotNull(uri.path)).delete() else context.contentResolver.delete(uri, null, null) }
    }

    /** Permanently deletes the media file from disk and MediaStore so it is removed from the gallery. */
    fun deleteMediaFile(uri: Uri): Boolean {
        var deleted = false
        try {
            if (uri.scheme == "file") {
                val file = File(uri.path ?: "")
                if (file.exists()) {
                    deleted = file.delete()
                }
            } else if (uri.scheme == "content") {
                try {
                    val count = context.contentResolver.delete(uri, null, null)
                    if (count > 0) deleted = true
                } catch (e: Exception) {
                    try {
                        val doc = DocumentFile.fromSingleUri(context, uri)
                        if (doc != null && doc.exists()) {
                            deleted = doc.delete()
                        }
                    } catch (_: Exception) {}
                }
                // Also query the underlying physical path if MediaStore column _data is present
                try {
                    val proj = arrayOf(MediaStore.MediaColumns.DATA)
                    context.contentResolver.query(uri, proj, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val dataIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                            if (dataIndex >= 0) {
                                val filePath = cursor.getString(dataIndex)
                                if (!filePath.isNullOrBlank()) {
                                    val physicalFile = File(filePath)
                                    if (physicalFile.exists()) {
                                        physicalFile.delete()
                                        deleted = true
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}

        // Always notify MediaScanner so the system gallery immediately updates
        try {
            val path = uri.path
            if (!path.isNullOrBlank()) {
                android.media.MediaScannerConnection.scanFile(context, arrayOf(path), null, null)
            }
        } catch (_: Exception) {}

        return deleted
    }

    private fun getNonCollidingFile(dir: File, filename: String): File {
        var file = File(dir, filename)
        if (!file.exists()) return file

        val nameWithoutExt = file.nameWithoutExtension
        val ext = file.extension
        var count = 1
        while (file.exists()) {
            val nextName = if (ext.isNotEmpty()) "$nameWithoutExt ($count).$ext" else "$nameWithoutExt ($count)"
            file = File(dir, nextName)
            count++
        }
        return file
    }
}
