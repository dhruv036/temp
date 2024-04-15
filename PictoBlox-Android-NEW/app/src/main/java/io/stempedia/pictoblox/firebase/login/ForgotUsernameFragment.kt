package io.stempedia.pictoblox.firebase.login

import android.app.Application
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
import androidx.fragment.app.viewModels
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.GsonBuilder
import com.trello.rxlifecycle4.android.lifecycle.kotlin.bindToLifecycle
import com.trello.rxlifecycle4.android.lifecycle.kotlin.bindUntilEvent
import io.reactivex.rxjava3.subjects.PublishSubject
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.databinding.FragForgotUsernameBinding
import io.stempedia.pictoblox.util.PictobloxLogger

class ForgotUsernameFragment : Fragment() {
    private val animHelper = PictoBloxActionButtonAnimHelper(lifecycle)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val mBinding = DataBindingUtil.inflate<FragForgotUsernameBinding>(inflater, R.layout.frag_forgot_username, container, false)
        val viewModel by viewModels<ForgotUsernameVM>()

        mBinding.data = viewModel

        viewModel.outputShowToast
            .bindToLifecycle(this)
            .subscribe {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        viewModel.outputShowErrorAlert
            .bindToLifecycle(this)
            .subscribe {
                AlertDialog.Builder(requireContext())
                    .setTitle("Error")
                    .setMessage(it)
                    .setNeutralButton("okay") { d, _ -> d.dismiss() }
                    .create()
                    .show()
            }
        viewModel.outputSuccess
            .bindToLifecycle(this)
            .subscribe {
                AlertDialog.Builder(requireContext())
                    .setTitle("Success")
                    .setMessage("Please check your email to recover username(s).")
                    .setNeutralButton("okay") { d, _ ->
                        d.dismiss()
                        (activity as LoginActivity).switchLoginFragment()
                    }
                    .create()
                    .show()
            }

        viewModel.outputShowCreatingAccountAnimation
            .bindUntilEvent(this, Lifecycle.Event.ON_STOP)
            .subscribe { startAnimation ->
                if (startAnimation) {
                    animHelper.buttonToProgress(mBinding.textView57) {}

                } else {
                    animHelper.progressToButton(mBinding.textView57) {}
                }
            }


        return mBinding.root
    }
}

class ForgotUsernameVM(application: Application) : AndroidViewModel(application) {
    val email = ObservableField<String>()
    val showProgress = ObservableBoolean()

    val outputShowToast: PublishSubject<String> = PublishSubject.create<String>()
    val outputShowErrorAlert: PublishSubject<String> = PublishSubject.create<String>()
    val outputSuccess: PublishSubject<Unit> = PublishSubject.create<Unit>()
    val outputShowCreatingAccountAnimation: PublishSubject<Boolean> = PublishSubject.create<Boolean>()

    fun onSubmitClicked() {

        if (!TextUtils.isEmpty(email.get()) && android.util.Patterns.EMAIL_ADDRESS.matcher(email.get()!!).matches()) {
            sendUsernameEmail(email.get()!!)
        } else {
            outputShowToast.onNext("Please enter valid Email")
        }
    }

    private fun sendUsernameEmail(email: String) {
        outputShowCreatingAccountAnimation.onNext(true)
        showProgress.set(true)
        FirebaseFunctions.getInstance("asia-east2")
            .getHttpsCallable("sendUsernameRecoverEmail")
            .call(mapOf("email" to email))
            .addOnSuccessListener { httpsCallableResult ->
                outputShowCreatingAccountAnimation.onNext(false)
                showProgress.set(false)
                val res = GsonBuilder().create().fromJson(
                    httpsCallableResult.data as String,
                    SendEmailRes::class.java
                )

                if (res.status == "success") {
                    outputSuccess.onNext(Unit)

                } else {
                    outputShowErrorAlert.onNext(res.error ?: "Unknown server error! please try later.")
                }
            }.addOnFailureListener {
                showProgress.set(false)
                outputShowCreatingAccountAnimation.onNext(true)
                PictobloxLogger.getInstance().logException(it)
                outputShowErrorAlert.onNext("Please check your Internet connection")
            }
    }

}