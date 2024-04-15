package io.stempedia.pictoblox.firebase.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import com.trello.rxlifecycle4.android.lifecycle.kotlin.bindUntilEvent
import io.reactivex.rxjava3.subjects.PublishSubject
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.databinding.FragAgeQueryBinding

class AgeQueryFragment : Fragment() {
    private lateinit var mBinding: FragAgeQueryBinding


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.frag_age_query, container, false)
        val vm by activityViewModels<LoginActivityViewModel>()
        mBinding.data = vm

        return mBinding.root
    }


}

