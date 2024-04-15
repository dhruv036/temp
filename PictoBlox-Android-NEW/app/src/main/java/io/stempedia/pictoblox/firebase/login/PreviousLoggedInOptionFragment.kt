package io.stempedia.pictoblox.firebase.login

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.databinding.FragPreviousLoginsBinding
import kotlin.math.log

class PreviousLoggedInOptionFragment : Fragment() {
    private val vm = PreviousLoggedInOptionVM(this)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val mBinding = DataBindingUtil.inflate<FragPreviousLoginsBinding>(inflater, R.layout.frag_previous_logins, container, false)
        mBinding.data = vm
        return mBinding.root
    }
}

class PreviousLoggedInOptionVM(val fragment: PreviousLoggedInOptionFragment) {

    fun onNewCLicked() {
        if (fragment.isResumed) {
            fragment.activity?.also { activity ->
                val loginActivity = activity as LoginActivity
                loginActivity.switchLoginFragment()
            }
        }
    }

}

