# Network Error Reactive Snackbar 

In the early days of Android, you could declare in your `AndroidManifest.xml` a broadcast receiver that could listen for any network changes of the device in the background. This sounds amazing to a developer, but was terrible for the user's battery since every app could potentially be woken up by any network state change of the device. 

So in order to get the wild west of network state change broadcast receivers under control for all apps on a device, starting in Android 7.0 (API level 24) and higher applications would no longer receive any broadcasts of network state changes. 

After digging around some [documentation](https://developer.android.com/reference/android/net/ConnectivityManager.html#CONNECTIVITY_ACTION), you will find that in order to register a broadcast receiver for this purpose you can use [`Context.registerReceiver()`](https://developer.android.com/reference/android/content/Context#registerReceiver(android.content.BroadcastReceiver,%20android.content.IntentFilter)) so long as that context remains valid. 

## Overview

This app will register a broadcast receiver to listen to network state changes of the device and will display a snackbar in a reactive way. Most of the magic lives in `NetworkSnackbarHelper` and is used as so in kotlin: 

```kotlin
NetworkSnackbarHelper(
                context = this,
                snackbarContainer = coordinator,
                lifecycleOwner = this)
```
![Demo](https://github.com/plusmobileapps/network-snackbar-error/blob/master/network-snackbar.gif)

Typically will instantiate this helper class in a `Fragment` or `Activity` to pass it the `Context` and `LifecycleOwner`. Then also pass a reference to the `View` in which to display the reactive snackbar onto. 

If you would like to get a callback of when the network changes, the last parameter in the method is a callback function. So we can place a lambda on the outside of the declaration since we're using kotlin for simplicity. 

```kotlin 
        NetworkSnackbarHelper(
                context = this,
                snackbarContainer = coordinator,
                lifecycleOwner = this
        ) { isConnected ->
            //delegate this call to a presenter or viewmodel
            Toast.makeText(this, "Network state is $isConnected", Toast.LENGTH_LONG).show()
        }
```

![Demo](https://github.com/plusmobileapps/network-snackbar-error/blob/master/screen-record-201808-29T02:48:48Z.mp4.gif)

If you would like a little bit more functionality, you may also pass in snackbar duration or message to be displayed too. These are default parameters which will default to `Snackbar.LENGTH_INDEFINITE` and a string resource id for "No Network Connection" 

```kotlin
        NetworkSnackbarHelper(
                context = this,
                snackbarContainer = coordinator,
                lifecycleOwner = this,
                duration = Snackbar.LENGTH_LONG,
                stringId = R.string.abc_action_bar_home_description
        ) 
```

## Setup 

1. Declare permission in the `AndroidManifest.xml` to access the devices network state. 

```xml
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

```


2. Create a `NetworkChangeBroadcastReceiver` class that will listen for network state changes after being registered and will send a broadcast out to the `LocalBroadcastManager` of the devices network state. 

```kotlin
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
```

3. Now we can create a class called `NetworkSnackBarHelper` which will take a few mandatory parameters in its constructor with optional parameters for more customization. 

```kotlin
class NetworkSnackbarHelper(private val context: Context,
                            private val snackbarContainer: View,
                            lifecycleOwner: LifecycleOwner,
                            stringId: Int = R.string.no_network,
                            duration: Int = Snackbar.LENGTH_INDEFINITE,
                            private val callback: ((Boolean) -> Unit)? = null) : LifecycleObserver {
    
    private val networkChangeReceiver = NetworkChangeBroadcastReceiver()
    private val snackbar by lazy {
        Snackbar.make(snackbarContainer, context.getString(stringId), duration)
    }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
        registerNetworkListener() // will discuss this method soon enough
    }    
}
```

Now to describe what is going on in `registerNetworkListener()`

```kotlin
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
```

We first create an `IntentFilter()` and add the action `CONNECTIVITY_ACTION` which will filter intents broadcasted from the system for network changes. Then register the receiver using the context passed in to the class using our `networkChangeReceiver` and `appIntentFilter` created earlier. 

The last part is take advantage of the fact that this class is observing a lifecycle owner, so to prevent users from having to unsubscribe from the broadcasts. Using the annotations for the `OnDestroy()` lifecycle event, we can create a method to unregister the receiver. 
                   
```kotlin
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun disconnectListener() = context.unregisterReceiver(networkChangeReceiver)
```
