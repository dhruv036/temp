package io.stempedia.pictoblox.util

import android.os.Build
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import io.reactivex.rxjava3.core.Completable
import io.stempedia.pictoblox.BuildConfig


fun createNewAccountEntry() = FirebaseAuth.getInstance().signInAnonymously()
    .onSuccessTask {
        firebaseUserDetail(it!!.user!!.uid)
            .set(getUserDetailMap())
    }
    .onSuccessTask {
        FirebaseMessaging.getInstance().token
    }
    .onSuccessTask { token ->
        val doc = firebaseNewUserDevice()
        doc.set(getUserDeviceMap(FirebaseAuth.getInstance().currentUser!!.uid, token))
        Tasks.call { doc.id }
    }

private fun getUserDetailMap() = mutableMapOf<String, Any>().apply {
    put("account_created_on", Timestamp.now())
    put("app_version", BuildConfig.VERSION_NAME)
    put("sign_in_version", "2.0.1")
}

private fun getUserDeviceMap(uid: String, token: String?) = mutableMapOf<String, Any>().apply {
    val manufacturer = Build.MANUFACTURER
    val model = Build.MODEL
    val deviceInfo = if (model.toLowerCase().startsWith(manufacturer.toLowerCase())) {
        model
    } else {
        "$manufacturer $model"
    }

    put("device_info", deviceInfo)
    put("device_os", "Android SDK ${Build.VERSION.SDK_INT} (Version ${Build.VERSION.RELEASE})")
    put("notification_token", token ?: "")
    put("uid", uid)
    put("first_access", Timestamp.now())
    put("latest_access", Timestamp.now())
    put("is_active", true)


}