package io.stempedia.pictoblox.learn

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableDouble
import androidx.databinding.ObservableField
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.storage.StorageReference
import io.reactivex.observers.DisposableCompletableObserver
import io.reactivex.observers.DisposableSingleObserver
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
import io.stempedia.pictoblox.util.SPManager


class CourseDetailVM(val activity: CourseDetailActivity) : AbsCourseVM(), AccountHandlerCallback {
    val price = ObservableDouble()
    private val accountHelper = AccountHelper()
    private var courseFlow: CourseFlow? = null
    private lateinit var courseManager: CourseManager
    val title = ObservableField<String>("")
    val subTitle = ObservableField<String>("")
    val difficulty = ObservableField<String>("")
    val noOfLessons = ObservableField<String>("")
    val duration = ObservableField<String>("")
    val thumbReference = ObservableField<StorageReference>()
    val contentText = ObservableField<String>()
    val buttonText = ObservableField<String>("")
    val isPaid = ObservableBoolean()
    val profileIcon = ObservableField<Bitmap>()
    val isCourseRetrievalProcessOngoing = ObservableBoolean(false)
    private var youtubeLink = ""
    private lateinit var spManager: SPManager
    private val accountIconHandler = AccountIconHandler(activity, this)
    val showProfileIncompleteError = ObservableBoolean(false)

    private var story: CourseManager.CourseDetailStory? = null

    override fun init(bundle: Bundle?, courseFlow: CourseFlow?, courseManager: CourseManager) {
        this.courseManager = courseManager
        this.courseFlow = courseFlow

        spManager = SPManager(activity)

        courseFlow?.course?.also {
            fetchCourseDetailFromCache(courseFlow, courseManager)

        } ?: kotlin.run {
            activity.showError("")
        }
    }

    override fun onServiceConnected(commManagerService: CommManagerServiceImpl) {

    }

    fun onCreate() {
        accountIconHandler.onCreate()
    }

    fun onResume() {
        accountIconHandler.onResume()

        if (accountHelper.isLoggedIn()) {
            buttonText.set("Enroll")

        } else {
            buttonText.set("Sign in to enroll")

        }
    }

    fun onHelpClicked() {

    }

    fun onAccountClicked() {
        accountIconHandler.onAccountClicked()
    }

    private fun fetchCourseDetailFromCache(courseFlow: CourseFlow, courseManager: CourseManager) {
        activity.add(
            courseManager.getCourseDetail(courseFlow)
                .doAfterSuccess {
                    story = it
                }
                .subscribeWith(object : DisposableSingleObserver<CourseManager.CourseDetailStory>() {
                    override fun onSuccess(t: CourseManager.CourseDetailStory) {
                        title.set(t.title)
                        subTitle.set(t.subTitle)
                        thumbReference.set(t.thumbReference)
                        contentText.set(t.description.replace("\\n", "\n"))
                        difficulty.set(t.difficulty)
                        noOfLessons.set("Lessons:  ${t.noOfLessons}")
                        duration.set(t.duration)
                        youtubeLink = t.videoLink
                    }

                    override fun onError(e: Throwable) {
                        activity.showError(e.message ?: "Unknown error")
                    }

                })
        )
    }


    fun onEnrollPressed() {
        if (accountHelper.isLoggedIn()) {
            courseFlow?.also { courseFlow -> updateUserEnrollment2(courseFlow) }

        } else {
            isCourseRetrievalProcessOngoing.set(true)
            activity.startActivityForResult(Intent(activity, LoginActivity::class.java), 201)

        }
    }

    fun onPlayVideoPressed() {
        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(youtubeLink)))

    }


    private fun updateUserEnrollment2(courseFlow: CourseFlow) {
        activity.add(
            courseManager.getFirstLessonOfCourse(courseFlow)
                .andThen(courseManager.enrollUserInACourse(story?.noOfLessons ?: 0, courseFlow))
                .doOnSubscribe {
                    isCourseRetrievalProcessOngoing.set(true)
                }
                .subscribeWith(object : DisposableCompletableObserver() {
                    override fun onComplete() {
                        isCourseRetrievalProcessOngoing.set(false)
                        activity.startActivity(Intent(activity, CourseContentRetrieverActivity::class.java)
                            .apply {
                                putExtra(COURSE_FLOW, courseFlow)
                            })

                        activity.finish()
                    }

                    override fun onError(e: Throwable) {
                        isCourseRetrievalProcessOngoing.set(false)
                        activity.showError(e.message ?: "Unknown error")
                    }

                }
                )
        )

    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 201) {
            if (resultCode == Activity.RESULT_OK) {
                buttonText.set("Enroll")

                //checke here if user has already subscribed to this course

                courseFlow?.also { courseFlow ->
                    checkUserEnrollment(courseFlow)
                }

            } else {
                isCourseRetrievalProcessOngoing.set(false)
                Toast.makeText(activity, "Sign In cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkUserEnrollment(courseFlow: CourseFlow) {
        accountHelper.getUserProgress2().get()
            .addOnSuccessListener {
                if (it["course_enrolled.${courseFlow.course.id}"] != null) {
                    verifyContentAndRedirectAccordingly(courseFlow, it)

                } else {
                    onEnrollPressed()
                }

            }.addOnFailureListener {
                //TODO
            }

    }

    private fun verifyContentAndRedirectAccordingly(courseFlow: CourseFlow, userProgressSnap: DocumentSnapshot) {
        activity.add(
            courseManager.checkIfOfflineAccessIsPossible(courseFlow, SPManager(activity)).subscribeWith(object : DisposableCompletableObserver() {
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
        )
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
        activity.finish()
    }

    private fun startLessonListActivity(courseFlow: CourseFlow) {
        activity.startActivity(Intent(activity, LessonsListActivity::class.java).apply {
            putExtra(COURSE_FLOW, courseFlow)
        })
        activity.finish()
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