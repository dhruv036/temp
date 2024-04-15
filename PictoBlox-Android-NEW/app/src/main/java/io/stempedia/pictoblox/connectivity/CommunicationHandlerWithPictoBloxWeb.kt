package io.stempedia.pictoblox.connectivity

import android.Manifest
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.StatFs
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.webkit.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableCompletableObserver
import io.reactivex.schedulers.Schedulers
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.firebase.CourseFlow
import io.stempedia.pictoblox.util.PictoBloxAnalyticsEventLogger
import io.stempedia.pictoblox.util.PictobloxLogger
import io.stempedia.pictoblox.util.SPManager
import org.json.JSONArray
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.util.Locale
import java.util.zip.ZipInputStream


//TODO prevent reinitialization when startservice called multiple times

class CommunicationHandlerWithPictoBloxWeb(
    private val handler: Handler,
    private val commManagerServiceImpl: CommManagerServiceImpl
) {
    private var webView: WebView? = null
    private val apisForPictoblox = APIsForPictoBlox()
    val apiFromPictobloxWeb = APIsFromPictoBlox()
    private val spManager = SPManager(commManagerServiceImpl)
    private var selectedBoard: Board? = null
    private var pictoBloxCallbacks: PictoBloxCallbacks? = null
    //private var fileOperationCallbacks: FileOperationCallbacks? = null
    //private var targetCallbacks: TargetCallbacks? = null

    private val gson = GsonBuilder().create()
    private val dp48 = convertDpToPixel(28f, commManagerServiceImpl)
    private val spriteResourceDir = "build/static/scratch/"
    private val tempResourceDir = "temp_resources/"
    private val defaultBackDropMD5 = "cd21514d0531fdffb22204e0ec5ed84a.svg"
    private val indexPath = "file:///android_asset/build/index.html"
    private val indexPath2 = "https://appassets.androidplatform.net/assets/build/index.html"

    //https://appassets.androidplatform.net/assets/user_files/hanged (1).sb3
    //private val indexPath = "https://5f320fb8.ngrok.io/"
    private val indexPathExternal =
        "file://${commManagerServiceImpl.cacheDir}/pictoDir/pictoBloxUnzipped/build/index.html"
    private val indexPathExternal2 =
        "https://appassets.androidplatform.net/external_picto/build/index.html"

    private val compositeDisposable = CompositeDisposable()

    val storageHandler = StorageHandler(commManagerServiceImpl, spManager)

    //We have to have this here as well because saving is async and when webview callback comes we need to know what kind of file we are saving here
    var storageType = StorageType.NONE
    val soundHandler = SoundHandler(commManagerServiceImpl)

    //26 June 2020, This is a patch applied after discussion with brijesh. We only need callback once pictoblox is initialised but the 'onProjectLoaded()' is getting called multiple times.
    private var isWaitingForPictobloxToBeReady = false

    var extensiontoLoad = mutableMapOf<String, String>()
    private var retryAttempts = 0
    private var openingCourseJson = ""
    private var courseFlow: CourseFlow? = null

    private var functionType = PictoWebFunctions.OPEN_PROJECT

    private var sharedSessionDocReference: DocumentReference? = null

    private var sharedSessionCallbacks: SharedSessionCallbacks? = null

    //For camera permission
    private var mMediaPermissionRequest: PermissionRequest? = null
    private var mGeoLocationCallback: GeolocationPermissions.Callback? = null
    private var mGeoLocationOrigin: String? = null

    val permissionPendingList = mutableListOf<String>()
    val permissionGivenList = mutableListOf<String>()

    private var isWaitingForFirmwareQuery = false

    private var userAccountStatus = ""
    private var userCredits = ""
    private var cacheName = ""

    var payloadData = MutableLiveData<Map<String, String>>()
    private val requestList = mutableMapOf<String, String>()
    var isOTGConnected = false
    lateinit var data: MutableMap<String, String>

    enum class PermissionTypes(s: String) {
        CAMERA_PERMISSION("CAMERA_PERMISSION"),
        IMAGE_PERMISSION("IMAGE_PERMISSION"),
        AUDIO_PERMISSION("AUDIO_PERMISSION"),
        BLUETOOTH_PERMISSION("BLUETOOTH_PERMISSION")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun testMlFunction() {
//        apisForPictoblox.sendGetDiskSpace(30)
//        apisForPictoblox.sendIsFileExist(30,"/eighth/seventh")
//        apisForPictoblox.sendIsFileExist(30,"/eighth/seventjh")
//        apisForPictoblox.sendIsFileExist(30,"/eighth/seventh/Dhruv_Resume_.pdf")
//        apisForPictoblox.sendIsFileExist(30,"/eighth/seventh/Dhrudv_Resume_.pdf")
//        apisForPictoblox.sendMakeFileDirectory(53,"eighth/seventh")
//        apisForPictoblox.sendMakeFileDirectory(53,"eighth/seventash")
//        apisForPictoblox.sendIsFileExist(1, "/Dhruv_Resume_.pdf/")
//        apisForPictoblox.sendStatSync(2,"/Dhruv_Resume_.pdf/")
        apisForPictoblox.sendGetDiskSpace(9)
//        apisForPictoblox.sendReadDir(3,"/eighth/seventh/thired")
//        apisForPictoblox.sendCopyFile(30,"/eighth/fourth/","/thired/hello.sb3")
//        apisForPictoblox.sendUnzipFile(4,"/thired/darlington-font.zip","/seventh/thired","demo") // create seventh folder if not present
//        apisForPictoblox.sendCopyFile(5,  "/thired/","/eighth/seventh/")
//        apisForPictoblox.sendWriteFile(30,"JEKSD", byteArrayOf(),"thired/")
//        apisForPictoblox.sendRenameFile(30,"/first/","eighth")
//        apisForPictoblox.sendRenameFile(30,"/eighth/fourth/memo.png","mimi.png")
//        apisForPictoblox.sendRemoveFile(30,"first/fourth/sixth/Dhruv_Sharma_Android.pdf")
//        apisForPictoblox.sendRemoveFile(30, "first/fourth/archi.zip")
//        apisForPictoblox.sendRemoveFile(70, "first/fourth/")
//        apisForPictoblox.sendRemoveFile(30, "second/")
//        apisForPictoblox.sendRemoveFile(30,"/hj")
//        apisForPictoblox.sendRemoveFile(30,"/hj")
    }

    /*
    *
    * APIs
     * ******************************************************************************************
     */

    fun setWebView(webView: WebView?) {
        this.webView = webView
        webView?.webChromeClient = PictobloxChromeClient()
        webView?.addJavascriptInterface(apisForPictoblox, "Android")
        PictobloxLogger.getInstance().logd("Android interface loaded")
//        webView?.isHorizontalScrollBarEnabled = true
//        webView?.isVerticalScrollBarEnabled = true

        //? to load url
        if (spManager.isExternalPictoBloxEnabled) {
            webView?.loadUrl(indexPathExternal2)
            Log.d("WHICH", "EXTERNAL ")
        } else {
            webView?.loadUrl(indexPath2)
            Log.d("WHICH", "INTERNAL ")
        }
        if (webView != null) {
            cacheName = webView.context.getString(R.string.project_cached)
        }
        testMlFunction()
        // to open ml file
        unzipFile(
            File(storageHandler.getSb3FileDir(), storageHandler.openingFileName),
            File(storageHandler.getAIModelFileDir(), "mlTemp"),
            0
        )
    }

    fun clearWebViewReference() {
        this.webView = null
    }


    fun setPictobloxCallbacks(pictoBloxCallbacks: PictoBloxCallbacks?) {
        this.pictoBloxCallbacks = pictoBloxCallbacks
    }

    fun onDestroy() {
        data = mutableMapOf()
        isOTGConnected = false
        compositeDisposable.dispose()
    }

    fun getWebView(): WebView? {
        return webView
    }

    fun cacheCurrentWorkIfApplicable() {
        if (functionType == PictoWebFunctions.OPEN_PROJECT) {
            storageType = StorageType.CACHE

            apiFromPictobloxWeb.saveProject(storageHandler.cachedFileName)
        }
    }

    /**
     * @param fileName Must end with .sb3.
     */
    fun saveCurrentWork(fileName: String) {
        // first
        storageType = StorageType.INTERNAL
        //fileOperationCallbacks?.savingFile()
        apiFromPictobloxWeb.saveProject(fileName)
    }

    fun saveCacheWork(fileName: String) {
        Log.e("TAG", "saveCurrentWork: ")
        storageType = StorageType.CACHE
        //fileOperationCallbacks?.savingFile()
        apiFromPictobloxWeb.saveProject(fileName)
    }

    fun setBoardSelected(board: Board, projectSelected: Boolean) {
        selectedBoard = board
        spManager.selectedBoard = board.stringValue
        apiFromPictobloxWeb.boardSelected(board, projectSelected)
    }

    fun getSelectedBoard(): Board? {
        return selectedBoard
    }

    fun getAllFrame(frame: String?) {
        apiFromPictobloxWeb.setFrame(frame)
    }

    fun openProject() {

        storageHandler.openingFileBas64?.apply {
            /*apiFromPictobloxWeb.openProject(
                storageHandler.openingFileName,
                this
            )*/
            val projectPath = when (storageHandler.getFileType()) {
                StorageType.EXAMPLE -> {
                    "example_projects"
                }

                StorageType.CACHE -> {
                    "cached_projects"
                }

                else -> {
                    "user_projects"
                }
            }
            apiFromPictobloxWeb.openProjectWithWebAssetLoader(
                storageHandler.openingFileName,
                projectPath
            )
        } ?: run {
            Log.e(
                "title",
                "openproject ${storageHandler.openingFileName} --- ${storageHandler.getFileType()} "
            )
            apiFromPictobloxWeb.openNewProject()
        }

        if (functionType == PictoWebFunctions.OPEN_COURSE) {
            apiFromPictobloxWeb.openCourse(openingCourseJson)

        } else if (functionType == PictoWebFunctions.OPEN_GUIDE_TOUR) {
            apiFromPictobloxWeb.startTourSession()
        }
    }

    fun setStartSequence(userId: String?) {
        PictobloxLogger.getInstance().logd("setStartSequence called")
        apiFromPictobloxWeb.apply {
            this.setStart()
            this.setUserId(userId)
            if (commManagerServiceImpl.resources.getBoolean(R.bool.isTablet)) {
                this.isTabletMode(true)
            }
            this.setLocale(spManager.pictobloxLocale)
        }

        if (!TextUtils.isEmpty(userCredits)) {
            apiFromPictobloxWeb.onCreditsUpdate(userCredits)
        }

        if (!TextUtils.isEmpty(userAccountStatus)) {
            apiFromPictobloxWeb.onUserStateUpdated(userAccountStatus)
        }

        apiFromPictobloxWeb.setWebviewSharedPreferenceJson(spManager.pictobloxWebviewPreferenceJson)
        notifyPictobloxOnBluetoothConnectivity()

        //openProject()
    }

    fun checkSystemLanguage(): String {
        val sysLang = Locale.getDefault().getLanguage()
        Log.e("lange", "${sysLang}")
        var lang = PictoBloxWebLocale.values().find {
            it.code.equals(sysLang)
        }
        lang = if (lang != null) lang else PictoBloxWebLocale.ENGLISH
        return lang.code
    }

    fun updateUserCredit(credit: Long) {
        if (isWaitingForPictobloxToBeReady) {
            userCredits = credit.toString()
        } else {
            apiFromPictobloxWeb.onCreditsUpdate(credit.toString())
        }
    }

    fun updateUserAccountStatus(state: String) {
        if (isWaitingForPictobloxToBeReady) {
            userAccountStatus = state

        } else {
            apiFromPictobloxWeb.onUserStateUpdated(state)
        }
    }

    fun setStopSequence() {
        PictobloxLogger.getInstance().logd("setStopSequence called")
        apiFromPictobloxWeb.setStop()
    }

    fun openProjectOncePictobloxIsReady() {
        isWaitingForPictobloxToBeReady = true
    }

    fun processFrame(byteArray: ByteArray) {
        when (byteArray[3].toInt()) {
            EviveProtocolResponseType.TYPE_BYTE -> {
                PictobloxLogger.getInstance().logd("TYPE_BYTE ${byteArray[4]}")
                handler.post {
                    apiFromPictobloxWeb.responseFromHardware(
                        byteArray[4].toString(),
                        false
                    )
                }

            }

            EviveProtocolResponseType.TYPE_FLOAT -> {
                val buffer = ByteBuffer.wrap(byteArray.copyOfRange(4, 8))
                buffer.order(ByteOrder.LITTLE_ENDIAN)

                val value = buffer.float
                PictobloxLogger.getInstance().logd("TYPE_FLOAT $value")
                handler.post { apiFromPictobloxWeb.responseFromHardware(value.toString(), false) }
            }

            EviveProtocolResponseType.TYPE_SHORT -> {
                val buffer = ByteBuffer.wrap(byteArray.copyOfRange(4, 6))
                buffer.order(ByteOrder.LITTLE_ENDIAN)
                val value = buffer.short
                PictobloxLogger.getInstance().logd("TYPE_SHORT $value")
                handler.post { apiFromPictobloxWeb.responseFromHardware(value.toString(), false) }
            }

            EviveProtocolResponseType.TYPE_STRING -> {

                val str = String(byteArray.copyOfRange(5, 5 + byteArray[4].toInt()))

                handler.post {
                    if (isWaitingForFirmwareQuery) {
                        isWaitingForFirmwareQuery = false
                        apiFromPictobloxWeb.responseForFirmware(str)
                        val firmware = str
                    } else {
                        apiFromPictobloxWeb.responseFromHardwareForStringValue(str, false)

                    }

                }
                PictobloxLogger.getInstance()
                    .logd(
                        "TYPE_STRING ${
                            String(
                                byteArray.copyOfRange(
                                    5,
                                    5 + byteArray[4].toInt()
                                )
                            )
                        }"
                    )
            }

            EviveProtocolResponseType.TYPE_DOUBLE -> {
                val buffer = ByteBuffer.wrap(byteArray.copyOfRange(4, 12))
                buffer.order(ByteOrder.LITTLE_ENDIAN)
                val value = buffer.double
                PictobloxLogger.getInstance().logd("TYPE_DOUBLE $value")
                handler.post { apiFromPictobloxWeb.responseFromHardware(value.toString(), false) }
            }

            EviveProtocolResponseType.TYPE_INT -> {
                val buffer = ByteBuffer.wrap(byteArray.copyOfRange(4, 8))
                buffer.order(ByteOrder.LITTLE_ENDIAN)
                val value = buffer.int
                PictobloxLogger.getInstance().logd("TYPE_INT $value")
                handler.post { apiFromPictobloxWeb.responseFromHardware(value.toString(), false) }
            }
            //according to mimansa old_fw 12 bytes 5OCT21
            EviveProtocolResponseType.TYPE_SENSOR_OLD_FW -> {
                val sensorData = byteArray.copyOfRange(4, 16)
                val obj = SensorData(sensorData)
                val objJson = GsonBuilder().create().toJson(obj)
                val objJson2 = GsonBuilder().create().toJson(obj).toString()
                PictobloxLogger.getInstance().logd("$objJson")
                PictobloxLogger.getInstance().logd("$objJson2")
                handler.post { apiFromPictobloxWeb.responseFromHardware(objJson, true) }
            }

            //according to mimansa new_fw 16 bytes 5OCT21
            EviveProtocolResponseType.TYPE_SENSOR_NEW_FW -> {
                val sensorData = byteArray.copyOfRange(4, 20)
                val obj = SensorData(sensorData)
                val objJson = GsonBuilder().create().toJson(obj)
                val objJson2 = GsonBuilder().create().toJson(obj).toString()
                PictobloxLogger.getInstance().logd("$objJson")
                PictobloxLogger.getInstance().logd("$objJson2")
                handler.post { apiFromPictobloxWeb.responseFromHardware(objJson, true) }
            }

            //String(01001001010110)
            //4 5 6 7 8 9 10 11 12 13 14
        }

    }

    fun loadCachedProject(): Completable {
        return storageHandler.loadCachedProject()
            .doOnSuccess {
                functionType = PictoWebFunctions.OPEN_PROJECT
            }.ignoreElement()

    }

    fun loadEmptyProject(): Completable {
        return storageHandler.loadEmptyProject()
            .doOnComplete {
                functionType = PictoWebFunctions.OPEN_PROJECT
            }
    }

    fun loadInternalProject(file: File): Completable {
        return storageHandler.loadInternalProject(file)
            .doOnSuccess {
                functionType = PictoWebFunctions.OPEN_PROJECT
            }.ignoreElement()
    }

    fun loadCourse(
        startingFile: File,
        courseFlow: CourseFlow,
        attempts: Int,
        json: String
    ): Completable {
        return storageHandler.loadInternalProject(startingFile)
            .doOnSuccess {
                functionType = PictoWebFunctions.OPEN_COURSE
                this.courseFlow = courseFlow
                this.retryAttempts = attempts
                this.openingCourseJson = json
            }.ignoreElement()
    }

    fun loadCompletedLessonProject(endFile: File): Completable {
        return storageHandler.loadInternalProject(endFile)
            .doOnSuccess {
                functionType = PictoWebFunctions.OPEN_COURSE_PROJECT
            }.ignoreElement()
    }

    fun loadExternalProject(uri: Uri): Completable {
        return storageHandler.loadExternalProject(uri)
            .doOnSuccess {
                functionType = PictoWebFunctions.OPEN_PROJECT

            }.ignoreElement()
    }

    fun loadDeepLink(uri: Uri): Single<String> {
        return storageHandler.loadDeepLinkProject(uri)
            .doOnSuccess {
                functionType = PictoWebFunctions.OPEN_PROJECT

            }
    }

    fun loadTour(): Completable {
        return storageHandler.clear()
            .doOnComplete {
                functionType = PictoWebFunctions.OPEN_GUIDE_TOUR
            }
    }

    fun readFile() {
        val zipFile = File(storageHandler.getMLFolder(), "/")
        if (zipFile.exists() && zipFile.isDirectory) {
            val files = zipFile.listFiles()
            for (file in files) {
                Log.e("readFile", "readFile: ${file.name}")
            }
        }
    }

    fun unzipFile(fileToUnzip: File, targetFile: File, option: Int) {
        try {
            var zipfile = fileToUnzip
            if (option == 0) {
                zipfile = fileToUnzip
            } else {
                zipfile =
                    File(storageHandler.getMLFolder().path + fileToUnzip.parent, fileToUnzip.name)
            }
//        Log.e("TAG", "unzipFile: ${zipfile.absolutePath}", )
//        Log.e("TAG", "unzipFile: ${zipfile.name}", )
//        Log.e("TAG", "unzipFile: ${zipfile.parent}", )


            val inputStream = ZipInputStream(zipfile.inputStream())
            var zipEntry = inputStream.nextEntry
            val buffer = ByteArray(1024)
            val pictoBloxUnzipDir = targetFile
            if (!pictoBloxUnzipDir.exists()) {
                pictoBloxUnzipDir.mkdirs()
            }
            while (zipEntry != null) {
                PictobloxLogger.getInstance().logd("Unzip____ ${zipEntry.name}")
                val entry = File(pictoBloxUnzipDir, zipEntry.name)
//            ensureZipPathSafety(entry, zipEntry.name)
                if (zipEntry.isDirectory) {
                    val dir = File(pictoBloxUnzipDir, zipEntry.name)
                    if (!dir.exists()) {
                        dir.mkdirs()
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
            inputStream.close()
        } catch (e: Exception) {

        }
    }

    fun loadExample(id: String, fileName: String): Completable {
        return storageHandler.loadExampleProject(id, fileName)
            .doOnSuccess {
                functionType = PictoWebFunctions.OPEN_PROJECT

            }.ignoreElement()
    }

    fun loadAIModelFromStorage(model: String) {
        // FOR ML
        val assetUri = "https://appassets.androidplatform.net/ai/$model"
        if (model.equals("ml")) {
            apiFromPictobloxWeb.onModelFilesLocated("https://appassets.androidplatform.net/ai/mlTemp/")
        }
        apiFromPictobloxWeb.onModelFilesLocated(assetUri)

    }

    var cameraPermissionCount = 0

    fun onPermissionsResult(grantedPermissions: IntArray) {
        cameraPermissionCount++
        permissionPendingList.forEachIndexed { index, s ->

            when (s) {
                Manifest.permission.CAMERA -> {
                    if (grantedPermissions.size > 0 && grantedPermissions[index] == PackageManager.PERMISSION_GRANTED) {
                        permissionGivenList.add(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
                    } else {
                        Toast.makeText(
                            commManagerServiceImpl,
                            "Please provide camera permission",
                            Toast.LENGTH_SHORT
                        ).show()
                        if (cameraPermissionCount > 2) {
                            Handler().postDelayed({
                                pictoBloxCallbacks?.goToSettings()
                            }, 500)
                        }
                    }
                }

                Manifest.permission.RECORD_AUDIO -> {
                    if (grantedPermissions[index] == PackageManager.PERMISSION_GRANTED) {
                        permissionGivenList.add(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
                    }
                }

                Manifest.permission.ACCESS_FINE_LOCATION -> {
                    if (grantedPermissions[index] == PackageManager.PERMISSION_GRANTED) {
                        mGeoLocationCallback?.invoke(mGeoLocationOrigin, true, false)
                    } else {
                        mGeoLocationCallback?.invoke(mGeoLocationOrigin, false, false)

                    }
                }

            }
        }
        if (permissionGivenList.isEmpty()) {
            mMediaPermissionRequest?.deny()

        } else {
            mMediaPermissionRequest?.grant(permissionGivenList.toTypedArray())
        }

        mMediaPermissionRequest = null
        mGeoLocationCallback = null
    }

    fun isPermissionAllowed(permission: String, msg: String) {
        if (ActivityCompat.checkSelfPermission(
                commManagerServiceImpl,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(commManagerServiceImpl, msg, Toast.LENGTH_SHORT).show()
            Handler().postDelayed({ pictoBloxCallbacks?.goToSettings() }, 1000)
        }
    }

    fun getTotalDiskSpace(): Long {
        val statFs = StatFs(Environment.getRootDirectory().absolutePath)
        return (statFs.blockCountLong * statFs.blockSizeLong)
    }
    /*
    *
    *
    *
    *
    ***************************************************************************************************************
     */

    /**
     * APIs called from web to android
     */
    inner class APIsForPictoBlox {

        @JavascriptInterface
        fun setSpriteList(sprintArray: Array<String>?) {
            PictobloxLogger.getInstance().logd("jsonArray $sprintArray")
        }

        @JavascriptInterface
        fun sendIsFileExist(requestId: Int, filePath: String?) {
            // path and file both
//            filePath  = /hello/jj
            filePath?.let {
                File(storageHandler.getMLFolder(), filePath).also {
                    if (it.exists()) {
                        apiFromPictobloxWeb.mlFileExist(requestId, true, ErrorStatus.SUCCESSFULL)
                        Log.e("TAG", "sendIsFileExist: PRESENT")
                        return
                    } else {
                        apiFromPictobloxWeb.mlFileExist(requestId, false, ErrorStatus.ALREADY_PRESENT)
                        Log.e("TAG", "sendIsFileExist: NOT PRESENT")
                        return
                    }
                }
            }
            apiFromPictobloxWeb.mlFileExist(requestId, false, ErrorStatus.PATH_EMPTY)
            return
        }

        @JavascriptInterface
        fun sendMakeFileDirectory(requestId: Int, dirName: String?) {
            // only path
            dirName?.let {
                val newDir = File(storageHandler.getMLFolder(), dirName)
                if (!newDir.exists()) {
                    newDir.mkdir()
                    apiFromPictobloxWeb.mlMakeDir(requestId, ErrorStatus.SUCCESSFULL)
                    Log.e("TAG", "sendMakeFileDirectory: Generated")
                    return
                } else {
                    apiFromPictobloxWeb.mlMakeDir(requestId, ErrorStatus.ALREADY_PRESENT)
                    Log.e("TAG", "sendMakeFileDirectory: Already present")
                    return
                }
            }
            apiFromPictobloxWeb.mlFileExist(requestId, false, ErrorStatus.PATH_EMPTY)
        }

        @JavascriptInterface
        fun sendRenameFile(requestId: Int, path: String?, newName: String?) {
            // both
            path?.let {
                val removedSuffix = path.removeSuffix("/")
                if (File(storageHandler.getMLFolder(), removedSuffix).exists()) {
                    Log.e("TAG", "sendRenameFile: exist ")
                    val newPath = removedSuffix.substringBeforeLast("/") + "/" + newName
                    var renamedFile = File(storageHandler.getMLFolder(), newPath).let {
                        if (it.exists()) {
                            apiFromPictobloxWeb.mlRenameFile(
                                requestId,
                                ErrorStatus.ALREADY_PRESENT
                            )
                            return
                        }
                        it
                    }
                    File(storageHandler.getMLFolder(), removedSuffix).renameTo(renamedFile).let {
                        if (it) {
                            Log.e("TAG", "sendRenameFile: Successfull")
                            apiFromPictobloxWeb.mlRenameFile(requestId, ErrorStatus.SUCCESSFULL)
                            return
                        } else {
                            apiFromPictobloxWeb.mlRenameFile(requestId, ErrorStatus.FAILED)
                            return
                        }
                    }
                } else {
                    apiFromPictobloxWeb.mlRenameFile(requestId, ErrorStatus.FILE_NOT_EXIST)
                }
            }
        }

        @JavascriptInterface
        fun sendReadDir(requestId: Int, dirName: String?) {
            // only name of files
            dirName?.let {
                if (File(storageHandler.getMLFolder(), dirName).isDirectory) {
                    val filesName = File(storageHandler.getMLFolder(), dirName).list()
                    if (filesName != null) {
                        Log.e("TAG", "sendReadDir: ${filesName.asList()}")
                        apiFromPictobloxWeb.mlReadFile(requestId, ErrorStatus.SUCCESSFULL, filesName)
                        return
                    } else {
                        apiFromPictobloxWeb.mlReadFile(requestId, ErrorStatus.SUCCESSFULL, arrayOf())
                        return
                    }
                }else{
                    apiFromPictobloxWeb.mlReadFile(requestId, ErrorStatus.ERROR_NOT_DIREACTORY_PATH, arrayOf())
                    return
                }
            }
            apiFromPictobloxWeb.mlReadFile(requestId, ErrorStatus.PATH_EMPTY, arrayOf())

        }

        @RequiresApi(Build.VERSION_CODES.O)
        @JavascriptInterface
        fun sendStatSync(requestId: Int, path: String?) {
            // both
            // create = null
            // modified =
            path?.let {
                File(storageHandler.getMLFolder(), it).let {
                    if (it.exists()){
                        val lastModified = it.lastModified()
                        val createTime =
                            Files.readAttributes(it.toPath(), BasicFileAttributes::class.java)
                                .creationTime().toMillis()
                        apiFromPictobloxWeb.mlFileStateSync(
                            requestId,
                            createTime.toString(),
                            lastModified.toString(),
                            ErrorStatus.SUCCESSFULL
                        )
                        Log.e("TAG",
                            "sendStatSync: creation: $createTime and lastmodified: $lastModified")
                        return
                    }else{
                        apiFromPictobloxWeb.mlFileStateSync(requestId, "", "", ErrorStatus.FILE_NOT_EXIST)
                        Log.e("TAG", "sendStatSync: file not exist", )
                        return
                    }
                }
            }
            apiFromPictobloxWeb.mlFileStateSync(requestId, "", "", ErrorStatus.PATH_EMPTY)
        }

        @JavascriptInterface
        fun sendWriteFile(requestId: Int, fileName: String, byteArray: ByteArray?, path: String) {
            // perticular file
            // filname content bytearray

            if (!path.isNullOrEmpty() && byteArray != null) {
                val file = File(storageHandler.getMLFolder().absolutePath + "/" + path, fileName)
                if (file.exists()) {
                    apiFromPictobloxWeb.mlWriteFile(requestId, ErrorStatus.ALREADY_PRESENT)
                    return
                }
                compositeDisposable.add(
                    storageHandler.saveFile(file, byteArray)
                        .subscribeOn(Schedulers.computation())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(object : DisposableCompletableObserver() {
                            override fun onComplete() {
                                apiFromPictobloxWeb.mlWriteFile(requestId, ErrorStatus.SUCCESSFULL)
                                Log.e("TAG", "onComplete: File Saved")
                                return
                            }

                            override fun onError(e: Throwable) {
                                apiFromPictobloxWeb.mlWriteFile(requestId, ErrorStatus.FAILED)
                                Log.e("TAG", "onComplete: File Saved FAILED")
                                return
                            }
                        })
                )
            }else{
                apiFromPictobloxWeb.mlWriteFile(requestId, ErrorStatus.PATH_EMPTY)
            }
        }

        @JavascriptInterface
        fun sendCopyFile(requestId: Int, targetPath: String, curPath: String) {

            if (targetPath.isNotBlank() && !curPath.isNotBlank()) {
                apiFromPictobloxWeb.mlSendCopyFile(requestId, ErrorStatus.PATH_EMPTY)
                return
            }
            val targetFile = File(storageHandler.getMLFolder(), targetPath)
            val curFile = File(storageHandler.getMLFolder(), curPath)

            if (curFile.isFile) {
                File(storageHandler.getMLFolder(), curPath).copyRecursively(
                    File(
                        targetFile,
                        curFile.name
                    ), false, onError = { _, ioException ->
                        // error
                        Log.e("TAG", "sendCopyFile: ${ioException.localizedMessage}")
                        apiFromPictobloxWeb.mlSendCopyFile(requestId, ErrorStatus.FAILED)
                        OnErrorAction.SKIP
                    })
            } else {
                File(storageHandler.getMLFolder(), curPath).copyRecursively(
                    File(
                        targetFile,
                        curFile.name
                    ), false, onError = { _, ioException ->
                        // error
                        Log.e("TAG", "sendCopyFile: ${ioException.localizedMessage}")
                        apiFromPictobloxWeb.mlSendCopyFile(requestId, ErrorStatus.FAILED)
                        OnErrorAction.SKIP
                    })
            }
            apiFromPictobloxWeb.mlSendCopyFile(requestId, ErrorStatus.SUCCESSFULL)
        }

        fun sendUnzipFile(requestId: Int, zipFilePath: String, unzipFilePath: String, fileName: String?) {
            if (fileName != null) {
                if (zipFilePath.isNotEmpty() && unzipFilePath.isNotEmpty() && fileName.isNotEmpty()) {
                    val newFile = File(
                        storageHandler.getMLFolder().path + "/" + unzipFilePath,
                        fileName
                    ).let {
                        if (it.exists()) {
                            apiFromPictobloxWeb.mlUnzipFile(requestId, ErrorStatus.ALREADY_PRESENT)
                            return
                        } else {
                            it.mkdirs()
                        }
                        it
                    }
                    Log.e("TAG", "sendUnzipFile: called")
                    unzipFile(File(zipFilePath), newFile, 1)
                    apiFromPictobloxWeb.mlUnzipFile(requestId, ErrorStatus.SUCCESSFULL)
                    return
                }
            }
            apiFromPictobloxWeb.mlUnzipFile(requestId, ErrorStatus.FAILED)
        }

        @JavascriptInterface
        fun sendRemoveFile(requestId: Int, path: String) {

            File(storageHandler.getMLFolder(), path).let {
                if (it.exists()) {
                    if (it.deleteRecursively()) {
                        apiFromPictobloxWeb.mlRemoveFile(requestId, ErrorStatus.SUCCESSFULL)
                        Log.e("TAG", "sendRemoveFile: File deleted successfully")
                    } else {
                        apiFromPictobloxWeb.mlRemoveFile(requestId, ErrorStatus.FAILED)
                        Log.e("TAG", "sendRemoveFile: Unable to delete file")
                    }
                } else {
                    apiFromPictobloxWeb.mlRemoveFile(requestId, ErrorStatus.FILE_NOT_EXIST)
                    Log.e("TAG", "sendRemoveFile: not present")
                }
            }
        }

        @JavascriptInterface
        fun sendGetDiskSpace(requestId: Int) {
            var space = getTotalDiskSpace()
            apiFromPictobloxWeb.mlPhoneDiskSpace(requestId, space.toString())
            Log.e("TAG", "sendGetDiskSpace: $space")
        }

        /*        @JavascriptInterface
                fun setSpriteList(sprintArray: String?, backdropObject: String?) {
                    compositeDisposable.add(
                        getTargetProcessor(sprintArray, backdropObject)
                            .subscribeOn(Schedulers.computation())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeWith(object :
                                DisposableSingleObserver<Pair<Sprite2?, List<Sprite2>>>() {
                                override fun onSuccess(t: Pair<Sprite2?, List<Sprite2>>) {
                                    pictoBloxCallbacks?.setSprites2(t)
                                }

                                override fun onError(e: Throwable) {
                                    e.printStackTrace()
                                    //TODO
                                }

                            })
                    )
                }*/

        @JavascriptInterface
        fun write(byteArray: ByteArray?) {
            byteArray?.apply {
                commManagerServiceImpl.write(this)
            }
        }

        /**
         * This is a specific case for writing a frame, we need a flag to remaind us when hardware responds.
         */
        @JavascriptInterface
        fun writeForFirmware(byteArray: ByteArray?) {
            byteArray?.apply {
                isWaitingForFirmwareQuery = true
                commManagerServiceImpl.write(this)
            }
        }


        @JavascriptInterface
        fun initializationCompleted() {

        }

        @JavascriptInterface
        fun promptConnectDialog() {

        }

        @JavascriptInterface
        fun isDeviceConnected(): Boolean {
            return commManagerServiceImpl.isConnected()
        }

        @JavascriptInterface
        fun getConnectedBTDeviceName(): String {
            return "<TODO NOT IMPLEMENTED>"
        }

        @JavascriptInterface
        fun onProjectSaved(fileName: String, byteArray: ByteArray?) {
            Log.e("PictoBloxWebActivity", "onProjectSaved: ")
            if (byteArray != null) {
                Log.d("project", "onProjectSaved: not null")
                PictobloxLogger.getInstance().logd("byteArray not null size is ${byteArray.size}")
            } else {
                Log.d("project", "onProjectSaved: null")
                PictobloxLogger.getInstance().logd("byteArray is null")

            }
            Log.e("TAG", "onProjectSaved: $storageType")
            apisForPictoblox.sendWriteFile(43, "hello.sb3", byteArray, "thired/")
            byteArray?.apply {
                compositeDisposable.add(
                    storageHandler.saveProject(this, fileName, storageType)
                        .subscribeOn(Schedulers.computation())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(object : DisposableCompletableObserver() {
                            override fun onComplete() {
                                when (storageType) {
                                    StorageType.CACHE -> {
                                        Toast.makeText(
                                            commManagerServiceImpl,
                                            cacheName,
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }

                                    StorageType.SHARED_SESSION -> {
                                        syncFileWithWatchers()
                                    }

                                    else -> {
                                        Toast.makeText(
                                            commManagerServiceImpl,
                                            commManagerServiceImpl.getString(
                                                R.string.project_save
                                            ),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }


                                pictoBloxCallbacks?.onSaveComplete()
                                    ?.subscribeOn(AndroidSchedulers.mainThread())?.subscribe()
                            }

                            override fun onError(e: Throwable) {

                                if (storageType == StorageType.CACHE) {
                                    Toast.makeText(
                                        commManagerServiceImpl,
                                        "${webView?.context?.getString(R.string.error_in_caching) ?: "Error in caching project:"}  $fileName",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    pictoBloxCallbacks?.onSaveComplete()
                                        ?.subscribeOn(AndroidSchedulers.mainThread())?.subscribe()

                                } else {
                                    Toast.makeText(
                                        commManagerServiceImpl,
                                        "${webView?.context?.getString(R.string.error_in_saving) ?: "Error in saving project:"} $fileName",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    pictoBloxCallbacks?.onSaveComplete()
                                        ?.subscribeOn(AndroidSchedulers.mainThread())?.subscribe()

                                }

                                PictobloxLogger.getInstance().logException(e)
                            }
                        })
                )

            } ?: run {
                if (storageType == StorageType.INTERNAL) {
                    Toast.makeText(
                        commManagerServiceImpl,
                        webView?.context?.getString(R.string.fail_to_save_file)
                            ?: "Failed to save file",
                        Toast.LENGTH_LONG
                    )
                        .show()
                    pictoBloxCallbacks?.onSaveComplete()
                        ?.subscribeOn(AndroidSchedulers.mainThread())?.subscribe()

                } else if (storageType == StorageType.CACHE) {
                    Toast.makeText(
                        commManagerServiceImpl,
                        webView?.context?.getString(R.string.fail_to_save_file)
                            ?: "Failed to cache work",
                        Toast.LENGTH_LONG
                    ).show()
                    pictoBloxCallbacks?.onSaveComplete()
                        ?.subscribeOn(AndroidSchedulers.mainThread())?.subscribe()
                } else {

                }
            }
        }

        /*@JavascriptInterface
        fun onModelChanged(modelName: String) {
            handler.post {
                pictoBloxCallbacks?.apply {
                    NavigationModalStack.values().find { it.value == modelName }?.apply {
                        onModalChanged(this)
                    }
                }
            }
        }*/


        @JavascriptInterface
        fun openUserInput(
            argType: String?,
            argPlaceholder: String?,
            handlerFunction: String?,
            currValue: String?
        ) {
            PictobloxLogger.getInstance()
                .logw("argType:$argType argPlaceholder:$argPlaceholder handlerFunction:$handlerFunction currValue:$currValue")

            if (argType == null || handlerFunction == null) {
                PictobloxLogger.getInstance().logw("ignoring :: ")
            }

            pictoBloxCallbacks?.onPromptUserInputDialog(
                argType!!,
                argPlaceholder,
                handlerFunction!!,
                currValue
            )?.subscribe()

        }

        @JavascriptInterface
        fun openUserInput(argType: String?, argParam: String?, handlerFunction: String?) {
            PictobloxLogger.getInstance()
                .logw("ignoring :: argType:$argType argParam:$argParam handlerFunction:$handlerFunction")

            if (argType == null || argParam == null || handlerFunction == null) {
                PictobloxLogger.getInstance().logw("ignoring :: ")
            }
            Log.d("Aniket", "openUserInput: ")

            //pictoBloxCallbacks?.onPromptUserInputDialog(argType!!, argParam!!, handlerFunction!!)?.subscribe()


        }

        fun openUserInputCode(argType: String?, argParam: String?) {

        }

        @JavascriptInterface
        fun exit() {
            pictoBloxCallbacks?.exit()?.subscribeOn(AndroidSchedulers.mainThread())?.subscribe()
        }

        @JavascriptInterface
        fun promtProjectSaveDialog() {
            pictoBloxCallbacks?.promptProjectSaveDialog(false)
                ?.subscribeOn(AndroidSchedulers.mainThread())?.subscribe()
        }

        @JavascriptInterface
        fun promptSaveAndExit() {
            pictoBloxCallbacks?.promptProjectSaveDialog(true)
                ?.subscribeOn(AndroidSchedulers.mainThread())?.subscribe()
        }

        @JavascriptInterface
        //this.props.currentBoard, this.props.projectChanged
        fun promptUserBoardSelectionDialog(currentBoard: String, isProjectChanged: Boolean) {

            val board: Board? = when (currentBoard) {
                Board.EVIVE.stringValue -> Board.EVIVE
                Board.UNO.stringValue -> Board.UNO
                Board.MEGA.stringValue -> Board.MEGA
                Board.NANO.stringValue -> Board.NANO
                Board.ESP32.stringValue -> Board.ESP32
                //Board.QUON.stringValue->Board.QUON
                else -> null
            }

            pictoBloxCallbacks?.promptUserBoardSelectionDialog(board, isProjectChanged)
                ?.subscribeOn(AndroidSchedulers.mainThread())
                ?.subscribe()

        }

        @JavascriptInterface
        fun promptUserForBluetoothConnection() {
            pictoBloxCallbacks?.promptUserForBluetoothConnection()
                ?.subscribeOn(AndroidSchedulers.mainThread())?.subscribe()

        }

        @JavascriptInterface
        fun askPermission(permissionType: String) {
            Log.e("TAG", "askCameraPermission: $permissionType")

            when (permissionType) {
                PermissionTypes.AUDIO_PERMISSION.name, PermissionTypes.IMAGE_PERMISSION.name -> {
                    isPermissionAllowed(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        "Please grant Photos and videos Permission"
                    )
                }

                PermissionTypes.CAMERA_PERMISSION.name -> {
                    isPermissionAllowed(
                        Manifest.permission.CAMERA,
                        "Please grant Camera Permission"
                    )
                }
            }

//            if (ActivityCompat.checkSelfPermission(commManagerServiceImpl,Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
//                Handler().postDelayed({
//                    pictoBloxCallbacks?.promptForPermissions(mutableListOf(Manifest.permission.CAMERA))
//
//                },if (cameraPermissionCount <2) 2000 else 100)
//            }
        }

        @JavascriptInterface
        fun stopOTGCamVideoFrames() {
            Log.e("TAG", "stopOTGCamVideoFrames: ")
            pictoBloxCallbacks?.onstopOTGCamVideoFrame()
        }


        @JavascriptInterface
        fun sendOTGCamVideoFrames() {
            Log.e("TAG", "sendOTGCamVideoFrames: ")
            pictoBloxCallbacks?.onsendOTGCamVideoFrame()
        }

        @JavascriptInterface
        fun playSoundKey(key: String) {
            try {
                soundHandler.playKey(key.toInt())

            } catch (e: NumberFormatException) {
                e.printStackTrace()
            }

        }

        @JavascriptInterface
        fun onProjectLoaded() {
            pictoBloxCallbacks?.onPictobloxReady()
            handler.post {
                PictobloxLogger.getInstance().logd("onProjectLoaded")

                setStartSequence(FirebaseAuth.getInstance().currentUser?.uid)
                isWaitingForPictobloxToBeReady = false
                openProject()
                Log.e("TAG", "onProjectLoaded isOTGConnected: $isOTGConnected")

                if (isOTGConnected) {
                    if (!data.isNullOrEmpty()) {
                        data?.let {
                            Log.e("TAG", "onProjectLoaded isOTGConnected: ")
                            apiFromPictobloxWeb.onUSBCameraConnected(it.get("name"), it.get("id")!!)
                        }
                    }
                }
            }
        }

        @JavascriptInterface
        fun promptComingSoon() {
            handler.post {
                Toast.makeText(
                    commManagerServiceImpl, commManagerServiceImpl.getString(
                        R.string.comming_soon
                    ), Toast.LENGTH_LONG
                ).show()
            }
        }


        @JavascriptInterface
        fun handleLessonRetry() {
            retryAttempts++
            PictobloxLogger.getInstance().logd("handleLessonRetry attempts made $retryAttempts")

            courseFlow?.also { courseFlow ->
                val d = commManagerServiceImpl.courseManager.setLessonTaskRetry(
                    retryAttempts,
                    courseFlow
                )
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(object : DisposableCompletableObserver() {
                        override fun onComplete() {
                            pictoBloxCallbacks?.onCourseRetry()
                            //Nothing to do
                        }

                        override fun onError(e: Throwable) {
                            e.printStackTrace()
                            //hmm??
                        }

                    })
            }

        }

        @JavascriptInterface
        fun handleLessonComplete(fileName: String, byteArray: ByteArray?) {
            PictobloxLogger.getInstance().logd("handleLessonCompleted")

            courseFlow?.also { courseFlow ->
                compositeDisposable.add(
                    commManagerServiceImpl.courseManager.setLessonTaskCompleted(
                        retryAttempts,
                        courseFlow
                    )
                        .andThen(
                            commManagerServiceImpl.courseManager.saveCompletedLessonFile(
                                courseFlow,
                                byteArray ?: ByteArray(0)
                            )
                        )
                        .subscribeOn(Schedulers.computation())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(object : DisposableCompletableObserver() {
                            override fun onComplete() {
                                pictoBloxCallbacks?.onCourseCompleted(courseFlow)
                            }

                            override fun onError(e: Throwable) {
                                e.printStackTrace()
                                //hmm?? ** Geralt intensifies
                            }

                        })
                )

            }
        }

        @JavascriptInterface
        fun authenticateUser(extensionUrl: String, extensionId: String) {
            Log.d("user", "authenticateUser: ")
            extensiontoLoad.let {
                it.put("id", extensionId)
                it.put("url", extensionUrl)
            }
            pictoBloxCallbacks?.showSignInDialog()
                ?.subscribeOn(AndroidSchedulers.mainThread())?.subscribe()
        }

        @JavascriptInterface
        fun getUserToken() {
            PictobloxLogger.getInstance().logd("getUserToken")
            handler.post { apiFromPictobloxWeb.setUserId(FirebaseAuth.getInstance().currentUser?.uid) }

        }

        @JavascriptInterface
        fun loadAIModel(modelName: String?) {
            PictobloxLogger.getInstance().logd("loadAIModel $modelName")
            modelName?.also { model ->
                pictoBloxCallbacks?.loadAIModel(model)?.subscribeOn(AndroidSchedulers.mainThread())
                    ?.subscribe()

            }
        }

        @JavascriptInterface
        fun openFirmwareUploader(board: String?) {
            board?.also {
                pictoBloxCallbacks?.openFirmwareUploader(it)
                    ?.subscribeOn(AndroidSchedulers.mainThread())?.subscribe()
            }
        }

        @JavascriptInterface
        fun saveToPersistentDatabase(json: String?) {
            spManager.pictobloxWebviewPreferenceJson = json ?: ""
        }

        @JavascriptInterface
        fun logAnalyticsEvent(eventJson: String?) {
            eventJson?.also {
                PictoBloxAnalyticsEventLogger.getInstance().setWebPictoBloxEvent(it)
            }
        }

        @JavascriptInterface
        fun logAnalyticsEvent(eventName: String?, extraKey: String?, extraValue: String?) {
            PictobloxLogger.getInstance().logd("$eventName | $extraKey | $extraValue")
            eventName?.also { event ->

                val bundle = extraKey?.let { key ->
                    Bundle().apply { putString(key, extraValue ?: "UNDEFINED") }
                }

                /*Handler().post {
                    Firebase.analytics.logEvent(event, bundle)
                }*/
            }
        }

        @JavascriptInterface
        fun openExternalLink(link: String?) {
            link?.also {
                pictoBloxCallbacks?.openExternalWebLink(it)
                    ?.subscribeOn(AndroidSchedulers.mainThread())?.subscribe()
            }
        }


        @JavascriptInterface
        fun showToast(link: String?) {
            PictobloxLogger.getInstance().logd(link ?: "LINK null")
        }

        @JavascriptInterface
        fun promptSignup() {
            pictoBloxCallbacks?.redirectToSignUp(false)
                ?.subscribeOn(AndroidSchedulers.mainThread())?.subscribe()
        }

        @JavascriptInterface
        fun promptSaveAndSignup() {
            pictoBloxCallbacks?.redirectToSignUp(true)
                ?.subscribeOn(AndroidSchedulers.mainThread())?.subscribe()
        }


    }

    enum class ErrorStatus(var error: String) {
        FAILED("failed"),
        SUCCESSFULL("successfull"),
        PATH_EMPTY("undefied"), // PATH EMPTY
        ALREADY_PRESENT("already present"),
        ERROR_NOT_DIREACTORY_PATH("not a directory path"),
        FILE_NOT_EXIST("not exist"),
        ERROR_PERMISSION("no permission"),
        ERROR_NO_SPACE("no storage"),
        OTHER("other");

        fun getExceptionMessage(message: String): String {
            return when (this) {
                OTHER -> message
                else -> {""}
            }
        }
    }


    /**
     * APIs exposed by pictoblox for Native side
     */
    inner class APIsFromPictoBlox {

        fun mlRemoveFile(requestId: Int, removeStatus: ErrorStatus) {
            handler.post {
                webView?.loadUrl("javascript:mlRemoveFile(${requestId},${removeStatus.ordinal})")
            }
        }

        fun mlUnzipFile(requestId: Int, unzipStatus: ErrorStatus) {
            handler.post {
                webView?.loadUrl("javascript:mlUnzipFile(${requestId},${unzipStatus.ordinal})")
            }
        }

        fun mlSendCopyFile(requestId: Int, copyStatus: ErrorStatus) {
            handler.post {
                webView?.loadUrl("javascript:mlCopyFile(${requestId},${copyStatus.ordinal})")
            }
        }

        fun mlWriteFile(requestId: Int, writeStatus: ErrorStatus) {
            handler.post {
                webView?.loadUrl("javascript:mlWriteFile(${requestId},${writeStatus.ordinal})")
            }
        }

        fun mlReadFile(requestId: Int, readStatus: ErrorStatus, files: Array<String>?) {
            handler.post {
                webView?.loadUrl(
                    "javascript:mlReadFile(${requestId},${readStatus.ordinal},${
                        JSONArray(
                            files
                        )
                    })"
                )
            }
        }

        fun mlRenameFile(requestId: Int, renameStatus: ErrorStatus) {
            handler.post {
                webView?.loadUrl("javascript:mlRenameFile(${requestId},${renameStatus.ordinal})")
            }
        }

        fun mlPhoneDiskSpace(requestId: Int, diskSpace: String) {
            handler.post {
                webView?.loadUrl("javascript:phoneStorage(${requestId},${diskSpace})")
            }
        }

        fun mlFileExist(requestId: Int, isFileExist: Boolean, renameStatus: ErrorStatus) {
            handler.post {
                webView?.loadUrl("javascript:mlFileExist(${requestId},${isFileExist},${renameStatus.ordinal})")
            }
        }


        fun mlMakeDir(requestId: Int, makeDirStatus: ErrorStatus) {
            handler.post {
                webView?.loadUrl("javascript:mlMakeDir(${requestId},${makeDirStatus.ordinal})")
            }
        }


        fun mlFileStateSync(
            requestId: Int,
            ctime: String,
            mtime: String,
            stateSyncStatus: ErrorStatus
        ) {
            handler.post {
                webView?.loadUrl("javascript:mlStateSync(${requestId},\"${ctime}\",\"${mtime}\",${stateSyncStatus.ordinal})")
            }
        }


        fun logException(msg: String, trace: String) {
            FirebaseCrashlytics.getInstance().recordException(Exception(msg, Throwable(trace)))
        }

        fun setFrame(frame: String?) {
            handler.post {
                webView?.loadUrl("javascript:sendVideoFrames(\"$frame\")")
            }
        }

        fun onUSBCameraDisConnected() {
            Log.e("TAG", "onUSBCameraDisConnected: ")
            data = mutableMapOf()
            webView?.loadUrl("javascript:onOTGDisconnected()")

        }

        fun onUSBCameraConnected(OTGName: String?, OTGId: String) {
            Log.e("TAG", "onUSBCameraConnected: ")
            webView?.loadUrl("javascript:onOTGConnected(\"$OTGName\",\"$OTGId\")")
        }

        fun openNewProject() {
            PictobloxLogger.getInstance().logd("javascript:openNewProject()")
            webView?.loadUrl("javascript:openNewProject()")
        }

        internal fun openProject(fileName: String, base64String: String) {
            PictobloxLogger.getInstance()
                .logd("javascript:openProject(\"$fileName\",<base64String>)")
            webView?.loadUrl("javascript:openProject(\"$fileName\",\"$base64String\")")
        }

        internal fun openProjectWithWebAssetLoader(fileName: String, path: String) {
            val assetUri = "https://appassets.androidplatform.net/$path/$fileName"
            PictobloxLogger.getInstance()
                .logd("javascript:openProjectWithWebAssetLoader(\"$assetUri\")")
            webView?.loadUrl("javascript:openProjectWithWebAssetLoader(\"$assetUri\")")
        }

        internal fun saveProject(name: String) {
            Log.e("PictoBloxWebActivity", "saveProject: ")
            webView?.loadUrl("javascript:saveProject(\"$name\")")
        }

        internal fun loadExtensionAfterSignIning() {
            Log.e("TAG", "loadExtensionAfterSignIning: ")
            webView?.loadUrl(
                "javascript:openExtensionAfterSignInUser(\"${extensiontoLoad.get("url")}\",\"${
                    extensiontoLoad.get(
                        "id"
                    )
                }\")"
            )
        }

        internal fun responseFromHardwareForStringValue(response: String, isVM: Boolean) {
            PictobloxLogger.getInstance().logd("javascript:responseFromHardware(\"$response\")")
            webView?.loadUrl("javascript:responseFromHardware(\"$response\", ${isVM})")
        }

        internal fun responseFromHardware(response: String, isVM: Boolean) {
            PictobloxLogger.getInstance().logd("javascript:responseFromHardware($response)")
            webView?.loadUrl("javascript:responseFromHardware(${response},${isVM})")
        }

        internal fun isTabletMode(isTablet: Boolean) {
            webView?.loadUrl("javascript:deviceIsTablet(\"$isTablet\")")
        }

        internal fun responseForFirmware(response: String) {
            PictobloxLogger.getInstance().logd("javascript:responseForFirmware(\"$response\")")
            webView?.loadUrl("javascript:responseForFirmware(\"$response\")")
        }

        internal fun boardSelected(board: Board, replaceProject: Boolean) {
            webView?.loadUrl("javascript:boardSelected(\"${board.stringValue}\",$replaceProject)")
        }

        internal fun removeSprite(sprite: String) {
            webView?.loadUrl("javascript:removeSprite(\"$sprite\")")
        }

        internal fun duplicateSprite(sprite: String) {
            webView?.loadUrl("javascript:duplicateSprite(\"$sprite\")")
        }

        internal fun editSprite(sprite: String) {
            webView?.loadUrl("javascript:editSprite(\"$sprite\")")
        }

        internal fun selectSprite(sprite: String) {
            webView?.loadUrl("javascript:selectSprite(\"$sprite\")")
        }

        fun toggleEditor() {
            webView?.loadUrl("javascript:toggleEditor()")
        }

        fun onAddSpriteClick() {
            webView?.loadUrl("javascript:onClickOpenSpriteLibrary()")
        }

        fun onAddBackdropClick() {
            webView?.loadUrl("javascript:onClickOpenBackdropLibrary()")
        }

        fun openStage() {
            webView?.loadUrl("javascript:openStage()")
        }

        fun startFlagClicked() {
            webView?.loadUrl("javascript:startFlagClicked()")
        }

        fun stopFlagClicked() {
            webView?.loadUrl("javascript:stopFlagClicked()")
        }

        fun onBackPressed() {
            webView?.loadUrl("javascript:onBackPressed()")
        }

        fun editSpriteParamters(handlerName: String, value: String) {
            webView?.loadUrl("javascript:editSpriteParamters(\"$handlerName\",\"$value\")")
        }

        fun editCodeParamters(handlerName: String, value: String) {
            webView?.loadUrl("javascript:editSpriteParamters(\"$handlerName\",\"$value\")")
        }

        /*fun onBoardSelected(board: Board){
            webView?.loadUrl("javascript:onBoardSelected(\"${board.stringValue}\")")
        }*/

        fun onBluetoothConnected() {
            updateConnectionState(true)
        }

        fun onBluetoothDisconnected() {
            updateConnectionState(false)
        }

        private fun updateConnectionState(isConnected: Boolean) {
            webView?.loadUrl("javascript:updateConnectionState($isConnected)")
        }

        fun openCourse(z: String) {
            webView?.loadUrl("javascript:onOpenLesson($z)")
        }

        fun startTourSession() {
            webView?.loadUrl("javascript:startTourSession()")
        }

        fun setUserId(userId: String?) {
            PictobloxLogger.getInstance().logd("registerUserToken $userId")
            webView?.loadUrl("javascript:registerUserToken(\"$userId\")")
        }

        fun onCreditsUpdate(credits: String) {
            PictobloxLogger.getInstance().logd("onCreditsUpdate $credits")
            webView?.loadUrl("javascript:onCreditsUpdate(\"$credits\")")
        }

        fun onUserStateUpdated(userState: String) {
            PictobloxLogger.getInstance().logd("onUserStateUpdated $userState")
            webView?.loadUrl("javascript:onUserStateUpdated(\"$userState\")")
        }

        fun setStart() {
            PictobloxLogger.getInstance().logd("javascript:onPictoBloxStart()")
            webView?.loadUrl("javascript:onPictoBloxStart()")
        }

        fun setStop() {
            webView?.loadUrl("javascript:onPictoBloxStop()")
        }

        fun onModelFilesLocated(path: String) {
            PictobloxLogger.getInstance().logd("javascript:onModelFilesLocated(\"$path\")")
            webView?.loadUrl("javascript:onModelFilesLocated(\"$path\")")
        }

        fun sendAiModelLoadingCanceled() {
            PictobloxLogger.getInstance().logd("javascript:onCancelModelLoading()")
            webView?.loadUrl("javascript:onCancelModelLoading()")
        }

        fun setWebviewSharedPreferenceJson(json: String) {
            PictobloxLogger.getInstance().logd("javascript:setPersistentDatabase($json)")
            //webView?.loadUrl("javascript:setPersistentDatabregisterUserTokenase(\"$json\")")
            webView?.loadUrl("javascript:setPersistentDatabase($json)")
        }

        fun setLocale(locale: String) {
            PictobloxLogger.getInstance().logd("javascript:onLanguageSelected(\"$locale\")")
            webView?.loadUrl("javascript:onLanguageSelected(\"$locale\")")
        }
    }

    /**
     *
     *
     * {
    "id": "%+J4;q~0.`j~oje5y#n4",
    "name": "Sprite1",
    "md5": "b7853f557e4426412e64bb3da6531a99.svg",
    "costumeName": "costume1",
    "isSelected": true
    }
     */

    inner class Sprite2 {
        @SerializedName("id")
        @Expose
        var id: String = ""

        @SerializedName("name")
        @Expose
        var name: String = ""

        @SerializedName("md5")
        @Expose
        var md5: String = ""

        @SerializedName("costumeName")
        @Expose
        var costumeName: String = ""

        @SerializedName("isSelected")
        @Expose
        var isSelected: Boolean = false

        @Expose(deserialize = false)
        var thumbBitmap: Bitmap? = null

        @Expose(deserialize = false)
        var isDefaultBackdrop = false
    }

    /*    private fun getTargetProcessor(
            sprintArray: String?,
            backdropObject: String?
        ): Single<Pair<Sprite2?, List<Sprite2>>> {

            return Single.create {

                try {
                    PictobloxLogger.getInstance().logd("string $sprintArray")
                    PictobloxLogger.getInstance().logd("backdropObject $backdropObject")

                    val spriteList: List<Sprite2>? =
                        (gson.fromJson(sprintArray, Array<Sprite2>::class.java)).toList()


                    spriteList?.apply {
                        forEach { item ->
                            run {
                                item.thumbBitmap = getBitmapFromVectorAsset2(0, item.md5)
                            }
                        }

                        val backDrop: Sprite2? =
                            (gson.fromJson(backdropObject, Sprite2::class.java))?.apply {
                                isDefaultBackdrop = (defaultBackDropMD5 == md5)

                                thumbBitmap = if (isDefaultBackdrop) {
                                    loadDefaultAsset(1)
                                } else {
                                    getBitmapFromVectorAsset2(1, md5)
                                }
                            }


                        it.onSuccess(Pair(backDrop, spriteList))

                    } ?: run {
                        throw Exception("Invalid Json")

                    }


                } catch (e: Exception) {
                    it.onError(e)

                }
            }
        }

        private fun getBitmapFromVectorAsset2(type: Int, assetName: String?): Bitmap {

            if (!TextUtils.isEmpty(assetName)) {
                try {
                    val svg =
                        SVG.getFromAsset(commManagerServiceImpl.assets, "$spriteResourceDir$assetName")
                            ?: return loadDefaultAsset(type)

                    val squareSide = if (svg.documentViewBox.right > svg.documentViewBox.bottom) {
                        svg.documentViewBox.right
                    } else {
                        svg.documentViewBox.bottom
                    }

                    if (squareSide == -1.0f) {
                        return loadDefaultAsset(type)
                    }

                    val bitmap = Bitmap.createBitmap(
                        squareSide.toInt(),
                        squareSide.toInt(),
                        Bitmap.Config.ARGB_8888
                    )

                    val vp = RectF(
                        (squareSide / 2) - (svg.documentViewBox.right / 2),
                        (squareSide / 2) - (svg.documentViewBox.bottom / 2),
                        (squareSide / 2) + (svg.documentViewBox.right / 2),
                        (squareSide / 2) + (svg.documentViewBox.bottom / 2)
                    )

                    val canvas = Canvas(bitmap)

                    svg.renderToCanvas(canvas, vp)

                    return Bitmap.createScaledBitmap(bitmap, dp48, dp48, true)


                } catch (e: IOException) {
                    e.printStackTrace()
                    return loadDefaultAsset(type)
                }

            } else {
                return loadDefaultAsset(type)
            }
        }

        private fun loadDefaultAsset(type: Int): Bitmap {
            return if (type == 0) {//0 is sprite, 1 is backdrop.
                loadSpriteDefaultAsset()
            } else {
                loadBackdropDefaultAsset()
            }
        }*/

    /*    private fun loadSpriteDefaultAsset(): Bitmap {
            val svg = SVG.getFromAsset(
                commManagerServiceImpl.assets,
                "${tempResourceDir}no_image_stub.svg"
            )
            val bitmap = Bitmap.createBitmap(dp48, dp48, Bitmap.Config.ARGB_8888)

            val canvas = Canvas(bitmap)

            svg.renderToCanvas(canvas)

            return bitmap
        }

        private fun loadBackdropDefaultAsset(): Bitmap {
            val bitmap = Bitmap.createBitmap(dp48, dp48, Bitmap.Config.ARGB_8888)

            val canvas = Canvas(bitmap)

            val d = commManagerServiceImpl.getDrawable(R.drawable.ic_add_backdrop_v1)

            d?.bounds = Rect(0, 0, dp48, dp48)

            d?.draw(canvas)

            return bitmap
        }*/


    private fun convertDpToPixel(dp: Float, context: Context): Int {
        val resources = context.resources
        val metrics = resources.displayMetrics
        return (dp * (metrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)).toInt()
    }


    private fun notifyPictobloxOnBluetoothConnectivity() {

        if (commManagerServiceImpl.isConnected()) {
            apiFromPictobloxWeb.onBluetoothConnected()

        } else {
            apiFromPictobloxWeb.onBluetoothDisconnected()
        }
    }

    private inner class PictobloxChromeClient : WebChromeClient() {

        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams
        ): Boolean {
            Log.e("image", "clicked")
            return pictoBloxCallbacks?.onFileChoose(filePathCallback, fileChooserParams)!!


        }

        override fun onGeolocationPermissionsHidePrompt() {
            super.onGeolocationPermissionsHidePrompt()
        }

        override fun onGeolocationPermissionsShowPrompt(
            origin: String?,
            callback: GeolocationPermissions.Callback?
        ) {
            mGeoLocationCallback = callback
            mGeoLocationOrigin = origin
            permissionPendingList.clear()

            if (ContextCompat.checkSelfPermission(
                    commManagerServiceImpl,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                callback?.invoke(origin, true, true)

            } else {
                permissionPendingList.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }

            if (permissionPendingList.isNotEmpty()) {
                pictoBloxCallbacks?.promptForPermissions(permissionPendingList)
            }
        }

        override fun onPermissionRequest(request: PermissionRequest) {
            mMediaPermissionRequest = request
            permissionGivenList.clear()
            permissionPendingList.clear()

            for (r in request.resources) {
                when (r) {
                    PermissionRequest.RESOURCE_VIDEO_CAPTURE -> {
                        if (ContextCompat.checkSelfPermission(
                                commManagerServiceImpl,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionGivenList.add(PermissionRequest.RESOURCE_VIDEO_CAPTURE)

                        } else {
                            permissionPendingList.add(Manifest.permission.CAMERA)
                        }

                    }

                    PermissionRequest.RESOURCE_AUDIO_CAPTURE -> {
                        if (ContextCompat.checkSelfPermission(
                                commManagerServiceImpl,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionGivenList.add(PermissionRequest.RESOURCE_AUDIO_CAPTURE)

                        } else {
                            permissionPendingList.add(Manifest.permission.RECORD_AUDIO)
                        }

                    }
                }
            }

            if (permissionPendingList.isEmpty()) {
                mMediaPermissionRequest?.grant(permissionGivenList.toTypedArray())

            } else {
                pictoBloxCallbacks?.promptForPermissions(permissionPendingList)
            }

        }

        override fun onPermissionRequestCanceled(request: PermissionRequest?) {
            super.onPermissionRequestCanceled(request)
        }
    }


    /*
     * WIP shared session things
     *
     *********
     */

    var isSharedSessionEnabled = false

    fun createSharedSession() {
        FirebaseAuth.getInstance().currentUser?.also { user ->

            sharedSessionDocReference =
                FirebaseFirestore.getInstance().collection("shared_sessions").document()

            sharedSessionDocReference?.set(
                mapOf(
                    "creator" to user.uid,
                    "edit_version" to 1,
                    "is_active" to true
                )
            )
                ?.addOnSuccessListener {
                    isSharedSessionEnabled = true
                    sharedSessionCallbacks?.onSessionCreated(sharedSessionDocReference!!.id, "")
                    syncNow()

                }
                ?.addOnFailureListener {
                    PictobloxLogger.getInstance().logException(it)
                }

        }
    }


    fun syncNow() {
        sharedSessionDocReference?.also {
            storageType = StorageType.SHARED_SESSION
            apiFromPictobloxWeb.saveProject("${it.id}.sb3")
            sharedSessionCallbacks?.onSyncingSession()
        }

    }

    /*
    *
    * creator
"<ID>"
edit_version
0
file_id
"<file_reference_id>"
is_active
false
    * */

    private fun syncFileWithWatchers() {
        sharedSessionDocReference?.also { documentReference ->


            val file = File(storageHandler.getSharedSessionDir(), "${documentReference.id}.sb3")

            FirebaseStorage.getInstance().getReference("shared_sessions")
                .child(documentReference.id)
                .putFile(Uri.fromFile(file))
                .addOnSuccessListener {
                    sharedSessionCallbacks?.onSessionSynced()
                    FirebaseFirestore.getInstance().collection("shared_sessions")
                        .document(documentReference.id)
                        .update("edit_version", FieldValue.increment(1))
                }
                .addOnFailureListener {
                    PictobloxLogger.getInstance().logException(it)
                    sharedSessionCallbacks?.onSessionError()

                }


        }
    }

    fun setSharedSessionCallbacks(sharedSessionCallbacks: SharedSessionCallbacks?) {
        this.sharedSessionCallbacks = sharedSessionCallbacks
    }


}

enum class Board(val stringValue: String) {
    EVIVE("evive"),
    UNO("Arduino Uno"),
    MEGA("Arduino Mega"),
    NANO("Arduino Nano"),
    ESP32("ESP32")/*,
    QUON("Quon")*/
}

enum class NavigationModalStack(val value: String, val title: String) {
    CODING_BLOCK_MODAL("coding_block_modal", ""),
    SPRITE_EDIT_MODAL("sprite_edit_modal", ""),
    STAGE_MODAL("stage_modal", ""),
    ADD_EXTENSION_MODAL("add_extension_modal", "Choose an Extension"),
    ADD_BACKDROP_MODAL("add_backdrop_modal", "Choose a Backdrop"),
    ADD_SPRITE_MODAL("add_sprite_modal", "Choose a Sprite"),
    MAKE_A_BLOCK_MODAL("make_a_block_modal", "Make a Block"),
    EXIT("exit", "")
}

interface PictoBloxCallbacks {
    @Deprecated("Won't bee called anymore as Toolbar is implemented on web side")
    fun setSprites2(targets: Pair<CommunicationHandlerWithPictoBloxWeb.Sprite2?, List<CommunicationHandlerWithPictoBloxWeb.Sprite2>>)

    //fun onModalChanged(modal: NavigationModalStack)
    fun onPromptUserInputDialog(
        argType: String,
        argPlaceholder: String?,
        handlerFunction: String,
        currValue: String?
    ): Completable

    fun exit(): Completable
    fun promptProjectSaveDialog(existAfterSave: Boolean): Completable
    fun redirectToSignUp(showSaveDialog: Boolean): Completable
    fun promptUserBoardSelectionDialog(board: Board?, isProjectChanged: Boolean): Completable
    fun promptUserForBluetoothConnection(): Completable
    fun onCourseRetry()
    fun onPictobloxReady()
    fun goToSettings()
    fun onCourseCompleted(courseFlow: CourseFlow?)
    fun promptForPermissions(permissionPendingList: List<String>)
    fun saveProject(fileName: String, byteArray: ByteArray)
    fun showSignInDialog(): Completable
    fun onSaveComplete(): Completable
    fun loadAIModel(model: String): Completable
    fun openFirmwareUploader(board: String): Completable
    fun openExternalWebLink(link: String): Completable
    fun onFileChoose(
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: WebChromeClient.FileChooserParams
    ): Boolean

    fun onsendOTGCamVideoFrame()
    fun onstopOTGCamVideoFrame()
}

interface SharedSessionCallbacks {
    fun onSessionCreated(id: String, deepLinkPath: String)
    fun onSyncingSession()
    fun onSessionSynced()
    fun onSessionEnded()
    fun onSessionError()
}

enum class PictoWebFunctions {
    OPEN_PROJECT,
    OPEN_COURSE,
    OPEN_GUIDE_TOUR,
    OPEN_COURSE_PROJECT
}

enum class PictoBloxWebLocale(val code: String, val localisedName: String) {
    ENGLISH("en", "English"),
    HINDI("hi", "हिन्दी (Hindi)"),
    KOREAN("ko", "한국인 (Korean)"),
    ITALIAN("it", "italiano (Italian)"),
    GERMAN("de", "deutsch (German)"),
    RUSSIAN("ru", "Русский (Russian)"),
    SIMPLIFIED_CHINESE("zh-cn", "简体中文 (Simplified Chinese)"),
    TRADITIONAL_CHINESE("zh-tw", "繁體中文 (Traditional Chinese)"),
}

class SensorData(dataInBytes: ByteArray) {
    val data: Array<Int>

    init {
        data = dataInBytes.map { it.toInt().and(0xff) }.toTypedArray()

    }
}
