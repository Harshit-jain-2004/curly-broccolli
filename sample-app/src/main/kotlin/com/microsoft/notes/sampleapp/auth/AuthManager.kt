package com.microsoft.notes.sampleapp.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.microsoft.aad.adal.AuthenticationResult

interface AuthProvider {
    fun login(activity: Activity,
              loginHint: String? = null,
              onSuccess: ((AuthenticationResult?) -> Unit)? = null,
              onError: ((AuthFailedReason) -> Unit)? = null)

    fun refreshAccounts(activity: Activity): List<String>
    fun logout(activity: Activity, userID: String, onSuccess: (() -> Unit)? = null)
    fun onResultAfterRequestToken(requestCode: Int, resultCode: Int, data: Intent?)
    fun acquireTokenForResource(resource: String, userID: String, onSuccess: (String) -> Unit, onFailure: () -> Unit)
}

enum class AuthFailedReason {
    CANCELED,
    NETWORK_CONNECTION,
    NO_TOKEN,
    OTHER
}

object AuthManager {
    fun getAuthProvider(context: Context): AuthProvider {
        return AADAuthProvider(context)
    }
}

fun Activity.handleTokenOnActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    val authProvider = AuthManager.getAuthProvider(this)
    authProvider.onResultAfterRequestToken(requestCode, resultCode, data)
}