package com.hraj9258.oralvisassignment.storage

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ImageStorageManager(private val context: Context) {

    fun getSessionDirectory(sessionId: String): File {
        // Create path: Android/media/<AppName>/Sessions/<SessionID>/
        val mediaDir = File(
            Environment.getExternalStorageDirectory(),
            "Android/media/${context.packageName}/Sessions"
        )
        val sessionDir = File(mediaDir, sessionId)
        if (!sessionDir.exists()) {
            sessionDir.mkdirs()
        }
        return sessionDir
    }

    suspend fun saveImage(sessionId: String, bitmap: Bitmap): String {
        val timestamp = System.currentTimeMillis()
        val filename = "IMG_$timestamp.jpg"
        val sessionDir = getSessionDirectory(sessionId)
        val imageFile = File(sessionDir, filename)

        withContext(Dispatchers.IO) {
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
        }

        return imageFile.absolutePath
    }

    fun getSessionImages(sessionId: String): List<File> {
        val sessionDir = getSessionDirectory(sessionId)
        return sessionDir.listFiles()?.filter {
            it.isFile && it.extension.lowercase() == "jpg"
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun deleteSessionImages(sessionId: String): Boolean {
        val sessionDir = getSessionDirectory(sessionId)
        return if (sessionDir.exists()) {
            sessionDir.deleteRecursively()
        } else false
    }

    fun getImageCount(sessionId: String): Int {
        return getSessionImages(sessionId).size
    }
}