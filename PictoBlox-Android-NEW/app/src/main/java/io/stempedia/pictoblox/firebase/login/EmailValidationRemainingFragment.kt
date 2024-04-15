package io.stempedia.pictoblox.firebase.login

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.method.PasswordTransformationMethod
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.GsonBuilder
import com.trello.rxlifecycle4.android.lifecycle.kotlin.bindUntilEvent
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.observers.DisposableCompletableObserver
import io.reactivex.rxjava3.observers.DisposableObserver
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.databinding.FragEmailVerificationRemainingBinding
import io.stempedia.pictoblox.util.PictobloxLogger
import io.stempedia.pictoblox.util.firebaseUserDetail

const val EMAIL_TO_VERIFY = "email_to_verify"
const val LAST_EMAIL_TIMESTAMP = "last_email_timestamp"

class EmailValidationRemainingFragment : Fragment() {
    private val vm by viewModels<EmailValidationRemainingVm>()
    private val animHelper = PictoBloxActionButtonAnimHelper(lifecycle)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val mBinding = DataBindingUtil.inflate<FragEmailVerificationRemainingBinding>(
            inflater,
            R.layout.frag_email_verification_remaining,
            container,
            false
        )
        mBinding.data = vm

        vm.inputArguments.onNext(arguments!!)

        vm.outputResetClickEvent
            .bindUntilEvent(this, Lifecycle.Event.ON_STOP)
            .subscribe {
                animHelper.buttonToProgress(mBinding.textView44) {}
            }

        vm.outputEmailResend
            .bindUntilEvent(this, Lifecycle.Event.ON_STOP)
            .subscribeWith(object : DisposableObserver<Unit>() {
                override fun onNext(t: Unit) {
                    animHelper.progressToButton(mBinding.textView44) {}
                }

                override fun onError(e: Throwable) {
                    PictobloxLogger.getInstance().logException(e)
                    Toast.makeText(requireContext(), "Error in sending email", Toast.LENGTH_LONG).show()
                }

                override fun onComplete() {

                }

            })

        vm.outputUserEmailVerified
            .bindUntilEvent(this, Lifecycle.Event.ON_STOP)
            .subscribeWith(object : DisposableCompletableObserver() {
                override fun onComplete() {
                    val activityVm by activityViewModels<LoginActivityViewModel>()
                    activityVm.inputVerificationComplete.onNext(Unit)
                }

                override fun onError(e: Throwable) {

                }

            })

        vm.outputCompleteVerificationLater
            .bindUntilEvent(this, Lifecycle.Event.ON_STOP)
            .subscribe {
                val activityVm by activityViewModels<LoginActivityViewModel>()
                activityVm.wrapUp.onNext(Unit)
            }


        /*     mBinding.editText12.setOnEditorActionListener { v, actionId, event ->

                 if (actionId == EditorInfo.IME_ACTION_UNSPECIFIED || actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_NONE) {
                     hideKeyboard(mBinding.editText12)
                     return@setOnEditorActionListener true

                 }
                 return@setOnEditorActionListener false
             }

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
     */

        return mBinding.root
    }

    private fun hideKeyboard(view: View) {
        //mBinding.editText7.inputType = InputType.TYPE_NULL
        activity?.also {
            val imm: InputMethodManager = it.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

}


@SuppressLint("CheckResult")
class EmailValidationRemainingVm(application: Application) : AndroidViewModel(application) {

    //private val spManager = SPManager(application)

    val shouldShowResendEmail = ObservableBoolean(false)

    //val emailToVerify = ObservableField<String>()
    private val handler = Handler()
    private val oneMinInMillis = 60_000
    val showProgress = ObservableBoolean(false)
    val infoText = ObservableField<Spannable>()

    val inputArguments: PublishSubject<Bundle> = PublishSubject.create<Bundle>()


    val outputUserEmailVerified: Completable = Completable.create { emitter ->

        if (FirebaseAuth.getInstance().currentUser == null) {
            emitter.onError(Error("User not created"))
            return@create
        }

        val listenerRegistration = firebaseUserDetail(FirebaseAuth.getInstance().currentUser!!.uid)
            .addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                documentSnapshot?.also {

                    if (firebaseFirestoreException != null) {
                        PictobloxLogger.getInstance().logException(firebaseFirestoreException)
                        return@also
                    }
                    try {
                        val isVerified = it.get("is_verified") as Boolean

                        if (isVerified) {
                            emitter.onComplete()
                        }

                    } catch (e: Exception) {
                        PictobloxLogger.getInstance().logException(e)
                        emitter.onError(Exception("Error in verification"))
                    }
                }
            }


        emitter.setCancellable {
            listenerRegistration.remove()
        }
    }

    val outputResetClickEvent: PublishSubject<String> = PublishSubject.create<String>()

    val outputEmailResend: Observable<Unit> = outputResetClickEvent
        .observeOn(Schedulers.io())
        .map {
            val task = FirebaseFunctions.getInstance("asia-east2")
                .getHttpsCallable("sendVerificationEmail")
                .call()

            val res = Tasks.await(task)
            val sendEmailRes = GsonBuilder().create().fromJson<SendEmailRes>(res.data as String, SendEmailRes::class.java)

            if (sendEmailRes.status != "success") {
                throw java.lang.Exception(sendEmailRes.error)
            }
        }
        .observeOn(AndroidSchedulers.mainThread())

    val outputCompleteVerificationLater: PublishSubject<String> = PublishSubject.create<String>()


    private val resendTimerRunnable = Runnable {
        shouldShowResendEmail.set(true)
    }

    init {
        inputArguments
            .subscribe { arguments ->

                val email = arguments?.getString(EMAIL_TO_VERIFY)!!
                setInfoText(application, email)

                val lastEmailTimestamp: Timestamp = arguments.getParcelable(LAST_EMAIL_TIMESTAMP)!!
                setResendTimer(lastEmailTimestamp.seconds)
            }

        outputResetClickEvent
            .subscribe {
                showProgress.set(true)
                //shouldShowResendEmail.set(false)
            }

        outputEmailResend
            .subscribeWith(object : DisposableObserver<Unit>() {
                override fun onNext(t: Unit) {
                    shouldShowResendEmail.set(false)
                    showProgress.set(false)
                    setResendTimer(System.currentTimeMillis() / 1000)
                }

                override fun onError(e: Throwable) {
                    shouldShowResendEmail.set(false)
                    showProgress.set(false)
                    setResendTimer(System.currentTimeMillis() / 1000)
                    //showError that email failed to send
                }

                override fun onComplete() {

                }
            })

    }

    private fun setResendTimer(lastMailSentTime: Long) {
        val currentTimeInSec = System.currentTimeMillis() / 1000

        if (lastMailSentTime < currentTimeInSec - 60) {
            resendTimerRunnable.run()

        } else {
            val time = (lastMailSentTime + 60) - currentTimeInSec
            handler.postDelayed(resendTimerRunnable, time * 1000)
        }
    }

    private fun setInfoText(context: Context, email: String) {
        val builder = SpannableStringBuilder()

        builder.append(SpannableString(context.getString(R.string.verify_email_info_p1)))

        val emailSpan = SpannableString(email)


        emailSpan.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            email.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        builder.append(" ")
        builder.append(emailSpan)
        builder.append(".")
        builder.append(SpannableString(context.getString(R.string.verify_email_info_p2)))

        infoText.set(builder)
    }
}