package io.stempedia.pictoblox.profile

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableLong
import androidx.lifecycle.AndroidViewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Source
import com.google.gson.GsonBuilder
import com.trello.rxlifecycle4.android.lifecycle.kotlin.bindToLifecycle
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.observers.DisposableObserver
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.databinding.ActivityCreditDetailBinding
import io.stempedia.pictoblox.firebase.login.PictoBloxActionButtonAnimHelper
import io.stempedia.pictoblox.help.HelpActivity
import io.stempedia.pictoblox.util.*
import retrofit2.Response
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

//add test and oth
class CreditDetailActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityCreditDetailBinding
    private val animHelper = PictoBloxActionButtonAnimHelper(lifecycle)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_credit_detail)

        setSupportActionBar(mBinding.tbCredit)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        mBinding.tbCredit.setNavigationOnClickListener { finish() }


        val viewModel by viewModels<CreditDetailViewModel>()

        mBinding.data = viewModel

        viewModel.outputHelpClicked
            .bindToLifecycle(this)
            .subscribe {
                startActivity(Intent(this, HelpActivity::class.java))
            }

        viewModel.outputCouponValidationError
            .bindToLifecycle(this)
            .subscribe {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }

        viewModel.outputRedeemClicked
            .bindToLifecycle(this)
            .subscribeWith(object : DisposableObserver<Long>() {
                override fun onNext(t: Long) {
                    AlertDialog.Builder(this@CreditDetailActivity)
                        .setTitle("Code redeemed successfully")
                        .setMessage("Yay!, $t credits added to your account!")
                        .setNeutralButton("Okay") { _, _ -> }
                        .create()
                        .show()

                }

                override fun onError(e: Throwable) {
                    e?.also {
                        AlertDialog.Builder(this@CreditDetailActivity)
                            .setTitle("Error")
                            .setMessage("Please check your connectivity")
                            .setNeutralButton("Okay") { _, _ -> }
                            .create()
                            .show()

                    }
                }

                override fun onComplete() {

                }

            })


        viewModel.outputCouponValidationError
            .bindToLifecycle(this)
            .subscribe {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }

        viewModel.outputCouponServerErros
            .bindToLifecycle(this)
            .subscribe {
                AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage(it)
                    .setNeutralButton("Okay") { _, _ -> }
                    .create()
                    .show()
            }

        viewModel.outputShowCreatingAccountAnimation
            .bindToLifecycle(this)
            .subscribe { show ->
                if (show) {
                    animHelper.buttonToProgress(mBinding.textView104) {}

                } else {
                    animHelper.progressToButton(mBinding.textView104) {}

                }
            }

    }
}


class CreditDetailViewModel(application: Application) : AndroidViewModel(application) {
    val totalScore = ObservableLong()
    val isLoadingData = ObservableBoolean(false)
    val isErrorWhileLoadingData = ObservableBoolean(false)
    val isRedeemCouponCallActive = ObservableBoolean(false)
    val error = ObservableField("")
    val profileIcon = ObservableField<Bitmap>()
    val username = ObservableField("")
    val email = ObservableField("")
    val dobOrAge = ObservableField("")
    val country = ObservableField("")
    val couponCode = ObservableField("")

    val inputGoClicked: PublishSubject<String> = PublishSubject.create<String>()

    val outputHelpClicked: PublishSubject<String> = PublishSubject.create<String>()

    val outputCouponValidationError: PublishSubject<String> = PublishSubject.create<String>()

    val outputCouponServerErros: PublishSubject<String> = PublishSubject.create<String>()

    val inputRetryClicked: PublishSubject<String> = PublishSubject.create<String>()

    private val inputInitFirstTime: PublishSubject<String> = PublishSubject.create<String>()

    val outputShowCreatingAccountAnimation: PublishSubject<Boolean> = PublishSubject.create()

    val outputRedeemClicked: Observable<Long> = inputGoClicked
        .filter {

            PictobloxLogger.getInstance().logd("outputRedeemClicked filter")

            if (TextUtils.isEmpty(couponCode.get()) || couponCode.get()?.length != 8) {
                outputCouponValidationError.onNext("coupon code can only be 8 characters long")
                return@filter false
            }


            val pattern = Pattern.compile("^[a-zA-Z0-9]*\$")
            if (!pattern.matcher(couponCode.get() ?: "").matches()) {
                outputCouponValidationError.onNext("invalid coupon code")
                return@filter false
            }

            return@filter true

        }
        .doAfterNext {
            outputShowCreatingAccountAnimation.onNext(true)
        }
        .observeOn(Schedulers.io())
        .map {
            FirebaseAuth.getInstance().currentUser?.let { user ->

                val code = couponCode.get() ?: ""

                val result = Tasks.await(user.getIdToken(true))

                pictoBloxFirebaseFunctionAPI.checkCouponV1(code, "Bearer ${result.token}").execute()

            } ?: kotlin.run {
                return@map Response.error(1,null)
            }
        }
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext {
            outputShowCreatingAccountAnimation.onNext(false)
        }
        .filter { functionRes ->

            if (functionRes == null){
                outputCouponServerErros.onNext("User not logged in!")
                return@filter false
            }

            if (functionRes.isSuccessful) {
                functionRes.body()?.also { body ->
                    if (TextUtils.isEmpty(body.error)) {
                        return@filter true
                    } else {
                        outputCouponServerErros.onNext("${body.text}")
                        return@filter false

                    }
                }

                outputCouponServerErros.onNext("Unknown Server Error! please try later")
                return@filter false


            } else {
                functionRes.errorBody()?.also { body ->
                    val res = GsonBuilder().create().fromJson(body.string(), CheckCouponErrorPOJO::class.java)
                    outputCouponServerErros.onNext("${res.text}")

                } ?: kotlin.run {
                    outputCouponServerErros.onNext("Unknown Server Error! please try later")
                }

                return@filter false
            }
        }
        .map { functionRes ->
            functionRes!!.body()?.credits ?: 0L
        }
        .doOnNext { score ->
            totalScore.set(totalScore.get() + score)
            couponCode.set("")
        }

    private val outputUserData = PublishSubject.merge(inputInitFirstTime, inputRetryClicked)
        .map { FirebaseAuth.getInstance().currentUser?.uid.toString() }
        .filter { uid ->
            if (uid != null) {
                true

            } else {
                error.set("No user signed in")
                isErrorWhileLoadingData.set(true)
                false
            }
        }
        .doOnNext {
            isLoadingData.set(true)
            isErrorWhileLoadingData.set(false)
        }
        .observeOn(Schedulers.io())
        .map { uid ->
            try {
                val userDetail = Tasks.await(firebaseUserDetail(uid!!).get(Source.SERVER), 30, TimeUnit.SECONDS)
                val userCreditDetail = Tasks.await(firebaseUserCredits(uid!!).get(Source.SERVER), 30, TimeUnit.SECONDS)

                Pair(userDetail, userCreditDetail)
            } catch (e: Exception) {
                Pair(null, null)

            }
        }
        .doOnNext {
            isLoadingData.set(false)
        }
        .doOnError {
            isLoadingData.set(false)
        }
        .filter { userSnapPair ->
            if (userSnapPair.first != null && userSnapPair.first!!.exists()) {
                true
            } else {
                error.set("Error in fetching user detail, please try later")
                isErrorWhileLoadingData.set(true)
                false
            }
        }
        .observeOn(AndroidSchedulers.mainThread())


    private val outputUserThumb = PublishSubject.merge(inputInitFirstTime, inputRetryClicked)
        .map { FirebaseAuth.getInstance().currentUser?.uid.toString() }
        .filter { uid ->
            uid != null
        }

    init {
        //TODO current user null error handling
        outputUserData
            .subscribeWith(object : DisposableObserver<Pair<DocumentSnapshot?, DocumentSnapshot?>>() {

                override fun onNext(userSnapPair: Pair<DocumentSnapshot?, DocumentSnapshot?>) {

                    totalScore.set(userSnapPair.second!!.getLong("pictoblox_credits")!!)

                    username.set(userSnapPair.first!!.get("username") as String)
                    email.set(userSnapPair.first!!.get("email") as String)
                    country.set(userSnapPair.first!!.get("country") as String)

                    val isMinor = if (userSnapPair.first!!.get("is_minor") != null) {
                        userSnapPair.first!!.get("is_minor") as Boolean
                    } else {
                        userSnapPair.first!!.get("isMinor") as Boolean
                    }

                    if (isMinor) {
                        dobOrAge.set("Age : ${userSnapPair.first!!.get("age").toString()}")

                    } else {
                        val birthdate = if (userSnapPair.first!!.get("birthdate") != null) {
                            userSnapPair.first!!.get("birthdate") as Timestamp

                        } else {
                            userSnapPair.first!!.get("birthDate") as Timestamp
                        }

                        val date = birthdate.toDate()

                        val mEEEddMMMyyyy = SimpleDateFormat("EEE dd-MMM-yyyy", Locale.US)

                        val z = mEEEddMMMyyyy.format(date)

                        dobOrAge.set("B'date : $z")

                    }
                }

                override fun onError(e: Throwable) {
                    PictobloxLogger.getInstance().logException(e)
                    error.set("Error in fetching user detail, please try later")
                    isErrorWhileLoadingData.set(true)

                }

                override fun onComplete() {
                }


            })


        outputUserThumb
            .subscribe { uid ->
                Glide.with(application)
                    .asBitmap()
                    .apply(RequestOptions().circleCrop().error(R.drawable.ic_account3))
                    .load(fireStorageUserThumb(uid!!))
                    .error(R.drawable.ic_account3)
                    .into(object : CustomTarget<Bitmap>() {

                        override fun onLoadFailed(errorDrawable: Drawable?) {
                            super.onLoadFailed(errorDrawable)

                            Glide.with(application)
                                .asBitmap()
                                .apply(RequestOptions().circleCrop())
                                .load(R.drawable.ic_account3)
                                .into(object : CustomTarget<Bitmap>() {
                                    override fun onLoadCleared(placeholder: Drawable?) {

                                    }

                                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                        profileIcon.set(resource)
                                    }
                                })

                        }

                        override fun onLoadCleared(placeholder: Drawable?) {

                        }

                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            profileIcon.set(resource)
                        }
                    })
            }

        outputShowCreatingAccountAnimation.subscribe {
            isRedeemCouponCallActive.set(it)
        }

        inputInitFirstTime.onNext("")
    }
}