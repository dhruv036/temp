package io.stempedia.pictoblox.projectListing

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import androidx.core.text.toSpannable
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableInt
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.observers.DisposableCompletableObserver
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.connectivity.CommManagerServiceImpl
import io.stempedia.pictoblox.util.PictobloxLogger
import java.io.*
import java.util.zip.ZipInputStream


abstract class AbsProjectListFragmentVM(val fragment: AbsProjectListFragment) {
    val isListEmpty = ObservableBoolean(true)
    val emptyMessage = ObservableInt(R.string.empty_message)
    val isLoadingData = ObservableBoolean(false)
    val isSelectionEnabled = ObservableBoolean(false)
    private lateinit var activity: ProjectListActivity
    protected lateinit var commManagerServiceImpl: CommManagerServiceImpl
    private var filesToDelete: List<File>? = null
    private var filesToShare: List<File>? = null
    private var isProcessingFiles = false


    abstract fun applyFilter(file: File): Boolean
    abstract fun fetchData()
    abstract fun setLoadingMessage(emptyMessage: ObservableInt)
    abstract fun setEmptyDirMessage(emptyMessage: ObservableInt)
    abstract fun setErrorMessage(emptyMessage: ObservableInt, exception: Throwable)


    fun onAttached(activity: ProjectListActivity, commManagerServiceImpl: CommManagerServiceImpl) {
        this.activity = activity
        this.commManagerServiceImpl = commManagerServiceImpl
    }

    fun setSelectionFlag() {
        isSelectionEnabled.set(true)
        activity.getVM().setSelectionFlag(isSelectionEnabled.get())
        //activity.restartOptionsMenu()
    }

    fun resetSelectors() {

    }

    //TODO cancel refresh if its already going
    fun refreshData() {
        if (!isLoadingData.get()) {
            fragment.clearList()
            isListEmpty.set(true)
            isLoadingData.set(true)
            setLoadingMessage(emptyMessage)
            fetchData()

        }
    }

    fun onDataFetched(list: Array<File>) {
        fragment.add(
            Observable.fromIterable(list.asList())
                .filter { queryFilter(activity.getVM().queryText.get() ?: "", it) }
                .filter { applyFilter(it) }
                .map { createItemVM(activity.getVM().queryText.get() ?: "", it) }
                .map { it.apply { thumb = getThumb(file) } }
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<ProjectListItemVM>() {
                    override fun onComplete() {
                        isLoadingData.set(false)
                        if (isListEmpty.get()) {
                            setEmptyDirMessage(emptyMessage)
                        }

                    }

                    override fun onNext(t: ProjectListItemVM) {
                        isListEmpty.set(false)
                        fragment.addItemInList(t)
                    }

                    override fun onError(e: Throwable) {
                        e.printStackTrace()
                        isLoadingData.set(false)
                        setErrorMessage(emptyMessage, e)
                    }
                })
        )
    }

    private fun createItemVM(query: String, file: File): ProjectListItemVM {
        val titleWithoutExt = file.nameWithoutExtension

        //If true then we are filtering
        val title: Spannable = if (query.isNotEmpty()) {

            var startIndex = 0
            var wordLength = 0
            val indices = mutableListOf<Int>()

            while (startIndex != -1) {
                startIndex = titleWithoutExt.indexOf(query, startIndex + wordLength, true)

                if (startIndex != -1) {
                    indices.add(startIndex)
                }
                wordLength = query.length
            }

            startIndex = 0

            val spannableStringBuilder = SpannableStringBuilder()

            while (startIndex < titleWithoutExt.length) {

                if (indices.contains(startIndex)) {
                    val spannableString = SpannableString(
                        titleWithoutExt.subSequence(
                            startIndex,
                            startIndex + wordLength
                        )
                    )
                    spannableString.setSpan(RelativeSizeSpan(1.2f), 0, spannableString.length, 0)
                    spannableString.setSpan(
                        ForegroundColorSpan(Color.parseColor("#531e73")),
                        0,
                        spannableString.length,
                        0
                    )
                    startIndex += wordLength

                    spannableStringBuilder.append(spannableString)

                } else {
                    spannableStringBuilder.append(titleWithoutExt[startIndex])
                    startIndex++
                }
            }

            spannableStringBuilder.toSpannable()

        } else {
            SpannableString(file.nameWithoutExtension).toSpannable()
        }

        //TODO make sure that commImpl is initialised
        return ProjectListItemVM(activity, fragment, this, title, file, commManagerServiceImpl)
    }

    private fun queryFilter(query: String, file: File): Boolean {
        return if (query.isBlank()) {
            true

        } else {
            file.nameWithoutExtension.contains(query, true)
        }
    }

    private fun getThumb(file: File): Bitmap? {
        val inputStream = ZipInputStream(file.inputStream())

        var zipEntry = inputStream.nextEntry

        while (zipEntry != null) {

            if (!zipEntry.isDirectory && zipEntry.name == "ProjectSnap.png") {

                val baos = ByteArrayOutputStream()
                val array = ByteArray(512)

                var byteRead = inputStream.read(array)

                while (byteRead != -1) {
                    baos.write(array, 0, byteRead)
                    byteRead = inputStream.read(array)
                }

                val bytes = baos.toByteArray()
                baos.close()

                return if (bytes.isNotEmpty()) {
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                } else {
                    null
                }

            }
            inputStream.closeEntry()

            zipEntry = inputStream.nextEntry
        }

        inputStream.close()

        return null
    }

    fun disableItemMSelection() {
        isSelectionEnabled.set(false)
        activity.getVM().setSelectionFlag(isSelectionEnabled.get())
        fragment.clearSelection()
        //activity.restartOptionsMenu()
    }

    fun deleteSelectedFiles() {
        if (isProcessingFiles) {
            return
        }

        filesToDelete = fragment.getSelectedFiles()

        filesToDelete?.also {

            if (it.isNotEmpty()) {
                fragment.askForConfirmation(it.size)

            } else {
                fragment.showNoFilesToDeleteMessage()
            }
        }
    }

    fun onDeleteConfirmed() {
        filesToDelete?.apply {
            fragment.add(
                createDeleteCompletable(this)
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(object : DisposableCompletableObserver() {
                        override fun onComplete() {
                            fragment.showDeleteSuccessMessage(size)
                            refreshData()
                            disableItemMSelection()
                        }

                        override fun onError(e: Throwable) {
                            fragment.showDeleteErrorMessage(size)
                            refreshData()
                        }

                    })
            )
        }
    }

    fun shareSelectedFiles() {

        if (isProcessingFiles) {
            return
        }

        filesToShare = fragment.getSelectedFiles()

        filesToShare?.apply {
            if (size > 0) {
                //fragment.shareMultipleFiles2(activity, this)
                if (isMiUi()) {
                    fragment.shareMultipleFiles2(activity, this)

                } else {
                    fragment.shareMultipleFiles(activity, this)
                }

            } else {
                activity.showNoFilesToShareMessage()
            }
        }
    }

    open fun isMiUi(): Boolean {
        return !TextUtils.isEmpty(getSystemProperty("ro.miui.ui.version.name"))
    }

    open fun getSystemProperty(propName: String): String? {
        val line: String
        var input: BufferedReader? = null
        try {
            val p = Runtime.getRuntime().exec("getprop $propName")
            input = BufferedReader(InputStreamReader(p.inputStream), 1024)
            line = input.readLine()
            input.close()
        } catch (ex: IOException) {
            return null
        } finally {
            if (input != null) {
                try {
                    input.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return line
    }


    private fun createDeleteCompletable(list: List<File>): Completable {
        return Completable.create {
            try {
                list.forEach { file ->
                    run {
                        file.delete()
                    }
                }

                it.onComplete()

            } catch (e: Exception) {
                PictobloxLogger.getInstance().logException(e)
                it.onError(e)
            }
        }
    }

}