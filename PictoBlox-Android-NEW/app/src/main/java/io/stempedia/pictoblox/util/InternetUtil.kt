package io.stempedia.pictoblox.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import io.reactivex.rxjava3.core.Completable

fun hasInternet(context: Context): Completable = Completable.create { emitter ->

    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val networkCapabilities = connectivityManager.activeNetwork
        if (networkCapabilities != null) {
            val activeNetwork = connectivityManager.getNetworkCapabilities(networkCapabilities)
            if (activeNetwork != null) {
                if (activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                ) {
                    emitter.onComplete()

                } else {
                    emitter.onError(Exception("No Internet"))

                }

            } else {
                emitter.onError(Exception("No Internet"))
            }
        } else {
            emitter.onError(Exception("No Internet"))
        }

    } else {

        val networkCapabilities = connectivityManager.activeNetworkInfo

        if (networkCapabilities != null) {
            if (networkCapabilities.type == ConnectivityManager.TYPE_WIFI
                || networkCapabilities.type == ConnectivityManager.TYPE_MOBILE
                || networkCapabilities.type == ConnectivityManager.TYPE_ETHERNET
            ) {
                emitter.onComplete()

            } else {
                emitter.onError(Exception("No Internet"))
            }

        } else {
            emitter.onError(Exception("No Internet"))
        }

    }
}