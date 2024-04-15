package io.stempedia.pictoblox.connectivity

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.stempedia.pictoblox.util.PictobloxLogger
import io.stempedia.pictoblox.util.SPManager
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

const val SB3_FILES_DIR = "sb3_files"
const val SB3_CACHE_DIR = "cached"

class StorageHandler(val context: Context, val spManager: SPManager) {
    val cachedFileName: String = "CachedSb3File.sb3"
    private val externalTempFileName: String = "ExternalSb3File.sb3"

    private var loadedFileType = StorageType.NONE
    private var sb3Dir: File = File(context.filesDir, SB3_FILES_DIR)
        .apply {
            if (!exists())
                mkdirs()
        }

    private var sb3CacheDir: File = File(context.filesDir, SB3_CACHE_DIR)
        .apply {
            if (!exists())
                mkdirs()
        }

    private var courseDir: File = File(context.filesDir, "courses")
        .apply {
            if (!exists())
                mkdirs()
        }

    private var sharedSessionDir: File = File(context.filesDir, "sessions")
        .apply {
            if (!exists())
                mkdirs()
        }

    private var essentialFilesDir: File = File(context.filesDir, "essentials")
        .apply {
            if (!exists())
                mkdirs()
        }

    private var examplesFilesDir: File = File(context.filesDir, "examples")
        .apply {
            if (!exists())
                mkdirs()
        }

    private var aiModelsFilesDir: File = File(context.filesDir, "aiModels")
        .apply {
            if (!exists())
                mkdirs()
        }

    private var mlFolderDir: File = File(context.filesDir,"mlfolder")
        .apply {
            if (!exists())
                mkdirs()
        }

    private var popUpsFilesDir: File = File(context.filesDir, "popups")
        .apply {
            if (!exists()){
                mkdirs()
            }
        }

    /*private var savingFileType = StorageType.NONE
    private var savingFileName = ""*/
    var openingFileBas64: String? = null
    var openingFileName = ""
    //var openingCourseJson = ""
    //var previousAttempts = 0

    fun getFileType() = loadedFileType

    fun getCourseDir() = courseDir

    fun getSharedSessionDir() = sharedSessionDir

    fun getEssentialFilesDir() = essentialFilesDir

    fun popUpsFilesDir() = popUpsFilesDir

    fun getAIModelFileDir() = aiModelsFilesDir

    fun getSb3FileDir() = sb3Dir


    fun getMLFolder() = mlFolderDir

    fun getSb3CacheFileDir() = sb3CacheDir

    fun getExampleFilesDir() = examplesFilesDir

    fun listLocalFiles(): Array<File> {
        return sb3Dir.listFiles() ?: emptyArray()
    }

    fun loadEmptyProject(): Completable {
        return Completable.create {
            Log.e("TAG", "loadEmptyProject: ", )
            loadedFileType = StorageType.NONE
            openingFileBas64 = null
            openingFileName = ""
            it.onComplete()
        }
    }

    fun isCachedVersionExists(): Boolean {
        return File(sb3CacheDir, cachedFileName).exists()
    }

    fun loadCachedProject(): Single<String> {
        PictobloxLogger.getInstance().logd("loadCachedProject")
        return loadInternalFile(File(sb3CacheDir, cachedFileName))
            .map {
               /* convertSb3ToBase64String(it)*/
                ""
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess {
                loadedFileType = StorageType.CACHE
                openingFileBas64 = it
                openingFileName = cachedFileName
            }
    }

    fun loadInternalProject(internalFile: File): Single<String> {
        PictobloxLogger.getInstance().logd("loadExampleProject $internalFile")
        return loadInternalFile(internalFile)
            .map {
                /*convertSb3ToBase64String(it)*/
                ""
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess {
                loadedFileType = StorageType.INTERNAL
                openingFileName = internalFile.name
                openingFileBas64 = it
            }
    }

    fun loadExternalProject(externalFileUri: Uri): Single<String> {
        return loadExternalFile(externalFileUri)
            .map {
                verifySb3File(it)
            }
            .map {
                openingFileName = it.first
                /*convertSb3ToBase64String(it.second)*/
                ""
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess {
                loadedFileType = StorageType.EXTERNAL
                openingFileBas64 = it
            }
    }

    fun loadDeepLinkProject(externalFileUri: Uri): Single<String> {
        return loadDeepLinkFile(externalFileUri)
            .map {
                verifySb3File(it)
            }
            .map {
                openingFileName = it.first
                /*convertSb3ToBase64String(it.second)*/
                ""
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess {
                loadedFileType = StorageType.EXTERNAL
                openingFileBas64 = it
            }
    }

    fun loadExampleProject(id: String, fileName: String): Single<String> {
        PictobloxLogger.getInstance().logd("loadExampleProject $id $fileName")
        return loadInternalFile(File(examplesFilesDir, "${id}code.sb3"))
            .map {
                /*convertSb3ToBase64String(it)*/
                ""
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess {
                Log.e("example file in string", " $it", )
                loadedFileType = StorageType.EXAMPLE
                openingFileBas64 = it
                openingFileName = id+"code.sb3"
            }
    }

    fun loadAiModelDir(model: String): Single<String> {
        PictobloxLogger.getInstance().logd("loadAiModelDir ${model}")
        return loadInternalFile(File(aiModelsFilesDir, model))
            .map { it.absolutePath }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

    }

    fun clear(): Completable {
        return Completable.create {
            loadedFileType = StorageType.NONE
            openingFileBas64 = null
            it.onComplete()
        }
    }

    fun createExampleFile(id: String): Single<File> {
        val fileName = "${id}code.sb3"
        return getFile(fileName, StorageType.EXAMPLE)
    }

    fun createImageFile(id : String): File{
        val filname = "${id}.jpeg"
        return getFile(filname,StorageType.POPUPS).blockingGet()
    }

    fun createAiModelDir(model: String): Single<File> {
        return getFile(model, StorageType.AI_MODELS)
    }

    fun copyCacheFileToInternalFile() {
        PictobloxLogger.getInstance().logd("copyCacheFileToInternalFile")
        val cachedFile = File(sb3CacheDir, cachedFileName)
        val sb3File = File(sb3Dir, "ValidationError.sb3")

        cachedFile.inputStream().use {
            FileOutputStream(sb3File).use { out ->
                run {

                    PictobloxLogger.getInstance().logd("##COPY 256")
                    val array = ByteArray(256)

                    var length = it.read(array)

                    while (length > 0) {
                        out.write(array, 0, length)
                        length = it.read(array)
                    }
                }
            }
        }
        PictobloxLogger.getInstance().logd("copyCacheFileToInternalFile Done")
    }

    /*fun saveProject(byteArray: ByteArray, fileName: String, storageType: StorageType): Completable {
        return saveFileInternal(byteArray, fileName, storageType)
            .doOnComplete {
                if (storageType == StorageType.CACHE) {
                    //Whatever file was open when we cached the file, we will save its name so user can easily identify what cached file it has opened
                    spManager.cachedProjectName = openingFileName
                }
            }
    }*/

    fun saveProject(byteArray: ByteArray, fileName: String, storageType: StorageType): Completable {
        return getFile(fileName, storageType)
            .flatMapCompletable { saveFile(it, byteArray) }
            .doOnComplete {
                PictobloxLogger.getInstance().logd("saveProject doOnComplete $storageType")

                if (storageType == StorageType.CACHE) {
                    //Whatever file was open when we cached the file, we will save its name so user can easily identify what cached file it has opened
                    spManager.cachedProjectName = openingFileName
                }
                openingFileName = fileName
                PictobloxLogger.getInstance().logd("saveProject COMPLETE")
            }
    }

    fun saveFile(file: File, byteArray: ByteArray): Completable {
        PictobloxLogger.getInstance().logd("saveFile ${file.absolutePath}")
        return Completable.create {

            try {

                FileOutputStream(file).use { os ->
                    run {
                        os.write(byteArray)
                    }
                }

                it.onComplete()

            } catch (e: java.lang.Exception) {
                it.onError(e)
            }

        }
    }

    fun isFileExists(fileName: String): Completable {
        return Completable.create {
            if (File(sb3Dir, fileName).exists()) {
                it.onComplete()
            } else {
                it.onError(Exception("File does not already exists"))
            }
        }
    }

    /*
    *
    *
    *
    *
    *
    *
     */

    fun deleteFileIfExist(file : File){
        if (file.exists()) file.delete()
    }


    private fun saveFileInternal(byteArray: ByteArray, fileName: String, storageType: StorageType): Completable {
        return Completable.create {

            try {

                val file = when (storageType) {
                    StorageType.CACHE -> {
                        File(sb3CacheDir, cachedFileName)
                    }

                    StorageType.INTERNAL -> {
                        File(sb3Dir, fileName)
                    }

                    StorageType.EXTERNAL -> {
                        File(sb3Dir, fileName)
                    }
                    else -> {
                        File(sb3CacheDir, cachedFileName)
                    }
                }

                if (file.exists()) {
                    file.delete()
                }

                FileOutputStream(file).use { os ->
                    run {
                        os.write(byteArray)
                    }
                }

                it.onComplete()

            } catch (e: java.lang.Exception) {
                it.onError(e)
            }

        }
    }

    private fun getFile(fileName: String, storageType: StorageType): Single<File> {
        PictobloxLogger.getInstance().logd("Get file $fileName $storageType")
        return Single.create { emitter ->
            PictobloxLogger.getInstance().logd("Get file 1")

            val file = when (storageType) {

                StorageType.CACHE -> {
                    File(sb3CacheDir, cachedFileName)
                }

                StorageType.INTERNAL -> {
                    PictobloxLogger.getInstance().logd("Get file StorageType INTERNAL")

                    File(sb3Dir, fileName)
                }

                StorageType.EXTERNAL -> {
                    File(sb3Dir, fileName)
                }

                StorageType.SHARED_SESSION -> {
                    File(sharedSessionDir, fileName)
                }

                StorageType.EXAMPLE -> {
                    File(examplesFilesDir, fileName)
                }

                StorageType.AI_MODELS -> {
                    File(aiModelsFilesDir, fileName)
                }

                StorageType.POPUPS -> {
                    File(popUpsFilesDir, fileName)
                }

                else -> {
                    File(sb3CacheDir, cachedFileName)
                }
            }

            if (file.exists()) {
                file.delete()
                PictobloxLogger.getInstance().logd("Get file delete")
            }

            emitter.onSuccess(file)
        }
    }

    private fun loadInternalFile(file: File): Single<File> {
        PictobloxLogger.getInstance().logd("loadInternalFile ${file.absolutePath}")
        return Single.create {

            if (file.exists()) {
                it.onSuccess(file)

            } else {
                it.onError(FileNotFoundException("Find not found"))

            }
        }
    }

    private fun loadExternalFile(uri: Uri): Single<Pair<String, File>> {

        return Single.create {
            val type = context.contentResolver.getType(uri)
            PictobloxLogger.getInstance().logd("Mime type : $type")

            try {
                //var size: Int
                val displayName = getFileName(uri)

                if (TextUtils.isEmpty(displayName)) {
                    it.onError(Exception("No file name present"))
                    return@create
                }

                val file = File(sb3Dir, displayName!!)

                copyToCachedFile(uri, file)

                it.onSuccess(Pair(displayName, file))

            } catch (e: Exception) {
                it.onError(e)
            }
        }
    }

    private fun loadDeepLinkFile(uri: Uri): Single<Pair<String, File>> {

        return Single.create { emitter ->

            try {

                val url = URL(uri.toString())
                val httpCon = url.openConnection() as HttpURLConnection

                if (httpCon.responseCode != 200) {
                    // error handle here!;
                    emitter.onError(Exception("invalid url"))
                    return@create
                }

                val raw: String = httpCon.getHeaderField("Content-Disposition")
                val fileName = if (raw != null && raw.indexOf("=") != -1) {
                    val f1 = raw.split("=".toRegex()).toTypedArray()[1]

                    when {
                        f1.startsWith("utf-8''") -> {
                            f1.removePrefix("utf-8''")
                        }
                        f1.startsWith("utf-8") -> {
                            f1.removePrefix("utf-8''")
                        }
                        else -> {
                            f1
                        }
                    }

                } else {
                    "Link${System.currentTimeMillis()}.sb3"
                }

                val remoteStream = httpCon.inputStream


                //val file = File(sb3CacheDir, externalTempFileName)

                //We will use appassets.androidplatform.net to host the files using the Web assets loader.
                //val assetUri = "https://appassets.androidplatform.net/ai/$model"
                val file = File(sb3Dir, fileName)


                if (file.exists()) {
                    file.delete()
                }

                FileOutputStream(file).use { out ->
                    run {

                        val array = ByteArray(256)

                        var length = remoteStream.read(array)

                        while (length > 0) {
                            out.write(array, 0, length)
                            length = remoteStream.read(array)
                        }
                    }
                }

                emitter.onSuccess(Pair(fileName, file))

            } catch (e: Exception) {
                emitter.onError(e)
            }


        }
    }

    private fun getFileName(uri: Uri): String? {

        var displayName: String? = null

        val cursor = context.contentResolver.query(
            uri,
            null,
            null,
            null,
            null
        )

        cursor?.apply {

            if (moveToFirst()) {
                displayName = getString(getColumnIndex(OpenableColumns.DISPLAY_NAME))

                PictobloxLogger.getInstance().logd("Display name : $displayName")

                val sizeIndex: Int = getColumnIndex(OpenableColumns.SIZE)

                val size = if (!isNull(sizeIndex)) {
                    // Technically the column stores an int, but cursor.getString()
                    // will do the conversion automatically.
                    getString(sizeIndex).toInt()
                } else {
                    -1
                }

                PictobloxLogger.getInstance().logd("Size : $size")

            }

            close()
        }

        return displayName

    }

    private fun copyToCachedFile(uri: Uri, file: File) {

        context.contentResolver.openInputStream(uri)?.use {

            if (file.exists()) {
                file.delete()
            }

            FileOutputStream(file).use { out ->
                run {

                    val array = ByteArray(256)

                    var length = it.read(array)

                    while (length > 0) {
                        out.write(array, 0, length)
                        length = it.read(array)
                    }
                }
            }
        }
    }

    private fun convertSb3ToBase64String2(sb3: File): String {
        val inputString = FileInputStream(sb3)
        val bytes = inputString.readBytes()
        inputString.close()

        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun verifySb3File(pair: Pair<String, File>): Pair<String, File> {

        val inputStream = ZipInputStream(pair.second.inputStream())

        var zipEntry = inputStream.nextEntry

        var isValidEntry = false

        while (zipEntry != null) {

            PictobloxLogger.getInstance().logd("Extracting : ${zipEntry.name}")

            if (!zipEntry.isDirectory && zipEntry.name == "project.json") {
                isValidEntry = true
            }
            inputStream.closeEntry()

            zipEntry = inputStream.nextEntry
        }

        inputStream.close()

        if (isValidEntry) {
            return pair

        } else {
            throw java.lang.Exception("${pair.first} is not a valid file.")
        }

    }
}

enum class StorageType {
    NONE,
    CACHE,
    INTERNAL,
    EXTERNAL,
    SHARED_SESSION,
    EXAMPLE,
    AI_MODELS,
    POPUPS,
    ML
}