package io.stempedia.pictoblox.firebase.login

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.fragment.app.Fragment
import com.google.firebase.FirebaseApiNotAvailableException
import com.google.firebase.auth.FirebaseAuth
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.databinding.FragAdultSignUpBinding
import io.stempedia.pictoblox.util.PictobloxLogger

class AdultSignUpFragment : Fragment() {
    private val vm = AdultSignUpVM(this)
    private lateinit var mBinding: FragAdultSignUpBinding


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate<FragAdultSignUpBinding>(inflater, R.layout.frag_adult_sign_up, container, false)
        mBinding.data = vm
        mBinding.editText10.setOnEditorActionListener { _, actionId, event -> vm.onEditorAction(actionId, event) }
        return mBinding.root
    }


    fun hideKeyboard() {
        //mBinding.editText7.inputType = InputType.TYPE_NULL
        activity?.also {
            val imm: InputMethodManager = it.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(mBinding.editText10.windowToken, 0)
        }
    }
}

class AdultSignUpVM(val fragment: AdultSignUpFragment) {
    val email = ObservableField<String>()
    val password = ObservableField<String>()
    val isSigningUp = ObservableBoolean()


    fun onEditorAction(actionId: Int, event: KeyEvent?): Boolean {
        if (actionId == EditorInfo.IME_ACTION_GO) {
            signUserUp()

            return true
        }
        return false
    }


    fun signUserUp() {

        if (fragment.isResumed) {
            fragment.activity?.also {
                val loginActivity = it as LoginActivity
                if (verifyUserEntry(loginActivity)) {
                    isSigningUp.set(true)
                    fragment.hideKeyboard()
                    FirebaseAuth.getInstance().createUserWithEmailAndPassword(email.get()!!, password.get()!!)
                        .addOnSuccessListener {
                            //loginActivity.switchToAdultDetailScreen(email.get()!!)
                            isSigningUp.set(false)
                        }

                        .addOnFailureListener { e ->
                            if (e  is FirebaseApiNotAvailableException) {
                                Toast.makeText(loginActivity, "Login service is not supported on this device", Toast.LENGTH_SHORT).show()

                            } else {
                                Toast.makeText(loginActivity, "${e.message}", Toast.LENGTH_SHORT).show()
                            }

                            isSigningUp.set(false)
                            PictobloxLogger.getInstance().logException(e)
                        }
                }
            }
        }
    }

    private fun verifyUserEntry(context: Context): Boolean {

        if (!TextUtils.isEmpty(email.get())) {
            if (android.util.Patterns.EMAIL_ADDRESS.matcher(email.get()!!).matches()) {

                return if (!TextUtils.isEmpty(password.get())) {

                    if (password.get()!!.length >= 6) {
                        true

                    } else {
                        Toast.makeText(context, "Password needs to be at least 6 character long", Toast.LENGTH_LONG).show()
                        false
                    }

                } else {
                    Toast.makeText(context, "Password cannot be empty", Toast.LENGTH_LONG).show()
                    false
                }

            } else {
                Toast.makeText(context, "Email is invalid", Toast.LENGTH_LONG).show()
                return false
            }

        } else {
            Toast.makeText(context, "Email cannot be empty", Toast.LENGTH_LONG).show()
            return false
        }

    }

}