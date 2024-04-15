package io.stempedia.pictoblox.experimental.workers

import android.content.Context
import android.net.Uri
import androidx.work.ListenableWorker
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableResult
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.gson.GsonBuilder
import io.reactivex.rxjava3.core.Single
import io.stempedia.pictoblox.experimental.db.files.Sb3FileEntity
import io.stempedia.pictoblox.experimental.db.files.Sb3FileUpdateEntity
import io.stempedia.pictoblox.experimental.db.files.Sb3FilesDao
import io.stempedia.pictoblox.experimental.db.files.UserSyncIndexDao
import io.stempedia.pictoblox.util.PictobloxLogger
import java.io.File

fun createWork(
    uid: String,
    applicationContext: Context,
    userSyncIndexDao: UserSyncIndexDao,
    sb3FilesDao: Sb3FilesDao
): Single<ListenableWorker.Result> = downwardSync(uid, applicationContext, sb3FilesDao, userSyncIndexDao)
    .flatMap { isSuccess ->
        if (isSuccess) {
            upwardSync(uid, sb3FilesDao)
        } else {
            Single.fromCallable { ListenableWorker.Result.retry() }
        }
    }


fun downwardSync(
    uid: String,
    applicationContext: Context,
    sb3FilesDao: Sb3FilesDao,
    syncIndexDao: UserSyncIndexDao
): Single<Boolean> {

    return Single.create { emitter ->

        var continueLoop = true

        emitter.setCancellable {
            continueLoop = false
        }

        val userDir = getUserFilesDir(applicationContext, uid)
        val localSyncIndex = syncIndexDao.getSb3SyncIndex(uid)

        val snapshot = Tasks.await(getPendingSyncFileFromCloud(uid, localSyncIndex.sb3SyncIndex))

        snapshot.documents.forEach { doc ->

            if (!continueLoop) {
                return@forEach
            }

            val status = doc.get("status") as String

            // files above local sync flag marked ACTIVE needs to be downloaded and local db needs to be updated
            if (status == "ACTIVE") {

                //Download the file and update local db
                val entity = createSb3Entity(uid, userDir, doc)
                sb3FilesDao.insert(entity)

                //update cloud flag for one more copy
                Tasks.await(createLocalCopyFlags(doc, uid))

                // files above local sync flag marked FLAGGED_FOR_DELETION needs to be deleted and local db needs to be updated
            } else if (status == "FLAGGED_FOR_DELETION") {

                //update cloud flag for deletion of the copy
                Tasks.await(createDeleteFlags(doc, uid))

                //local file deletion
                sb3FilesDao.getEntry(doc.id)?.also { entry ->

                    File(entry.filePath).delete()
                    File(entry.thumbPath).delete()

                    sb3FilesDao.deleteEntry(entry.id)
                }

            }

        }

        emitter.onSuccess(continueLoop)
    }
}

fun upwardSync(uid: String, sb3FilesDao: Sb3FilesDao) = Single.create<ListenableWorker.Result> { emitter ->

    var continueLoop = true

    emitter.setCancellable {
        continueLoop = false
    }

    var filesFromDB = sb3FilesDao.getLocalFilesToBeDeleted(uid)

    //Local files that are not synced but already flagged for deletion is deleted here without interacting with the cloud
    filesFromDB.forEach { sb3Entity ->

        if (!continueLoop) {
            return@forEach
        }
        File(sb3Entity.filePath).delete()
        File(sb3Entity.thumbPath).delete()
        sb3FilesDao.deleteEntry(sb3Entity.id)
    }

    val currentTimeStamp = Timestamp.now().seconds

    //Local files that needs to be synced to cloud
    filesFromDB = sb3FilesDao.getLocalFilesToBeSync(uid)

    filesFromDB.forEach { sb3Entity ->
        if (!continueLoop) {
            return@forEach
        }

        PictobloxLogger.getInstance().logd("upwardSync : ${sb3Entity.fileName}")

        val fileRef = getFileRef(uid, sb3Entity.id)
        val thumbRef = getThumbRef(uid, sb3Entity.id)

        //Get token///
        try {
            val token = Tasks.await(getCallableFunction(fileRef.path, thumbRef.path))

            val res = GsonBuilder().create().fromJson(token.data as String?, TokenResponse::class.java)
            PictobloxLogger.getInstance().logd("${res.status} : ${res.token} : ${res.error}")

            if (res.status == "success") {
                //Sign in with custom token
                Tasks.await(FirebaseAuth.getInstance().signInWithCustomToken(res.token!!))

                //upload file and thumb both
                Tasks.await(
                    Tasks.whenAll(
                        fileRef.putFile(Uri.fromFile(File(sb3Entity.filePath))),
                        thumbRef.putFile(Uri.fromFile(File(sb3Entity.thumbPath)))
                    )
                )

                //Create entry in cloud.
                Tasks.await(
                    FirebaseFirestore.getInstance()
                        .collection("user_sb3_files")
                        .document(uid)
                        .collection("sb3_files")
                        .document(sb3Entity.id)
                        .set(
                            mapOf(
                                "name" to sb3Entity.fileName,
                                "copies_present" to 1,
                                "created_date" to Timestamp(sb3Entity.createdDate, 0),
                                "origin" to sb3Entity.origin,
                                "status" to "ACTIVE",
                                "sync_index" to Timestamp(currentTimeStamp, 0)

                            )
                        )
                )

                //update local entry with sync stamp
                sb3FilesDao.update(Sb3FileUpdateEntity(sb3Entity.id, currentTimeStamp, 1))

            } else {
                continueLoop = false
                emitter.onSuccess(ListenableWorker.Result.retry())
            }

        } catch (e: Exception) {
            PictobloxLogger.getInstance().logException(e)
            //TODO the insufficient error will come here
            continueLoop = false
            emitter.onSuccess(ListenableWorker.Result.retry())
        }
    }

    //Synced files that is locally deleted.
    filesFromDB = sb3FilesDao.getSyncedToBeDeleted(uid)

    filesFromDB.forEach { sb3FileEntity ->
        if (!continueLoop) {
            return@forEach
        }

        try {
            Tasks.await(
                getCloudEntry(sb3FileEntity, uid)
                    .get(Source.SERVER)
                    .onSuccessTask {
                        markCloudFileForDeletion(it!!, uid, currentTimeStamp)
                    }
            )

            File(sb3FileEntity.filePath).delete()
            File(sb3FileEntity.thumbPath).delete()
            sb3FilesDao.deleteEntry(sb3FileEntity.id)

        } catch (e: Exception) {
            continueLoop = false
            emitter.onSuccess(ListenableWorker.Result.retry())
        }
    }

    //if everything went fine
    if (continueLoop) {
        PictobloxLogger.getInstance().logd("upwardSync : success")
        emitter.onSuccess(ListenableWorker.Result.success())
    }
}

private fun getCallableFunction(filePath: String, thumbPath: String): Task<HttpsCallableResult> {
    return FirebaseFunctions
        .getInstance("asia-east2")
        .getHttpsCallable("generateUploadToken")
        .call(mapOf("filePath" to filePath, "thumbPath" to thumbPath))
}

private fun getPendingSyncFileFromCloud(userId: String, localSyncTimestamp: Long): Task<QuerySnapshot> {
    return FirebaseFirestore.getInstance()
        .collection("user_sb3_files")
        .document(userId)
        .collection("sb3_files")
        .whereGreaterThanOrEqualTo("sync_timestamp", localSyncTimestamp)
        //.whereNotEqualTo("status", "DELETED")
        .orderBy("sync_timestamp", Query.Direction.ASCENDING)
        .get(Source.SERVER)
}

private fun getFileRef(uid: String, docId: String): StorageReference {
    return FirebaseStorage.getInstance()
        .getReference("user_assets")
        .child(uid)
        .child("sb3_files")
        .child("${docId}.sb3")

}

private fun getThumbRef(uid: String, docId: String): StorageReference {

    return FirebaseStorage.getInstance()
        .getReference("user_assets")
        .child(uid)
        .child("sb3_files")
        .child("${docId}.png")

}

private fun createSb3Entity(uid: String, userDir: File, doc: DocumentSnapshot): Sb3FileEntity {
    val fileRef = getFileRef(uid, doc.id)
    val thumbRef = getFileRef(uid, doc.id)

    val file = File(userDir, "${doc.id}.sb3")
    val thumb = File(userDir, "${doc.id}.png")

    Tasks.await(Tasks.whenAll(fileRef.getFile(file), thumbRef.getFile(thumb)))

    return Sb3FileEntity(
        doc.id,
        uid,
        doc.get("name") as String,
        doc.getTimestamp("created_date")!!.seconds,
        0,
        doc.getTimestamp("sync_index")!!.seconds,
        doc.get("origin") as String,
        file.absolutePath,
        thumb.absolutePath,
        1,
        1
    )
}

private fun createDeleteFlags(doc: DocumentSnapshot, uid: String): Task<Void> {


    val copies = doc.get("copies_present") as Long

    val flags = if (copies == 1.toLong()) {
        mapOf("copies_present" to 0, "status" to "DELETED")

        //in case copies > 1
    } else {
        mapOf("copies_present" to FieldValue.increment(-1))
    }

    return FirebaseFirestore.getInstance()
        .collection("user_sb3_files")
        .document(uid)
        .collection("sb3_files")
        .document(doc.id)
        .update(flags)

}

private fun createLocalCopyFlags(doc: DocumentSnapshot, uid: String): Task<Void> {

    return FirebaseFirestore.getInstance()
        .collection("user_sb3_files")
        .document(uid)
        .collection("sb3_files")
        .document(doc.id)
        .update(mapOf("copies_present" to FieldValue.increment(1)))

}

private fun markCloudFileForDeletion(doc: DocumentSnapshot, uid: String, updatedTimestamp: Long): Task<Void> {
    val copies = doc.get("copies_present") as Long

    val flags = if (copies == 1.toLong()) {
        mapOf("copies_present" to 0, "status" to "DELETED", "sync_index" to Timestamp(updatedTimestamp, 0))

        //in case copies > 1
    } else {
        mapOf("copies_present" to FieldValue.increment(-1), "status" to "FLAGGED_FOR_DELETION", "sync_index" to Timestamp(updatedTimestamp, 0))
    }

    return FirebaseFirestore.getInstance()
        .collection("user_sb3_files")
        .document(uid)
        .collection("sb3_files")
        .document(doc.id)
        .update(flags)
}

private fun getCloudEntry(sb3FileEntity: Sb3FileEntity, uid: String): DocumentReference {
    return FirebaseFirestore.getInstance()
        .collection("user_sb3_files")
        .document(uid)
        .collection("sb3_files")
        .document(sb3FileEntity.id)

}

private fun getUserFilesDir(applicationContext: Context, uid: String): File {
    val parentDir = File("${applicationContext.filesDir}", "user_files")
    val userDir = File(parentDir, uid)

    return File(userDir, "sb3_dir").apply {
        if (!exists()) {
            userDir.mkdirs()
        }
    }
}

class TokenResponse(val status: String?, val token: String?, val error: String?)