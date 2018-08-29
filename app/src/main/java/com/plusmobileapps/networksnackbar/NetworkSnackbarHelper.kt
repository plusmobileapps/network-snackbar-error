package com.plusmobileapps.networksnackbar

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.OnLifecycleEvent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.support.design.widget.Snackbar
import android.support.v4.content.LocalBroadcastManager
import android.view.View
import com.plusmobileapps.networksnackbar.NetworkChangeBroadcastReceiver.Companion.IS_NETWORK_AVAILABLE
import com.plusmobileapps.networksnackbar.NetworkChangeBroadcastReceiver.Companion.NETWORK_AVAILABLE_ACTION

/**
 * A helper class to listen to devices network state changes and will
 * show or hide a persistent snackbar based off the state
 *
 * @param context context for the activity or fragment in view
 * @param snackbarContainer the android view for which to show the snackbar on
 * @param lifecycleOwner owner of the fragment or activity lifecycle used to for OnDestroy event to
 *                       unregister the network change broadcast receiver
 * @param stringId the string resource id for the snackbar message of no network
 * @param duration duration for the snackbar
 * @param callback callback method for activity to delegate network changes back to presenter if other
 *                 behavior is desired past the snackbar
 */
class NetworkSnackbarHelper(private val context: Context,
                            private val snackbarContainer: View,
                            lifecycleOwner: LifecycleOwner,
                            stringId: Int = R.string.no_network,
                            duration: Int = Snackbar.LENGTH_INDEFINITE,
                            private val callback: ((Boolean) -> Unit)? = null) : LifecycleObserver {

    private val networkChangeReceiver = NetworkChangeBroadcastReceiver()
    private val snackbar by lazy { Snackbar.make(snackbarContainer, context.getString(stringId), duration) }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
        registerNetworkListener()
    }

    private fun registerNetworkListener() {
        val appIntentFilter = IntentFilter().apply {
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        }
        context.registerReceiver(networkChangeReceiver, appIntentFilter)
        val networkIntentFilter = IntentFilter(NETWORK_AVAILABLE_ACTION)
        LocalBroadcastManager.getInstance(context).registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val isNetworkAvailable = intent.getBooleanExtra(IS_NETWORK_AVAILABLE, false)
                callback?.invoke(isNetworkAvailable)
                snackbar.apply {
                    if (isNetworkAvailable) dismiss() else show()
                }
            }
        }, networkIntentFilter)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun disconnectListener() {
        try {
            context.unregisterReceiver(networkChangeReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}