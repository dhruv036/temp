package io.stempedia.pictoblox.firebase.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.subjects.PublishSubject
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.databinding.FragSelectStudentTeacherBinding

class SelectStudentTeacherFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val mBinding = DataBindingUtil.inflate<FragSelectStudentTeacherBinding>(inflater, R.layout.frag_select_student_teacher, container, false)
        val vm by activityViewModels<LoginActivityViewModel>()
        mBinding.data = vm

        return mBinding.root
    }
}

enum class AccountType(val value: String){
    TYPE_STUDENT("TYPE_STUDENT"),
    TYPE_TEACHER("TYPE_TEACHER")
}
