package com.microsoft.notes.platform.extensions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.ConnectivityManager.CONNECTIVITY_ACTION
import android.net.Network
import android.net.NetworkInfo
import android.os.Build
import androidx.annotation.RequiresApi
import com.microsoft.notes.noteslib.NotesLibrary

fun Context.isNetworkConnected(): Boolean =
    getConnectivityManager()?.activeNetworkInfo?.isConnected ?: false

/**
 * Calls the observer as soon as the network is available. Will call the callback immediately if already connected.
 */
fun Context.registerOnNetworkAvailableCompat(observer: () -> Unit) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val connectivityManager = getConnectivityManager()
            val networkCallback = OnNetworkAvailable(observer)
            connectivityManager?.registerDefaultNetworkCallback(networkCallback)
        } else {
            // Deprecated in API level >= 24
            @Suppress("DEPRECATION")
            val intentFilter = IntentFilter(CONNECTIVITY_ACTION)
            registerReceiver(OnNetworkAvailableBroadcastReceiver(observer), intentFilter)
        }
    } catch (exception: SecurityException) {
        NotesLibrary.getInstance().log(message = "Security Exception while registering default network callback")
    }
}

private class OnNetworkAvailableBroadcastReceiver(val observer: () -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // Deprecated in API level >= 24
        @Suppress("DEPRECATION")
        if (intent?.action == CONNECTIVITY_ACTION) {
            val networkInfo = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO)
                as? NetworkInfo
            if (networkInfo?.isConnected == true) {
                observer()
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.N)
private class OnNetworkAvailable(val observer: () -> Unit) : ConnectivityManager.NetworkCallback() {
    override fun onAvailable(network: Network) {
        observer()
    }
}

private fun Context.getConnectivityManager(): ConnectivityManager? =
    getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
