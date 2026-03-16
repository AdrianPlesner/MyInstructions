package com.example.myinstructions.util

import android.content.Context
import android.net.Uri
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
}
