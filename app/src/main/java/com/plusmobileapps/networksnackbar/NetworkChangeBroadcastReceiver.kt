package com.plusmobileapps.networksnackbar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.support.v4.content.LocalBroadcastManager

class NetworkChangeBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val networkStateIntent = Intent(NETWORK_AVAILABLE_ACTION).apply {
            putExtra(IS_NETWORK_AVAILABLE, isConnectedToInternet(context))
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(networkStateIntent)
    }

    private fun isConnectedToInternet(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    companion object {
        const val NETWORK_AVAILABLE_ACTION = "com.plusmobileapps.networksnackbar.NetworkAvailable"
        const val IS_NETWORK_AVAILABLE = "is Network Available"
    }
}