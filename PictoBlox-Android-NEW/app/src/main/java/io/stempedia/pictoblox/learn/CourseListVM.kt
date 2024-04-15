package io.stempedia.pictoblox.learn

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.firestore.DocumentSnapshot
import io.reactivex.observers.DisposableCompletableObserver
import io.reactivex.observers.DisposableObserver
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.account.AccountHelper
import io.stempedia.pictoblox.connectivity.CommManagerServiceImpl
import io.stempedia.pictoblox.firebase.COURSE_FLOW
import io.stempedia.pictoblox.firebase.CourseFlow
import io.stempedia.pictoblox.firebase.login.LoginActivity
import io.stempedia.pictoblox.learn.courseIntroConclusion.CourseIntroActivity
import io.stempedia.pictoblox.learn.courseIntroConclusion.INTRO
import io.stempedia.pictoblox.learn.courseIntroConclusion.INTRO_CONCLUSION_FLAG
import io.stempedia.pictoblox.learn.lessons.LessonsListActivity
import io.stempedia.pictoblox.profile.ProfileActivity
import io.stempedia.pictoblox.util.AccountHandlerCallback
import io.stempedia.pictoblox.util.AccountIconHandler
import io.stempedia.pictoblox.util.PictobloxLogger
import io.stempedia.pictoblox.util.SPManager

class CourseListVM(val activity: CourseListActivity) : AbsCourseVM(), AccountHandlerCallback {
    private val accountHelper = AccountHelper()
    val isRetrievingData = ObservableBoolean(false)
    val profileIcon = ObservableField<Bitmap>()
    private lateinit var courseManager: CourseManager
    lateinit var spManager: SPManager
    private val accountIconHandler = AccountIconHandler(activity, this)
    val showProfileIncompleteError = ObservableBoolean(false)

    override fun init(bundle: Bundle?, courseFlow: CourseFlow?, courseManager: CourseManager) {
        this.courseManager = courseManager
        spManager = SPManager(activity)
        populateCourses(courseManager)
    }

    override fun onServiceConnected(commManagerService: CommManagerServiceImpl) {

    }

    fun onCreate() {
        accountIconHandler.onCreate()
    }

    fun onResume() {
        accountIconHandler.onResume()
    }

    fun onHelpClicked() {

    }

    fun onAccountClicked() {
        accountIconHandler.onAccountClicked()
    }

    private fun populateCourses(courseManager: CourseManager) {
        val disposable = courseManager.getCourses2()
            .doOnSubscribe { isRetrievingData.set(true) }
            ?.map { it.map { vmList -> CourseListItemVM(vmList, this@CourseListVM) } }
            ?.subscribeWith(object : DisposableObserver<List<CourseListItemVM>>() {
                override fun onComplete() {

                }

                override fun onError(e: Throwable) {
                    e.printStackTrace()
                    activity.showError("error in fetching data")
                    isRetrievingData.set(false)
                }

                override fun onNext(t: List<CourseListItemVM>) {
                    isRetrievingData.set(false)
                    activity.populateCourse(t)
                }

            })


        disposable?.also {
            activity.add(it)
        }
    }

    //TODO This needs to be shifted to course manager
    fun onCourseClicked(courseFlow: CourseFlow) {
        if (accountHelper.isLoggedIn()) {

            accountHelper.getUserProgress2().get()

                .addOnSuccessListener {
                    if (it["course_enrolled.${courseFlow.course.id}"] != null) {

                        //Before launching into course we'll have to make sure that we have everything offline.
                        PictobloxLogger.getInstance().logd("Checking data")
                        verifyContentAndRedirectAccordingly(courseFlow, it)


                    } else {
                        startCourseDetailActivity(courseFlow)
                    }

                }.addOnFailureListener {
                    it.printStackTrace()
                    //TODO
                }

        } else {
            startCourseDetailActivity(courseFlow)

        }

    }

    private fun verifyContentAndRedirectAccordingly(courseFlow: CourseFlow, userProgressSnap: DocumentSnapshot) {
        val d = courseManager.checkIfOfflineAccessIsPossible(courseFlow, SPManager(activity))
            .subscribeWith(object : DisposableCompletableObserver() {
                override fun onComplete() {
                    if (userProgressSnap["course_enrolled.${courseFlow.course.id}.is_story_fully_shown"] as Boolean) {
                        startLessonListActivity(courseFlow)

                    } else {
                        startCourseIntroActivity(courseFlow)
                    }
                }

                override fun onError(e: Throwable) {
                    activity.startActivity(Intent(activity, CourseContentRetrieverActivity::class.java)
                        .apply {
                            putExtra(COURSE_FLOW, courseFlow)
                        })

                    activity.finish()

                }

            })
    }

    private fun startCourseDetailActivity(courseFlow: CourseFlow) {
        activity.startActivity(Intent(activity, CourseDetailActivity::class.java).apply {
            putExtra(COURSE_FLOW, courseFlow)
        })
    }

    private fun startCourseIntroActivity(courseFlow: CourseFlow) {
        val intent = Intent(activity, CourseIntroActivity::class.java).apply {
            putExtra(
                INTRO_CONCLUSION_FLAG,
                INTRO
            )
            putExtra(COURSE_FLOW, courseFlow)
        }

        activity.startActivity(intent)
    }

    private fun startLessonListActivity(courseFlow: CourseFlow) {
        activity.startActivity(Intent(activity, LessonsListActivity::class.java).apply {
            putExtra(COURSE_FLOW, courseFlow)
        })
    }

    fun showWIP() {
        activity.showWIP()
    }


    private fun setAccountIcon() {
        Glide.with(activity)
            .asBitmap()
            .apply(RequestOptions().circleCrop())
            .load(R.drawable.ic_account2)
            .into(object : CustomTarget<Bitmap>() {
                override fun onLoadCleared(placeholder: Drawable?) {

                }

                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    profileIcon.set(resource)
                }
            })
    }

    fun onWindowFocusChanged(hasFocus: Boolean) {
        accountIconHandler.onWindowFocusChanged(hasFocus)
    }

    override fun showSignUpIncompleteAnimation() {
        showProfileIncompleteError.set(true)
        activity.showLoginIncompleteDialog()
    }

    override fun dismissSignUpIncompleteAnimationIfRequired() {
        activity.hideLoginIncompleteDialog()
        showProfileIncompleteError.set(false)
    }

    override fun redirectToSignInProcess() {
        activity.startActivity(Intent(activity, LoginActivity::class.java))
    }

    override fun redirectToProfile() {
        activity.startActivity(Intent(activity, ProfileActivity::class.java))
    }

    override fun setAccountImage(bitmap: Bitmap) {
        profileIcon.set(bitmap)
    }
}