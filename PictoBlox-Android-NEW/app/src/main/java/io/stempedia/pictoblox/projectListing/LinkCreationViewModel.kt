package io.stempedia.pictoblox.projectListing

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.text.Html
import android.text.Spannable
import android.text.Spanned
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.github.alexzhirkevich.customqrgenerator.QrData
import com.github.alexzhirkevich.customqrgenerator.vector.QrCodeDrawable
import com.github.alexzhirkevich.customqrgenerator.vector.QrVectorOptions
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorBackground
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorBallShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorColor
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorColors
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorFrameShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorLogo
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorLogoPadding
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorLogoShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorPixelShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorShapes
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.dynamiclinks.ktx.androidParameters
import com.google.firebase.dynamiclinks.ktx.component1
import com.google.firebase.dynamiclinks.ktx.component2
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.dynamiclinks.ktx.shortLinkAsync
import com.google.firebase.dynamiclinks.ktx.socialMetaTagParameters
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.ktx.Firebase
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.CompletableSource
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.observers.DisposableObserver
import io.reactivex.rxjava3.schedulers.Schedulers
import io.stempedia.pictoblox.BuildConfig
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.util.PictobloxLogger
import io.stempedia.pictoblox.util.SPManager
import io.stempedia.pictoblox.util.fireStorageLinkFile
import io.stempedia.pictoblox.util.firebaseCreateNewLinkStorageEntry
import io.stempedia.pictoblox.util.firebaseLinkStorageEntry
import io.stempedia.pictoblox.util.firebaseUserDetail
import io.stempedia.pictoblox.util.hasInternet
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream


class LinkCreationViewModel(val activity: ProjectListActivity, val activityVm: ProjectListActivityVM, val file: File) {

    val showLink = ObservableInt(View.GONE)
    val link = ObservableField<String>()
    val showInfoText = ObservableInt(View.GONE)
    val buttonTitle = ObservableField<String>()
    val processPercentage = ObservableInt(View.GONE)
    val showDownloadStarted = ObservableBoolean()
        val showbt  = ObservableBoolean()
    val infoText = ObservableField<String>()
    val showError = ObservableInt()
    val drawable :ObservableField<Drawable> = ObservableField()
    private var processDisposable: Disposable? = null
    private var qrDisposable :  Disposable? = null
    val text = ObservableField<String>()

    private val spManager = SPManager(activity)

    init {
        text.set(activity.resources.getString(R.string.share_file_limit))
        if (spManager.isGlobalLinkInfoShown) {
            showInfoText.set(View.GONE)
            showbt.set(false)
            startLinkCreation()
        } else {
            showInfoText.set(View.VISIBLE)
            infoText.set(activity.getString(R.string.link_creation_info))
            showbt.set(true)
            buttonTitle.set(activity.getString(R.string.got_it))
        }
    }


    fun dispose() {
        processDisposable?.dispose()
    }

    fun onActionButtonClicked() {

        when (buttonTitle.get()) {
            activity.getString(R.string.got_it) -> {
                spManager.isGlobalLinkInfoShown = true
                startLinkCreation()
            }

            activity.getString(R.string.try_again) -> {
                startLinkCreation()
            }

        }


    }

    fun textCopyThenPost() {
        val url = link.get().toString()
        val clipboardManager = activity.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        // When setting the clipboard text.
        clipboardManager.setPrimaryClip(ClipData.newPlainText   ("", url))
        // Only show a toast for Android 12 and lower.
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
            Toast.makeText(activity.applicationContext,activity.getString(R.string.copied), Toast.LENGTH_SHORT).show()
    }

    fun shareQR(){

        if (activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED  && activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED ){
           Log.e("OK","OKKK")
        }

        drawable.get()?.let{
            val bitmap = it.toBitmap(500,500,Bitmap.Config.ARGB_8888)
//            saveImage(bitmap)
            val qrfile  = saveBitmapToFile(bitmap)
            if (qrfile!!.exists()){
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "image/jpg"
                val imageUri = FileProvider.getUriForFile(activity.applicationContext, BuildConfig.APPLICATION_ID , qrfile)
                shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri)
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Shared Image")
                shareIntent.putExtra(Intent.EXTRA_TEXT, file.nameWithoutExtension)
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                activity.grantUriPermission("android", imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

                activity.startActivity(Intent.createChooser(shareIntent, "Share QR"))
//              file!!.delete()
            }
        }
    }
    private fun saveImage(image: Bitmap): String? {
        var savedImagePath: String? = null
        val imageFileName = System.currentTimeMillis().toString() + ".jpg"
        val storageDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                .toString() + "/PictoBlox"
        )
        var success = true
        if (!storageDir.exists()) {
            success = storageDir.mkdirs()
        }
        if (success) {
            val imageFile = File(storageDir, imageFileName)
            savedImagePath = imageFile.absolutePath
            try {
                val fOut: OutputStream = FileOutputStream(imageFile)
                image.compress(Bitmap.CompressFormat.JPEG, 100, fOut)
                fOut.close()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }

            // Add the image to the system gallery
            galleryAddPic(savedImagePath)
            Toast.makeText(activity, activity.resources.getString(R.string.image_saved), Toast.LENGTH_LONG).show()
        }
        return savedImagePath
    }
    private fun galleryAddPic(imagePath: String) {
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        val f = File(imagePath)
        val contentUri = Uri.fromFile(f)
        mediaScanIntent.data = contentUri
        activity.sendBroadcast(mediaScanIntent)
    }

    fun saveBitmapToFile(bitmap: Bitmap): File?{
        return try {
            val imagefolder: File = File(activity.getCacheDir(), "images")
            imagefolder.mkdirs();
            val file = File(imagefolder, "image.jpg")
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.close()
            file
        } catch (e: IOException) {
            Toast.makeText(activity, "" + e.localizedMessage, Toast.LENGTH_LONG).show();
            e.printStackTrace()
            null
        }
    }

    fun exitIfApplicable() {
        if (!showDownloadStarted.get()) {
            activityVm.linkCreationCompleted()
        }
    }

    fun onIgnoreClick() {
        //Do nothing. This required so when user presses in the center white portion of the save dialog it does not close.
    }

    private fun startLinkCreation() {
        if (FirebaseAuth.getInstance().currentUser == null) {
            Toast.makeText(activity,activity.resources.getString(R.string.user_not_sign),Toast.LENGTH_SHORT).show()
            activityVm.linkCreationCompleted()
            return
        }
        showDownloadStarted.set(true)
        showInfoText.set(View.GONE)
        buttonTitle.set("")
        showbt.set(false)
        var fileToUploadId = ""

        processDisposable = hasInternet(activity.applicationContext)
                .andThen(getUserId())
                .flatMap { uid ->
                    Single.just(Tasks.await(firebaseUserDetail(uid).get()))
                }
                .flatMap { snap ->
                    createNewLinkEntry(snap, file)
                }
                .doOnSuccess { docId ->
                    fileToUploadId = docId
                }
                .flatMapObservable { docId ->
                    uploadFile(docId)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<Int>() {
                    override fun onNext(t: Int) {
                        processPercentage.set(t)
                    }

                    override fun onError(e: Throwable) {
                        PictobloxLogger.getInstance().logException(e)
                        showInfoText.set(View.VISIBLE)
                        showDownloadStarted.set(false)
                        showbt.set(true)
                        buttonTitle.set(activity.resources.getString(R.string.try_again))
                        infoText.set("${activity.resources.getString(R.string.error_in_creation_link)} ${ if(e.message.equals("no internet",true)) activity.resources.getString(R.string.no_internet) else e.message}")
                    }

                    override fun onComplete() {
                        createDownloadLink(file.nameWithoutExtension, fileToUploadId)
                    }
                })


    }

    private fun createDownloadLink(fileTitle: String, docId: String) {

        val task = fireStorageLinkFile(docId)
        task.downloadUrl
            .onSuccessTask { uri ->
                Firebase.dynamicLinks.shortLinkAsync {
                    link = Uri.parse(uri.toString())
                    domainUriPrefix = "https://pictoblox.page.link"

                    androidParameters(BuildConfig.APPLICATION_ID) {
                        minimumVersion = 53
                    }
                    /*iosParameters("com.example.ios") {
                        appStoreId = "123456789"
                        minimumVersion = "1.0.1"
                    }
                    googleAnalyticsParameters {
                        source = "orkut"
                        medium = "social"
                        campaign = "example-promo"
                    }
                    itunesConnectAnalyticsParameters {
                        providerToken = "123456"
                        campaignToken = "example-promo"
                    }*/
                    socialMetaTagParameters {
                        title = fileTitle
                        description = "Created using PictoBlox!"
                        imageUrl =
                            Uri.parse("https://firebasestorage.googleapis.com/v0/b/pictobloxdev.appspot.com/o/STEMpedia-Logo-1.png?alt=media&token=bbede80f-6741-4e6d-9a5c-4803dda9caf9")
                    }
                }
            }
            .addOnSuccessListener { (shortLink, flowchartLink) ->

                firebaseLinkStorageEntry(docId)
                    .update(mapOf("link" to shortLink.toString()))

                link.set(shortLink.toString())
                buttonTitle.set(activity.resources.getString(R.string.copy_to_clipboard))
                showLink.set(View.VISIBLE)
                showbt.set(false)
                showDownloadStarted.set(false)
                createQR()
            }
            .addOnFailureListener { e ->
                showInfoText.set(View.VISIBLE)
                showDownloadStarted.set(false)
                showbt.set(true)
                buttonTitle.set(activity.resources.getString(R.string.try_again))
                infoText.set("${activity.resources.getString(R.string.error_in_creation_link)} ${e.message}")
            }
    }

    private fun createQR(){
        showbt.set(false)
        val url = link.get()
        val qrData =  QrData.Url(url.toString())
        val options = QrVectorOptions.Builder()
            .setPadding(.15f)
            .setLogo(
                QrVectorLogo(
                    drawable = ContextCompat
                        .getDrawable(activity, R.drawable.tobi_logo_white_bg),
                    size = .30f,
                    padding = QrVectorLogoPadding.Natural(.2f),
                    shape = QrVectorLogoShape
                        .Circle
                )
            )
            .setBackground(
                QrVectorBackground(color = QrVectorColor.Solid(Color.WHITE))
            )
            .setColors(
                QrVectorColors(
                    dark = QrVectorColor
                        .Solid(Color.BLACK),

                    ball = QrVectorColor.Solid(
                        ContextCompat.getColor(activity, R.color.black)
                    )
                )
            )
            .setShapes(shapes = QrVectorShapes(
                frame = QrVectorFrameShape.RoundCorners(0.4f,topLeft = true, topRight = true, bottomLeft = true, bottomRight = false),
                darkPixel = QrVectorPixelShape.RoundCorners(.3f)

            ))
            .build()

        val image : Drawable = QrCodeDrawable(qrData, options)
//        Log.d("image", "createQR: ${ QrCodeDrawable(qrData, options).toBitmap(250,250)}")
        drawable.set(image)
    }

    private fun uploadFile(docId: String) = Observable.create<Int> { emitter ->

        val ref = fireStorageLinkFile(docId)

        val task = ref.putFile(Uri.fromFile(file))
        task.addOnProgressListener {
            val percentage = (it.bytesTransferred * 100.0f) / it.totalByteCount
            emitter.onNext(percentage.toInt())
        }
            .addOnSuccessListener {
                emitter.onComplete()
            }
            .addOnFailureListener {
                emitter.onError(it)
            }

        emitter.setCancellable {
            task.cancel()
        }
    }

    private fun createNewLinkEntry(snap: DocumentSnapshot, file: File) = Single.create<String> { emitter ->
        val isVerified = snap.getBoolean("is_verified") ?: false

        if (isVerified) {
            val ref = firebaseCreateNewLinkStorageEntry()
            Tasks.await(ref.set(getLinkData(snap, file)))
            emitter.onSuccess(ref.id)
        } else {
            emitter.onError(Exception(activity.resources.getString(R.string.registration_info)))
        }
    }

    private fun getUserId(): Single<String> {
        return Single.just(
            if (FirebaseAuth.getInstance().currentUser != null) {
                FirebaseAuth.getInstance().currentUser!!.uid
            } else {
              activity.resources.getString(R.string.user_not_sign)
            }
        )
    }

    private fun getLinkData(snap: DocumentSnapshot, file: File): Map<String, Any> {
        val username = snap.getString("username") ?: ""

        return mapOf(
            "creator_username" to username,
            "is_active" to true,
            "size" to file.length(),
            "timestamp" to Timestamp.now(),
            "title" to file.nameWithoutExtension,
            "uid" to snap.id
        )
    }
}