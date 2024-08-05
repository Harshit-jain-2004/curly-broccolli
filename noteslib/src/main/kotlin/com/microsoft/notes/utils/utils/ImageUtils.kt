package com.microsoft.notes.utils.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

internal class ImageUtils {
    companion object {
        private fun createImageFile(id: String?, context: Context): File {
            val notesFilesDir = File(context.filesDir, Constants.NOTES_FOLDER_NAME)
            if (!notesFilesDir.exists()) {
                notesFilesDir.mkdirs()
            }
            val notesImagesDir = File(notesFilesDir, Constants.NOTEREFERENCE_IMAGES_FOLDER_NAME)
            if (!notesImagesDir.exists()) {
                notesImagesDir.mkdirs()
            }
            return File(notesImagesDir, "media_$id.png")
        }

        fun saveBase64UrlToFileAsLocalUrl(imageBase64String: String, id: String, context: Context): String? {
            val base64Url = imageBase64String.replace(Regex("^data:image/[a-z]+;base64,"), "")
            val byteArray = Base64.decode(base64Url, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            val file = createImageFile(id, context)
            try {
                val outputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                outputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
                return null
            }
            return file.toURI().toString()
        }

        fun deleteCachedImage(mediaId: String, context: Context) {
            val notesImagesFileDir = getImageFolder(context)
            if (notesImagesFileDir.exists()) {
                val fi = File(notesImagesFileDir, "media_$mediaId.png")
                if (fi.exists()) {
                    fi.delete()
                }
            }
        }

        private fun getImageFolder(context: Context): File = File(context.filesDir, Constants.NOTES_FOLDER_NAME + "/" + Constants.NOTEREFERENCE_IMAGES_FOLDER_NAME)

        fun extractPageId(url: String?): String? {
            val regex = Regex("page-id=\\{([^}]*)\\}")
            val matchResult = url?.let { regex.find(it) }
            val pageId = matchResult?.groupValues?.getOrNull(1)
            return "{$pageId}"
        }

        fun getBitmapFromBase64(base64Image: String): Bitmap? {
            val decodedString = Base64.decode(base64Image, Base64.DEFAULT)
            return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
        }
    }
}
