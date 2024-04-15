package io.stempedia.pictoblox.firebase.login

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.method.PasswordTransformationMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.GsonBuilder
import com.trello.rxlifecycle4.android.lifecycle.kotlin.bindUntilEvent
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.observers.DisposableCompletableObserver
import io.reactivex.rxjava3.subjects.AsyncSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.databinding.FragSignUpStage1Binding
import io.stempedia.pictoblox.util.PictoBloxAnalyticsEventLogger
import io.stempedia.pictoblox.util.PictobloxLogger
import io.stempedia.pictoblox.util.SPManager
import java.util.Locale
import java.util.concurrent.TimeUnit

const val ACCOUNT_TYPE = "account_type"

//Account type enum
//AsyncSubject
class SignUpStage1Fragment : Fragment() {
    private val vm by viewModels<SignUpStage1Vm>()

    private lateinit var mBinding: FragSignUpStage1Binding
    private val animHelper = PictoBloxActionButtonAnimHelper(lifecycle)
    private var popupWindow: PopupWindow? = null
    private val handler = Handler()
    private  val TAG = "SignUpStage1Fragment"
    lateinit var spManager :SPManager


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)


        vm.inputArguments.onNext(arguments ?: Bundle())
        vm.inputArguments.onComplete()
    }
    fun updateLocale(c: Context, localeToSwitchTo: Locale) {
        var context = c
        val resources: Resources = context.resources
        val configuration: Configuration = resources.configuration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(localeToSwitchTo)
        } else {
            configuration.locale = localeToSwitchTo
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            activity?.createConfigurationContext(configuration)
        }
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        spManager =  SPManager(requireContext())
        var code =  spManager.pictobloxLocale
        code =  if (code.contains("cn") || code.contains("tw")) code.substring(3,5) else code
        updateLocale(requireContext(),Locale(code))
        mBinding = DataBindingUtil.inflate(inflater, R.layout.frag_sign_up_stage_1, container, false)
        mBinding.data = vm

        val activityVm by activityViewModels<LoginActivityViewModel>()

        mBinding.textView59.movementMethod = LinkMovementMethod.getInstance()


        val builder = SpannableStringBuilder()

        builder.append(activity?.resources?.getString(R.string.terms)+"\n")

        val text3 = SpannableString(activity?.resources?.getString(R.string.privacy))

        text3.setSpan(
            clickSpanListener,
            0,
            text3.length,
            Spannable.SPAN_INCLUSIVE_INCLUSIVE
        )

        text3.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.hyperlink_color)),
            0,
            text3.length,
            Spannable.SPAN_INCLUSIVE_INCLUSIVE
        )

        builder.append(text3)

        mBinding.textView59.text = builder

        mBinding.imageView37.setOnTouchListener { _, event ->
            when (event!!.action) {
                MotionEvent.ACTION_DOWN -> {
                    mBinding.editText8.transformationMethod = null
                }

                MotionEvent.ACTION_UP -> {
                    mBinding.editText8.transformationMethod = PasswordTransformationMethod()
                }

                MotionEvent.ACTION_CANCEL -> {
                    mBinding.editText8.transformationMethod = PasswordTransformationMethod()
                }

            }

            true
        }

        vm.outputOnStage1Completed
            .bindUntilEvent(this, Lifecycle.Event.ON_STOP)
            .subscribe { email ->
                activityVm.inputOnStage1Completed.onNext(email)
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

        vm.outputShowInvalidEmailPopup
            .bindUntilEvent(this, Lifecycle.Event.ON_STOP)
            .subscribe {
                showPopWindowError(mBinding.etEmail, it)
            }

        vm.outputShowInvalidUserNamePopup
            .bindUntilEvent(this, Lifecycle.Event.ON_STOP)
            .subscribe {
                showPopWindowError(mBinding.editText12, it)
            }

        vm.outputShowInvalidPasswordPopup
            .bindUntilEvent(this, Lifecycle.Event.ON_STOP)
            .subscribe {
                showPopWindowError(mBinding.editText8, it)
            }

        vm.outputOpenPrivacyLink
            .bindUntilEvent(this, Lifecycle.Event.ON_STOP)
            .subscribe {
                startActivity(it)
            }



        return mBinding.root
    }

    override fun onStop() {
        super.onStop()
        popupWindow?.dismiss()
        handler.removeCallbacks(cancelRunnable)
    }

    private val clickSpanListener = object : ClickableSpan() {
        override fun onClick(widget: View) {
            vm.inputPrivacySpanClicked.onNext(Unit)
        }
    }

    private fun showPopWindowError(v: View, text: String) {
        popupWindow?.dismiss()

        val view =
            layoutInflater.inflate(
                R.layout.include_login_error,
                null,
                false
            )

        view.findViewById<TextView>(R.id.textView45).also {
            it.text = text
        }

        val width = LinearLayout.LayoutParams.WRAP_CONTENT
        val height = LinearLayout.LayoutParams.WRAP_CONTENT
        val focusable = false
        popupWindow = PopupWindow(view, width, height, focusable)

        view.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        val vPos = IntArray(2)

        v.getLocationInWindow(vPos)

        val x = (vPos[0] + v.width)
        val y = (vPos[1] + v.height / 2) - (view.measuredHeight / 2)

        popupWindow?.showAtLocation(v, Gravity.NO_GRAVITY, x, y)

        handler.postDelayed(cancelRunnable, 2000)

    }

    private val cancelRunnable = Runnable {
        popupWindow?.dismiss()
        popupWindow = null
    }


}

@SuppressLint("CheckResult")
class SignUpStage1Vm(application: Application) : AndroidViewModel(application) {

    val inputArguments: AsyncSubject<Bundle?> = AsyncSubject.create()

    val inputEmailTexChange: PublishSubject<CharSequence> = PublishSubject.create<CharSequence>()

    private val modifiedInputEmailTexChange = inputEmailTexChange
        .debounce(2000, TimeUnit.MILLISECONDS)
        .observeOn(AndroidSchedulers.mainThread())
        .map { email ->
            email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
        }

    val outputShowInvalidEmailPopup: Observable<String> = modifiedInputEmailTexChange
        .filter { isValid -> !isValid }
        .map { "Email is invalid" }


    val inputUsernameTextChange: PublishSubject<CharSequence> = PublishSubject.create<CharSequence>()

    private val modifiedInputCheckUsername = inputUsernameTextChange
        .debounce(2000, TimeUnit.MILLISECONDS)
        //.observeOn(AndroidSchedulers.mainThread())
        //.observeOn(Schedulers.io())
        .doOnNext {
            showUsernameCheckingProgress.set(true)
            isUsernameValid.set(false)
        }
        .map { checkUsername(it) }
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext {
            showUsernameCheckingProgress.set(false)
        }

    val outputShowInvalidUserNamePopup: Observable<String> = modifiedInputCheckUsername
        .filter { it.status == "error" || (it.status == "success" && !it.isAvailable) }
        .map {
            if (it.status == "success") {
                "This username is not available"
            } else {
                it.error
            }
        }

    val inputPasswordTextChange: PublishSubject<CharSequence> = PublishSubject.create<CharSequence>()

    private val modifiedInputPasswordTextChange = inputPasswordTextChange
        .debounce(2000, TimeUnit.MILLISECONDS)
        .map { it.length >= 6 }
        .observeOn(AndroidSchedulers.mainThread())

    val outputShowInvalidPasswordPopup: Observable<String> = modifiedInputPasswordTextChange

        .filter { isValid -> !isValid }
        .map { "Password needs to be at least 6 characters long" }

    val outputShowCreatingAccountAnimation: PublishSubject<Boolean> = PublishSubject.create<Boolean>()
    val inputPrivacySpanClicked: PublishSubject<Unit> = PublishSubject.create<Unit>()

    val outputOpenPrivacyLink: Observable<Intent> = inputPrivacySpanClicked
        .map { Intent(Intent.ACTION_VIEW, Uri.parse("https://thestempedia.com/privacy-policy/")) }
        .filter { it.resolveActivity(application.packageManager) != null }

    val outputOnStage1Completed: PublishSubject<String> = PublishSubject.create<String>()

    val isEmailValid = ObservableBoolean()
    val isUsernameValid = ObservableBoolean()
    private var isPasswordValid = false
    private var isMinor = false
    private var accountType = ""

    val isTermAccepted = ObservableBoolean(false)
    val username = ObservableField<String>()
    val password = ObservableField<String>()

    val showProgress = ObservableBoolean()
    val showUsernameCheckingProgress = ObservableBoolean()

    val email = ObservableField<String>()
    val emailHint = ObservableField<String>()
    val spManager = SPManager(application)

    val hasError = ObservableBoolean()
    val errorMessage = ObservableField<String>()


    fun updateLocale(c: Context, localeToSwitchTo: Locale) {
        var context = c
        val resources: Resources = context.resources
        val configuration: Configuration = resources.configuration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(localeToSwitchTo)
        } else {
            configuration.locale = localeToSwitchTo
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            c?.createConfigurationContext(configuration)
        }
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }
    fun fetchLocal(context :Context) {
        var code = spManager.pictobloxLocale
        var lang = code
        var local = Locale(lang)
        if (lang.contains("tw",true) ) {
            local  = Locale.TRADITIONAL_CHINESE
        }
        if (lang.contains("cn",true) ) {
            local  = Locale.SIMPLIFIED_CHINESE
        }
        Locale.setDefault(local)
        updateLocale(context, local)
    }


    init {
        fetchLocal(application)
        Log.d("name", application.resources.getString(R.string.guardian_email))
        Log.d("name", "${application.resources.configuration.locale}")

        inputArguments
            .subscribe { bundle ->
                isMinor = bundle?.getBoolean(IS_MINOR) ?: false
                accountType = bundle?.getString(ACCOUNT_TYPE) ?: "TYPE_STUDENT"

                emailHint.set(if (isMinor) application.resources.getString(R.string.guardian_email) else application.resources.getString(R.string.your_email))
            }

        modifiedInputEmailTexChange
            .subscribe { isValid ->
                PictobloxLogger.getInstance().logd("isvalid $isValid")
                isEmailValid.set(isValid)
            }

        modifiedInputCheckUsername
            .filter { it.status == "success" && it.isAvailable }
            .subscribe {
                isUsernameValid.set(it.isAvailable)
            }

        modifiedInputPasswordTextChange
            .subscribe {
                isPasswordValid = it
            }

        outputShowCreatingAccountAnimation
            .subscribe {
                showProgress.set(it)
            }
        inputPrivacySpanClicked
            .subscribe {

            }
    }

    fun onCreateAccountCalled() {
        hasError.set(false)

        if (!isEmailValid.get()) {
            hasError.set(true)
            errorMessage.set("Please enter valid email address")
            return
        }
        if (!isUsernameValid.get()) {
            hasError.set(true)
            errorMessage.set("Please enter valid username")
            return

        }
        if (!isPasswordValid) {
            hasError.set(true)
            errorMessage.set("Password needs to be at least 6 characters long")
            return

        }
        if (!isTermAccepted.get()) {
            hasError.set(true)
            errorMessage.set("Please accept Terms and Privacy Policy")
            return
        }

        val emailFirstPart = email.get()!!.split("@")[0]
        val proxy = "${emailFirstPart}_${username.get()}@learn.thestempedia.com"

        val payload = createPayload(email.get()!!, proxy, username.get()!!, accountType, isMinor)

        outputShowCreatingAccountAnimation.onNext(true)

        linkCredential(proxy, password.get()!!)
            .andThen(setUserDetailCompletable(payload))
            .andThen(sendVerificationEmail())
            .subscribeWith(object : DisposableCompletableObserver() {
                override fun onComplete() {
                    PictoBloxAnalyticsEventLogger.getInstance().setSignUpCompleted()
                    spManager.isSignUpIncomplete = false
                    outputShowCreatingAccountAnimation.onNext(false)
                    outputOnStage1Completed.onNext(email.get()!!)

                    PictobloxLogger.getInstance().logd("Stage 1 complete : ${FirebaseAuth.getInstance().currentUser!!.uid}")

                }

                override fun onError(e: Throwable) {
                    outputShowCreatingAccountAnimation.onNext(false)
                    PictobloxLogger.getInstance().logException(e)
                    hasError.set(true)
                    errorMessage.set("Server error: please check your connectivity")
                }

            })

    }


    private fun checkUsername(characterSeq: CharSequence): CheckUsernameResponse {

        return if (TextUtils.isEmpty(characterSeq.toString().trim())) {
            CheckUsernameResponse("error", "", false, "Username cannot be empty")

        } else {
            val task = FirebaseFunctions.getInstance("asia-east2")
                .getHttpsCallable("checkUsername3")
                .call(mapOf("userName" to characterSeq.toString()))

            try {
                val result = Tasks.await(task)
                if (result.data != null) {
                    GsonBuilder().create().fromJson<CheckUsernameResponse>(result.data as String, CheckUsernameResponse::class.java)
                } else {
                    CheckUsernameResponse("error", "", false, "Server error, please try later")
                }

            } catch (e: Exception) {
                PictobloxLogger.getInstance().logException(e)
                CheckUsernameResponse("error", "", false, "Please check your Internet connection")
            }
        }
    }

    private fun linkCredential(email: String, pwd: String) = Completable.create { emitter ->
        val credential = EmailAuthProvider.getCredential(email, pwd)
        FirebaseAuth.getInstance().currentUser?.linkWithCredential(credential)
            ?.addOnSuccessListener {
                emitter.onComplete()
            }
            ?.addOnFailureListener {
                emitter.onError(Exception(it))

            } ?: kotlin.run {
            emitter.onError(Exception("No user Logged in"))
        }
    }

    private fun createPayload(email: String, proxy: String, username: String, accountType: String, isMinor: Boolean): Map<String, Any> {
        val mutableMap = mutableMapOf(
            "email" to email,
            "is_verified" to false,
            "proxy_email" to proxy,
            "user_created_on" to Timestamp.now(),
            "username" to username,
            "is_secondary_detail_filled" to false,
            "account_type" to accountType,
            "is_minor" to isMinor
        )

        if (accountType == "TYPE_TEACHER") {
            mutableMap["is_teacher_credential_verified"] = false
        }


        return mutableMap

    }

    private fun setUserDetailCompletable(payload: Map<String, Any>) = Completable.create { emitter ->
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(FirebaseAuth.getInstance().currentUser!!.uid)
            .set(payload)
            .addOnSuccessListener {
                PictobloxLogger.getInstance().logd("Data upload succeeded")
                emitter.onComplete()
            }

            .addOnFailureListener {
                PictobloxLogger.getInstance().logd("Data upload error")
                emitter.onError(it)
            }
    }

    private fun sendVerificationEmail() = Completable.create { emitter ->

        FirebaseFunctions.getInstance("asia-east2")
            .getHttpsCallable("sendVerificationEmail")
            .call()
            .addOnSuccessListener { httpsCallableResult ->
                val res = GsonBuilder().create().fromJson<SendEmailRes>(httpsCallableResult.data as String, SendEmailRes::class.java)

                if (res.status == "success") {
                    emitter.onComplete()
                } else {
                    emitter.onError(Exception(res.error))
                }
            }.addOnFailureListener {
                PictobloxLogger.getInstance().logException(it)
                emitter.onError(Exception("Please check your Internet connection"))
            }
    }

}

class SendEmailRes(var status: String, var error: String?)


