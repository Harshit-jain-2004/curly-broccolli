package com.microsoft.notes.sampleapp.images

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.microsoft.notes.sampleapp.BuildConfig
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val REQUEST_IMAGE_CAPTURE = 1

fun AppCompatActivity.dispatchTakePhotoIntent(): Uri? {
    Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
        // Ensure that there's a camera activity to handle the intent
        takePictureIntent.resolveActivity(packageManager)?.also {
            // Create the File where the photo should go
            val photoFile: File? = try {
                createImageFile(this)
            } catch (ex: IOException) {
                // Error occurred while creating the File
                Log.d("dispatchTakePhotoIntent", ex.toString())
                null
            }
            // Continue only if the File was successfully created
            photoFile?.also { file ->
                val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        BuildConfig.APPLICATION_ID,
                        file
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                return Uri.fromFile(photoFile)
            }
        }
    }

    return null
}

@Throws(IOException::class)
private fun createImageFile(context: Context): File {
    // Create an image file name
    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
    )
}