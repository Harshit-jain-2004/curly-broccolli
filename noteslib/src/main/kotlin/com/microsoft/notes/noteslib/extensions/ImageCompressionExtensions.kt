package com.microsoft.notes.noteslib.extensions

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.utils.utils.Constants
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.concurrent.thread

private const val MAX_EDGE: Int = 1000
private const val MAX_SIZE: Int = 10 * 1024 * 1024 // 10 Mb

// compression steps:
// 1. decode to sample size, which is closest we can get to desired size by using powers of 2
// 2. do more exact scaling to max 1000x1000
// 3. rotate to the correct orientation if we can
// - do a sanity check on bitmap size
// 4. write to file
fun File.compress(context: Context, deleteOriginal: Boolean): File? {
    val notesFilesDir = File(context.filesDir, Constants.NOTES_FOLDER_NAME)
    if (!notesFilesDir.exists()) {
        notesFilesDir.mkdirs()
    }
    val notesImagesDir = File(notesFilesDir, Constants.NOTES_IMAGES_FOLDER_NAME)
    if (!notesImagesDir.exists()) {
        notesImagesDir.mkdirs()
    }
    val newImageFile = File(notesImagesDir, "media_" + System.currentTimeMillis() + ".png")

    var bitmap = scaleDownAndDecodeFromFile(this) ?: return null
    bitmap = scaleDown(bitmap)
    bitmap = rotate(bitmap, absolutePath)
    if (bitmap.byteCount > MAX_SIZE) {
        // this should not happen for an image which is max 1000x1000 in dimensions
        if (deleteOriginal) {
            deleteAsync()
        }
        return null
    }
    writeToFile(bitmap, newImageFile.absolutePath)
    if (deleteOriginal) {
        deleteAsync()
    }

    return File(newImageFile.absolutePath)
}

private fun File.deleteAsync() {
    thread { delete() }
}

// 1. first decode and scaling
private fun scaleDownAndDecodeFromFile(imageFile: File): Bitmap? {
    val options = BitmapFactory.Options()

    // this gets bounds only, without loading full bitmap
    options.inJustDecodeBounds = true
    BitmapFactory.decodeFile(imageFile.absolutePath, options)

    options.inSampleSize = calculateSampleSize(options.outWidth, options.outHeight)

    // decode full bitmap, and resize with sample size
    options.inJustDecodeBounds = false
    return BitmapFactory.decodeFile(imageFile.absolutePath, options)
}

internal fun calculateSampleSize(width: Int, height: Int): Int {
    if (width <= MAX_EDGE && height <= MAX_EDGE) {
        return 1
    }

    // sample size is used to determine the size of the image
    // returned image will be 1/x the size, where x is power of 2
    // for example, sample size of 4 would return an image which is 1/4 of the size
    var sampleSize = 1
    while ((width / sampleSize) >= MAX_EDGE || (height / sampleSize) >= MAX_EDGE) {
        sampleSize *= 2
    }
    // we always want the smallest possible image, which is NOT smaller than our desired MAX_EDGE size
    // so we take the previous sample size. this prevents the image from being scaled down too much
    sampleSize /= 2
    return sampleSize
}

// 2. exact scaling
private fun scaleDown(image: Bitmap): Bitmap {
    val widthRatio = MAX_EDGE / image.width.toDouble()
    val heightRatio = MAX_EDGE / image.height.toDouble()
    val ratio = Math.min(widthRatio, heightRatio)
    // do not scale if already under max size
    if (ratio >= 1) {
        return image
    }

    val width = Math.round(ratio * image.width).toInt()
    val height = Math.round(ratio * image.height).toInt()

    return Bitmap.createScaledBitmap(image, width, height, true)
}

// 3. rotation
// filename should be of the original file, as that is there the exif information will be available
private fun rotate(imageBitmap: Bitmap, filename: String): Bitmap {
    val rotation = getRotation(filename)
    // do not rotate if rotation already correct
    if (rotation == 0) {
        return imageBitmap
    }
    val matrix = Matrix()
    matrix.postRotate(rotation.toFloat())
    return Bitmap.createBitmap(imageBitmap, 0, 0, imageBitmap.width, imageBitmap.height, matrix, true)
}

private fun getRotation(filename: String): Int {
    val exif: ExifInterface
    try {
        exif = ExifInterface(filename)
    } catch (ex: IOException) {
        NotesLibrary.getInstance().log(message = "IOException while accessing Exif data")
        return 0
    }
    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

    return when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }
}

// 4. write to file
private fun writeToFile(file: Bitmap, destinationPath: String) {
    val fileOutputStream = FileOutputStream(destinationPath)
    file.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
    fileOutputStream.close()
}
