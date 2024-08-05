package com.microsoft.notes.sampleapp.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.webkit.CookieManager
import android.webkit.CookieSyncManager
import android.widget.Toast
import com.microsoft.aad.adal.ADALError
import com.microsoft.aad.adal.AuthenticationCallback
import com.microsoft.aad.adal.AuthenticationCancelError
import com.microsoft.aad.adal.AuthenticationContext
import com.microsoft.aad.adal.AuthenticationException
import com.microsoft.aad.adal.AuthenticationResult
import com.microsoft.aad.adal.PromptBehavior
import com.microsoft.notes.models.AccountType
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.sampleapp.utils.executePostSignInTask
import com.microsoft.notes.utils.utils.IdentityMetaData
import java.io.UnsupportedEncodingException
import java.lang.IllegalArgumentException
import java.security.NoSuchAlgorithmException

class AADAuthProvider(private val context: Context) : AuthProvider {

    companion object {
        const val NAME = "AAD"
        const val AUTHORITY_URL = "https://login.windows.net/common"
        const val CLIENT_ID = "7fba38f4-ec1f-458d-906c-f4e3c4f41335"
        const val REDIRECT_URL = "ms-notes://msaadauthcallback"
        const val RESOURCE_URL = "https://outlook.office365.com"
    }

    private val TAG = AADAuthProvider::class.java.simpleName
    private val authContext = AuthenticationContext(context, AUTHORITY_URL, true)

    override fun login(activity: Activity,
                       loginHint: String?,
                       onSuccess: ((AuthenticationResult?) -> Unit)?,
                       onError: ((AuthFailedReason) -> Unit)?) {

        val authCallback = object : AuthenticationCallback<AuthenticationResult> {
            override fun onSuccess(result: AuthenticationResult?) {
                onLoginSuccess(activity, result, onError, onSuccess)
            }

            override fun onError(error: Exception?) {
                onLoginError(error, onError)
            }
        }
        try {
            if (loginHint != null && loginHint.isNotEmpty()) {
                authContext.acquireToken(
                    activity,
                    RESOURCE_URL,
                    CLIENT_ID,
                    REDIRECT_URL,
                    loginHint,
                    authCallback)
            } else {
                authContext.acquireToken(
                    activity,
                    RESOURCE_URL,
                    CLIENT_ID,
                    REDIRECT_URL,
                    PromptBehavior.Always,
                    authCallback)
            }
        } catch (e: Exception) {
            catchAcquireTokenException(e)
        }
    }

    override fun acquireTokenForResource(resource: String, userID: String, onSuccess: (String) -> Unit, onFailure: () -> Unit) {
        val authCallback = object : AuthenticationCallback<AuthenticationResult> {
            override fun onSuccess(result: AuthenticationResult?) {
                if(result?.accessToken?.isNotEmpty() == true) {
                    onSuccess(result.accessToken)
                } else {
                    onFailure()
                }
            }

            override fun onError(error: Exception?) {
                onFailure()
            }
        }

        authContext.acquireTokenSilentAsync(resource, CLIENT_ID, userID, authCallback)
    }

    private fun catchAcquireTokenException(e: Exception) {
        when (e) {
            is IllegalArgumentException -> {
                Log.d(TAG, "IllegalArgumentException in acquireToken: ${e.message}")
            }
            is AuthenticationException -> {
                Log.d(TAG, "AuthenticationException in acquireToken: ${e.message}")
            }
            is UnsupportedEncodingException -> {
                Log.d(TAG, "UnsupportedEncodingException in acquireToken: ${e.message}")
            }
            is NoSuchAlgorithmException -> {
                Log.d(TAG, "NoSuchAlgorithmException in acquireToken: ${e.message}")
            }
            else -> {
                Log.d(TAG, "Exception occurred in acquireToken: ${e.message}")
                throw e
            }
        }
    }

    override fun refreshAccounts(activity: Activity): List<String> {
        val emails = IdentityProvider.getInstance().getSignedInEmails(activity)
        emails.forEach { login(activity, it) }
        return emails
    }

    private fun onLoginSuccess(activity: Activity,
                               authResult: AuthenticationResult?,
                               onError: ((AuthFailedReason) -> Unit)?,
                               onSuccess: ((AuthenticationResult?) -> Unit)?) {
        if (authResult == null || authResult.accessToken == null || authResult.accessToken.isEmpty()) {
            authResult?.let {
                NotesLibrary.getInstance().userAuthFailed(userID = it.userInfo.displayableId)
            }
            Log.d(TAG, "Token is empty!")
            Toast.makeText(context, "Token is empty", Toast.LENGTH_SHORT).show()
            onError?.invoke(AuthFailedReason.NO_TOKEN)
        } else {
            val isMSAAccount: Boolean = authResult.userInfo.identityProvider == "live.com"
            val accountType = if (isMSAAccount) AccountType.MSA else AccountType.ADAL

            IdentityProvider.getInstance().addAuth(activity,
                AuthData(name = authResult.userInfo.givenName + " " + authResult.userInfo.familyName,
                    email = authResult.userInfo.displayableId,
                    accountType = accountType,
                    accessToken = authResult.accessToken,
                    refreshToken = authResult.refreshToken))

            Log.d(TAG, "Status: ${authResult.status}. Expired: ${authResult.expiresOn}")
            handleNewAuthToken(authResult, accountType, onSuccess)
        }
    }

    private fun handleNewAuthToken(authResult: AuthenticationResult,
                                   accountType: AccountType,
                                   onSuccess: ((AuthenticationResult?) -> Unit)?) {
        with(authResult) {
            executePostSignInTask(IdentityMetaData(userID = userInfo.displayableId,
                    email = userInfo.displayableId,
                    accessToken = accessToken,
                    accountType = accountType),
                    onCompletionTask = { onSuccess?.invoke(authResult) })
        }
    }

    private fun onLoginError(error: Exception?,
                             onError: ((AuthFailedReason) -> Unit)?) {
        val reason = when (error) {
            is AuthenticationCancelError -> {
                Log.d(TAG, "Canceled by user")
                AuthFailedReason.CANCELED
            }
            is AuthenticationException -> {
                when (error.code) {
                    ADALError.DEVICE_CONNECTION_IS_NOT_AVAILABLE -> {
                        Log.d(TAG, "No network connection")
                        AuthFailedReason.NETWORK_CONNECTION
                    }
                    else -> {
                        Log.d(TAG, "Other AuthException: ${error.message}")
                        AuthFailedReason.OTHER
                    }
                }
            }
            else -> {
                Log.d(TAG, "Other exception: #{error?.message}")
                AuthFailedReason.OTHER
            }
        }
        onError?.invoke(reason)
    }

    override fun logout(activity: Activity, userID: String, onSuccess: (() -> Unit)?) {
        // This clears all account cookies, which isn't ideal, but good enough for testing
        // single-account sign-out
        cleanCookies()
        authContext.cache.removeAll()

        NotesLibrary.getInstance().logout(userID).then {
            IdentityProvider.getInstance().removeAuth(activity, userID)
            NotesLibrary.getInstance().deleteAllNotes(userID)
            onSuccess?.invoke()
        }
    }

    private fun cleanCookies() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies(null)
        cookieManager.flush()
    }

    override fun onResultAfterRequestToken(requestCode: Int, resultCode: Int, data: Intent?) {
        authContext.onActivityResult(requestCode, resultCode, data)
    }
}