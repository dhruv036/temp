package io.stempedia.pictoblox.projectListing

import android.content.Context
import androidx.databinding.ObservableInt
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.connectivity.StorageType
import io.stempedia.pictoblox.util.SPManager
import java.io.File
import java.io.FileOutputStream

class LocalProjectListFragVM(fragment: AbsProjectListFragment) : AbsProjectListFragmentVM(fragment) {
    override fun setEmptyDirMessage(emptyMessage: ObservableInt) {
        emptyMessage.set(R.string.no_saved_file)
    }

    override fun setLoadingMessage(emptyMessage: ObservableInt) {
        emptyMessage.set(R.string.project_list_loading_all_project)
    }

    override fun setErrorMessage(emptyMessage: ObservableInt, exception: Throwable) {
        if (isListEmpty.get()) {
            emptyMessage.set(R.string.project_list_no_local_project)

            //TODO some user friendly error message
        } else {
            fragment.showError(exception.message)
        }
    }


    override fun applyFilter(file: File): Boolean {
        return true
    }

    override fun fetchData() {



        val spManager = SPManager(fragment.requireContext())

        if (!spManager.isEssentialFileCopied) {
            copyDefaultShowcaseFile(fragment.requireContext())
            spManager.isEssentialFileCopied = true
        }

        val localFiles = commManagerServiceImpl.communicationHandler.storageHandler.listLocalFiles()
        /*val mutableList = mutableListOf<File>()

        val file = File(commManagerServiceImpl.communicationHandler.storageHandler.getEssentialFilesDir(), "Tobi Walking.sb3")

        if (file.exists()) {
            mutableList.add(file)
        }

        mutableList.addAll(localFiles)*/

        onDataFetched(localFiles)
    }

    private fun copyDefaultShowcaseFile(context: Context) {

/*
        val inputStream = context.assets.open("essential_files/Tobi Walking.sb3")

        val file = File(commManagerServiceImpl.communicationHandler.storageHandler.getEssentialFilesDir(), "Tobi Walking.sb3")

        val baos = FileOutputStream(file)

        val array = ByteArray(512)

        var byteRead = inputStream.read(array)

        while (byteRead != -1) {
            baos.write(array, 0, byteRead)
            byteRead = inputStream.read(array)
        }

        baos.close()*/

        val inputStream = context.assets.open("essential_files/Tobi Walking.sb3")
        commManagerServiceImpl.communicationHandler
            .storageHandler
            .saveProject(inputStream.readBytes(), "Tobi Walking.sb3", StorageType.INTERNAL)
            .subscribe()


    }
}