package io.stempedia.pictoblox.experimental

import android.app.Application
import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FileDownloadTask
import com.google.firebase.storage.FirebaseStorage
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableEmitter
import io.reactivex.rxjava3.core.ObservableOnSubscribe
import io.reactivex.rxjava3.functions.Cancellable
import io.stempedia.pictoblox.util.PictobloxLogger
import io.stempedia.pictoblox.util.SPManager
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class PictoBloxAIRepository(val applicationContext: Context) {

    //TODO
    val aiDir: File by lazy { File("") }

    private val spManager: SPManager by lazy { SPManager(applicationContext) }

    fun loadAIModel(model: String): Observable<Int> {
        return Observable.just(Tasks.await(getAiModelCloudVersion(model)))
            .flatMap { snapshot ->
                val cloudVersion = snapshot.get("version") as Long
                val localVersion = spManager.getVersionOfAIModel(model)

                if (cloudVersion > localVersion) {
                    Observable.create(AIModelObservableOnSubscribe(model))
                        .doOnComplete {
                            spManager.setVersionOfAIModel(model, cloudVersion)
                        }
                } else {
                    Observable.empty()
                }

            }
    }


    private fun getAiModelCloudVersion(model: String): Task<DocumentSnapshot> {

        return FirebaseFirestore.getInstance()
            .collection("static_docs")
            .document("ai_models")
            .collection("docs")
            .document(model)
            .get()
    }

    private inner class AIModelObservableOnSubscribe(val model: String) :
        ObservableOnSubscribe<Int>, Cancellable {
        private var fileDownloadTask: FileDownloadTask? = null
        private var isZipExtractionInProgress = false
        private var cancelZipExtraction = false

        override fun subscribe(emitter: ObservableEmitter<Int>) {
            emitter.setCancellable(this)
            val zipFile = File(aiDir, "data.zip")
            /*if (zipFile.exists()) {
                zipFile.delete()
            }*/

            //gs://pictobloxdev.appspot.com/pictoblox_ai_model_assets/customObjectDetection/data.zip
            //gs://pictobloxdev.appspot.com/pictoblox_ai_model_assets/customObjectDetection/data.zip
            fileDownloadTask = FirebaseStorage.getInstance()
                .getReference("pictoblox_ai_model_assets")
                .child(model)
                .child("data.zip")
                .getFile(zipFile)

            fileDownloadTask?.addOnSuccessListener {
                //just to be safe if rounding leave it at 99%
                emitter.onNext(100)
                extractAssetsFromZip(zipFile)

                emitter.onComplete()
            }

            fileDownloadTask?.addOnFailureListener {
                emitter.onError(it)
            }

            fileDownloadTask?.addOnProgressListener {
                val percentage = (it.bytesTransferred * 100.0f) / it.totalByteCount
                emitter.onNext(percentage.toInt())
            }
        }

        override fun cancel() {
            if (fileDownloadTask?.isInProgress == true) {
                fileDownloadTask?.cancel()

            } else if (isZipExtractionInProgress) {
                cancelZipExtraction = true
            }
        }

        private fun extractAssetsFromZip(zipFile: File) {
            isZipExtractionInProgress = true

            ZipInputStream(zipFile.inputStream()).use { inputStream ->
                var zipEntry = inputStream.nextEntry

                val buffer = ByteArray(1024)

                while (zipEntry != null && !cancelZipExtraction) {

                    PictobloxLogger.getInstance().logd("Unzip____ ${zipEntry.name}")

                    val entry = File(aiDir, zipEntry.name)

                    if (zipEntry.isDirectory) {

                        if (!entry.exists()) {
                            entry.mkdirs()
                        }

                    } else {
                        entry.parentFile?.also {
                            if (!it.exists())
                                it.mkdirs()
                        }

                        BufferedOutputStream(FileOutputStream(entry)).use {

                            var read = inputStream.read(buffer)
                            while (read != -1) {
                                it.write(buffer, 0, read)
                                read = inputStream.read(buffer)
                            }
                        }
                    }

                    inputStream.closeEntry()

                    zipEntry = inputStream.nextEntry
                }

            }

            isZipExtractionInProgress = false
        }
    }
}