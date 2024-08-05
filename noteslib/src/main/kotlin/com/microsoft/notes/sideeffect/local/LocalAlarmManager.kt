package com.microsoft.notes.sideeffect.local

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat.getSystemService
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.reminders.AlarmBroadcastReceiver
import com.microsoft.notes.ui.extensions.asPreviewSpannable
import com.microsoft.notes.utils.epochToDate
import com.microsoft.notes.utils.epochToTime
import com.microsoft.notes.utils.utils.Constants
import java.util.Calendar

/**
 * LocalAlarmManager class is responsible for scheduling alarms. Each alarm requires a unique integer request code which needs to be persisted
 * across sessions (for cancelling or rescheduling of a particular alarm). For this we are storing below values in shared preference
 * (1) Next available request code counter:
 *      This counter(with shared pref key 'NEXT_AVAILABLE_REQUEST_CODE_COUNTER') is incremented with each new alarm, ensuring no two alarms share the same request code. When the counter reaches
 *      its maximum value (Integer.MAX_VALUE), it wraps around to avoid overflow.
 * (2) AlarmLocalId to Request Code Mapping
 *      Since alarms in the app are identified by a local ID ('alarmLocalId'), but the AlarmManager requires a integer request code to identify an alarm,
 *      we map each 'alarmLocalId' to its generated request code and save this mapping in this shared preference.This mapping allows for retrieval of the
 *      request code associated with a specific alarm ID, which is required for updating or cancelling scheduled alarms.
 */

class LocalAlarmManager {
    companion object {
        private const val REMINDER_SHARED_PREFERENCES = "com.microsoft.notes.reminder_shared_preferences"
        private const val NEXT_AVAILABLE_REQUEST_CODE_COUNTER: String = "request_code_counter"
    }

    fun scheduleAlarm(
        context: Context,
        alarmDateTime: Long,
        alarmLocalId: String,
        noteLocalId: String
    ) {

        getAlarmPermissionIfRequired(context)
        val alarmManager = context.getSystemService(ALARM_SERVICE) as AlarmManager
        val calendar: Calendar = Calendar.getInstance()
        calendar.timeInMillis = alarmDateTime

        val requestCodeForAlarm = getRequestCodeForAlarm(context, alarmLocalId)

        val note = NotesLibrary.getInstance().notesList.getNote(noteLocalId)
        val intent = Intent(context, AlarmBroadcastReceiver::class.java)

        intent.putExtra(Constants.NOTE_LOCAL_ID, noteLocalId)
        val documentPreview = context.let { note?.document?.asPreviewSpannable(it) }
        intent.putExtra(Constants.NOTE_PREVIEW, documentPreview)
        intent.putExtra(Constants.ALARM_LOCAL_ID, alarmLocalId)
        intent.putExtra(
            Constants.ALARM_DATE_TIME,
            "${epochToDate(alarmDateTime)} ${epochToTime(alarmDateTime)}"
        )

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCodeForAlarm,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
    }

    private fun getRequestCodeForAlarm(context: Context, alarmLocalId: String): Int {
        val sharedPref = context.getSharedPreferences(
            REMINDER_SHARED_PREFERENCES,
            Context.MODE_PRIVATE
        )
        val savedRequestCodeForAlarmId = sharedPref.getInt(alarmLocalId, -1)

        val requestCode: Int
        if (savedRequestCodeForAlarmId != -1) {
            // already request code exists for given alarmId, use same request code
            requestCode = savedRequestCodeForAlarmId
        } else {
            // new alarmId; get next available counter and store as (alarmId,requestCode) mapping in shared pref
            var availableRequestCode = sharedPref.getInt(NEXT_AVAILABLE_REQUEST_CODE_COUNTER, 0)

            sharedPref.edit()
                .putInt(alarmLocalId, availableRequestCode)
                .apply()
            requestCode = availableRequestCode

            if (availableRequestCode == Integer.MAX_VALUE) {
                availableRequestCode = -1
            }

            sharedPref.edit()
                .putInt(NEXT_AVAILABLE_REQUEST_CODE_COUNTER, availableRequestCode + 1)
                .apply()
        }
        return requestCode
    }

    private fun getAlarmPermissionIfRequired(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(context, AlarmManager::class.java)
            if (alarmManager?.canScheduleExactAlarms() == false) {
                Intent().also { intent ->
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                    context.startActivity(intent)
                }
            }
        }
    }
}
