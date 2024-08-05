package com.microsoft.notes.sampleapp.utils

import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.microsoft.notes.sampleapp.utils.Constants.Companion.REQUEST_RECORD_AUDIO_PERMISSION
import com.microsoft.notes.sampleapp.utils.Constants.Companion.STORAGE_PERMISSIONS_REQUEST_CODE

fun requestStoragePermissions(activity: AppCompatActivity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && activity.checkSelfPermission(android.Manifest.permission
                    .WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(activity, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSIONS_REQUEST_CODE)
    }
}

fun checkStoragePermissions(activity: AppCompatActivity, grantResults: IntArray) {
    if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        Toast.makeText(activity, "Permission is granted", Toast.LENGTH_SHORT).show()
    }
}

fun requestAudioPermissions(activity: AppCompatActivity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && activity.checkSelfPermission(android.Manifest.permission
                    .RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(activity, arrayOf(android.Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)
    }
}
