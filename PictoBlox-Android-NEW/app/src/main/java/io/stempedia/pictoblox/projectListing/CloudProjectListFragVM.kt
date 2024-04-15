package io.stempedia.pictoblox.projectListing

import androidx.databinding.ObservableInt
import java.io.File

class CloudProjectListFragVM(fragment: AbsProjectListFragment) : AbsProjectListFragmentVM(fragment) {
    override fun setEmptyDirMessage(emptyMessage: ObservableInt) {

    }

    override fun setLoadingMessage(emptyMessage: ObservableInt) {

    }

    override fun setErrorMessage(emptyMessage: ObservableInt, exception: Throwable) {

    }

    override fun applyFilter(file: File): Boolean {
        return false
    }

    override fun fetchData() {

    }

}