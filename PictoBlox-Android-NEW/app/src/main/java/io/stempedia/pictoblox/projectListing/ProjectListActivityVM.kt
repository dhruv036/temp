package io.stempedia.pictoblox.projectListing

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.Settings
import android.text.Editable
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.ktx.Firebase
import com.google.zxing.client.android.Intents
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableCompletableObserver
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import io.stempedia.pictoblox.QR.CustomScanner
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.connectivity.CommManagerServiceImpl
import io.stempedia.pictoblox.connectivity.CommunicationHandlerWithPictoBloxWeb
import io.stempedia.pictoblox.databinding.DialogTutsBinding
import io.stempedia.pictoblox.firebase.login.LoginActivity
import io.stempedia.pictoblox.profile.ProfileActivity
import io.stempedia.pictoblox.util.*
import io.stempedia.pictoblox.util.PathUtil.getPath
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.lang.Exception
import java.nio.channels.FileChannel
import java.nio.file.*
import java.text.SimpleDateFormat
import java.util.*


class ProjectListActivityVM(val activity: ProjectListActivity) : AccountHandlerCallback {
    private  val TAG = "ProjectListActivityVM"
    val selectedTab = ObservableInt()
    val queryText = ObservableField<String>()
    private val  requestExternalFile = 120
    private val REQUEST_DIRECTORY_CHOOSER = 99
//    private val MANAGE_STORAGE_REQUEST_CODE = 101
    private var isFirstTime = true
    //This flag is declared both here and at fragment level. Here only because we need to switch toolbar functions. It is set only from fragment.
    val isSelectionEnabled = ObservableBoolean(false)
    private var commManagerService: CommManagerServiceImpl? = null
    val isCachedVersionAvailable = ObservableBoolean(false)
    private lateinit var spManager: SPManager
    val profileIcon = ObservableField<Bitmap>()
    val showProfileIncompleteError = ObservableBoolean(false)
    val isDeleteClicked = ObservableBoolean(false)
    val isShareClicked = ObservableBoolean(false)

    val accountIconHandler = AccountIconHandler(activity, this)

    val showExternalFileLoading = ObservableBoolean(false)
    val showShareList = ObservableBoolean(false)
    val shareType = ObservableInt(0)
    private var disposable: Disposable? = null
    private var shouldCopyAllFiles = false // true for all files. false for only selected files

    //private var externalUri: Uri? = null
    private var intent: Intent? = null

    val showCreateLinkIcon = ObservableBoolean()
    val showCreateLinkView = ObservableBoolean()

    val linkCreationViewModel = ObservableField<LinkCreationViewModel>()

    private val barcodeLauncher: ActivityResultLauncher<ScanOptions?>? = activity.registerForActivityResult<ScanOptions, ScanIntentResult>(
        ScanContract()
    ) { result: ScanIntentResult ->
        if (result.contents == null) {
            val originalIntent = result.originalIntent
            if (originalIntent == null) {
//                Log.d("MainActivity", "Cancelled scan")
                    Toast.makeText(activity, "Cancelled", Toast.LENGTH_LONG).show()
                } else if (originalIntent.hasExtra(Intents.Scan.MISSING_CAMERA_PERMISSION)) {
                    Log.d("MainActivity", "Cancelled scan due to missing camera permission")
                    Toast.makeText(
                        activity,
                        "Cancelled due to missing camera permission",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                checkQRUrl(result.contents.toUri().toString())
            }
        }

    fun checkQRUrl(url: String) {
        if (!url.contains("pictoblox.page.link")) {
            Toast.makeText(activity, activity.resources.getString(R.string.qr_error), Toast.LENGTH_LONG).show()
            Log.e("TAG", "1")
            return
        } else {
            val intent = Intent(activity, ProjectListActivity::class.java)
            intent.data = url.toUri()
            handleDeepLink(intent)
        }
    }

    fun checkClicked(){
        isShareClicked.set(false)
        isDeleteClicked.set(false)
        showShareList.set(false)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun setShareType(choice: Int) {
        when (choice) {
            1 -> {
                shareType.set(1)
                onCreatePublicLink()
                showShareList.set(false)
                isShareClicked.set(false)
            }

            2 -> {
                shareType.set(2)
//                onExportClicked()
                startExport()
                showShareList.set(false)
                isShareClicked.set(false)
            }

            3 -> {
                shareType.set(3)
                when (selectedTab.get()) {
                    0 -> {
                        Log.e(TAG, "setShareType: Recent clicked", )
                        activity.getRecentFragment().getViewModel().shareSelectedFiles()
                    }

                    1 -> {
                        Log.e(TAG, "setShareType: Local clicked", )
                        activity.getLocalFragment().getViewModel().shareSelectedFiles()
                    }
                }
                isShareClicked. set(false)
                showShareList.set(false)
            }
        }
    }

    fun onServiceConnected(commManagerServiceImpl: CommManagerServiceImpl) {
        this.commManagerService = commManagerServiceImpl
        spManager = SPManager(activity)
        checkBoxClicked(1)
        isCachedVersionAvailable.set(commManagerServiceImpl.communicationHandler.storageHandler.isCachedVersionExists())
        intent?.data?.also { uri ->

            if (uri.host == "pictoblox.page.link") {
                Log.e("Method", "3")
                handleDeepLink(intent!!)

            } else if (intent?.action == Intent.ACTION_VIEW) {
                Log.e("Method", "1")
                handleExternalFile(uri)

            } else if (intent?.getStringExtra("url") != null) {
                Log.e("Method", "2")
                handleDeepLink(intent!!)
            }
            intent = null
        }
        //commManagerServiceImpl.communicationHandler.storageHandler.copyCacheFileToInternalFile()
    }
    fun onBeforeServiceGetsDisconnected(commManagerServiceImpl: CommManagerServiceImpl) {
        //isSelectionEnabled.set(false)
        //showCreateLinkIcon.set(false)
    }

    fun onCreate(intent: Intent?) {
        accountIconHandler.onCreate()
        this.intent = intent
    }

    fun onNewIntent(intent: Intent?) {
        this.intent = intent
    }

    fun onResume() {
        accountIconHandler.onResume()
    }

    fun onWindowFocusChanged(hasFocus: Boolean) {
        accountIconHandler.onWindowFocusChanged(hasFocus)
    }

    // search project
    fun afterTextChanged(s: Editable) {
        when (selectedTab.get()) {
            0 -> {
                activity.getRecentFragment().getViewModel().refreshData()
            }

            1 -> {
                activity.getLocalFragment().getViewModel().refreshData()
            }
        }
    }

    fun checkBoxClicked(pos: Int) {
        selectedTab.set(pos)

        when (pos) {
            0 -> {
                activity.populateRecentContent()
            }

            1 -> {
                activity.populateLocalContent()
            }

            2 -> {
                activity.populateCloudContent()
            }
        }
    }

    fun onNewProjectFABClicked() {
        commManagerService?.communicationHandler?.also {
            if (!spManager.hasUserSeenTour) {
                spManager.hasUserSeenTour = true
                popTutDialog(it)

            } else {
                loadEmptyProject(it)
            }
        }
    }

    fun openProject() {
        if (activity.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            scanQR()
        } else {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.CAMERA), 103)
        }
    }

    fun scanQR() {
        val options = ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE).setOrientationLocked(false).setCaptureActivity(CustomScanner::class.java)
        barcodeLauncher!!.launch(options)
    }

    fun opOpenCachedProjectClicked() {
        commManagerService?.communicationHandler?.also {
            activity.add(
                it.loadCachedProject()
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(object : DisposableCompletableObserver() {

                        override fun onComplete() {
                            activity.startPictobloxWeb()
                        }

                        override fun onError(e: Throwable) {
                            activity.errorInOpeningCachedFile(e)
                        }

                    })
            )
        }
    }

    fun onFolderIconClicked() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            //! ONLY SP3 FILE SHOULD SEE
            type = "application/octet-stream"
        }

        activity.startActivityForResult(intent, requestExternalFile)

        /*FirebaseAnalytics
            .getInstance(activity)
            .logEvent(MY_SPACE_EXTERNAL_FILE_OPEN, null)*/
    }

    //On activity result for above call
    fun onActivityResultVM(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            requestExternalFile -> {
                if (resultCode == Activity.RESULT_OK) {

                    data?.apply {
                        this.data?.apply {
                            handleExternalFile(this)

                        } ?: run {
                            activity.showErrorInRetrievingFile()
                        }

                    } ?: run {
                        activity.showErrorInRetrievingFile()
                    }

                } else {
                    activity.showNoFileSelectedMessage()
                }
            }

            REQUEST_DIRECTORY_CHOOSER -> {
                if (data == null) {
                    Toast.makeText(activity, "No Directory Selected", Toast.LENGTH_SHORT).show()
                }
                else {
                    startExport()
                }
            }
        }
    }
    fun goToAppSetting(){
        Toast.makeText(activity,"Please provide camera permission",Toast.LENGTH_SHORT).show()
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", activity.packageName, null)
        intent.data = uri
        activity.startActivityForResult(intent, 102)
    }
    fun startExport(){
        copyProjectsInternalToExternal()
    }

    private fun handleExternalFile(uri: Uri) {
        showExternalFileLoading.set(true)
        disposable = commManagerService?.communicationHandler?.loadExternalProject(uri)
            ?.subscribeOn(Schedulers.computation())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribeWith(object : DisposableCompletableObserver() {

                override fun onComplete() {
                    activity.startPictobloxWeb()
                    showExternalFileLoading.set(false)
                    //intent = null
                    //externalUri = null
                }

                override fun onError(e: Throwable) {
                    activity.errorInOpeningExternalFile(e)
                    showExternalFileLoading.set(false)
                    //intent = null
                    //externalUri = null
                    Toast.makeText(activity, "Error in loading external file", Toast.LENGTH_LONG).show()

                }

            })

        disposable?.apply {
            activity.add(this)
        }
    }


    private fun handleDeepLink(intent: Intent) {
        showExternalFileLoading.set(true)

        Firebase.dynamicLinks
            .getDynamicLink(intent)
            .addOnSuccessListener(activity) { pendingDynamicLinkData ->
                if (pendingDynamicLinkData != null && pendingDynamicLinkData.link != null) {
                    disposable = commManagerService?.communicationHandler?.loadDeepLink(pendingDynamicLinkData.link!!)
                        ?.subscribeOn(Schedulers.computation())
                        ?.observeOn(AndroidSchedulers.mainThread())
                        ?.subscribeWith(object : DisposableSingleObserver<String>() {

                            override fun onError(e: Throwable) {
                                activity.errorInOpeningExternalFile(e)
                                showExternalFileLoading.set(false)
                                //this@ProjectListActivityVM.intent = null
                                //externalUri = null
                                Toast.makeText(activity, "Error in loading external file", Toast.LENGTH_LONG).show()

                            }

                            override fun onSuccess(t: String) {
                                activity.startPictobloxWeb()
                                showExternalFileLoading.set(false)
                                //this@ProjectListActivityVM.intent = null
                                //externalUri = null
                            }

                        })

                    disposable?.apply {
                        activity.add(this)
                    }
                }

            }
            .addOnFailureListener(activity) { e ->
                showExternalFileLoading.set(false)
                PictobloxLogger.getInstance().logException(e)
            }


    }

    fun shouldExitOnBackPress(): Boolean {
        when (selectedTab.get()) {
            0 -> {
                return if (isSelectionEnabled.get()) {
                    activity.getRecentFragment().getViewModel().disableItemMSelection()
                    false

                } else {
                    true
                }
            }

            1 -> {
                return if (isSelectionEnabled.get()) {
                    activity.getLocalFragment().getViewModel().disableItemMSelection()
                    false

                } else {
                    true
                }
            }

            else -> {
                return true
            }
        }
    }

    fun setSelectionFlag(get: Boolean) {
        isSelectionEnabled.set(get)
    }

    fun onHelpClicked() {
        activity.showHelp()
    }

    fun onSettingsClicked() {
        activity.showSettings()
    }

    fun onDeleteClicked() {
        isDeleteClicked.set(true)
        if (showShareList.get()) showShareList.set(false)
        if (isShareClicked.get()) isShareClicked.set(false)
        when (selectedTab.get()) {
            0 -> {
                activity.getRecentFragment().getViewModel().deleteSelectedFiles()
            }
            1 -> {
                activity.getLocalFragment().getViewModel().deleteSelectedFiles()
            }
        }
    }

    fun onAccountClicked() {
        accountIconHandler.onAccountClicked()
    }

    var callbackRef: SharePopUpCallback? = null

    fun showPopUpcallback(callback: SharePopUpCallback) {
        callbackRef = callback
    }

    interface SharePopUpCallback {
        fun showPopUp()
    }

    fun onShareClicked() {
        showShareList.set(true)
        isShareClicked.set(true)
        if (isDeleteClicked.get()) isDeleteClicked.set(false)
    }

    @RequiresApi(Build.VERSION_CODES.R)
//    fun onExportClicked() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            if (hasAllFilesPermission()) {
//                askUserForDirectory()
//            } else {
//                val uri = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
//                activity.startActivity(
//                    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri)
//                )
//            }
//        } else {
//            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
//            ) {
//                checkPermissionForStorage()
//            } else {
//                askUserForDirectory()
//            }
//        }
//    }

    fun showComingSoon() {
        Toast.makeText(activity, activity.resources.getString(R.string.comming_soon), Toast.LENGTH_LONG).show()
    }

    private fun onStartTutorials(handler: CommunicationHandlerWithPictoBloxWeb) {
        activity.add(
            handler.loadTour().subscribeWith(object : DisposableCompletableObserver() {
                override fun onComplete() {
                    activity.startPictobloxWeb()
                }

                override fun onError(e: Throwable) {
                    //TODO
                }
            })

        )
    }

    private fun loadEmptyProject(handler: CommunicationHandlerWithPictoBloxWeb) {
        activity.add(
            handler.loadEmptyProject().subscribeWith(object : DisposableCompletableObserver() {
                override fun onComplete() {
                    activity.startPictobloxWeb()
                }

                override fun onError(e: Throwable) {
                    activity.errorInOpeningEmptyFile(e)
                }

            })
        )
    }

    private fun onSkipTutorials(handler: CommunicationHandlerWithPictoBloxWeb) {
        loadEmptyProject(handler)
    }


    private fun popTutDialog(handler: CommunicationHandlerWithPictoBloxWeb) {

        val mBinding = DataBindingUtil.inflate<DialogTutsBinding>(activity.layoutInflater, R.layout.dialog_tuts, null, false)

        val dialog = AlertDialog.Builder(activity)
            .setView(mBinding.root)
            .create()

        mBinding.tvTutContinue.setOnClickListener {
            dialog.dismiss()
            onStartTutorials(handler)
            PictoBloxAnalyticsEventLogger.getInstance().setTourStarted()

        }
        mBinding.tvTutSkip.setOnClickListener {
            dialog.dismiss()
            onSkipTutorials(handler)
            PictoBloxAnalyticsEventLogger.getInstance().setTourSkipped()
        }


        /*val lp = WindowManager.LayoutParams()
        lp.copyFrom(dialog.window!!.attributes)
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
        dialog.show()
        dialog.window!!.attributes = lp*/
        dialog.show()
        dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)

    }


    override fun showSignUpIncompleteAnimation() {
        showProfileIncompleteError.set(true)
        activity.showLoginIncompleteDialog()
    }

    override fun dismissSignUpIncompleteAnimationIfRequired() {
        activity.hideLoginIncompleteDialog()
        showProfileIncompleteError.set(false)
    }

    override fun redirectToSignInProcess() {
        activity.startActivity(Intent(activity, LoginActivity::class.java))
    }

    override fun redirectToProfile() {
        activity.startActivity(Intent(activity, ProfileActivity::class.java))
    }

    override fun setAccountImage(bitmap: Bitmap) {
        profileIcon.set(bitmap)
    }

    fun onIgnoreClick() {

    }

    fun onActionButtonClicked() {
        showExternalFileLoading.set(false)
        disposable?.dispose()
    }

    fun onOptionItemSelected(item: MenuItem, default: Boolean): Boolean {
        return when (item.itemId) {
            R.id.export_local_projects -> {
                shouldCopyAllFiles = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    checkPermissionForStorage()
                } else {
                    // copyProjectsInternalToExternal()
                    //askUserForDirectory()
                }
                true
            }
            R.id.export_selected_projects -> {
                shouldCopyAllFiles = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    checkPermissionForStorage()
                } else {
                    // copyProjectsInternalToExternal()
                    //askUserForDirectory()
                }
                true
            }
            else -> {
                default
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.R)
    private fun hasAllFilesPermission() = Environment.isExternalStorageManager()

    private fun checkPermissionForStorage() {
        if ((activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)) {
            val permission = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

            activity.requestPermissions(
                permission,
                1001
            ) // GIVE AN INTEGER VALUE FOR PERMISSION_CODE_READ LIKE 1001
        } else {
            //copyProjectsInternalToExternal()
            //askUserForDirectory()
        }
    }

    fun onRequestPermissionResultVM(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {

        when (requestCode) {
            1001 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //askUserForDirectory()
                } else {
                    Toast.makeText(activity, "Please grant permissions.", Toast.LENGTH_SHORT).show()
                }
            }

//            MANAGE_STORAGE_REQUEST_CODE -> {
//                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    askUserForDirectory()
//                } else {
//                    Toast.makeText(activity, "Please grant Storage permissions.", Toast.LENGTH_SHORT).show()
//                }
//            }

            103 -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    scanQR()
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)) {
                        AlertDialog.Builder(activity)
                            .setMessage("Please allow camera permission to scan qr code")
                            .setPositiveButton("Allow", DialogInterface.OnClickListener { dialog, which ->
                                ActivityCompat.requestPermissions(activity, arrayOf(permissions[0]),103)
                            })
                            .create().show()
                    } else {
                        if (isFirstTime) {
//                            Toast.makeText(activity,"two",Toast.LENGTH_SHORT).show()
                            isFirstTime = false
                            ActivityCompat.requestPermissions(activity, arrayOf(permissions[0]),103)
                        } else {
                            goToAppSetting()
                        }
                    }
                }
            }

        }
    }

//    private fun askUserForDirectory() {
//        val i = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
//        i.addCategory(Intent.CATEGORY_DEFAULT)
//        activity.startActivityForResult(i, REQUEST_DIRECTORY_CHOOSER)
//    }


    private fun copyProjectsInternalToExternal() {
        if (shouldCopyAllFiles) {
            val filesToTransfer = commManagerService?.communicationHandler?.storageHandler?.listLocalFiles()
            if (filesToTransfer != null) {
                if (filesToTransfer.isEmpty()) {
                    Toast.makeText(activity, "There are no projects.", Toast.LENGTH_SHORT).show()
                    return
                }
                for (eachFile in filesToTransfer) {
                    eachFile.setLastModified(1577817000000)
                    Log.e(TAG, "copyProjectsInternalToExternal: ", )
                    createTextFileInExternalStorage(eachFile)
                }
            }
        } else {
            val filesToTransfer = if (selectedTab.get() == 0) {
                activity.getRecentFragment().getSelectedFiles()
            } else {
                activity.getLocalFragment().getSelectedFiles()
            }
            if (filesToTransfer.isEmpty()) {
                Toast.makeText(activity, "Please select one or more file(s).", Toast.LENGTH_SHORT).show()
                return
            }
            for (eachFile in filesToTransfer) {
                createTextFileInExternalStorage(eachFile)
            }
        }
    }
    private fun createTextFileInExternalStorage(src: File) {
        val values = ContentValues().apply {
            put(MediaStore.Files.FileColumns.DISPLAY_NAME, src.name)
            put(MediaStore.Files.FileColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        src.setLastModified(1577817000000)

        val externalUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val resolver: ContentResolver = activity.contentResolver
        val uri = resolver.insert(externalUri, values)

        uri?.let {
            try {
                resolver.openOutputStream(it)?.use { outputStream ->
                    // Write data to the output stream
                    outputStream.write(src.readBytes())
                }
                Toast.makeText(activity, "Projects saved in Downloads", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    fun enableLinkCreation(enable: Boolean) {
        showCreateLinkIcon.set(enable)
    }


    fun onCreatePublicLink() {

        val fileList = if (selectedTab.get() == 0) {
            activity.getRecentFragment().getSelectedFiles()
        } else {
            activity.getLocalFragment().getSelectedFiles()
        }

        if (fileList.size > 1) {
            Toast.makeText(activity, "Too many items selected", Toast.LENGTH_LONG).show()
            return
        }

        if (fileList.size == 0) {
            Toast.makeText(activity, "No file selected", Toast.LENGTH_LONG).show()
            return
        }

        showCreateLinkView.set(true)
        linkCreationViewModel.set(LinkCreationViewModel(activity, this, fileList[0]))
    }

    fun linkCreationCompleted() {
        showCreateLinkView.set(false)
    }


}
