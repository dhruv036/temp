package io.stempedia.pictoblox.projectListing

import androidx.databinding.ObservableInt
import io.stempedia.pictoblox.R
import java.io.File

class RecentProjectListFragVM(fragment: RecentProjectFragment) : AbsProjectListFragmentVM(fragment) {
    override fun setEmptyDirMessage(emptyMessage: ObservableInt) {
        emptyMessage.set(R.string.project_list_no_recent_item)
    }

    override fun setLoadingMessage(emptyMessage: ObservableInt) {
        emptyMessage.set(R.string.project_list_loading_recent_project)
    }

    override fun setErrorMessage(emptyMessage: ObservableInt, exception: Throwable) {
        if (isListEmpty.get()) {
            emptyMessage.set(R.string.project_list_no_recent_project)

            //TODO some user friendly error message
        } else {
            fragment.showError(exception.message)
        }
    }

    private val secondsInThreeDays = 259_200_000//3 days in millis

    override fun applyFilter(file: File): Boolean {
        val cutOffForRecent = System.currentTimeMillis() - secondsInThreeDays

        return (file.lastModified() > cutOffForRecent)
    }


    // * FETCHING LOCAL FILE
    override fun fetchData() {
        onDataFetched(commManagerServiceImpl.communicationHandler.storageHandler.listLocalFiles())
    }

}