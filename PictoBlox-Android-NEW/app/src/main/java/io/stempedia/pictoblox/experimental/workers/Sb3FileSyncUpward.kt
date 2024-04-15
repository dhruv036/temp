package io.stempedia.pictoblox.experimental.workers

import android.content.Context
import androidx.work.WorkerParameters
import androidx.work.rxjava3.RxWorker
import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.GsonBuilder
import io.reactivex.rxjava3.core.Single
import io.stempedia.pictoblox.home.TestResponse
import io.stempedia.pictoblox.util.PictobloxLogger

class Sb3FileSyncUpward(appContext: Context, workerParams: WorkerParameters) : RxWorker(appContext, workerParams) {

    override fun createWork(): io.reactivex.rxjava3.core.Single<Result> {


        FirebaseFunctions.getInstance("asia-east2").getHttpsCallable("generateUploadToken")
            .call(mapOf("path" to "/path/to/resource"))
            .addOnSuccessListener {
                PictobloxLogger.getInstance().logd("Call success")
                val res = GsonBuilder().create().fromJson(it.data as String?, TestResponse::class.java)
                PictobloxLogger.getInstance().logd("${res.status} : ${res.token} : ${res.error}")

                //Authenticate user
            }
            .addOnFailureListener {
                PictobloxLogger.getInstance().logException(it)
            }

        return Single.never()
    }

}