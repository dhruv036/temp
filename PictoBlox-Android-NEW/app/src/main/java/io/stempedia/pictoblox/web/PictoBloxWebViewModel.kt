package io.stempedia.pictoblox.web
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import io.reactivex.Completable
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.connectivity.*
import io.stempedia.pictoblox.firebase.CourseFlow
//import io.stempedia.pictoblox.userInputArgument.ARGUMENT_PARAM
import io.stempedia.pictoblox.userInputArgument.ARGUMENT_TYPE
import io.stempedia.pictoblox.util.AbsViewModel
import io.stempedia.pictoblox.util.PictobloxLogger
import kotlin.random.Random


class PictoBloxWebViewModel(val activity: PictoBloxWebActivity) : PictoBloxCallbacks,
    View.OnClickListener, AbsViewModel(activity) {


    private lateinit var fileRequestCallback: ValueCallback<Array<Uri>>
    private val REQUEST_FILE_CHOOSER = 101

/*    override fun promptProjectSaveDialog() = Completable.create {

        onSaveClicked()
        it.onComplete()
    }*/

    override fun promptUserBoardSelectionDialog(board: Board?, isProjectChanged: Boolean) = Completable.create {
        onBoardClicked()
        it.onComplete()
    }

    override fun promptUserForBluetoothConnection() = Completable.create {
        onConnectClicked()
        it.onComplete()
    }

    override fun onCourseRetry() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onPictobloxReady() {

    }

    override fun goToSettings() {

    }

    override fun onCourseCompleted(courseFlow: CourseFlow?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun promptForPermissions(permissionPendingList: List<String>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun saveProject(fileName: String, byteArray: ByteArray) {
        TODO("Not yet implemented")
    }


    override fun showSignInDialog(): Completable {
        TODO("Not yet implemented")
    }

    override fun onSaveComplete(): Completable {
        TODO("Not yet implemented")
    }

    override fun loadAIModel(model: String): Completable {
        TODO("Not yet implemented")
    }

    override fun openFirmwareUploader(board: String): Completable {
        TODO("Not yet implemented")
    }

    override fun openExternalWebLink(link: String): Completable {
        TODO("Not yet implemented")
    }

    override fun onFileChoose(filePathCallback: ValueCallback<Array<Uri>>, fileChooserParams: WebChromeClient.FileChooserParams): Boolean {
        TODO("Not yet implemented")
    }

    override fun onsendOTGCamVideoFrame() {
        TODO("Not yet implemented")
    }

    override fun onstopOTGCamVideoFrame() {
        TODO("Not yet implemented")
    }


    override fun exit() = Completable.create {
        activity.finish()
        it.onComplete()
    }

    override fun promptProjectSaveDialog(existAfterSave: Boolean): Completable {
        TODO("Not yet implemented")
    }

    override fun redirectToSignUp(showSaveDialog: Boolean): Completable {
        TODO("Not yet implemented")
    }


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

    val modal = ObservableField(NavigationModalStack.STAGE_MODAL)
    val isLoading = ObservableBoolean(false)
    val loadingMessage = tempMessageArray[Random.nextInt(11)]
    var commManagerServiceImpl: CommManagerServiceImpl? = null
    val isBackDropSelected = ObservableBoolean(false)
    val backdropBitmap = ObservableField<Bitmap>()
    val isBackDropDefault = ObservableBoolean(false)
    val connectIcon = ObservableInt(R.drawable.ic_disconnect3)
    val boardIcon = ObservableInt(R.drawable.ic_dummy_board_24px)

    private var backdrop: CommunicationHandlerWithPictoBloxWeb.Sprite2? = null

    /**
     * If user pressed back and webview does not respond it will proceed with usual function.
     */
    private var backCounter = 0

    override fun setSprites2(targets: Pair<CommunicationHandlerWithPictoBloxWeb.Sprite2?, List<CommunicationHandlerWithPictoBloxWeb.Sprite2>>) {
        this.backdrop = targets.first

        backdrop?.apply {
            isBackDropSelected.set(isSelected)
            setBackdropBitmap(this)
        }

        //  activity.setSpriteListInRecyclerView(targets.second)

    }

    override fun onPromptUserInputDialog(argType: String, argPlaceholder: String?, handlerFunction: String, currValue: String?): Completable {
        TODO("Not yet implemented")
    }

    /* override fun onModalChanged(modal: NavigationModalStack) {
     backCounter = 0

     when (modal) {
        // NavigationModalStack.STAGE_MODAL -> activity.hideActionBar()
         NavigationModalStack.EXIT -> activity.finish()
         else -> {
            // activity.showActionBar()
             this.modal.set(modal)
         }
     }
 }*/
/*
    override fun onPromptUserInputDialog(argType: String, argParam: String, func: String) = Completable.create {
        val bundle = Bundle().apply {
            putString(ARGUMENT_TYPE, argType)
            putString(ARGUMENT_PARAM, argParam)
        }

        activity.showUserInputDialog(bundle)

        it.onComplete()

    }*/

    fun onBackdropClicked() {
        backdrop?.apply {

            if (isBackDropSelected.get()) {
                //activity.popOptionForSprites(this)

            } else {
                isBackDropSelected.set(true)
                //commManagerServiceImpl?.communicationHandler?.selectSprite(this)
                backdrop?.apply { setBackdropBitmap(this) }
            }
        }
    }

    fun onAddSpriteClicked() {
        commManagerServiceImpl?.communicationHandler?.apiFromPictobloxWeb?.onAddSpriteClick()
    }

    fun onSaveClicked() {
        commManagerServiceImpl?.apply {

            val fileName = with(communicationHandler.storageHandler.openingFileName) {
                if (trim().toLowerCase().endsWith(".sb3")) {
                    trim().substring(0, length - 4)

                } else {
                    this
                }
            }

            //val vm = SaveProjectViewModel(activity, this, (fileName), ObservableField(activity.getString(R.string.save)))
            //activity.showSaveDialog(vm)
        }
    }

    fun onConnectClicked() {
        activity.handleDeviceClick()
    }

    fun onHelpClicked() {
        activity.openHelp()
    }

    fun onBoardClicked() {
        val boardArray: Array<String> = Board.values().map { it.stringValue }.toTypedArray()

        var selectedPos = -1

        commManagerServiceImpl?.communicationHandler?.getSelectedBoard()?.apply {
            selectedPos = boardArray.indexOf(this.stringValue)

        }

        activity.showBoardSelectionDialog(boardArray, selectedPos, false)
    }

    fun onBoardSelected(selectedPos: Int) {
        activity.showBoardConfirmationDialog(Board.values()[selectedPos], false)
    }

    fun onBoardSelectionConfirmed(selectedBoard: Board) {
        commManagerServiceImpl?.communicationHandler?.setBoardSelected(selectedBoard, false)
        activity.setBoard(selectedBoard)
        setBoardIcon(selectedBoard)
    }

    private fun setBoardIcon(board: Board) {
        when (board) {
            Board.EVIVE -> boardIcon.set(R.drawable.ic_board_evive)
            Board.MEGA -> boardIcon.set(R.drawable.ic_board_arduino)
            Board.UNO -> boardIcon.set(R.drawable.ic_board_arduino)
            Board.NANO -> boardIcon.set(R.drawable.ic_board_arduino)
            Board.ESP32 -> boardIcon.set(R.drawable.ic_boards_esp32)
            //Board.QUON -> boardIcon.set(R.drawable.ic_boards_esp32)
        }
    }

    fun onServiceConnected(commManagerServiceImpl: CommManagerServiceImpl) {

        this.commManagerServiceImpl = commManagerServiceImpl

        if (commManagerServiceImpl.isConnected()) {
            connectIcon.set(R.drawable.ic_connect4)

        } else {
            connectIcon.set(R.drawable.ic_disconnect3)
        }

        commManagerServiceImpl.communicationHandler.getSelectedBoard()?.apply {
            activity.setBoard(this)
        }

        commManagerServiceImpl.communicationHandler.getWebView()?.apply {
            activity.attachWebView(this)
            this.clearHistory()
            this.clearFormData()
            this.clearFormData()
            //activity.showActionBar()
            isLoading.set(false)
            commManagerServiceImpl.communicationHandler.setPictobloxCallbacks(this@PictoBloxWebViewModel)
            PictobloxLogger.getInstance().logd("#### Webview present, loading project.")
            commManagerServiceImpl.communicationHandler.openProject()
            showSnackIfRequired()

        } ?: run {
            val webView = activity.inflateWebView()
            webView.webViewClient = PictoBloxWebClient()
            webView.webChromeClient = PictobloxChromeClient()
            commManagerServiceImpl.communicationHandler.setWebView(webView)
            commManagerServiceImpl.communicationHandler.setPictobloxCallbacks(this)
        }

    }

    fun onBeforeServiceGetsDisconnected() {
        commManagerServiceImpl?.communicationHandler?.cacheCurrentWorkIfApplicable()
        commManagerServiceImpl?.communicationHandler?.setPictobloxCallbacks(null)
        activity.detachWebView()
    }

    private fun showSnackIfRequired() {
        PictobloxLogger.getInstance().logd("#### showSnackIfRequired called")
        commManagerServiceImpl?.communicationHandler?.storageHandler?.apply {
            PictobloxLogger.getInstance().logd("#### storageType ${getFileType()}")

            if (getFileType() == StorageType.CACHE) {
                PictobloxLogger.getInstance().logd("#### method called")
                activity.showOpenedProjectSnack(openingFileName)

            }
        }
    }

    private fun setBackdropBitmap(backdrop: CommunicationHandlerWithPictoBloxWeb.Sprite2) {
        backdrop.thumbBitmap?.apply {

            backdropBitmap.set(
                if (backdrop.isSelected) {
                    val paint = Paint()
                    paint.colorFilter =
                        PorterDuffColorFilter(0xff531e73.toInt(), PorterDuff.Mode.SRC_IN)
                    val bitmapResult =
                        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmapResult)
                    canvas.drawBitmap(this, 0f, 0f, paint)

                    bitmapResult

                } else {
                    this
                }
            )
        }
    }

    override fun onClick(v: View?) {
        if (backCounter == 0) {
            backCounter = 1
            commManagerServiceImpl?.communicationHandler?.apiFromPictobloxWeb?.onBackPressed()

        } else {
            activity.finish()
        }
    }


    inner class PictoBloxWebClient : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            isLoading.set(true)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            PictobloxLogger.getInstance().logd("#### onPageFinished")
            super.onPageFinished(view, url)

            //activity.showActionBar()
            isLoading.set(false)

            //TODO, this may result in calls made to webViewClient after onStop?
            /*commManagerServiceImpl?.communicationHandler?.getWebView()?.apply {
                webViewClient = null
            }*/
            PictobloxLogger.getInstance().logd("#### New webvieew made, opening projext")
            commManagerServiceImpl?.communicationHandler?.openProject()
            showSnackIfRequired()

        }
    }

    inner class PictobloxChromeClient : WebChromeClient() {

        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            fileRequestCallback = filePathCallback
            val intent = Intent(Intent.ACTION_GET_CONTENT)// or action_chooser
            intent.type = "*/*"
            startActivityForResult(activity, intent, REQUEST_FILE_CHOOSER, null)
            filePathCallback.onReceiveValue(null);
            return true
        }

        override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
            PictobloxLogger.getInstance().logd("onJsAlert")
            return super.onJsAlert(view, url, message, result)
        }

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            PictobloxLogger.getInstance().logd("onProgressChanged")
            super.onProgressChanged(view, newProgress)
        }

        override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
            PictobloxLogger.getInstance().logd("onJsConfirm")
            return super.onJsConfirm(view, url, message, result)
        }

        override fun onPermissionRequest(request: PermissionRequest?) {
            super.onPermissionRequest(request)
            PictobloxLogger.getInstance().logd("onPermissionRequest")
            activity.runOnUiThread {
                PictobloxLogger.getInstance().logd("Granted:: $request")
                request?.grant(request.resources)
            }
        }

        override fun onPermissionRequestCanceled(request: PermissionRequest?) {
            super.onPermissionRequestCanceled(request)
            PictobloxLogger.getInstance().logd("onPermissionRequestCanceled")

        }
    }


}