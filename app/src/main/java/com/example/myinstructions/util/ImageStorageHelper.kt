package com.example.myinstructions.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

object ImageStorageHelper {

    private const val IMAGES_DIR = "images"

    fun copyImageToInternalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val imagesDir = File(context.filesDir, IMAGES_DIR)
            if (!imagesDir.exists()) imagesDir.mkdirs()
            val fileName = "${UUID.randomUUID()}.jpg"
            val destFile = File(imagesDir, fileName)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            "$IMAGES_DIR/$fileName"
        } catch (e: Exception) {
            null
        }
    }

    fun getAbsolutePath(context: Context, relativePath: String): File {
        return File(context.filesDir, relativePath)
    }

    fun deleteImage(context: Context, relativePath: String) {
        val file = File(context.filesDir, relativePath)
        if (file.exists()) file.delete()
    }

    fun deleteImages(context: Context, relativePaths: List<String>) {
        relativePaths.forEach { deleteImage(context, it) }
    }

    /**
     * Creates a new empty image file in internal storage and returns a pair of:
     * - the content URI (for passing to the camera intent)
     * - the relative path (for storing in the database)
     */
    fun createImageFileUri(context: Context): Pair<Uri, String> {
        val imagesDir = File(context.filesDir, IMAGES_DIR)
        if (!imagesDir.exists()) imagesDir.mkdirs()
        val fileName = "${UUID.randomUUID()}.jpg"
        val file = File(imagesDir, fileName)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return uri to "$IMAGES_DIR/$fileName"
    }
}
