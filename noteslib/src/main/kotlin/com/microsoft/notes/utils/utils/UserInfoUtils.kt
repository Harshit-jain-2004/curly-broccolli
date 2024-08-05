package com.microsoft.notes.utils.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.microsoft.notes.utils.utils.Constants.AUTH_SHARED_PREF_KEY
import com.microsoft.notes.utils.utils.Constants.EMPTY_USER_ID
import com.microsoft.notes.utils.utils.Constants.USERID_TO_EMAIL_MAP
import java.util.UUID

internal class UserInfoUtils {
    companion object {
        internal const val USER_INFO_SUFFIX = "UserInfoSuffix"
        internal const val DEFAULT_USER_INFO_SUFFIX_SHARED_PREFS_VALUE = "DefaultUserInfoSuffix"
        internal const val FIRST_USER_INFO_SUFFIX_SET_SHARED_PREFS_KEY = "DefaultUserInfoSuffixSet"
        private const val LOG_TAG = "UserInfoUtils"
        private val jsonParser: Gson by lazy { Gson() }

        /**
         * This api provides UserInfoSuffix for a userID which is appended in database names
         * and naming of folders for storing images and outbound queue backup files.
         * a. The first user to call for UserInfoSuffix will get Empty UserInfoSuffix as
         *    existing local notes will be migrated to his account
         * b. Empty UserID i.e not signed in users will get Empty UserInfoSuffix too.
         *    This will help in migrating non-signed in notes and related data to first
         *    signed in user
         * c. The second and onwards users will receive random string to keep their data
         *    separate from first and non-signed in users.
         * d. If first user signs out, second user will retain his UserInfoSuffix value,
         *    the next signed in used will now get "" as UserInfoSuffix
         */
        @Synchronized
        fun getUserInfoSuffix(userID: String, context: Context): String {
            val sharedPrefs = getSharedAuthPrefs(context)

            if (userID == EMPTY_USER_ID) {
                Log.v(LOG_TAG, "Returning empty UserInfoSuffix for empty userID")
                return ""
            }

            val userInfoSuffixSharedPrefsKey = getPrefsKey(userID)

            var userInfoSuffix = sharedPrefs.getString(
                userInfoSuffixSharedPrefsKey,
                DEFAULT_USER_INFO_SUFFIX_SHARED_PREFS_VALUE
            ) ?: DEFAULT_USER_INFO_SUFFIX_SHARED_PREFS_VALUE

            if (userInfoSuffix != DEFAULT_USER_INFO_SUFFIX_SHARED_PREFS_VALUE) {
                Log.v(LOG_TAG, "UserInfoSuffix found in sharedPrefs, returning")
                return userInfoSuffix
            }

            val isDefaultUserInfoSuffixSet = sharedPrefs.getBoolean(
                FIRST_USER_INFO_SUFFIX_SET_SHARED_PREFS_KEY,
                false
            )

            if (isDefaultUserInfoSuffixSet == false) {
                userInfoSuffix = ""
                val sharedPrefsEditor = sharedPrefs.edit()
                sharedPrefsEditor.putString(userInfoSuffixSharedPrefsKey, "")
                sharedPrefsEditor.putBoolean(FIRST_USER_INFO_SUFFIX_SET_SHARED_PREFS_KEY, true)
                sharedPrefsEditor.apply()
            } else {
                val uuID = UUID.randomUUID().toString()
                sharedPrefs.edit().putString(userInfoSuffixSharedPrefsKey, uuID).apply()
                userInfoSuffix = uuID
                Log.v(LOG_TAG, "Generating a random UserInfoSuffix")
            }

            sharedPrefs.edit().putString(userInfoSuffixSharedPrefsKey, userInfoSuffix).apply()

            return userInfoSuffix
        }

        fun updateUserInfoSuffixForSignedOutUser(userID: String, context: Context) {
            val sharedPrefs = getSharedAuthPrefs(context)
            val userInfoSuffixSharedPrefsKey = getPrefsKey(userID)

            val userInfoSuffix = sharedPrefs.getString(
                userInfoSuffixSharedPrefsKey,
                DEFAULT_USER_INFO_SUFFIX_SHARED_PREFS_VALUE
            )
                ?: DEFAULT_USER_INFO_SUFFIX_SHARED_PREFS_VALUE

            if (userInfoSuffix == DEFAULT_USER_INFO_SUFFIX_SHARED_PREFS_VALUE) {
                Log.v(LOG_TAG, "This should not happen but it's ok to ignore this")
                return
            } else if (userInfoSuffix == "") {
                val sharedPrefsEditor = sharedPrefs.edit()

                sharedPrefsEditor.putBoolean(FIRST_USER_INFO_SUFFIX_SET_SHARED_PREFS_KEY, false)
                sharedPrefsEditor.remove(userInfoSuffixSharedPrefsKey)
                sharedPrefsEditor.apply()

                Log.v(LOG_TAG, "This was first user, have reset the default key shared prefs")
            } else {
                sharedPrefs.edit().remove(userInfoSuffixSharedPrefsKey).apply()
                Log.v(LOG_TAG, "Removed shared prefs key")
            }
        }

        fun addUserIDToSharedPreferences(userID: String, emailID: String, context: Context) {
            val sharedPref = getSharedAuthPrefs(context)
            val signedInUserIDs = getSignedInUsers(context)
            if (!signedInUserIDs.contains(userID)) {
                val mutableSignedInUserIDs = signedInUserIDs.toMutableSet()
                mutableSignedInUserIDs.add(userID)
                sharedPref.edit().putStringSet(Constants.SIGNED_IN_ACCOUNTS, mutableSignedInUserIDs).apply()
            }

            if (!getSignedInUserIDToEmailMap(context).containsKey(userID))
                addUserIDToUserIDEmailMap(context, userID, emailID)
        }

        fun updateUserIDToSharedPreferences(context: Context, userID: String, emailID: String) {
            val userIDToEmailMap = getSignedInUserIDToEmailMap(context)

            if (userIDToEmailMap.containsKey(userID) && userIDToEmailMap[userID].isNullOrEmpty())
                addUserIDToUserIDEmailMap(context, userID, emailID)
        }

        fun removeUserIDFromSharedPreferences(userID: String, context: Context) {
            val sharedPref = getSharedAuthPrefs(context)
            val signedInUserIDs = getSignedInUsers(context)
            if (signedInUserIDs.contains(userID)) {
                val mutableSignedInUserIDs = signedInUserIDs.toMutableSet()
                mutableSignedInUserIDs.remove(userID)
                sharedPref.edit().putStringSet(Constants.SIGNED_IN_ACCOUNTS, mutableSignedInUserIDs).apply()
            }
            removeUserIDFromUserIDEmailMap(context, userID)
        }

        fun getSignedInUsers(context: Context): Set<String> {
            val sharedPref = getSharedAuthPrefs(context)
            return sharedPref.getStringSet(Constants.SIGNED_IN_ACCOUNTS, emptySet()) ?: emptySet()
        }

        fun getEmailIDFromUserID(userID: String, context: Context): String {
            val userIDToEmailMap = getSignedInUserIDToEmailMap(context)
            return userIDToEmailMap[userID] ?: ""
        }

        fun getUserIDFromEmailID(emailID: String, context: Context): String {
            val userIDToEmailMap = getSignedInUserIDToEmailMap(context)
            return userIDToEmailMap.filterValues { it == emailID }?.keys?.first()
        }

        private fun getSignedInUserIDToEmailMap(context: Context): Map<String, String> {
            val sharedPreferences = getSharedAuthPrefs(context)

            val storedJson: String = sharedPreferences.getString(USERID_TO_EMAIL_MAP, "") ?: ""

            return try {
                jsonParser.fromJson<Map<String, String>>(
                    storedJson,
                    object : TypeToken<Map<String, String>>() {}.type
                ) ?: emptyMap()
            } catch (e: JsonSyntaxException) {
                emptyMap()
            }
        }

        private fun addUserIDToUserIDEmailMap(context: Context, userID: String, emailID: String) {
            val mutableUserIDToEmailMap = getSignedInUserIDToEmailMap(context).toMutableMap()
            mutableUserIDToEmailMap[userID] = emailID

            val hashMapString: String = jsonParser.toJson(mutableUserIDToEmailMap)
            getSharedAuthPrefs(context).edit().putString(USERID_TO_EMAIL_MAP, hashMapString).apply()
        }

        private fun removeUserIDFromUserIDEmailMap(context: Context, userID: String) {
            val userIDToEmailMap = getSignedInUserIDToEmailMap(context)
            if (userIDToEmailMap.containsKey(userID)) {
                val mutableUserIDToEmailMap = getSignedInUserIDToEmailMap(context).toMutableMap()
                mutableUserIDToEmailMap.remove(userID)

                val hashMapString: String = jsonParser.toJson(mutableUserIDToEmailMap)
                getSharedAuthPrefs(context).edit().putString(USERID_TO_EMAIL_MAP, hashMapString).apply()
            }
        }

        private fun getSharedAuthPrefs(context: Context): SharedPreferences =
            context.getSharedPreferences(AUTH_SHARED_PREF_KEY, Context.MODE_PRIVATE)

        private fun getPrefsKey(userID: String): String = USER_INFO_SUFFIX + "_" + userID
    }
}
