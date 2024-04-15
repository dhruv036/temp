package io.stempedia.pictoblox.experimental

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import io.stempedia.pictoblox.experimental.db.PictoBloxDatabase
import io.stempedia.pictoblox.experimental.db.files.Sb3FileEntity
import io.stempedia.pictoblox.experimental.workers.createWork
import io.stempedia.pictoblox.util.PictobloxLogger
import java.io.File
import java.io.FileOutputStream
import java.util.*

//Show warning of file size is humongous
//change name to repo?
//TODO case when anonymous sign up fails, like when no internet
//TODO partial search queries from cloud

class PictoBloxUserSb3FilesRepository(val application: Context) {
    private val sb3FilesDao = PictoBloxDatabase.getDatabase(application).sb3FilesDao()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "temp_user"

    init {
        //Timestamp
        //setWorkForSync()
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onDisposeStream() {

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onCreateStream() {
    }

    //TODO everything from the date stamp, for local creation date, for cloud sync date
    /*fun getRecentFiles(query: String? = null): Observable<List<UserSb3File>> {
        //Local entries edited within last 3 days
        //cloud entry with sync flag within last 3 days
        val secondsInThreeDays = 259_200_000//3 days in millis
        val threeDaysBack = System.currentTimeMillis() - secondsInThreeDays

        val o1 = sb3FilesDao.getRecentFilesByUser(userId, threeDaysBack)
            .map { entities -> entities.map { sb3EntityToPojo(it) } }

        val o2 = getCloudFilesByTimestamp(threeDaysBack)
            .map { cloudEntriesToPojo(it) }

        return Observable.merge(o1, o2)

    }*/

    /**
     * must dispose at on stop,
     */
/*
    fun getLocalFiles(query: String? = null): Observable<List<UserSb3File>> {
        return sb3FilesDao.getLocalFileByUser(userId)
            .map { entities -> entities.map { sb3EntityToPojo(it) } }

    }
*/

    /**
     * must dispose at on stop,
     */
/*    fun getCloudFiles(query: String? = null): Observable<List<UserSb3File>> {
        return Observable.merge(getLocalFilesWithSyncEnabledObservable(), getCloudOFilesObservable())
    }*/

    //TODO the implementation is up for discussion
    fun getSharedFiles(query: String? = null) {

    }

    /**
     * If user insist of syncing on the spot.
     */
    fun syncNow() {

    }

    /**
     * Saves the file, makes entry in database
     */
    fun openFile(id: String): Single<String> {
        //TODO
        return Single.just("")
    }


    /**
     * Saves the file, makes entry in database
     */
    fun saveFile(name: String, bytes: ByteArray): Completable =
        createSb3Entry(bytes, name)
            .map { makeSb3DatabaseEntry(it) }
            .doAfterSuccess {
                //TODO if (SPManager(application).isSb3FileSyncEnabled)
                //setWorkForSync()
            }
            .ignoreElement()

    fun saveFileAsCache(name: String, bytes: ByteArray): Completable =
        createSb3Entry(bytes, name)
            .map { makeSb3DatabaseEntry(it) }
            .doAfterSuccess {
                //TODO if (SPManager(application).isSb3FileSyncEnabled)
                //setWorkForSync()
            }
            .ignoreElement()

    /**
     * Returns local files with sync enabled
     */
/*    private fun getLocalFilesWithSyncEnabledObservable(): Observable<List<UserSb3File>>? {
        return sb3FilesDao.getCloudFileByUser(userId)
            .map { entities -> entities.map { sb3EntityToPojo(it) } }
    }*/

    /**
     * Returns cloud files above sync flag
     */
/*
    private fun getCloudOFilesObservable(): Observable<List<UserSb3File>> {
        return Observable.just(PictoBloxDatabase.getDatabase(application).userSyncIndexDao().getSb3SyncIndex(userId))
            .flatMap { syncIndex -> getCloudFilesByTimestamp(syncIndex.sb3SyncIndex) }
            .map { cloudEntriesToPojo(it) }
            .map {
                if (it.isNotEmpty())
                    setWorkForSync()

                it
            }
    }
*/

    //TODO, cant set listener like so
    private fun getCloudFilesByTimestamp(timestamp: Long) = Observable.create<QuerySnapshot> { emitter ->

        val registration = getCloudEntries(timestamp)
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException != null) {
                    emitter.onError(firebaseFirestoreException)

                    return@addSnapshotListener
                }

                if (querySnapshot != null) {
                    emitter.onNext(querySnapshot)
                }
            }

        emitter.setCancellable {
            registration.remove()
        }
    }

    /*private fun sb3EntityToPojo(entity: Sb3FileEntity): UserSb3File {

        val res = Sb3FileResource(null, null, entity.thumbPath, entity.fileName)

        return UserSb3File(entity.id, entity.fileName, res, SyncState.LOCAL_FILE)
    }*/

/*    private fun cloudEntriesToPojo(querySnapshot: QuerySnapshot): List<UserSb3File> {
        return querySnapshot.documents.filter {
            it.getString("status") == "ACTIVE"
        }
            .map {

                *//*
                 * Check if local entry exists, if exists and entry from cloud with higher sync flag also exist then local file is out dated.
                 *//*
                val sb3FileEntity = sb3FilesDao.getEntry(it.id)

                val fileRef = FirebaseStorage.getInstance()
                    .getReference("user_assets")
                    .child(userId)
                    .child("sb3_files")
                    .child("${it.id}.sb3")

                val thumbRef = FirebaseStorage.getInstance()
                    .getReference("user_assets")
                    .child(userId)
                    .child("sb3_files")
                    .child("${it.id}.png")

                val resources = Sb3FileResource(thumbRef, fileRef, sb3FileEntity?.thumbPath, sb3FileEntity?.filePath)

                UserSb3File(
                    it.id,
                    it.getString("origin") ?: "<No Name>",
                    resources,
                    if (sb3FileEntity != null) SyncState.CLOUD_FILE_OUTDATED else SyncState.CLOUD_FILE_SYNCED
                )

            }

    }*/

    //TODO can get away with collection>doc>collection
    private fun getCloudEntries(localSyncTimestamp: Long): Query {

        return FirebaseFirestore.getInstance()
            .collection("user_sb3_files")
            .document(userId)
            .collection("sb3_files")
            //.whereEqualTo("status", "ACTIVE")
            .whereGreaterThan("sync_timestamp", localSyncTimestamp)
    }

    private fun getUserDir(): File {
        val parentDir = File(application.filesDir, "user_files")
        return File(parentDir, userId).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    private fun createSb3Entry(bytes: ByteArray, name: String) = Single.create<Sb3FileEntity> { emitter ->

        PictobloxLogger.getInstance().logd("createSb3Entry $name")
        val uuid = UUID.randomUUID().toString()
        val file = File(getUserDir(), "${uuid}.sb3")
        val thumb = File(getUserDir(), "${uuid}.png")

        try {

            //save file
            FileOutputStream(file).use { os ->
                run {
                    os.write(bytes)
                }
            }

            //save thumb
/*
            getThumbFromSb3(file)?.also {
                FileOutputStream(thumb).use { os ->
                    run {
                        os.write(bytes)
                    }
                }
            }
*/

/*
            val sb3FileEntity = Sb3FileEntity(
                uuid,
                userId,
                name,
                Timestamp.now().seconds,
                0,
                0,
                "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
                file.absolutePath,
                thumb.absolutePath,
                0,
                0
            )

            emitter.onSuccess(sb3FileEntity)
*/

        } catch (e: java.lang.Exception) {
            emitter.onError(e)
        }
    }

    private fun makeSb3DatabaseEntry(sb3FileEntity: Sb3FileEntity) {
        PictobloxLogger.getInstance().logd("makeSb3DatabaseEntry :")
        PictoBloxDatabase.getDatabase(application)
            .sb3FilesDao()
            .insert(sb3FileEntity)
    }

    private fun setWorkForSync() {
        PictobloxLogger.getInstance().logd("setWorkForSync")
        /* val request = OneTimeWorkRequestBuilder<Sb3FileSyncWorker>()
             .setConstraints(
                 Constraints.Builder()
                     //.setRequiredNetworkType(NetworkType.UNMETERED)
                     .setRequiredNetworkType(NetworkType.CONNECTED)
                     .build()
             )
             .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 60, TimeUnit.SECONDS)
             .build()

         request.id


         WorkManager.getInstance(application)
             .enqueueUniqueWork("sb3Sync", ExistingWorkPolicy.KEEP, request)*/


        val userSyncIndexDao = PictoBloxDatabase.getDatabase(application).userSyncIndexDao()

        createWork(userId, application, userSyncIndexDao, sb3FilesDao)
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .ignoreElement()
            .subscribe()
    }

    /*
    / Gotta check the cloud here as well.
     */
/*    fun getUserFileEntry(id: String): UserSb3File? {
        val entry = sb3FilesDao.getEntry(userId)

        return entry?.let { sb3EntityToPojo(it) }
    }*/


    data class UserSb3File(
        val id: String,
        val name: String,
        val filesRef: Sb3FileResource,
        val syncState: SyncState
    )

    data class Sb3FileResource(
        val thumbCloudRef: StorageReference?,
        val fileCloudRef: StorageReference?,
        val thumbLocalRef: String?,
        val FileLocalRef: String?
    )

    enum class SyncState {
        CLOUD_FILE_SYNCED,
        CLOUD_FILE_OUTDATED,
        CLOUD_FILE_DELETED,
        CLOUD_FILE_SYNCING,
        LOCAL_FILE,
        LOCAL_FILE_DELETED

    }
}

