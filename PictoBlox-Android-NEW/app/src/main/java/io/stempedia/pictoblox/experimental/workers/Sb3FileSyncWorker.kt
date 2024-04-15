package io.stempedia.pictoblox.experimental.workers

import android.content.Context
import androidx.work.WorkerParameters
import androidx.work.rxjava3.RxWorker
import com.google.firebase.auth.FirebaseAuth
import io.reactivex.rxjava3.core.Single
import io.stempedia.pictoblox.experimental.db.PictoBloxDatabase
import io.stempedia.pictoblox.util.PictobloxLogger

class Sb3FileSyncWorker(appContext: Context, workerParams: WorkerParameters) : RxWorker(appContext, workerParams) {

    override fun createWork(): Single<Result> {
        PictobloxLogger.getInstance().logd("createWork")

        val sb3FilesDao = PictoBloxDatabase.getDatabase(applicationContext).sb3FilesDao()
        val syncIndexDao = PictoBloxDatabase.getDatabase(applicationContext).userSyncIndexDao()
        val uid = FirebaseAuth.getInstance().uid!!

        return downwardSync(uid, applicationContext, sb3FilesDao, syncIndexDao)
            .flatMap { isSuccess ->
                if (isSuccess) {
                    upwardSync(uid, sb3FilesDao)
                } else {
                    Single.fromCallable { Result.retry() }
                }
            }
    }
}