package io.stempedia.pictoblox.learn

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import io.reactivex.observers.DisposableObserver
import io.stempedia.pictoblox.account.AccountHelper
import io.stempedia.pictoblox.connectivity.CommManagerServiceImpl
import io.stempedia.pictoblox.firebase.COURSE_FLOW
import io.stempedia.pictoblox.firebase.CourseFlow
import io.stempedia.pictoblox.learn.courseIntroConclusion.CourseIntroActivity
import io.stempedia.pictoblox.learn.courseIntroConclusion.INTRO
import io.stempedia.pictoblox.learn.courseIntroConclusion.INTRO_CONCLUSION_FLAG
import io.stempedia.pictoblox.learn.lessons.LessonsListActivity
import io.stempedia.pictoblox.util.SPManager

class CourseContentRetrieverVM(val activity: CourseContentRetrieverActivity) : AbsCourseVM() {
    private val accountHelper = AccountHelper()
    val processMessage = ObservableField<String>("Retrieving course data...")
    val processDone = ObservableInt(0)
    val errorWhileRetrievingData = ObservableBoolean(false)
    private var courseFlow: CourseFlow? = null
    private val spManager by lazy { SPManager(activity) }

    override fun init(bundle: Bundle?, courseFlow: CourseFlow?, courseManager: CourseManager) {
        this.courseFlow = courseFlow
        courseFlow?.also {
            fetchCourseResources(courseFlow, courseManager)

        } ?: run {
            //TODO error
        }
    }

    override fun onServiceConnected(commManagerService: CommManagerServiceImpl) {

    }

    private fun fetchCourseResources(courseFlow: CourseFlow, courseManager: CourseManager) {
        errorWhileRetrievingData.set(false)
        processMessage.set("Retrieving course data...")

        activity.add(
            courseManager.fetchCourseContentForOfflineAccess(courseFlow)
                .andThen(courseManager.fetchCourseAssets(courseFlow.course.id, courseFlow.course.courseId))
                .subscribeWith(object : DisposableObserver<Int>() {
                    override fun onComplete() {
                        spManager.setCourseAssetEntry(courseFlow.course.id)
                        redirectBasedOnSubscription(courseFlow)
                        activity.finish()
                    }

                    override fun onNext(t: Int) {
                        processDone.set(t)
                    }

                    override fun onError(e: Throwable) {
                        e.printStackTrace()
                        processMessage.set("Error occurred while retrieving courses, please retry")
                        errorWhileRetrievingData.set(true)
                        Toast.makeText(activity, "Error ${e.message}", Toast.LENGTH_LONG).show()
                    }

                })
        )


    }

    private fun redirectBasedOnSubscription(courseFlow: CourseFlow) {
        accountHelper.getUserProgress2().get()
            .addOnSuccessListener {
                if (it["course_enrolled.${courseFlow.course.id}"] != null) {

                    if (it["course_enrolled.${courseFlow.course.id}.is_story_fully_shown"] as Boolean) {
                        startLessonListActivity(courseFlow)

                    } else {
                        startCourseIntroActivity(courseFlow)
                    }

                }

            }.addOnFailureListener {
                //TODO
            }

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

}