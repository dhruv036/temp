package io.stempedia.pictoblox.web

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FileDownloadTask
import com.google.firebase.storage.FirebaseStorage
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Cancellable
import io.reactivex.observers.DisposableObserver
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.connectivity.CommManagerServiceImpl
import io.stempedia.pictoblox.util.PictobloxLogger
import io.stempedia.pictoblox.util.SPManager
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class AIResourcesRetrievalViewModel(
    val activity: PictoBloxWebActivity,
    val commManagerServiceImpl: CommManagerServiceImpl,
    val activityVM: PictoBloxWebViewModelM2,
    val model: String
) {

    val processPercentage = ObservableInt()
    val isDownloadStarted = ObservableBoolean(false)
    val title = ObservableField<String>()

    //val buttonText = ObservableField<String>()
    val isError = ObservableBoolean(false)
    private var isCancelled = false
    private var disposable: Disposable? = null
    private val spManager = SPManager(activity)

    init {
        title.set("Loading AI model resources")
        // buttonText.set("Cancel")
        getCloudVersion()
    }

    fun onDispose() {
        isCancelled = true
        disposable?.dispose()
        commManagerServiceImpl.communicationHandler.apiFromPictobloxWeb.sendAiModelLoadingCanceled()
    }

    fun onIgnoreClick() {
        //Do nothing. This required so when user presses in the center white portion of the save dialog it does not close.
    }

    fun onActionButtonClicked() {
        /*if (isError.get()) {
            title.set("Retrying...")
            isError.set(false)
            //buttonText.set("Cancel")
            getCloudVersion()

        } else {
            isCancelled = true
            onDispose()
            activityVM.dismissLoadingAIDialog()
        }*/

        onDispose()
        activityVM.dismissLoadingAIDialog()
    }

    private fun getCloudVersion() {


        FirebaseFirestore.getInstance()
            .collection("static_docs")
            .document("ai_models")
            .collection("docs")
            .document(model)
            .get()
            .addOnFailureListener {
                isError.set(true)
                title.set("Error while retrieving data")
                //buttonText.set("Retry")

                commManagerServiceImpl.communicationHandler.apiFromPictobloxWeb.sendAiModelLoadingCanceled()
            }
            .addOnSuccessListener { snapshot ->
                if (isCancelled) {
                    return@addOnSuccessListener
                }

                if (!snapshot.exists()){
                    isError.set(true)
                    title.set("Model does not exist")
                    return@addOnSuccessListener
                }

                val cloudVersion = snapshot.get("version") as Long
                val localVersion = spManager.getVersionOfAIModel(model)

                if (cloudVersion > localVersion) {
                    title.set(activity.resources.getString(R.string.downloading_res))
                    isDownloadStarted.set(true)
                    downloadAndLoadModel(model, cloudVersion)
                } else {
                    commManagerServiceImpl.communicationHandler.loadAIModelFromStorage(model)
                    ///onDispose()
                    activityVM.dismissLoadingAIDialog()
                }

            }
    }

    private fun downloadAndLoadModel(model: String, cloudVersion: Long) {
        val aiDir = commManagerServiceImpl.communicationHandler.storageHandler.getAIModelFileDir()
        disposable =
            Observable.create(AIModelObservableOnSubscribe(model, aiDir))
                .subscribeWith(object : DisposableObserver<Int>() {
                    override fun onComplete() {
                        spManager.setVersionOfAIModel(model, cloudVersion)
                        commManagerServiceImpl.communicationHandler.loadAIModelFromStorage(model)
                        //onDispose()
                        activityVM.dismissLoadingAIDialog()
                    }

                    override fun onNext(t: Int) {
                        processPercentage.set(t)
                    }

                    override fun onError(e: Throwable) {
                        isError.set(true)
                        title.set("Error while retrieving data")
                        commManagerServiceImpl.communicationHandler.apiFromPictobloxWeb.sendAiModelLoadingCanceled()
                        //buttonText.set("Retry")
                    }
                })
    }

    class AIModelObservableOnSubscribe(val model: String, val aiDir: File) :
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