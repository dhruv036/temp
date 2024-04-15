package io.stempedia.pictoblox.web

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.webkit.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.databinding.Observable
import androidx.databinding.Observable.OnPropertyChangedCallback
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.webkit.WebViewAssetLoader
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import io.reactivex.Completable
import io.reactivex.Single
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.connectivity.*
import io.stempedia.pictoblox.experimental.db.PictoBloxDatabase
import io.stempedia.pictoblox.firebase.COURSE_FLOW
import io.stempedia.pictoblox.firebase.CourseFlow
import io.stempedia.pictoblox.firebase.login.LoginActivity
import io.stempedia.pictoblox.quiz.QuizActivity
import io.stempedia.pictoblox.userInputArgument.ARGUMENT_CURRENT_VALUE
import io.stempedia.pictoblox.userInputArgument.ARGUMENT_PLACE_HOLDER
import io.stempedia.pictoblox.userInputArgument.ARGUMENT_TYPE
import io.stempedia.pictoblox.userInputArgument.HANDLER_FUNCTION
import io.stempedia.pictoblox.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.concurrent.CountDownLatch
import kotlin.random.Random


//Credit listener
//user doc listener
class PictoBloxWebViewModelM2(val activity: PictoBloxWebActivity) : PictoBloxCallbacks,

    AbsViewModel(activity), SharedSessionCallbacks {

    private val TAG = "PictoBloxWebViewModelM2"

    private val tempMessageArray = intArrayOf(
        R.string.loading_screen_help_1,
        R.string.loading_screen_help_2,
        R.string.loading_screen_help_3,
        R.string.loading_screen_help_4,
        R.string.loading_screen_help_5,
        R.string.loading_screen_help_6,
        R.string.loading_screen_help_7,
        R.string.loading_screen_help_8,
        R.string.loading_screen_help_9,
        R.string.loading_screen_help_10,
        R.string.loading_screen_help_11
    )

    val isLoading = ObservableBoolean(false)
    val loadingMessage = tempMessageArray[Random.nextInt(11)]
    var commManagerServiceImpl: CommManagerServiceImpl? = null
    val sessionId = ObservableField("")
    val showLoginDialog = ObservableBoolean(false)

    val showSaveProjectView = ObservableBoolean(false)
    val saveProjectViewModel = ObservableField<SaveProjectViewModel>()
    val showPopUp = ObservableBoolean(false)
    val popUpViewModel = ObservableField<PopUpViewModel>()
    var popUpData = ObservableField<PopUpData>()
    private lateinit var fileChooseCallback: ValueCallback<Array<Uri>>
    private val REQUEST_FILE_CHOOSER = 101
    private val REQUEST_STORAGE_READ_PERMISSIONS = 102
    private val REQUEST_CAMERA_PERMISSIONS = 103
    private val REQUEST_CAMERA_IMAGE = 104

    private var cameraImageUri: Uri? = null

    val showAIModelView = ObservableBoolean(false)
    val aiViewModel = ObservableField<AIResourcesRetrievalViewModel>()
    private var accountStateChangeListener: ListenerRegistration? = null
    private var creditChangeListener: ListenerRegistration? = null
    private var shouldSkipCachingTheFile = false
    private var isCreated = false
    private lateinit var mimesOncePermissionsAreAcquired: String
    var usbCamVideoFrameStatus = false

    fun onCreate() {
        isCreated = true
    }


    fun onServiceConnected(commManagerServiceImpl: CommManagerServiceImpl) {
        this.commManagerServiceImpl = commManagerServiceImpl


        commManagerServiceImpl.communicationHandler.getWebView()?.apply {
            activity.attachWebView(this)
            isLoading.set(false)
            this.webViewClient = PictoBloxWebClient(commManagerServiceImpl)
            this.setWebChromeClient(object : WebChromeClient() {
                override fun onPermissionRequest(request: PermissionRequest) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        request.grant(request.resources)
                        if (ContextCompat.checkSelfPermission(
                                commManagerServiceImpl,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
//                            Toast.makeText(activity, "Granted", Toast.LENGTH_LONG).show()
                        }

                    }
                }
            })
            commManagerServiceImpl.communicationHandler.setPictobloxCallbacks(this@PictoBloxWebViewModelM2)

            commManagerServiceImpl.communicationHandler.setSharedSessionCallbacks(this@PictoBloxWebViewModelM2)

            //here we initiate start sequence immediately because pictoblox is already initialized.
            commManagerServiceImpl.communicationHandler.setStartSequence(FirebaseAuth.getInstance().currentUser?.uid)
            PictobloxLogger.getInstance()
                .logd("onServiceConnected shouldSkipCachingTheFile : ${shouldSkipCachingTheFile}")
            if (isCreated && !shouldSkipCachingTheFile) {
                Log.e(TAG, "before onServiceConnected: ")
                commManagerServiceImpl.communicationHandler.openProject()
                showSnackIfRequired()
            }

            setCreditChangeListener()
            setUserDocChangeListener()

        } ?: run {
            isLoading.set(true)
            val webView = activity.inflateWebView()
            webView.webViewClient = PictoBloxWebClient(commManagerServiceImpl)
            commManagerServiceImpl.communicationHandler.setWebView(webView)
            commManagerServiceImpl.communicationHandler.setPictobloxCallbacks(this)
            commManagerServiceImpl.communicationHandler.setSharedSessionCallbacks(this@PictoBloxWebViewModelM2)
        }

        isCreated = false
    }

    fun addPopUp(popdata : PopUpData){
//        Toast.makeText(activity,"122",Toast.LENGTH_SHORT).show()
        popUpData.set(popdata)
    }

    fun onBeforeServiceGetsDisconnected() {

        //When webview caching is disabled
        //commManagerServiceImpl?.communicationHandler?.clearWebViewReference()
        if (!shouldSkipCachingTheFile) {
            commManagerServiceImpl?.communicationHandler?.cacheCurrentWorkIfApplicable()
        }
        commManagerServiceImpl?.communicationHandler?.setPictobloxCallbacks(null)
        commManagerServiceImpl?.communicationHandler?.setSharedSessionCallbacks(null)
        commManagerServiceImpl?.communicationHandler?.setStopSequence()

        commManagerServiceImpl?.communicationHandler?.clearWebViewReference()

        activity.detachWebView()
        //cancel if download is ongoing
        aiViewModel.get()?.onDispose()

        creditChangeListener?.remove()
        accountStateChangeListener?.remove()

    }

//    fun getFrame(frame: String?) {
//        commManagerServiceImpl?.communicationHandler?.getAllFrame(frame)
//    }

    private fun setCreditChangeListener() {
        FirebaseAuth.getInstance().currentUser?.also { user ->

            creditChangeListener = firebaseUserCredits(user.uid)
                .addSnapshotListener { documentSnapshot, error ->
                    if (error != null) {
                        PictobloxLogger.getInstance().logException(error)
                        return@addSnapshotListener
                    }

                    if (documentSnapshot != null) {
                        documentSnapshot.getLong("pictoblox_credits")?.also { credits ->
                            commManagerServiceImpl?.communicationHandler?.updateUserCredit(credits)

                        }

                    }
                }


        }
    }

    private fun setUserDocChangeListener() {
        FirebaseAuth.getInstance().currentUser?.also { user ->

            accountStateChangeListener = firebaseUserDetail(user.uid)
                .addSnapshotListener { documentSnapshot, error ->
                    if (error != null) {
                        PictobloxLogger.getInstance().logException(error)
                        return@addSnapshotListener
                    }
                    commManagerServiceImpl?.communicationHandler?.updateUserAccountStatus(
                        parseStatus(documentSnapshot)
                    )
                }


        }
    }


    override fun setSprites2(targets: Pair<CommunicationHandlerWithPictoBloxWeb.Sprite2?, List<CommunicationHandlerWithPictoBloxWeb.Sprite2>>) {

    }

    /*   override fun onModalChanged(modal: NavigationModalStack) {

       }
   */

    override fun onSaveComplete() = Completable.create {
        saveProjectViewModel.get()?.onSaveFinished()
        it.onComplete()
    }

    fun cacheProject() {
        saveProjectViewModel.get()?.onSaveClicked()
    }

    override fun promptForPermissions(permissionPendingList: List<String>) {
        activity.askForPermissions(permissionPendingList)
    }

    override fun saveProject(fileName: String, byteArray: ByteArray) {
        TODO("Not yet implemented")
    }


    override fun showSignInDialog(): Completable {
        return Completable.create { emitter ->
            PictobloxLogger.getInstance().logd("getUser token from showSignInDialog")

            /*if (FirebaseAuth.getInstance().currentUser != null) {
                commManagerServiceImpl?.communicationHandler?.apiFromPictobloxWeb?.setUserId(
                    FirebaseAuth.getInstance().currentUser?.uid
                )

            } else {
                if (!showLoginDialog.get()) {
                    showLoginDialog.set(true)
                    activity.showSignInDialog()
                }
            }*/
            if (!showLoginDialog.get()) {
                showLoginDialog.set(true)
                activity.showSignInDialog()
            }

            emitter.onComplete()

        }
    }

    fun onDismissLoginDialog() {
        activity.hideSignInDialog()
        activity.hideKeyboard()
        showLoginDialog.set(false)
        popUpData.set(null)
        popUpViewModel.set(null)
    }

    override fun onPromptUserInputDialog(
        argType: String,
        argPlaceholder: String?,
        handlerFunction: String,
        currValue: String?,
    ) = Completable.create {

        val bundle = Bundle().apply {
            putString(ARGUMENT_TYPE, argType)
            putString(ARGUMENT_CURRENT_VALUE, currValue)
            putString(ARGUMENT_PLACE_HOLDER, argPlaceholder)
            //putString(ARGUMENT_PARAM, argParam)
            putString(HANDLER_FUNCTION, handlerFunction)
        }

        activity.showUserInputDialog(bundle)
        it.onComplete()
    }


    override fun exit() = Completable.create {
        activity.finish()
        it.onComplete()
    }

    override fun promptProjectSaveDialog(existAfterSave: Boolean): Completable =
        Single.create<SaveProjectViewModel> {

            if (commManagerServiceImpl != null) {
                val fileName =
                    with(commManagerServiceImpl!!.communicationHandler.storageHandler.openingFileName) {
                        if (trim().lowercase().endsWith(".sb3")) {
                            trim().substring(0, length - 4)

                        } else {
                            this
                        }
                    }

                it.onSuccess(
                    SaveProjectViewModel(
                        activity,
                        commManagerServiceImpl!!,
                        fileName,
                        ObservableField(activity.getString(R.string.save)), this,
                        existAfterSave,
                        false
                    )
                )
            } else {
                it.onError(Exception("Service not found"))
            }
        }.doOnSuccess {
                showSaveProjectView.set(true)
                saveProjectViewModel.set(it)
        }.ignoreElement()

    override fun redirectToSignUp(showSaveDialog: Boolean): Completable {

        return Completable.create { emitter ->
            if (showSaveDialog) {
                if (commManagerServiceImpl != null) {
                    val fileName =
                        with(commManagerServiceImpl!!.communicationHandler.storageHandler.openingFileName) {
                            if (trim().toLowerCase().endsWith(".sb3")) {
                                trim().substring(0, length - 4)

                            } else {
                                this
                            }
                        }

                    showSaveProjectView.set(true)
                    saveProjectViewModel.set(
                        SaveProjectViewModel(
                            activity,
                            commManagerServiceImpl!!,
                            fileName,
                            ObservableField(activity.getString(R.string.save)), this,
                            exitAfterSave = true,
                            redirectToSignUp = true
                        )
                    )

                } else {
                    emitter.onError(Exception("Service not found"))
                }

            } else {
                activity.startActivity(Intent(activity, LoginActivity::class.java))
                emitter.onComplete()
            }
        }
    }

    data class PopUpData(val popUpId: String,val title: String, val buttonText:String, val link:String)

    override fun promptUserBoardSelectionDialog(board: Board?, isProjectChanged: Boolean) =
        Completable.create {
            val boardArray: Array<String> =
                Board.values().map { b -> run { b.stringValue } }.toTypedArray()

            val selectedPos = board?.let { board ->
                run {
                    boardArray.indexOf(board.stringValue)

                }
            } ?: run {
                -1
            }

            /*commManagerServiceImpl?.communicationHandler?.getSelectedBoard()?.apply {
                selectedPos = boardArray.indexOf(this.stringValue)

            }*/

            activity.showBoardSelectionDialog(boardArray, selectedPos, isProjectChanged)

            it.onComplete()
        }

    override fun openFirmwareUploader(board: String): Completable {

        val frag = OTGFirmwareUploadFragment()

        frag.arguments = Bundle().apply {
            putString(OTG_BOARD_NAME, board)
        }

        return Completable.create { emitter ->
            activity
                .supportFragmentManager
                .beginTransaction()
                //.setReorderingAllowed()
                .addToBackStack(null)
                .replace(R.id.fl_firmware_container, frag)
                .commit()
            emitter.onComplete()
        }
    }

    fun onBoardSelected(selectedPos: Int, isProjectChanged: Boolean) {
        if (isProjectChanged) {
            activity.showBoardConfirmationDialog(Board.values()[selectedPos], isProjectChanged)

        } else {
            commManagerServiceImpl?.communicationHandler?.setBoardSelected(
                Board.values()[selectedPos],
                isProjectChanged
            )
        }
    }

    fun onBoardSelectionConfirmed(selectedBoard: Board, isProjectChanged: Boolean) {
        commManagerServiceImpl?.communicationHandler?.setBoardSelected(
            selectedBoard,
            isProjectChanged
        )
    }


    override fun promptUserForBluetoothConnection() = Completable.create {
        activity.handleDeviceClick()
        //loadAIModel("objectDetection").subscribe()
        it.onComplete()
    }

    override fun onCourseRetry() {
        PictobloxLogger.getInstance().logd("User retrying")
    }

    override fun onPictobloxReady() {

//        Toast.makeText(activity, "Started", Toast.LENGTH_SHORT).show()
        Log.e(TAG, "onPictobloxReady: 1", )
        popUpData.addOnPropertyChangedCallback(object : OnPropertyChangedCallback(){
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
//                Toast.makeText(activity, "change", Toast.LENGTH_SHORT).show()
                popUpData.get()?.let {
                    popUpViewModel.set(PopUpViewModel(this@PictoBloxWebViewModelM2, it))
                    CoroutineScope(Dispatchers.IO).launch{
                        PictoBloxDatabase.getDatabase(activity).popUpCountDao().updatePopUpCount(it.popUpId)
                    }
                }
                showPopUp.set(true)
            }
        })
    }

    override fun onCourseCompleted(courseFlow: CourseFlow?) {

        val intent = Intent(activity, QuizActivity::class.java).apply {
            putExtra(COURSE_FLOW, courseFlow)
        }

        activity.startActivity(intent)
        activity.finish()
    }

    override fun loadAIModel(model: String) = Completable.create {
        commManagerServiceImpl?.also { commManagerServiceImpl ->
            val aiVM = AIResourcesRetrievalViewModel(
                activity,
                commManagerServiceImpl,
                this@PictoBloxWebViewModelM2,
                model
            )
            aiViewModel.set(aiVM)
            showAIModelView.set(true)

        }
        it.onComplete()
    }

    override fun openExternalWebLink(link: String): Completable {
        return Completable.create { emitter ->

            val stemLinkIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
            if (stemLinkIntent.resolveActivity(activity.packageManager) != null) {
                activity.startActivity(stemLinkIntent)

            } else {
                Toast.makeText(activity, "Cannot open external link", Toast.LENGTH_LONG).show()

            }

            emitter.onComplete()
        }
    }


    fun dismissLoadingAIDialog() {
        showAIModelView.set(false)
    }

    fun dismissPopUpDialog() {
        showPopUp.set(false)
    }


    /*
    *
    *
    *
    *
    *
    *
    *
    *
    *
    *
     */

    inner class PictoBloxWebClient(val commManagerServiceImpl: CommManagerServiceImpl) :
        WebViewClient() {

        private val pictoCacheDir = File(commManagerServiceImpl.cacheDir, "pictoDir")

        private val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler(
                "/ai/",
                WebViewAssetLoader.InternalStoragePathHandler(
                    activity,
                    commManagerServiceImpl.communicationHandler.storageHandler.getAIModelFileDir()
                )
            ).addPathHandler(
                "/external_picto/",
                WebViewAssetLoader.InternalStoragePathHandler(
                    activity,
                    File(pictoCacheDir, "pictoBloxUnzipped")
                )
            ).addPathHandler(
                "/user_projects/",
                WebViewAssetLoader.InternalStoragePathHandler(
                    activity,
                    commManagerServiceImpl.communicationHandler.storageHandler.getSb3FileDir()
                )
            ).addPathHandler(
                "/cached_projects/",
                WebViewAssetLoader.InternalStoragePathHandler(
                    activity,
                    commManagerServiceImpl.communicationHandler.storageHandler.getSb3CacheFileDir()
                )
            ).addPathHandler(
                "/example_projects/",
                WebViewAssetLoader.InternalStoragePathHandler(
                    activity,
                    commManagerServiceImpl.communicationHandler.storageHandler.getExampleFilesDir()
                )
            )
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(activity))
            .build()

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            //isLoading.set(true)
            PictobloxLogger.getInstance().logd("onPageStarted")
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            if (isLoading.get()) {
                isLoading.set(false)
                PictobloxLogger.getInstance().logd("onPageFinished")

                //isLoading.set(false)

                /*commManagerServiceImpl.communicationHandler.getWebView()?.apply {
                    webViewClient = null
                }*/

                //here we wil wait for pictoblox to be loaded and then initiate start sequence
                commManagerServiceImpl.communicationHandler.openProjectOncePictobloxIsReady()

                showSnackIfRequired()
                setCreditChangeListener()
                setUserDocChangeListener()
            }

        }

        override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
            return assetLoader.shouldInterceptRequest(Uri.parse(url))
        }

        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?,
        ): WebResourceResponse? {
            return assetLoader.shouldInterceptRequest(request!!.url)
        }
    }

    private fun showSnackIfRequired() {
        commManagerServiceImpl?.communicationHandler?.storageHandler?.apply {
            if (getFileType() == StorageType.CACHE) {
                activity.showOpenedProjectSnack(openingFileName)
            }
        }
    }

    fun onSyncWithSessionClicked() {
        activity.z()
    }

    fun onUploadSessionClicked() {
        FirebaseAuth.getInstance().currentUser?.also {
            commManagerServiceImpl?.communicationHandler?.also { handler ->
                if (handler.isSharedSessionEnabled) {
                    handler.syncNow()

                } else {
                    handler.createSharedSession()
                }
            }

        } ?: run {
            Toast.makeText(
                activity,
                "You need to be logged in to user this feature",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onSessionCreated(id: String, deepLinkPath: String) {
        sessionId.set(id)
    }

    override fun onSyncingSession() {
        PictobloxLogger.getInstance().logd("onSyncingSession")
    }

    override fun onSessionSynced() {
        PictobloxLogger.getInstance().logd("onSessionSynced")
    }

    override fun onSessionEnded() {
        PictobloxLogger.getInstance().logd("onSessionEnded")
    }

    override fun onSessionError() {
        PictobloxLogger.getInstance().logd("onSessionError")

    }

    fun onCameraPermissionResult(grantedPermissions: IntArray) {
        commManagerServiceImpl?.communicationHandler?.onPermissionsResult(grantedPermissions)
    }

    fun dismissSaveDialog() {
        showSaveProjectView.set(false)
    }

    fun onSignInVerified() {
        PictobloxLogger.getInstance().logd("getUser token from onSignInVerified")
        onDismissLoginDialog()
        FirebaseAuth.getInstance().currentUser?.also { user ->
            firebaseUserDetail(user.uid)
                .get()
                .addOnCompleteListener {
                    commManagerServiceImpl?.communicationHandler?.apiFromPictobloxWeb?.setUserId(
                        user.uid
                    )
                    if (it.isSuccessful) {
                        commManagerServiceImpl?.communicationHandler?.apiFromPictobloxWeb?.onUserStateUpdated(
                            parseStatus(it.result)
                        )
                        commManagerServiceImpl?.communicationHandler?.apiFromPictobloxWeb?.loadExtensionAfterSignIning()

                    } else {
                        commManagerServiceImpl?.communicationHandler?.apiFromPictobloxWeb?.onUserStateUpdated(
                            parseStatus(null)
                        )
                    }
                }
        }
    }

    fun parseStatus(documentSnapshot: DocumentSnapshot?): String {
        return if (documentSnapshot != null) {
            val stage1Completed = documentSnapshot.getString("email") != null
            val accountVerified = documentSnapshot.getBoolean("is_verified") ?: false
            val stage2Completed = documentSnapshot.getBoolean("is_secondary_detail_filled") ?: false

            if (!stage1Completed) {
                "ANONYMOUS"
            } else if (stage1Completed && !accountVerified) {
                "NOT_VERIFIED"
            } else if (stage1Completed && accountVerified && !stage2Completed) {
                "VERIFIED"
            } else if (stage1Completed && accountVerified && stage2Completed) {
                "COMPLETED"
            } else {
                "UNKNOWN"
            }
        } else {
            "UNKNOWN"
        }

    }

    fun goToAppSetting() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", activity.packageName, null)
        intent.data = uri
        activity.startActivityForResult(intent, 103)
    }

    override fun goToSettings() {
        goToAppSetting()
    }

    fun handleBackPress(): Boolean {
        return if (showLoginDialog.get()) {
            onDismissLoginDialog()
            false
        } else {
            true
            //Todo forward to
        }
    }

    fun showFeedbackDialog() {
        AlertDialog.Builder(activity)
            .setTitle("Feedback")
            .setMessage("Please share your valuable feedback on PictoBlox")
            .setCancelable(false)
            .setPositiveButton("Okay") { d, _ ->

                val stemLinkIntent =
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://bit.ly/Feedback-PictoBlox"))
                activity.startActivity(stemLinkIntent)

                SPManager(activity).isFeedbackFormShownForThisVersion = true

                d.dismiss()
            }

            .setNegativeButton("Maybe later") { d, _ ->
                d.dismiss()
            }
            .create()
            .show()
    }


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onFileChoose(
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: WebChromeClient.FileChooserParams,
    ): Boolean {
//        Toast.makeText(activity, "onFileChoose", Toast.LENGTH_SHORT).show()
        shouldSkipCachingTheFile = true
        PictobloxLogger.getInstance()
            .logd("onFileChoose shouldSkipCachingTheFile : $shouldSkipCachingTheFile")
        fileChooseCallback = filePathCallback
        if (fileChooserParams.isCaptureEnabled) {
            checkPermissionsCamera()

        } else {
            mimesOncePermissionsAreAcquired =
                if (fileChooserParams.acceptTypes.contains(".mp3") || fileChooserParams.acceptTypes.contains(
                        ".wav"
                    ) || fileChooserParams.acceptTypes.contains(".m4a")
                ) {
                    //arrayOf("audio/mp3", "audio/wav")
                    "audio/*"
                } else if (fileChooserParams.acceptTypes.contains(".png") || fileChooserParams.acceptTypes.contains(
                        ".jpg"
                    )
                ) {
                    //arrayOf("image/jpg", "image/png", "image/png", "image/png", "image/jpeg", "image/gif")
                    "image/*"
                } else {
                    //emptyArray()
                    ""
                }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if ((checkSelfPermission(
                        activity,
                        Manifest.permission.READ_MEDIA_IMAGES
                    ) == PackageManager.PERMISSION_GRANTED)
                ) {
                    pickFiles(mimesOncePermissionsAreAcquired)
                } else {
                    requestPermissions(
                        activity,
                        arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                        REQUEST_STORAGE_READ_PERMISSIONS
                    )
                }
            } else {
                if ((checkSelfPermission(
                        activity,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED)
                ) {
                    pickFiles(mimesOncePermissionsAreAcquired)
                } else {
                    requestPermissions(
                        activity,
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        REQUEST_STORAGE_READ_PERMISSIONS
                    )
                }
            }

        }
        return true

    }

    override fun onsendOTGCamVideoFrame() {
        Log.e(TAG, "onsendOTGCamVideoFrame: ")
        usbCamVideoFrameStatus = true
    }

    override fun onstopOTGCamVideoFrame() {
        Log.e(TAG, "onstopOTGCamVideoFrame: ")
        usbCamVideoFrameStatus = false

    }

    private fun checkPermissionsCamera() {
        if ((checkSelfPermission(
                activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_DENIED)
            || (checkSelfPermission(
                activity,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_DENIED)
        ) {
            val permission = arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
            )

            requestPermissions(
                activity,
                permission,
                REQUEST_CAMERA_PERMISSIONS
            ) // GIVE AN INTEGER VALUE FOR PERMISSION_CODE_READ LIKE 1001
        } else {

            getImageFromCamera()
        }
    }

    private fun getImageFromCamera() {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "New Image")
            put(MediaStore.Images.Media.DESCRIPTION, "From Camera")

        }
        cameraImageUri = activity.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
            startActivityForResult(activity, this, REQUEST_CAMERA_IMAGE, null)
        }
    }

    fun onActivityResultViewModel(requestCode: Int, resultCode: Int, data: Intent?) {

        when (requestCode) {
            REQUEST_FILE_CHOOSER -> {
                shouldSkipCachingTheFile = false
                PictobloxLogger.getInstance()
                    .logd("onActivityResultViewModel shouldSkipCachingTheFile : $shouldSkipCachingTheFile")
                if (resultCode == RESULT_OK) {
                    if (data == null) {
                        Toast.makeText(activity, "No File Selected.", Toast.LENGTH_SHORT).show()
                        fileChooseCallback.onReceiveValue(null)
                    } else {
                        if (fileChooseCallback != null) {
                            val result = Uri.parse(data.dataString)
                            fileChooseCallback.onReceiveValue(arrayOf(result))
                        }
                    }
                } else {
                    Toast.makeText(activity, "No File Selected.", Toast.LENGTH_SHORT).show()
                    fileChooseCallback.onReceiveValue(null)
                }
                //fileChooseCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data))
            }

            REQUEST_CAMERA_IMAGE -> {
                if (resultCode == RESULT_OK) {
                    fileChooseCallback.onReceiveValue(arrayOf(cameraImageUri!!))
                } else {
                    Toast.makeText(activity, "No File Selected.", Toast.LENGTH_SHORT).show()
                    if (fileChooseCallback != null) {
                        fileChooseCallback.onReceiveValue(null)
                    }
                }
            }
        }
    }

    fun onRequestPermissionsResultViewModel(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        when (requestCode) {
            REQUEST_STORAGE_READ_PERMISSIONS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        pickFiles(mimesOncePermissionsAreAcquired)
                    } else {
                        Toast.makeText(
                            activity,
                            "Please grant Photos and Media permissions.",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                        goToAppSetting()
                    }
                } else {
                    if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        pickFiles(mimesOncePermissionsAreAcquired)
                    } else {
                        Toast.makeText(
                            activity,
                            "Please grant storage permissions.",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                        goToAppSetting()
                    }
                }
            }

            REQUEST_CAMERA_PERMISSIONS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getImageFromCamera()
                } else {
                    Toast.makeText(activity, "Please grant camera permissions.", Toast.LENGTH_SHORT)
                        .show()
                    goToAppSetting()
                }
            }
        }
    }

    private fun pickFiles(mime: String) {
        val intentFileChooser = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            //type = "*/*"
            type = mime
        }
        //intentFileChooser.putExtra(Intent.EXTRA_MIME_TYPES, mimes)
        Log.d(TAG, "CHOOSE IMAGE")
        startActivityForResult(activity, intentFileChooser, REQUEST_FILE_CHOOSER, null)
    }

}