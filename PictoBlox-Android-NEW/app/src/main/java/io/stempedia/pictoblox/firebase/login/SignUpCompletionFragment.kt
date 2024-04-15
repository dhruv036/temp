package io.stempedia.pictoblox.firebase.login

import android.app.Application
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import com.trello.rxlifecycle4.android.lifecycle.kotlin.bindUntilEvent
import io.reactivex.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.databinding.FragSignUpCompletionBinding
import java.util.concurrent.TimeUnit

class SignUpCompletionFragment : Fragment() {
    private val vm by viewModels<SignUpCompletionVM>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm.inputArguments.onNext(arguments!!)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val mBinding = DataBindingUtil.inflate<FragSignUpCompletionBinding>(inflater, R.layout.frag_sign_up_completion, container, false)
        mBinding.data = vm


        vm.outputFinishAfterDelay
            .bindUntilEvent(this, Lifecycle.Event.ON_STOP)
            .subscribe {
                val activityVm by activityViewModels<LoginActivityViewModel>()
                activityVm.wrapUp.onNext(Unit)
            }

        return mBinding.root
    }
}

class SignUpCompletionVM(application: Application) : AndroidViewModel(application) {

    private val showProgress = ObservableBoolean()
    val info = ObservableField<String>()

    val inputArguments: PublishSubject<Bundle> = PublishSubject.create<Bundle>()

    val outputFinishAfterDelay: Observable<Long> = Observable.timer(5000, TimeUnit.MILLISECONDS)

    init {
        inputArguments
            .subscribe {
                val credit = it.getLong("credit")
                info.set(application.getString(R.string.sign_up_completion_message, credit.toString()))
            }
    }
}

