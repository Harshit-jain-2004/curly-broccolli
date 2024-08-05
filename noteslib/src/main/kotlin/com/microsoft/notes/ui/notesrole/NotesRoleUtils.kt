package com.microsoft.notes.ui.notesrole

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings

class NotesRoleUtils {

    companion object {
        fun launchDefaultAppSetting(context: Context) {
            val defaultAppSettingIntent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
            if (context !is Activity) {
                defaultAppSettingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(defaultAppSettingIntent)
        }
    }
}
