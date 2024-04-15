package io.stempedia.pictoblox.firebase.login

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.google.android.gms.tasks.Tasks
import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.GsonBuilder
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.databinding.FragForgetPwdBinding
import io.stempedia.pictoblox.util.PictoBloxAnalyticsEventLogger
import io.stempedia.pictoblox.util.PictobloxLogger

class ForgotPasswordFragment : Fragment() {

    private val vm = ForgotPasswordVM(this)
    private lateinit var mBinding: FragForgetPwdBinding

    override fun onAttach(context: Context) {
        super.onAttach(context)
        lifecycle.addObserver(vm)
    }

    override fun onDetach() {
        super.onDetach()
        lifecycle.removeObserver(vm)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.frag_forget_pwd, container, false)
        mBinding.data = vm

        return mBinding.root
    }

    fun showSuccessDialog() {
        if (isResumed) {
            activity?.also {
                AlertDialog.Builder(requireContext())
                    .setTitle("Success")
                    .setMessage("Please check your email")
                    .setNegativeButton("okay") { d, _ ->
                        d.dismiss()
                        (activity as LoginActivity).switchLoginFragment()
                    }
                    .create()
                    .show()

            }
        }
    }

    fun showErrorDialog(error: String) {
        if (isResumed) {
            activity?.also {
                AlertDialog.Builder(requireContext())
                    .setTitle("Error")
                    .setMessage(error)
                    .setNegativeButton("okay") { d, _ ->
                        d.dismiss()
                    }
                    .create()
                    .show()
            }
        }
    }

}

class ForgotPasswordVM(val fragment: ForgotPasswordFragment) : LifecycleObserver {
    val username = ObservableField<String>()
    val email = ObservableField<String>()
    val showProgress = ObservableBoolean()
    private var isSendSuccess = false

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun init() {
        if (isSendSuccess) {
            fragment.showSuccessDialog()
        }
    }

    fun onSubmitClicked() {
        if (fragment.isResumed) {
            fragment.activity?.also {
                val loginActivity = it as LoginActivity

                if (verifyDetails(loginActivity)) {
                    tryGeneratingResetPwdLink(loginActivity)
                }
            }
        }
    }

    private fun tryGeneratingResetPwdLink(activity: LoginActivity) {
        showProgress.set(true)
        val map = if (!TextUtils.isEmpty(username.get())) {
            mapOf("userName" to username.get())
        } else {
            mapOf("email" to email.get())
        }

        FirebaseFunctions.getInstance("asia-east2").getHttpsCallable("initiatePasswordResetSequence")
            .call(map)
            .onSuccessTask { result ->
                result?.data?.let { data ->
                    Tasks.call { GsonBuilder().create().fromJson<EmailCheckResponse>(data as String, EmailCheckResponse::class.java) }

                } ?: kotlin.run {
                    Tasks.call<EmailCheckResponse> { null }
                }
            }
            .addOnFailureListener {
                showProgress.set(false)
                if (fragment.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    PictobloxLogger.getInstance().logException(it)
                    Toast.makeText(activity, "Error: check connectivity", Toast.LENGTH_LONG).show()
                }
            }
            .addOnSuccessListener {
                showProgress.set(false)
                isSendSuccess = true
                if (fragment.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {

                    PictobloxLogger.getInstance().logd(it.status)

                    PictobloxLogger.getInstance().logd(it.error ?: "error null")

                    if (it.status == "success") {
                        PictoBloxAnalyticsEventLogger.getInstance().setPwdResetLinkSent()
                        fragment.showSuccessDialog()

                    } else {
                        fragment.showErrorDialog(it.error)
                    }
                }
            }
    }

    private fun verifyDetails(activity: LoginActivity): Boolean {
        if (TextUtils.isEmpty(username.get()) && TextUtils.isEmpty(email.get())) {
            Toast.makeText(activity, "Please enter username or email", Toast.LENGTH_LONG).show()
            return false
        }

        if (TextUtils.isEmpty(username.get()) && TextUtils.isEmpty(email.get())) {
            Toast.makeText(activity, "Please enter either username or email", Toast.LENGTH_LONG).show()
            return false
        }

        return true
    }
}