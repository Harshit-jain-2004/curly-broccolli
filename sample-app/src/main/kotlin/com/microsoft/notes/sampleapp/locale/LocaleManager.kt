package com.microsoft.notes.sampleapp.com.microsoft.notes.sampleapp.locale

import android.app.Activity
import android.content.SharedPreferences
import androidx.appcompat.app.AlertDialog
import java.util.Locale

class LocaleManager {
    companion object {
        private const val LOCALE_PREFERENCE_KEY = "LOCALE_PREFERENCE_KEY"

        @JvmStatic
        val testLocales = arrayOf(
                Locale("en", "English"),
                Locale("ar", "Arabic"),
                Locale("sr", "Serbian"),
                Locale("zh", "Chinese")).distinctBy { it.language }

        @JvmStatic
        fun setLocaleFromPreferences(activity: Activity, preferenceManager: SharedPreferences) {
            val localePreference = preferenceManager.getString(LOCALE_PREFERENCE_KEY,
                    Locale.getDefault().language) ?: Locale.getDefault().language
            setLocale(activity, localePreference)
        }

        @JvmStatic
        fun setLocale(activity: Activity, localeStr: String) {
            val locale = Locale(localeStr)

            // Used for string resolution
            val resources = activity.resources
            val displayMetrics = resources.displayMetrics
            val configuration = resources.configuration
            configuration.setLocale(locale)
            @Suppress("DEPRECATION")
            resources.updateConfiguration(configuration, displayMetrics)

            // Used for formatting e.g., dates
            Locale.setDefault(locale)
        }

        @JvmStatic
        fun showLocalePicker(activity: Activity, preferenceManager: SharedPreferences) {
            val builder = AlertDialog.Builder(activity)
            builder.setItems(testLocales.map { it.country }.toTypedArray()) { _, which ->
                val preferenceEditor = preferenceManager.edit()
                preferenceEditor.putString(LOCALE_PREFERENCE_KEY, testLocales[which].language)
                preferenceEditor.apply()

                // RTL, string formatting, and string changes fully take effect on a brand-new
                // activity, as opposed to recreate()
                activity.finish()
                activity.startActivity(activity.intent)
            }
            builder.show()
        }
    }
}