package io.stempedia.pictoblox.util

import io.reactivex.disposables.Disposable
import io.stempedia.pictoblox.uiUtils.AbsActivity

abstract class AbsViewModel(private val absActivity: AbsActivity) {

    protected fun add(disposable: Disposable){
        absActivity.add(disposable)
    }

}