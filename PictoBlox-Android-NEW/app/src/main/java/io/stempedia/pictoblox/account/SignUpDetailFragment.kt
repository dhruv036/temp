package io.stempedia.pictoblox.account


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.databinding.FragmentSignUpDetailBinding

const val NEW_USER_TAG = "new"

class SignUpDetailFragment : DialogFragment() {
    private val signUpFragVM = SignUpFragVM(this)
    private lateinit var mBinding: FragmentSignUpDetailBinding


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_sign_up_detail, container, false)
        mBinding.data = signUpFragVM

        return mBinding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val isNewUser = arguments?.getBoolean(NEW_USER_TAG) ?: false
        signUpFragVM.onAttach(isNewUser)

    }

    override fun onDetach() {
        super.onDetach()
        signUpFragVM.onDetach()
    }


}
