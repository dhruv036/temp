package io.stempedia.pictoblox.firebase.login

import android.app.Activity.RESULT_OK
import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.esafirm.imagepicker.features.ImagePicker
import com.esafirm.imagepicker.features.ReturnMode
import com.esafirm.imagepicker.model.Image
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.GsonBuilder
import com.trello.rxlifecycle4.android.lifecycle.kotlin.bindUntilEvent
import com.yalantis.ucrop.UCrop
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import io.stempedia.pictoblox.BuildConfig
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.databinding.FragMinorPwdAgeBinding
import io.stempedia.pictoblox.util.PictobloxLogger
import io.stempedia.pictoblox.util.firebaseUserDetail
import io.stempedia.pictoblox.util.fireStorageUserThumb
import java.io.File

//const val SIGN_UP_EMAIL = "email"

class MinorPasswordAndAgeAfterConsentFragment : Fragment() {
    private val vm by viewModels<MinorPasswordAndAgeAfterConsentVM>()
    private val TAG = "MinorPasswordAndAgeAfte"
    private lateinit var mBinding: FragMinorPwdAgeBinding
    private val animHelper = PictoBloxActionButtonAnimHelper(lifecycle)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.frag_minor_pwd_age, container, false)
        mBinding.data = vm

        vm.outputThumbClicked
            .bindUntilEvent(this, Lifecycle.Event.ON_STOP)
            .subscribe {
                ImagePicker.create(this)
                    .returnMode(ReturnMode.ALL) // set whether pick and / or camera action should return immediate result or not.
                    .toolbarImageTitle("Select Profile picture") // image selection title
                    .toolbarArrowColor(Color.WHITE) // Toolbar 'up' arrow color
                    .includeVideo(false) // Show video on image picker
                    .single() // single mode
                    .showCamera(true) // show camera or not (true by default)
                    .imageDirectory("Camera") // directory name for captured image  ("Camera" folder by default)
                    .enableLog(false) // disabling log
                    .theme(R.style.PurpleToolbar)
                    .start(UCrop.REQUEST_CROP) // start image picker activity with request code
            }

        vm.outputOpenCropper
            .bindUntilEvent(this, Lifecycle.Event.ON_STOP)
            .subscribe { uCrop ->
                uCrop.start(requireContext(), this)
            }

        vm.outputOpenAgePicker
            .bindUntilEvent(this, Lifecycle.Event.ON_STOP)
            .subscribe { lastPos ->
                val ageArray = resources.getStringArray(R.array.minor_age_options)
                AlertDialog.Builder(requireContext())
                    .setTitle("Select Your Age")
                    .setSingleChoiceItems(ageArray, lastPos) { d, which ->
                        vm.inputAgeSelected.onNext(Pair(which, ageArray[which]))
                        d.dismiss()
                    }
                    .create()
                    .show()
            }

        vm.outputShowToast
            .bindUntilEvent(this, Lifecycle.Event.ON_STOP)
            .subscribe {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }

        vm.outputShowCreatingAccountAnimation
            .bindUntilEvent(this, Lifecycle.Event.ON_STOP)
            .subscribe { startAnimation ->
                if (startAnimation) {
                    animHelper.buttonToProgress(mBinding.button14) {}

                } else {
                    animHelper.progressToButton(mBinding.button14) {}
                }
            }


        vm.outputSignUpCompleted
            .bindUntilEvent(this, Lifecycle.Event.ON_STOP)
            .subscribeWith(object : io.reactivex.rxjava3.observers.DisposableObserver<SignUpCompleteResponse>() {
                override fun onNext(t: SignUpCompleteResponse) {
                    val activityVM by activityViewModels<LoginActivityViewModel>()
                    activityVM.inputStage2Completed.onNext(t)
                }

                override fun onError(e: Throwable) {
                    Toast.makeText(requireContext(), "Server error: please check your connectivity", Toast.LENGTH_LONG).show()
                }

                override fun onComplete() {

                }

            })

        mBinding.ccpMinorLogin
            .setOnCountryChangeListener {
                vm.inputCountrySelected.onNext(mBinding.ccpMinorLogin.selectedCountryName)
            }

        vm.inputCountrySelected.onNext(mBinding.ccpMinorLogin.selectedCountryName)

        return mBinding.root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (ImagePicker.shouldHandle(requestCode, resultCode, data)) {
            // or get a single image only
            val image = ImagePicker.getFirstImageOrNull(data)

            vm.inputOnImageChosen.onNext(image)

        } else if (requestCode == UCrop.REQUEST_CROP) {

            data?.also {
                if (resultCode == RESULT_OK) {
                    val path = UCrop.getOutput(it)
                    vm.inputHandleCroppedImage.onNext(path!!)

                } else if (resultCode == UCrop.RESULT_ERROR) {
                    Toast.makeText(requireContext(), "Error in processing image", Toast.LENGTH_LONG).show()
                    UCrop.getError(data)?.also { t ->
                        PictobloxLogger.getInstance().logException(t)
                    }
                }
            }
        }
    }
}

class MinorPasswordAndAgeAfterConsentVM(application: Application) : AndroidViewModel(application) {
    val profileImageBitmap = ObservableField<Bitmap>()
    private var imagePath: Uri? = null
    private var ageArrayItemPosition: Int = (-1)
    val age = ObservableField<String>("Age")
    val showProgress = ObservableBoolean()
    val key = ObservableField<String>("")

    private var country = ""

    val inputAgeClicked: PublishSubject<String> = PublishSubject.create<String>()
    val outputThumbClicked: PublishSubject<String> = PublishSubject.create<String>()

    val outputOpenAgePicker: Observable<Int> = inputAgeClicked
        .map { ageArrayItemPosition }

    val inputOnImageChosen: PublishSubject<Image> = PublishSubject.create<Image>()

    val outputOpenCropper: Observable<UCrop> = inputOnImageChosen
        .map { image ->
            val file = File.createTempFile("ProfilePic", ".png", application.cacheDir)

            UCrop.of(Uri.parse("file://${image.path}"), Uri.fromFile(file))
                .withAspectRatio(10f, 10f)
                .withMaxResultSize(300, 300)
        }

    val inputHandleCroppedImage: PublishSubject<Uri> = PublishSubject.create<Uri>()

    val inputAgeSelected: PublishSubject<Pair<Int, String>> = PublishSubject.create<Pair<Int, String>>()

    val inputCountrySelected: PublishSubject<String> = PublishSubject.create<String>()

    val inputSubmitClicked: PublishSubject<String> = PublishSubject.create<String>()

    val outputShowToast: PublishSubject<String> = PublishSubject.create<String>()

    val outputShowCreatingAccountAnimation: PublishSubject<Boolean> = PublishSubject.create<Boolean>()

    val outputSignUpCompleted: Observable<SignUpCompleteResponse> = inputSubmitClicked
        .filter {
            if (ageArrayItemPosition == -1) {
                outputShowToast.onNext("Please select your age")
                return@filter false
            }

            if (country.isEmpty()) {
                outputShowToast.onNext("Please select country")
                return@filter false
            }

            return@filter true
        }
        .doOnNext {
            outputShowCreatingAccountAnimation.onNext(true)
            showProgress.set(true)
        }
        .observeOn(Schedulers.io())
        .map {
            val uid = FirebaseAuth.getInstance().currentUser!!.uid

            if (imagePath != null) {
                val thumbUploadTask = fireStorageUserThumb(uid).putFile(imagePath!!)
                Tasks.await(thumbUploadTask)
            }

            val userDetailUpdateTask = firebaseUserDetail(uid)
                .update(
                    mapOf(
                        "age" to "${ageArrayItemPosition + 8}",
                        "server" to BuildConfig.SERVER,
                        "country" to country,
                        "key" to key.get(),
                        "is_secondary_detail_filled" to true
                    )
                )

            Tasks.await(userDetailUpdateTask)

            val task = FirebaseFunctions.getInstance("asia-east2")
                .getHttpsCallable("completeSignUpEntry")
                .call()

            val res = Tasks.await(task)

            GsonBuilder().create().fromJson(res.data as String, SignUpCompleteResponse::class.java)

        }
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext {
            outputShowCreatingAccountAnimation.onNext(false)
            showProgress.set(false)
        }
        .doOnError {
            outputShowCreatingAccountAnimation.onNext(false)
            showProgress.set(false)
        }

    init {

        inputHandleCroppedImage
            .subscribe { path ->

                imagePath = path

                Glide.with(application)
                    .asBitmap()
                    .apply(RequestOptions().circleCrop())
                    .load(path)
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onLoadCleared(placeholder: Drawable?) {
                        }

                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            profileImageBitmap.set(resource)
                        }
                    })
            }

        inputAgeSelected
            .subscribe {
                ageArrayItemPosition = it.first
                age.set(it.second)
            }

        inputCountrySelected
            .subscribe { selectedCountryName ->
                selectedCountryName?.also {
                    country = it
                }
            }

        inputSubmitClicked
            .subscribe {

            }

        Glide.with(application)
            .asBitmap()
            .apply(RequestOptions().circleCrop())
            .load(R.drawable.ic_account3)
            .into(object : CustomTarget<Bitmap>() {
                override fun onLoadCleared(placeholder: Drawable?) {

                }

                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    profileImageBitmap.set(resource)
                }
            })

    }

}

class SignUpCompleteResponse(var status: String?, var creditsAdded: Long?, var error: String?)