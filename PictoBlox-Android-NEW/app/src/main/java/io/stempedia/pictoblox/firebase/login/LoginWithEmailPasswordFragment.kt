package io.stempedia.pictoblox.firebase.login

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApiNotAvailableException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.GsonBuilder
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.databinding.FragLoginEmailPwdBinding
import io.stempedia.pictoblox.util.PictoBloxAnalyticsEventLogger
import io.stempedia.pictoblox.util.PictobloxLogger
import io.stempedia.pictoblox.util.SPManager
import java.util.concurrent.Callable

const val FUNCTION = "login_function"
/*const val FUNCTION_SIGN_IN_FLOW = "internal"
const val FUNCTION_EXTERNAL_INVOCATION = "external"*/

const val FUNCTION_INTERNAL_SIGN_IN = "ISI"
const val FUNCTION_EXTERNAL_SIGN_IN = "ESI"
const val FUNCTION_EXTERNAL_VERIFICATION = "EV"

interface LoginFragmentCallbacks {
    fun onSignInComplete()
    fun switchToAgeQueryFragment()
    fun switchToForgotPwdFragment()
    fun switchToForgotUsernameFragment()
}

class LoginWithEmailPasswordFragment : Fragment() {
    private val vm = LoginWithEmailPasswordVM(this, lifecycle)
    private lateinit var mBinding: FragLoginEmailPwdBinding

    //private val vm : LoginWithEmailPasswordVM by viewModels()
    private var popupWindow: PopupWindow? = null
    private val handler = Handler()
    private val animHelper = PictoBloxActionButtonAnimHelper(lifecycle)

    override fun onAttach(context: Context) {
        super.onAttach(context)
        lifecycle.addObserver(vm)
    }


    override fun onDetach() {
        super.onDetach()
        lifecycle.removeObserver(vm)
        popupWindow?.dismiss()
        handler.removeCallbacks(cancelRunnable)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.frag_login_email_pwd, container, false)
        mBinding.data = vm
        vm.setArgument(arguments)

        mBinding.imageView33.setOnTouchListener { _, event ->
            when (event!!.action) {
                MotionEvent.ACTION_DOWN -> {
                    mBinding.editText6.transformationMethod = null
                }

                MotionEvent.ACTION_UP -> {
                    mBinding.editText6.transformationMethod = PasswordTransformationMethod()
                }

                MotionEvent.ACTION_CANCEL -> {
                    mBinding.editText6.transformationMethod = PasswordTransformationMethod()
                }

            }

            true
        }

        return mBinding.root
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

    fun showPasswordError() {
        showPopWindowError(mBinding.editText6, "Password cannot be empty")
    }

    fun showUsernameError() {
        showPopWindowError(mBinding.editText5, "Username cannot be empty")
    }

    fun showProgress() {
        animHelper.buttonToProgress(mBinding.button11) {}
    }

    fun hideProgress() {
        animHelper.progressToButton(mBinding.button11) {}
    }

    fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    /*fun setUserSignUpFlag(isSignUpComplete: Boolean) {
        SPManager(requireContext()).isSignUpIncomplete = isSignUpComplete
    }*/
}

class LoginWithEmailPasswordVM(val fragment: LoginWithEmailPasswordFragment, val lifecycle: Lifecycle) : LifecycleObserver {
    val username = ObservableField<String>()
    val password = ObservableField<String>()
    val isLoggingIn = ObservableBoolean(false)
    val isLoggingSuccess = ObservableBoolean(false)
    val handler = Handler()
    val isSignInExternalFlow = ObservableBoolean()
    val contentBackground = ObservableInt()
    private var funtion = ""
    private var argument: Bundle? = null

    private val exitDelay = Runnable {
        if (fragment.isResumed) {
            fragment.activity?.also {
                val callback = it as LoginFragmentCallbacks
                callback.onSignInComplete()
            }
        }
    }


    fun setArgument(argument: Bundle?) {
        this.argument = argument
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun init() {

        if (isLoggingSuccess.get()) {
            if (isSignInExternalFlow.get()) {
                exitDelay.run()
            } else {
                handler.postDelayed(exitDelay, 1500)
            }
            return
        }

        argument?.also { argument ->
            funtion = argument.getString(FUNCTION, FUNCTION_INTERNAL_SIGN_IN)
            isSignInExternalFlow.set(funtion != FUNCTION_INTERNAL_SIGN_IN)

        } ?: kotlin.run {
            funtion = FUNCTION_INTERNAL_SIGN_IN
            isSignInExternalFlow.set(false)
        }

        if (isSignInExternalFlow.get()) {
            contentBackground.set(R.drawable.round_login_background_opaque)

        } else {
            contentBackground.set(R.drawable.round_login_background)

        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun dispose() {
        handler.removeCallbacks(exitDelay)
    }

    fun signInClicked() {
        if (verifyUserEntry()) {
            tryToLogIn()
        }
    }

    fun signUpClicked() {
        /*fragment.activity?.also {
            val callback = it as LoginFragmentCallbacks
            callback.switchToAgeQueryFragment()
        }*/

        val activityVM by fragment.activityViewModels<LoginActivityViewModel>()
        activityVM.inputSwitchToSignUp.onNext(Unit)
    }

    fun forgotPasswordClicked() {
        fragment.activity?.also {
            //val loginActivity = it as LoginActivity
            val callback = it as LoginFragmentCallbacks
            callback.switchToForgotPwdFragment()
        }
    }

    fun forgotUsernameClicked() {
        fragment.activity?.also {
            //val loginActivity = it as LoginActivity
            val callback = it as LoginFragmentCallbacks
            callback.switchToForgotUsernameFragment()
        }
    }

    private fun tryToLogIn() {
        if (isLoggingIn.get()) {
            fragment.showToast("processing, please wait")
            return
        }

        isLoggingIn.set(true)
        isLoggingSuccess.set(false)
        fragment.showProgress()

        FirebaseFunctions.getInstance("asia-east2").getHttpsCallable("checkUsername3")
            .call(mapOf("userName" to username.get()))
            .onSuccessTask { result ->
                //if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {

                if (result == null || result.data == null) {
                    throw Exception("Server error")
                }

                val responsePOJO = GsonBuilder().create().fromJson<EmailCheckResponse>(result.data as String, EmailCheckResponse::class.java)

                if (responsePOJO.status != "success") {
                    throw Exception("Server error")
                }

                if (responsePOJO.status == "success" && responsePOJO.isAvailable) {
                    throw Exception("No user exists with this username")
                }

                if (responsePOJO.status == "success" && TextUtils.isEmpty(responsePOJO.email)) {
                    throw Exception("DEV Error : link is missing")
                }

                when (funtion) {
                    FUNCTION_EXTERNAL_VERIFICATION -> {
                        return@onSuccessTask funLoginVerificationFlow(responsePOJO)

                    }
                    else -> {
                        return@onSuccessTask funSignInFlow(responsePOJO)

                    }
                }

                /*} else {
                    throw Exception("Fragment not resumed")
                }*/

            }
            .onSuccessTask {
                FirebaseFirestore.getInstance().collection("users").document(it!!).get()
            }

            .addOnSuccessListener {
                PictoBloxAnalyticsEventLogger.getInstance().setSignInCompleted()
                isLoggingIn.set(false)
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    //fragment.setUserSignUpFlag(!it.exists())
                    fragment.hideProgress()

                    if (isSignInExternalFlow.get()) {
                        exitDelay.run()
                    } else {
                        isLoggingSuccess.set(true)
                        handler.postDelayed(exitDelay, 1500)
                    }
                }
            }
            .addOnFailureListener {
                PictobloxLogger.getInstance().logException(it)
                isLoggingSuccess.set(false)
                isLoggingIn.set(false)

                if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {

                    PictobloxLogger.getInstance().logException(it)
                    fragment.hideProgress()

                    if (it is FirebaseApiNotAvailableException) {
                        fragment.showToast("Sign In service is not supported on this device")

                    } else {
                        fragment.showToast(it.message ?: "Error in Sign In")
                    }
                }
            }
    }

    private fun verifyUserEntry(): Boolean {

        return if (!TextUtils.isEmpty(username.get())) {

            if (!TextUtils.isEmpty(password.get())) {
                true

            } else {
                fragment.showPasswordError()
                false
            }


        } else {
            fragment.showUsernameError()
            false
        }

    }

    private fun funSignInFlow(responsePOJO: EmailCheckResponse): Task<String> {
        return FirebaseAuth.getInstance().signInWithEmailAndPassword(responsePOJO.email!!, password.get()!!)
            .onSuccessTask {

                if (it != null && it.user != null) {
                    FirebaseFirestore.getInstance().collection("users").document(it.user!!.uid).get()

                } else {
                    throw IllegalStateException("Sign In failed !!!")

                }
            }
            .onSuccessTask {
                if (it == null) {
                    throw IllegalStateException("User does not exists")
                }

                if (!it.exists()) {
                    throw IllegalStateException("User does not exists")
                }
                val accountType =  it.getString("account_type")
                Log.e("TAG", "fetchUserProfile: $accountType", )
                if (accountType != null) {
                    SPManager(fragment.requireActivity()).storeLastLoginAccountType(accountType)
                }
                Log.e("snapshot", "funSignInFlow: ${it.toString()}", )

                return@onSuccessTask Tasks.call(Callable<String> { return@Callable it.id })
            }

    }

    private fun funLoginVerificationFlow(responsePOJO: EmailCheckResponse): Task<String> {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: throw IllegalStateException("User does not exists")

        val credential = EmailAuthProvider.getCredential(responsePOJO.email!!, password.get()!!)

        return FirebaseAuth.getInstance().currentUser!!.reauthenticate(credential)
            .onSuccessTask {
                return@onSuccessTask Tasks.call<String> { return@call currentUser.uid }
            }

    }

    fun doNothingOnClick() {

    }
}
