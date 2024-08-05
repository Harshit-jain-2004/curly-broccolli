package com.microsoft.notes.sampleapp.auth

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.sampleapp.R
import kotlinx.android.synthetic.main.auth_layout.*

class AuthActivity : AppCompatActivity() {
    companion object {
        const val LOGGED_IN = "LOGGED_IN"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.auth_layout)
        setupToolbar()

        bindButtons()
        setup()
    }

    private fun setupToolbar() {
        setSupportActionBar(findViewById(R.id.auth_toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        auth_toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setup() {
        val identityProviders = IdentityProvider.getInstance().getSignedInEmails(activity = this)

        if (identityProviders.size > 0) {
            identityProviders.forEach { doLogin(it) }

            login.isEnabled = NotesLibrary.getInstance().experimentFeatureFlags.multiAccountEnabled
            logout.isEnabled = true
        }
        else {
            login.isEnabled = true
            logout.isEnabled = false
        }
    }

    private fun bindButtons() {
        login.setOnClickListener {
            doLogin()
        }
        logout.setOnClickListener {
            doLogout()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun doLogin(loginHint: String? = null) {
        val authProvider = AuthManager.getAuthProvider(this) 
        authProvider.login(
                activity = this,
                loginHint = loginHint,
                onSuccess = { authResult ->

                    authResult?.let {
                        val userLayout = layoutInflater.inflate(R.layout.user_layout, null) as
                                LinearLayout
                        val name = userLayout.findViewById<TextView>(R.id.name)
                        name.text = "${authResult.userInfo.givenName} ${authResult.userInfo.familyName}"

                        val email = userLayout.findViewById<TextView>(R.id.email)
                        email.text = authResult.userInfo.displayableId

                        val logoutButton = userLayout.findViewById<Button>(R.id.logoutUser)
                        logoutButton.setOnClickListener {
                            authProvider.logout(this, userID = authResult.userInfo.displayableId, onSuccess = {
                                Toast.makeText(this, "Logged out ${authResult.userInfo.displayableId}", Toast.LENGTH_SHORT).show()
                            })
                        }

                        name.visibility = View.VISIBLE
                        email.visibility = View.VISIBLE
                        userList.addView(userLayout)
                    }
                    val sharedPrefs = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE)
                    sharedPrefs.edit().putBoolean(LOGGED_IN, true).apply()
                    Toast.makeText(this, "Logged in", Toast.LENGTH_SHORT).show()
                },
                onError = {
                    Log.d("Auth", "Auth Failed: ${it.name}")
                    showLoginFailedDialog(it)
                })
    }

    private fun doLogout(userID: String = "") {
        val authProvider = AuthManager.getAuthProvider(this)
        authProvider.logout(this, userID = userID, onSuccess = {
            for (index in 0 until userList.childCount) {
                val linearLayout = userList.getChildAt(index)
                val textView = linearLayout.findViewById(R.id.name) as TextView
                val emailView = linearLayout.findViewById(R.id.email) as TextView
                val logoutView = linearLayout.findViewById(R.id.logoutUser) as Button
                textView.visibility = View.GONE
                emailView.visibility = View.GONE
                logoutView.visibility = View.GONE
            }

            login.isEnabled = true
            logout.isEnabled = false

            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        handleTokenOnActivityResult(requestCode, resultCode, data)
    }

    private fun showLoginFailedDialog(reason: AuthFailedReason) {
        val (titleString, messageString) = when (reason) {
            AuthFailedReason.NETWORK_CONNECTION ->
                Pair(R.string.auth_failed_no_internet_title, R.string.auth_failed_no_internet_message)
            AuthFailedReason.CANCELED ->
                Pair(R.string.auth_canceled_title, R.string.auth_canceled_message)
            AuthFailedReason.NO_TOKEN ->
                Pair(R.string.auth_no_token_title, R.string.auth_no_token_message)
            AuthFailedReason.OTHER ->
                Pair(R.string.auth_failed_other_title, R.string.auth_failed_other_message)
        }
        val builder = AlertDialog.Builder(this)
        builder.setTitle(titleString)
        builder.setMessage(messageString)
        builder.setPositiveButton(android.R.string.ok, null)
        builder.show()
    }
}