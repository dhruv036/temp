package io.stempedia.pictoblox.web

import android.app.Activity
import android.content.Intent
import android.text.Editable
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.firebase.analytics.FirebaseAnalytics
import io.reactivex.observers.DisposableCompletableObserver
import io.stempedia.pictoblox.BuildConfig
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.connectivity.CommManagerServiceImpl
import io.stempedia.pictoblox.firebase.login.LoginActivity
import io.stempedia.pictoblox.util.*

class SaveProjectViewModel(
    val activity: PictoBloxWebActivity,
    val commManagerServiceImpl: CommManagerServiceImpl,
    val name: String,
    val actionButton: ObservableField<String>,
    val activityViewModel: PictoBloxWebViewModelM2,
    val exitAfterSave: Boolean,
    val redirectToSignUp: Boolean

) : AbsViewModel(activity) {
    val projectName = ObservableField(name)
    val showError = ObservableBoolean(false)
    val errorMsg = ObservableField<String>(activity.getString(R.string.empty_project_error))
    var isOverridePrompted = false
    val isSavingInProgress = ObservableBoolean()
    val showExit = ObservableBoolean(exitAfterSave)
    val spManager = SPManager(activity)
    var reviewInfo: ReviewInfo? = null
    lateinit var reviewManager: ReviewManager

    init {
        if (!exitAfterSave) {
            if (!spManager.isFeedbackFormShownForThisVersion && spManager.saveFileCounter > 3) {
                reviewManager = ReviewManagerFactory.create(activity)
                reviewManager.requestReviewFlow()
                    .addOnSuccessListener {
                        PictobloxLogger.getInstance().logd("Review info object acquired")
                        this.reviewInfo = it
                    }

            }
        }
    }


    fun afterTextChanged(s: Editable) {
        showError.set(false)

        if (isOverridePrompted) {
            isOverridePrompted = false
            actionButton.set(activity.getString(R.string.save))
        }
        //We are only changing to override once user puts in a name of different project
        /*if (s.toString().trim().toLowerCase() == projectName.get()) {
            actionButton.set(R.string.override)
        } else {

            actionButton.set(R.string.save)
        }*/
    }

    fun onIgnoreClick() {
        //Do nothing. This required so when user presses in the center white portion of the save dialog it does not close.
    }

    fun onExternalPlaneClicked() {
        if (!isSavingInProgress.get()) {
            activity.hideKeyboard()
            activityViewModel.dismissSaveDialog()
        }
    }

    fun onSaveFinished() {
        PictoBloxAnalyticsEventLogger.getInstance().setFileSaved(BuildConfig.PICTOBLOX_BUILD_VERSION)

        if (isSavingInProgress.get()) {
            isSavingInProgress.set(false)
            activity.hideSaveProgress()
        }

        activityViewModel.dismissSaveDialog()

        if (exitAfterSave) {

            if (redirectToSignUp) {
                activity.startActivity(Intent(activity, LoginActivity::class.java))
            } else {
                activity.finish()

            }

        } else {
            if (!spManager.isFeedbackFormShownForThisVersion && spManager.saveFileCounter > 3) {
                reviewInfo?.also {
                    val flow = reviewManager.launchReviewFlow(activity, it)
                    flow.addOnCompleteListener { _ ->
                        // The flow has finished. The API does not indicate whether the user
                        // reviewed or not, or even whether the review dialog was shown. Thus, no
                        // matter the result, we continue our app flow.
                        spManager.saveFileCounter = 0
                        SPManager(activity).isFeedbackFormShownForThisVersion = true
                        //activityViewModel.showFeedbackDialog()
                    }
                }
            }
        }

        if (!spManager.isFeedbackFormShownForThisVersion) {
            spManager.saveFileCounter++
        }
    }

    fun onExitClicked() {
        if (isSavingInProgress.get()) {
            Toast.makeText(activity, "Saving already in process!", Toast.LENGTH_LONG).show()
        } else {
            activity.finish()
        }
    }

    /** on SAVE button clicked
     * */
    fun onSaveClicked() {
        Log.e("TAG", "onSaveClicked: ", )
        showError.set(false)
        val fileName: String = projectName.get()?.let {
            when {
                it.isEmpty() -> ""
                it.trim().toLowerCase().endsWith(".sb3") -> it.trim()
                else -> "${it.trim()}.sb3"
            }
        } ?: run {
            ""
        }

        if (TextUtils.isEmpty(fileName)) {
            Log.e("TAG", "onSaveClicked: isEmpty ", )

            showError.set(true)
            //errorMsg.set(R.string.empty_project_error)
            //activity.showEmptyProjectNameError()
            return
        }


        if (name == projectName.get()) {
            commManagerServiceImpl.communicationHandler.saveCurrentWork(fileName)
            //activityViewModel.dismissSaveDialog()
            activity.showSaveProgress()
            isSavingInProgress.set(true)

        } else {

            add(
                commManagerServiceImpl.communicationHandler.storageHandler.isFileExists(fileName)
                    .subscribeWith(object : DisposableCompletableObserver() {
                        override fun onComplete() {
                            //if file exist, we show error
                            showError.set(true)
                            errorMsg.set(activity.getString(R.string.save_project_unique_error))

                            isOverridePrompted = if (isOverridePrompted) {
                                commManagerServiceImpl.communicationHandler.saveCurrentWork(fileName)
                                //activityViewModel.dismissSaveDialog()
                                activity.showSaveProgress()
                                isSavingInProgress.set(true)
                                false

                            } else {
                                actionButton.set(activity.getString(R.string.override))

                                true
                            }


                        }

                        override fun onError(e: Throwable) { //if no file found then we'll move forward with saving
                            commManagerServiceImpl.communicationHandler.saveCurrentWork(fileName)
//                            activityViewModel.dismissSaveDialog()
                            activity.showSaveProgress()
                            isSavingInProgress.set(true)
                        }

                    })
            )
        }
    }

    private fun saveFile() {
        val spManager = SPManager(activity)

        if (spManager.saveFileCounter > 2) {


        } else {
            spManager.saveFileCounter++
        }

    }

    private fun hideKeyboard(view: View) {
        //mBinding.editText7.inputType = InputType.TYPE_NULL
        activity?.also {
            val imm: InputMethodManager = it.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}