package io.stempedia.pictoblox.web

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.hardware.usb.UsbDevice
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableField
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.callback.IPreviewDataCallBack
import com.jiangdg.ausbc.camera.CameraUVC
import com.jiangdg.ausbc.camera.bean.CameraRequest
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.connectivity.Board
import io.stempedia.pictoblox.connectivity.CommManagerServiceImpl
import io.stempedia.pictoblox.connectivity.StorageHandler
import io.stempedia.pictoblox.databinding.ActivityPictobloxWeb3Binding
import io.stempedia.pictoblox.experimental.db.PictoBloxDatabase
import io.stempedia.pictoblox.experimental.db.files.MapTypeConverter
import io.stempedia.pictoblox.experimental.db.files.PopupCountEntity
import io.stempedia.pictoblox.experimental.db.files.PopupsEntity
import io.stempedia.pictoblox.experimental.db.files.TargetAudience
import io.stempedia.pictoblox.firebase.login.*
import io.stempedia.pictoblox.uiUtils.AbsActivity
import io.stempedia.pictoblox.userInputArgument.KeyboardArgumentFragment
import io.stempedia.pictoblox.util.PictobloxLogger
import io.stempedia.pictoblox.util.SPManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


/**
 *
 *     <script type="text/javascript" src="../android-middleware.js"></script>
 */


class PictoBloxWebActivity : AbsActivity(), LoginFragmentCallbacks {

    private lateinit var mBinding: ActivityPictobloxWeb3Binding
    private var currentOrientation = 0
    private lateinit var viewModel: PictoBloxWebViewModelM2
    private val TAG = "PictoBloxWebActivity"
    private var gt: Long? = null
    private val myPermissionsRequestCamera = 213
    private lateinit var loginFragment: LoginWithEmailPasswordFragment
    private val animHelper = PictoBloxActionButtonAnimHelper(lifecycle)

    override fun getRootView(layoutInflater: LayoutInflater): View? {
        if (mBinding == null) {
            mBinding = ActivityPictobloxWeb3Binding.inflate(layoutInflater)
        }
        return mBinding!!.root
    }

    override fun onPBServiceConnected(commManagerService: CommManagerServiceImpl) {
        viewModel.onServiceConnected(commManagerService)
    }

    override fun onBeforeServiceGetsDisconnected(commManagerService: CommManagerServiceImpl) {
        viewModel.onBeforeServiceGetsDisconnected()
    }

    override fun onStop() {
        super.onStop()
//        Toast.makeText(this,"onstop",Toast.LENGTH_SHORT).show()
        Log.d("DEBUG", "onStop: Web Activity")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.cacheProject()
        outState.putInt("ABC", 100)
        PictobloxLogger.getInstance().logd("WEB act onSaveInstanceState")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        mBinding = DataBindingUtil.setContentView(
            this@PictoBloxWebActivity,
            R.layout.activity_pictoblox_web3
        )
        super.onCreate(savedInstanceState)
        PictobloxLogger.getInstance().logd("WEB act onCreate ${savedInstanceState?.getInt("ABC")}")

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        viewModel = PictoBloxWebViewModelM2(this)

        mBinding.data = viewModel

        currentOrientation = requestedOrientation

        viewModel.onCreate()



        if (internetIsConnected()) {
            fetchAllPopUps()
//            Toast.makeText(this, "online", Toast.LENGTH_SHORT).show()
        } else {
            fetchLocalPopups()
//            Toast.makeText(this, "offline", Toast.LENGTH_SHORT).show()
        }

        /*
                setSupportActionBar(mBinding.tbPictoblox)
                supportActionBar?.setDisplayShowTitleEnabled(false)
                supportActionBar?.hide()

                mBinding.rvSpriteList.layoutManager =
                    CenterLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
                mBinding.rvSpriteList.setHasFixedSize(true)
                mBinding.rvSpriteList.adapter = spriteAdapter

                mBinding.tbPictoblox.setNavigationOnClickListener(viewModel)
        */

    }

    fun internetIsConnected(): Boolean {
        (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).let { connection ->
            connection.activeNetwork?.let {
                return true
            } ?: run {
                return false
            }
        }
    }

    fun showSaveProgress() {
        animHelper.buttonToProgress(mBinding.includeSave.textView5) {}
    }

    fun fetchLocalPopups() {
        CoroutineScope(Dispatchers.IO).launch {
            PictoBloxDatabase.getDatabase(this@PictoBloxWebActivity)
                .popUpDao().fetchAllPopUps().forEach { popUpData ->
                    val curCount = PictoBloxDatabase.getDatabase(this@PictoBloxWebActivity).popUpCountDao().fetchPopupCountById(popUpData.popUpId)
                    if (popUpIsValid(
                            popUpData.targetAudience,
                            curCount,
                            popUpData.maxDisplayCount,
                            popUpData.maxDate!!
                        )
                    ) {
                        Log.e(TAG, "fetchLocalPopups: allow $popUpData", )
                        withContext(Dispatchers.Main){
                            Handler().postDelayed({
                                viewModel.addPopUp(
                                    PictoBloxWebViewModelM2.PopUpData(
                                        popUpData.popUpId,
                                        MapTypeConverter.stringToMap(popUpData.title).get("en")!!,
                                        MapTypeConverter.stringToMap(popUpData.buttonText).get("en")!!,
                                        popUpData.link!!
                                    )
                                )
                            },popUpData.popUpDelayInms.toLong())
                        }
                    } else {
                        Log.e(TAG, "fetchLocalPopups: disallow", )
                    }
                }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun fetchAllPopUps() {
        FirebaseFirestore.getInstance().collection("popups")
            .get()
            .addOnCompleteListener(OnCompleteListener { popUps ->
                popUps.isSuccessful.let {
                    var allPopups: MutableMap<String, MutableMap<String, Map<String, String>>> =
                        mutableMapOf()
                    popUps.result.documents.forEach { snapshot ->
                        snapshot.data?.let {
                            val values = it.get("data") as MutableMap<String, Map<String, String>>
                            values.put(
                                "target_audience",
                                mutableMapOf("type" to it.get("target_audience").toString())
                            )
                            allPopups.put(snapshot.id, values)
                        }
                    }
                    computePopup(allPopups)
                }
            })
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun computePopup(allPopups :MutableMap<String, MutableMap<String, Map<String, String>>> ){
        allPopups.forEach { popupid, popup ->
            storePopUpInDB(popup, popupid)
            Handler().postDelayed( Runnable {

                CoroutineScope(Dispatchers.IO).launch {
                    PictoBloxDatabase.getDatabase(this@PictoBloxWebActivity).let { db ->
                        val curCount = db.popUpCountDao().fetchPopupCountById(popupid)
                        Log.e("count  ", curCount.toString())
                        db.popUpDao().fetchPopUpById(popupid).let { popUpData ->
                            popUpData?.let {
                                Log.e(
                                    TAG,
                                    "fetchAllPopUps: ${popUpData.toString()}"
                                )
                                if (popUpIsValid(popUpData.targetAudience, curCount, popUpData.maxDisplayCount, popUpData.maxDate!!)) {
                                    Log.e("popup","verified")
                                    viewModel.addPopUp(
                                        PictoBloxWebViewModelM2.PopUpData(
                                            popupid,
                                            popup["title"]?.get("en").toString(),
                                            popup["button_text"]?.get("en")!!,
                                            popup["link"].toString()
                                        )
                                    )
                                }else{
                                    Log.e("popup","unverified")
                                }
                            }
                        }
                    }
                }
            }, popup["display_after_ms"].toString().toLong())
        }
    }

    fun popUpIsValid(
        audience: TargetAudience,
        curCount: Int,
        maxCount: Int,
        maxDate: Long
    ): Boolean {
        val curDate = System.currentTimeMillis()
        if (FirebaseAuth.getInstance().currentUser != null) {
            val type = SPManager(this).getLastLoginAccountType()

            if (maxCount > curCount && curDate < maxDate ) {
                if (type.equals(audience.name) || audience.name.equals(TargetAudience.TYPE_ALL.name)) {
                    return true
                }
            }
        } else {
            if (audience.name.equals(TargetAudience.TYPE_ALL.name) && curCount < maxCount && curDate < maxDate  ) {
                return true
            }
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun storePopUpInDB(popup: MutableMap<String, Map<String, String>>, popupid: String) {
        Log.e("count", "storePopUpInDB: ${popup.get("target_audience")?.get("type")}")
        val time = popup.get("max_date") as Timestamp
        Log.e("time", " ${time.seconds*1000}")
        CoroutineScope(Dispatchers.IO).launch {
            storePopUpLocally(popup.get("img_url").toString(), popupid)
            PictoBloxDatabase.getDatabase(this@PictoBloxWebActivity).let {
                it.popUpDao().savePopup(
                    PopupsEntity(
                        popupid,
                        MapTypeConverter.mapToString(popup.get("body")!!),
                        MapTypeConverter.mapToString(popup.get("button_text")!!),
                        popup.get("display_after_ms").toString().toInt(),
                        replaceQueryParam(popup.get("link").toString(),"utm_medium","Android"),
                        popupid,
                        time.seconds*1000,
                        popup.get("max_display_count").toString().toInt(),
                        MapTypeConverter.mapToString(popup.get("title")!!),
                        TargetAudience.valueOf(popup.get("target_audience")?.get("type").toString())
                    )
                )
                it.popUpCountDao().savePopupCount(PopupCountEntity(popupid))
            }
        }
    }
    fun replaceQueryParam(url: String, paramName: String, paramValue: String): String {
        val uri = Uri.parse(url)
        val builder = uri.buildUpon()

        val encodedValue = URLEncoder.encode(paramValue, "UTF-8")

        builder.clearQuery()
        uri.queryParameterNames.forEach {
            builder.appendQueryParameter(it, if (it == paramName) encodedValue else uri.getQueryParameter(it))
        }

        return builder.build().toString()
    }

    fun storePopUpLocally(url: String, id: String) {
        Log.e(TAG, "storePopUpLocally: url: $url id: $id", )
        val storageHandler = StorageHandler(this, sharedPreferencesManager)
        Glide.with(this)
            .asBitmap()
            .load(url)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    try {
                        Log.e(TAG, "onResourceReady: $resource", )
                        storageHandler.deleteFileIfExist(File(storageHandler.popUpsFilesDir(), id))
                        val outputStream = FileOutputStream(storageHandler.createImageFile(id))
                        resource.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                        outputStream.close()
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception ${e.localizedMessage}")
                    }

                }

                override fun onLoadCleared(placeholder: Drawable?) {

                }

            })
    }

    fun hideSaveProgress() {
        animHelper.progressToButton(mBinding.includeSave.textView5) {}
    }

    override fun onPause() {
        Log.e("TAG", "onPause: ")
//        Toast.makeText(this,"pause",Toast.LENGTH_SHORT).show()
        super.onPause()
        viewModel.cacheProject()
    }

    override fun onBackPressed() {
        if (viewModel.handleBackPress()) {
            super.onBackPressed()
        }
    }


    override fun onCameraAttached(camera: MultiCameraClient.ICamera) {

        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(
                this,
                getString(R.string.need_camera_permission),
                Toast.LENGTH_SHORT
            ).show()
            askForPermissions(mutableListOf(android.Manifest.permission.CAMERA))
        }
    }

    override fun onCameraConnected(camera: MultiCameraClient.ICamera) {
        Log.e(TAG, "Main onCameraConnected : ")
        Toast.makeText(this, getString(R.string.camera_connected), Toast.LENGTH_SHORT).show()
//        Log.e("DETAIL",camera.device.toString())
//        Log.e("DETAIL",camera.device.productName.toString())
//        Log.e("DETAIL",camera.device.deviceProtocol.toString())
        val request = CameraRequest.Builder()
            .setPreviewWidth(854) // camera preview width
            .setPreviewHeight(480)
            .setAspectRatioShow(true) // aspect render,default is true
            .create()
        camera.openCamera(null, request)
        camera.addPreviewDataCallBack(callback)
        commManagerService?.communicationHandler?.isOTGConnected = true
        commManagerService?.communicationHandler?.data = mutableMapOf<String, String>().apply {
            this.put("name", camera.device.productName.toString())
            this.put("id", camera.device.deviceId.toString())
        }
        commManagerService?.communicationHandler?.apiFromPictobloxWeb?.onUSBCameraConnected(
            camera.device.productName,
            camera.device.deviceId.toString()
        )
    }

    override fun onCameraDetached(camera: MultiCameraClient.ICamera) {
    }

    override fun onCameraDisConnected(camera: MultiCameraClient.ICamera) {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, getString(R.string.camera_disconnected), Toast.LENGTH_SHORT).show()
        }
        viewModel.usbCamVideoFrameStatus = false
        commManagerService?.communicationHandler?.apiFromPictobloxWeb?.onUSBCameraDisConnected()
    }

    fun bitmapFromRgba(width: Int, height: Int, bytes: ByteArray): Bitmap? {
        val pixels = IntArray(bytes.size / 4)
        var j = 0
        for (i in pixels.indices) {
            val R = bytes[j++].toInt() and 0xff
            val G = bytes[j++].toInt() and 0xff
            val B = bytes[j++].toInt() and 0xff
            val A = bytes[j++].toInt() and 0xff
            val pixel = A shl 24 or (R shl 16) or (G shl 8) or B
            pixels[i] = pixel
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    fun encodeTobase64(image: Bitmap): String? {
        val baos = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 50, baos)
        val b = baos.toByteArray()
        return Base64.encodeToString(b, Base64.DEFAULT)
    }

    val callback = object : IPreviewDataCallBack {
        override fun onPreviewData(
            data: ByteArray?,
            width: Int,
            height: Int,
            format: IPreviewDataCallBack.DataFormat
        ) {

            if (viewModel.usbCamVideoFrameStatus) {
                Log.e(TAG, "onPreviewData: ${viewModel.usbCamVideoFrameStatus}")
                if (data == null) { /* Log.d("gg", "failed") */
                }
                if (gt == null) {
                    commManagerService?.communicationHandler?.apiFromPictobloxWeb?.setFrame(
                        "data:image/jpeg;base64," + encodeTobase64(
                            bitmapFromRgba(width, height, data!!)!!
                        )!!
                    )
                    gt = System.currentTimeMillis()
                } else {
                    val ct = System.currentTimeMillis()
                    if (ct - gt!! >= 25) {
                        commManagerService?.communicationHandler?.apiFromPictobloxWeb?.setFrame(
                            "data:image/jpeg;base64," + encodeTobase64(
                                bitmapFromRgba(width, height, data!!)!!
                            )!!
                        )
                        gt = ct
                    } else {
                    }
                }
            }
        }
    }

    fun hideKeyboard() {
        val imm: InputMethodManager =
            getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(mBinding.includeSave.etProjectName.windowToken, 0)
    }

    override fun onDeviceConnecting(name: String) {
        Toast.makeText(
            this@PictoBloxWebActivity,
            getString(R.string.connecting_device, name),
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDeviceConnected(name: String, address: String) {
        Toast.makeText(
            this@PictoBloxWebActivity,
            getString(R.string.connected_device, name),
            Toast.LENGTH_SHORT
        ).show()
        //mBinding.ivConnect.setImageResource(R.drawable.ic_connect4)
        autoConnectDialog(name)
        commManagerService?.communicationHandler?.apiFromPictobloxWeb?.onBluetoothConnected()
    }


    // ON CLICK OF DISCONNECT THIS FUN CALL
    override fun onDeviceDisconnected(name: String) {
        Toast.makeText(
            this@PictoBloxWebActivity,
            getString(R.string.disconnected_device, name),
            Toast.LENGTH_SHORT
        ).show()
        //mBinding.ivConnect.setImageResource(R.drawable.ic_disconnect3)
//        commManagerService?.disconnect()
        commManagerService?.communicationHandler?.apiFromPictobloxWeb?.onBluetoothDisconnected()
    }

    override fun error(msg: String) {
        Toast.makeText(
            this@PictoBloxWebActivity,
            getString(R.string.error_device, msg),
            Toast.LENGTH_SHORT
        ).show()
        //mBinding.ivConnect.setImageResource(R.drawable.ic_disconnect3)
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.e(TAG, "onDestroy: ")
//        Toast.makeText(this,"ondestroy",Toast.LENGTH_SHORT).show()
        requestedOrientation = currentOrientation
//        commManagerService?.communicationHandler?.onDestroy()
        //handler.removeCallbacks(spriteAdapter)
    }

    override fun onSignInComplete() {
        viewModel.onSignInVerified()
    }

    override fun switchToAgeQueryFragment() {}

    override fun switchToForgotPwdFragment() {}
    override fun switchToForgotUsernameFragment() {}

    /*
    *
    *
    *
    *
    * ****************************************************************************************************************
    */

    fun showSignInDialog() {
        loginFragment = LoginWithEmailPasswordFragment()
        loginFragment.arguments = Bundle().apply {
            putString(FUNCTION, FUNCTION_EXTERNAL_SIGN_IN)
        }

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fl_login_container, loginFragment)
            .commitAllowingStateLoss()

    }

    fun hideSignInDialog() {
        supportFragmentManager
            .beginTransaction()
            .remove(loginFragment)
            .commitAllowingStateLoss()
    }


    fun openHelp() {
        AlertDialog.Builder(this)
            .setTitle("Help")
            .setMessage("you are helped.")
            .setPositiveButton("Thanks, feels good", null)
            .setCancelable(false)
            .show()
    }


    fun showBoardSelectionDialog(
        boardArray: Array<String>,
        selectedPos: Int,
        isProjectChanged: Boolean
    ) {

        android.app.AlertDialog.Builder(
            this,
            android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar
        )
            .setTitle(getString(R.string.selct_board))
            .setSingleChoiceItems(boardArray, selectedPos) { d, which ->
                run {
                    if (which != selectedPos) {
                        viewModel.onBoardSelected(which, isProjectChanged)
                    }
                    d.dismiss()
                }
            }
            .create()
            .show()
    }

    fun showBoardConfirmationDialog(board: Board, isProjectChanged: Boolean) {
        AlertDialog.Builder(this@PictoBloxWebActivity)
            .setTitle("Changing board will erase all current work, are you sure you want to continue?")
            .setPositiveButton("Yes") { d, _ ->
                run {
                    viewModel.onBoardSelectionConfirmed(board, isProjectChanged)
                    d.dismiss()
                }
            }
            .setNegativeButton("Cancel", null)
            .setCancelable(true)
            .create()
            .show()
    }

    fun setBoard(board: Board) {
        Toast.makeText(
            this@PictoBloxWebActivity,
            "${board.stringValue} selected",
            Toast.LENGTH_LONG
        ).show()
    }

    /*    fun showSaveDialog(saveProjectViewModel: SaveProjectViewModel) {

            val view = DataBindingUtil.inflate<FragSaveBinding>(
                layoutInflater,
                R.layout.frag_save,
                null,
                false
            )

            view.data = saveProjectViewModel

            saveDialog =
                AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
                    .setView(view.root)
                    //.setTitle(getString(R.string.save_project_title))
                    //.setPositiveButton(getString(R.string.save), null)
                    .create()

            saveDialog?.show()

        }*/

    fun attachWebView(webView: WebView) {
        if (webView.parent != null) {
            PictobloxLogger.getInstance().logd("WebView had parent, removed:: SIKE")
            (webView.parent as ViewGroup).removeView(webView)
        }
        mBinding.wbPictobloxContainer.addView(webView)
    }

    fun detachWebView() {
        mBinding.wbPictobloxContainer.removeAllViews()
    }

    fun inflateWebView(): WebView {

        val webView = layoutInflater.inflate(
            R.layout.include_pictoblox_webview,
            mBinding.wbPictobloxContainer,
            false
        ) as WebView

        mBinding.wbPictobloxContainer.addView(webView)

        configureWebView(webView.settings)

//        val cookieManager: CookieManager = CookieManager.getInstance()
//        cookieManager.setAcceptThirdPartyCookies(webView, true)

        return webView
    }

    /*fun dismissSaveDialog() {
        saveDialog?.dismiss()
    }*/

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    private fun hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView(webSettings: WebSettings) {
        webSettings.javaScriptEnabled = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        //webSettings.userAgentString = "mobile chrome android webgl"

        webSettings.domStorageEnabled = true
        webSettings.displayZoomControls = false
        webSettings.allowFileAccessFromFileURLs = true
        webSettings.allowUniversalAccessFromFileURLs = true
        webSettings.mediaPlaybackRequiresUserGesture = false
        webSettings.allowFileAccess = true
        webSettings.javaScriptCanOpenWindowsAutomatically = true;
        webSettings.setSupportZoom(false)
        webSettings.cacheMode = WebSettings.LOAD_NO_CACHE
        WebView.setWebContentsDebuggingEnabled(true)
    }

    fun showOpenedProjectSnack(projectName: String) {
        Snackbar.make(
            mBinding.wbPictobloxRootCoordinator,
            getString(R.string.wv_cached_open_snack, projectName),
            Snackbar.LENGTH_LONG
        ).show()
    }

    //TODO Called when user give input Ex:- speed in motor
    fun showUserInputDialog(bundle: Bundle) {
        val frag = KeyboardArgumentFragment()
        frag.arguments = bundle
//        Toast.makeText(this, "showUserInputDialog", Toast.LENGTH_SHORT).show()
        frag.show(supportFragmentManager, "KeyboardArgumentFragment")
    }

    fun z() {
        AlertDialog.Builder(this)
            .setTitle("Enter session key")
            .setView(R.layout.test)
            .setNeutralButton("Okie") { d, _ -> d.dismiss() }
            .create()
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        viewModel.onRequestPermissionsResultViewModel(requestCode, permissions, grantResults)
        when (requestCode) {
            myPermissionsRequestCamera -> {
                viewModel.onCameraPermissionResult(grantResults)
            }
        }
    }

    fun askForPermissions(permissionPendingList: List<String>) {
        ActivityCompat.requestPermissions(
            this,
            permissionPendingList.toTypedArray(),
            myPermissionsRequestCamera
        )
    }

    override fun generateCamera(ctx: Context, device: UsbDevice): MultiCameraClient.ICamera {
//        Toast.makeText(this, "generateCamera", Toast.LENGTH_SHORT).show()
        return CameraUVC(ctx, device)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        viewModel.onActivityResultViewModel(requestCode, resultCode, data)
    }


}

data class SessionKeyDialogVM(val key: ObservableField<String>)

