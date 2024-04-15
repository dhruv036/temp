package io.stempedia.pictoblox

import android.app.Application
import android.database.sqlite.SQLiteDatabaseLockedException
import android.text.TextUtils
import android.util.AndroidRuntimeException
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.stempedia.pictoblox.experimental.db.PictoBloxDatabase
import io.stempedia.pictoblox.util.*
import kotlin.Exception

class PictoBloxApp : Application() {
    override fun onCreate() {
        Log.e("error","7")
        PictoBloxAnalyticsEventLogger.createInstance(this)
        super.onCreate()
        Log.e("error","8")

        initDatabase()
//        try {
//            FirebaseApp.initializeApp(this)
//
//            if (FirebaseAuth.getInstance().currentUser == null) {
//                createNewAccountEntry()
//                    .addOnSuccessListener {
//                        SPManager(this).firebaseUserDeviceId = it
//                        PictobloxLogger.getInstance().logd("Account creation success: device id :  $it")
//                    }
//                    .addOnFailureListener {
//                        PictobloxLogger.getInstance().logd("Account creation failed")
//                        PictobloxLogger.getInstance().logException(it)
//                    }
//
//            } else {
//                updateExistingUserEntry()
//            }
//        }catch (_: Exception){
//        }
        Log.e("error","9")

//        RxJavaPlugins.setErrorHandler {}
//        io.reactivex.plugins.RxJavaPlugins.setErrorHandler { }
    }


    private fun initDatabase() {
        Log.e("error","10")

        try {
            PictoBloxDatabase.getDatabase(this)
            Log.e("SIZE", Runtime.getRuntime().maxMemory().toString())
        }catch (eeee : Exception){

        }
        Log.e("error","11")


        //TODO if entry is required, more sophisticated identification is requried
        /*val syncDao = PictoBloxDatabase.getDatabase(this).userSyncIndexDao()
         syncDao
             .getSyncIndexEntries()
             .subscribeOn(Schedulers.computation())
             .flatMapCompletable {
                 Completable.fromAction {
                     if (it == 0) {
                         val syncEntity = UserSyncIndexEntity(
                             FirebaseAuth.getInstance().currentUser?.uid ?: "temp_user",
                             0
                         )

                         syncDao.insert(syncEntity)
                     }
                 }
             }.subscribe()*/

    }

//    private fun updateExistingUserEntry() {
//        Log.e("error","12")
//        try {
//            if (!TextUtils.isEmpty(SPManager(this).firebaseUserDeviceId)) {
//                firebaseCurrentUserDevice(SPManager(this).firebaseUserDeviceId)
//                    .update(mapOf("latest_access" to Timestamp.now()))
//                    .addOnCompleteListener {
//                    }
//            }
//            Log.e("error","13")
//        }catch (eeee : Exception){
//
//        }
//    }
}