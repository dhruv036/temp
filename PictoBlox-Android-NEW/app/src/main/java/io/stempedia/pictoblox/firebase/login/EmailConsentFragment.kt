package io.stempedia.pictoblox.firebase.login

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.GsonBuilder
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.databinding.FragEmailConsentBinding
import io.stempedia.pictoblox.util.PictobloxLogger
import io.stempedia.pictoblox.util.SPManager

const val IS_MINOR = "is_minor"

class EmailConsentFragment : Fragment() {
    private val vm = GuardianEmailForConsentVM(this, lifecycle)
    private lateinit var mBinding: FragEmailConsentBinding
    private var popupWindow: PopupWindow? = null
    val handler = Handler()
    private val animHelper = PictoBloxActionButtonAnimHelper(lifecycle)
    private lateinit var spManager: SPManager

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
        mBinding = DataBindingUtil.inflate(inflater, R.layout.frag_email_consent, container, false)
        spManager = SPManager(requireContext())
        mBinding.data = vm
        mBinding.editText7.setOnEditorActionListener { _, actionId, event -> vm.onEditorAction(actionId, event) }

        mBinding.textView58.movementMethod = LinkMovementMethod.getInstance()


        val builder = SpannableStringBuilder()

        builder.append("I have read and agree to the ")

        val text3 = SpannableString("Terms and privacy policy")

        text3.setSpan(
            vm.clickSpanListener,
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

        mBinding.textView58.text = builder

        vm.setArgument(arguments)


        return mBinding.root
    }

    fun hideKeyboard() {
        //mBinding.editText7.inputType = InputType.TYPE_NULL
        activity?.also {
            val imm: InputMethodManager = it.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(mBinding.editText7.windowToken, 0)
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

    fun showEmailError1() {
        showPopWindowError(mBinding.editText7, "Email cannot be empty")
    }

    fun showEmailError2() {
        showPopWindowError(mBinding.editText7, "Email is invalid")
    }

    fun showProgress() {
        animHelper.buttonToProgress(mBinding.textView53) { }
    }

    fun hideProgress() {
        animHelper.progressToButton(mBinding.textView53) { }
    }

    fun getSpManager() = spManager

    fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }
}

class GuardianEmailForConsentVM(val fragment: EmailConsentFragment, val lifecycle: Lifecycle) : LifecycleObserver {
    val showProgress = ObservableBoolean(false)
    val isWaitingForGuardiansConsent = ObservableBoolean(false)
    val shouldShowResendEmail = ObservableBoolean(false)
    val emailToVerify = ObservableField<String>()
    private val handler = Handler()
    private val oneMinInMillis = 60_000
    private var listenerRegistration: ListenerRegistration? = null

    //private lateinit var spManager: SPManager
    val isTermAccepted = ObservableBoolean(false)

    private var isMinor = false
    val emailRequestText = ObservableField<String>()
    val emailRequestHint = ObservableField<String>()

    private var arguments: Bundle? = null

    fun setArgument(arguments: Bundle?) {
        this.arguments = arguments
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun init() {
        isMinor = arguments?.getBoolean(IS_MINOR) ?: false

        if (isMinor) {
            emailRequestText.set("We need your Guardian's consent")
            emailRequestHint.set("Guardian's Email")

        } else {
            emailRequestText.set("Please verify your email address")
            emailRequestHint.set("Your Email")
        }

        if (!TextUtils.isEmpty(fragment.getSpManager().guardianVerificationId)) {
            showProgress.set(true)

            setResendTimer()

            isWaitingForGuardiansConsent.set(true)

            emailToVerify.set(fragment.getSpManager().emailToBeVerified)

            val ref = FirebaseFirestore.getInstance()
                .collection("parent_verification_list")
                .document(fragment.getSpManager().guardianVerificationId)

            setSnapshotListener(ref)

        }

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onDispose() {
        handler.removeCallbacks(resendTimerRunnable)
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    fun onEditorAction(actionId: Int, event: KeyEvent?): Boolean {
        if (actionId == EditorInfo.IME_ACTION_SEND) {
            emailConfirmClicked()
            return true
        }
        return false
    }

    fun onResendClicked() {
        if (fragment.isResumed) {
            listenerRegistration?.remove()
            fragment.showToast("Sending verification email")
            initiateGuardianVerificationProcess(fragment.getSpManager().emailToBeVerified, true)
        }
    }

    fun onEditEmailClicked() {
        if (fragment.isResumed) {
            fragment.activity?.also {
                isWaitingForGuardiansConsent.set(false)
                shouldShowResendEmail.set(false)
            }
        }
    }

    fun emailConfirmClicked() {
        if (fragment.isResumed) {
            if (isEmailValid() && checkTermAccepted() && !showProgress.get()) {
                //initiateGuardianVerificationProcess(guardianEmail.get()!!.trim(), loginActivity)
                checkEmailAvailable(emailToVerify.get()!!.trim())
            }

        }
    }

    private fun checkEmailAvailable(email: String) {
        if (showProgress.get()) {
            fragment.showToast("processing, please wait")
            return
        }

        showProgress.set(true)
        isWaitingForGuardiansConsent.set(false)
        shouldShowResendEmail.set(false)
        fragment.showProgress()

        FirebaseFunctions.getInstance("asia-east2").getHttpsCallable("checkEmail3")
            .call(mapOf("email" to email))
            .onSuccessTask { result ->
                result?.data?.let { data ->
                    Tasks.call { GsonBuilder().create().fromJson<EmailCheckResponse>(data as String, EmailCheckResponse::class.java) }

                } ?: kotlin.run {
                    Tasks.call<EmailCheckResponse> { null }
                }
            }
            .addOnFailureListener {
                showProgress.set(false)
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    fragment.hideProgress()
                    PictobloxLogger.getInstance().logException(it)
                    fragment.showToast("Error: please check your network.")
                }
            }
            .addOnSuccessListener {
                if (it.status == "success") {
                    initiateGuardianVerificationProcess(email, false)

                } else {
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        showProgress.set(false)
                        fragment.showToast("Email is not available")
                        fragment.hideProgress()
                    }
                }
            }
    }

    private fun initiateGuardianVerificationProcess(email: String, isResendRequest: Boolean) {
        val docReference = FirebaseFirestore.getInstance().collection("parent_verification_list").document()

        docReference.set(
            mapOf(
                "email" to email,
                "verified" to false,
                "minor" to isMinor
            )
        )
            .addOnSuccessListener {
                PictobloxLogger.getInstance().logd("Email verification doc id ${docReference.id}")
                fragment.getSpManager().guardianVerificationId = docReference.id
                fragment.getSpManager().emailToBeVerified = email
                fragment.getSpManager().isMinorForLogin = isMinor
                fragment.getSpManager().isSignUpIncomplete = true
                fragment.getSpManager().resendTimeStamp = System.currentTimeMillis() + oneMinInMillis

                showProgress.set(false)
                isWaitingForGuardiansConsent.set(true)

                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    fragment.hideProgress()
                    setResendTimer()
                    setSnapshotListener(docReference)

                    if (isResendRequest) {
                        fragment.activity?.also {
                            Toast.makeText(it, "Email resent", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .addOnFailureListener {
                showProgress.set(false)
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    fragment.hideProgress()
                    fragment.showToast(it.message ?: "Error while processing your request, please try later")
                }
            }


        fragment.hideKeyboard()
    }

    private fun setResendTimer() {
        if (fragment.getSpManager().resendTimeStamp > System.currentTimeMillis()) {
            val time = fragment.getSpManager().resendTimeStamp - System.currentTimeMillis()

            handler.postDelayed(resendTimerRunnable, time)

        } else {
            resendTimerRunnable.run()
        }


    }

    private fun setSnapshotListener(documentReference: DocumentReference) {

        //
        listenerRegistration?.remove()

        listenerRegistration = documentReference.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            documentSnapshot?.also {

                if (firebaseFirestoreException != null) {
                    PictobloxLogger.getInstance().logException(firebaseFirestoreException)
                    return@also
                }

                // Progress will be showing in case of app restart
                showProgress.set(false)

                val isVerified = it.get("verified") as Boolean
                val email = it.get("email") as String

                if (isVerified) {
                    fragment.getSpManager().guardianVerificationId = ""
                    logInAnonymously(email)
                } else {
                    //TODO error in verification. Handle error
                }
            }
        }
    }

    private fun logInAnonymously(email: String) {
        FirebaseAuth.getInstance().signInAnonymously()
            .addOnCompleteListener { task ->
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {

                    if (task.isSuccessful) {

                        val activity = fragment.activity as LoginActivity

                        if (isMinor) {
                            //activity.switchToMinorDetailScreen(email)

                        } else {
                            //activity.switchToAdultDetailScreen(email)

                        }
                    }
                }

                // ...
            }
    }

    private fun isEmailValid(): Boolean {

        return if (!TextUtils.isEmpty(emailToVerify.get())) {
            if (android.util.Patterns.EMAIL_ADDRESS.matcher(emailToVerify.get()!!).matches()) {
                true

            } else {
                fragment.showEmailError2()
                false
            }

        } else {
            fragment.showEmailError1()

            false
        }
    }

    private fun checkTermAccepted(): Boolean {
        return if (isTermAccepted.get()) {
            true

        } else {
            fragment.showToast("Please accept Terms and Privacy Policy")
            false
        }
    }

    private val resendTimerRunnable = Runnable {
        shouldShowResendEmail.set(true)
    }

    val clickSpanListener = object : ClickableSpan() {
        override fun onClick(widget: View) {
            val stemLinkIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://thestempedia.com/privacy-policy/"))
            fragment.startActivity(stemLinkIntent)
        }

    }
}

class EmailCheckResponse(val status: String, val email: String?, val isAvailable: Boolean, val error: String)