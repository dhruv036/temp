package io.stempedia.pictoblox.projectListing

import android.graphics.Bitmap
import android.text.Spannable
import android.util.Log
import android.view.View
import androidx.databinding.ObservableBoolean
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.observers.DisposableCompletableObserver
import io.reactivex.schedulers.Schedulers
import io.stempedia.pictoblox.connectivity.CommManagerServiceImpl
import java.io.File

class ProjectListItemVM(
    val activity: ProjectListActivity,
    val fragment: AbsProjectListFragment,
    val fragVM: AbsProjectListFragmentVM,
    val title: Spannable,
    val file: File,
    var commManagerService: CommManagerServiceImpl
) {

    //TODO null and observable?
    var thumb: Bitmap? = null
    val isSelected = ObservableBoolean(false)
    var viewHolder: RecyclerView.ViewHolder? = null


    fun onLongClicked(view: View): Boolean {
        fragVM.setSelectionFlag()
        selectItem()

        return true
    }

    fun onItemClick() {
        commManagerService.apply {

            communicationHandler.loadInternalProject(file)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableCompletableObserver() {

                    override fun onComplete() {
                        activity.startPictobloxWeb()
                    }

                    override fun onError(e: Throwable) {
                        fragment.showError(e.message)
                    }

                })
        }
    }

    fun selectItem() {
        viewHolder?.apply {
            isSelected.set(!isSelected.get())
            fragment.refreshItem(adapterPosition)
            fragment.performCheckSequence()
        }
    }


}