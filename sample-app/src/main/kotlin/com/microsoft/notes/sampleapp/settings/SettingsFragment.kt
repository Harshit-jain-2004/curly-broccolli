package com.microsoft.notes.sampleapp.settings

import android.app.Activity
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import androidx.core.content.ContextCompat
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.sampleapp.R
import com.microsoft.notes.sampleapp.auth.AuthActivity
import com.microsoft.notes.sampleapp.auth.AuthManager

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_screen, rootKey)
        setupPreferences()
    }

    private fun setupPreferences() {
        val preferencesWithUserMessage = mutableListOf<Preference>().apply {
            add(findPreference(getString(R.string.transitions_key)))
            add(findPreference(getString(R.string.show_add_note_button_key)))
            add(findPreference(getString(R.string.show_add_ink_note_button_key)))
            add(findPreference(getString(R.string.show_feedback_key)))
            add(findPreference(getString(R.string.show_share_key)))
            add(findPreference(getString(R.string.show_notes_list_error_ui_key)))
            add(findPreference(getString(R.string.realtime_key)))
            add(findPreference(getString(R.string.gson_parser_key)))
            add(findPreference(getString(R.string.ink_key)))
            add(findPreference(getString(R.string.combined_list_for_multiacount_key)))
            add(findPreference(getString(R.string.show_action_mode_on_long_press)))
            add(findPreference(getString(R.string.show_search_in_notes_option)))
        }

        preferencesWithUserMessage.forEach { preference ->
            preference.setOnPreferenceClickListener {
                askUserToRestart()
                true
            }
        }

        val syncErrorSessionIds = getNoteSessionIdsForSyncErrors()

        if (syncErrorSessionIds.isNotEmpty()) {
            val syncErrorsPreferenceCategory = PreferenceCategory(preferenceScreen.context)
            syncErrorsPreferenceCategory.title = getString(R.string.sync_error_session_ids_title)
            preferenceScreen.addPreference(syncErrorsPreferenceCategory)

            syncErrorSessionIds.forEach {
                val sessionIdPreference = Preference(preferenceScreen.context)
                sessionIdPreference.title = it
                syncErrorsPreferenceCategory.addPreference(sessionIdPreference)
            }
        }

        val login1 = findPreference(getString(R.string.login_key))
        login1.setOnPreferenceClickListener {
            startActivity(Intent(activity, AuthActivity::class.java))
            true
        }

        val logoutAllUsers = findPreference(getString(R.string.logout_all_users_key))
        logoutAllUsers.setOnPreferenceClickListener {
            context?.let {
                val authProvider = AuthManager.getAuthProvider(it)
                NotesLibrary.getInstance().getAllUsers().forEach { userID ->
                    authProvider.logout(context as Activity, userID)
                }
            }
            true
        }
    }

    private fun askUserToRestart() {
        context?.let {
            val toast = Toast.makeText(it, "Please restart the app to see the changes", Toast.LENGTH_SHORT)
            toast.view?.background?.setColorFilter(
                    ContextCompat.getColor(it, R.color.toast_background), PorterDuff.Mode.SRC_IN)
            toast.show()
        }
    }

    fun setNoteSessionIdForSyncErrors(notesSessionId: String) {
        if (context != null) {
            val activity: Activity = context as Activity
            val sharedPref = activity.getPreferences(MODE_PRIVATE)
            val sharedPrefEditor = sharedPref.edit()
            val key: String = getString(R.string.sync_error_notes_session_ids)
            val valueSet: MutableSet<String> = (sharedPref.getStringSet(key, setOf()) ?: setOf<String>()).toMutableSet()
            if (!valueSet.contains(notesSessionId)) {
                valueSet.add(notesSessionId)
            }

            sharedPrefEditor.putStringSet(key, valueSet)
            sharedPrefEditor.apply()
        }
    }

    private fun getNoteSessionIdsForSyncErrors(): Set<String> {
        if (context != null) {
            val activity = context as Activity
            val sharedPref = activity.getPreferences(MODE_PRIVATE)
            val key = getString(R.string.sync_error_notes_session_ids)
            val valueSet: MutableSet<String> = sharedPref.getStringSet(key, setOf()) ?: setOf<String>().toMutableSet()
            return valueSet
        }

        return emptySet<String>()
    }
}