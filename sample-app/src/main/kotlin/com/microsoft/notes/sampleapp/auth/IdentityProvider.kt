package com.microsoft.notes.sampleapp.auth

import android.app.Activity
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import com.microsoft.notes.models.AccountType
import com.microsoft.notes.utils.utils.Constants.AUTH_SHARED_PREF_KEY

data class AuthData(val name: String,
                    val email: String,
                    val accountType: AccountType,
                    val accessToken: String,
                    val refreshToken: String)

class IdentityProvider private constructor() {
    companion object {
        private val authDataList: MutableList<AuthData> = mutableListOf();
        private const val MSA_EMAIL_SHARED_PREF_KEY = "MSA_EmailId"
        private const val ADAL_EMAIL_SHARED_PREF_KEY = "ADAL_EmailId"
        @Volatile private var sInstance: IdentityProvider? = null

        fun getInstance(): IdentityProvider {
            if (sInstance == null) {
                sInstance = IdentityProvider()
            }

            return sInstance!!
        }
    }

    fun addAuth(activity: Activity, authData: AuthData) {
        val sharedPrefEditor = activity.getSharedPreferences(AUTH_SHARED_PREF_KEY, MODE_PRIVATE).edit()

        if (authData.accountType == AccountType.MSA) {
            sharedPrefEditor.putString(MSA_EMAIL_SHARED_PREF_KEY, authData.email)
        } else if (authData.accountType == AccountType.ADAL){
            sharedPrefEditor.putString(ADAL_EMAIL_SHARED_PREF_KEY, authData.email)
        }
        sharedPrefEditor.apply()
        authDataList.add(authData)
    }

    fun removeAuth(activity: Activity, userID: String) {
        val sharedPref = activity.getSharedPreferences(AUTH_SHARED_PREF_KEY, MODE_PRIVATE)
        val msaEmailId = sharedPref.getString(MSA_EMAIL_SHARED_PREF_KEY, "")  ?: ""
        val adalEmailId = sharedPref.getString(ADAL_EMAIL_SHARED_PREF_KEY, "") ?: ""

        val sharedPrefEditor = sharedPref.edit()
        if (msaEmailId.isNotEmpty() && msaEmailId == userID) {
            sharedPrefEditor.remove(MSA_EMAIL_SHARED_PREF_KEY)
        }
        if (adalEmailId.isNotEmpty() && adalEmailId == userID) {
            sharedPrefEditor.remove(ADAL_EMAIL_SHARED_PREF_KEY)
        }

        sharedPrefEditor.apply()
    }

    fun getSignedInEmails(activity: Activity): List<String> {
        val emails = mutableListOf<String>()

        val sharedPref: SharedPreferences = activity.getSharedPreferences(AUTH_SHARED_PREF_KEY, MODE_PRIVATE)
        val msaEmailId = sharedPref.getString(MSA_EMAIL_SHARED_PREF_KEY, "")  ?: ""
        val adalEmailId = sharedPref.getString(ADAL_EMAIL_SHARED_PREF_KEY, "") ?: ""

        if (msaEmailId.isNotEmpty())
            emails.add(msaEmailId)

        if (adalEmailId.isNotEmpty())
            emails.add(adalEmailId)

        return emails
    }

    fun getAuthData(email: String): AuthData? = authDataList.find { it.email == email }
}