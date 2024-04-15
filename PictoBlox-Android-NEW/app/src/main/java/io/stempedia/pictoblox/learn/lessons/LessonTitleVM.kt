package io.stempedia.pictoblox.learn.lessons

import android.content.Intent
import android.os.Bundle
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import io.reactivex.observers.DisposableCompletableObserver
import io.stempedia.pictoblox.connectivity.CommManagerServiceImpl
import io.stempedia.pictoblox.firebase.COURSE_FLOW
import io.stempedia.pictoblox.firebase.CourseFlow
import io.stempedia.pictoblox.learn.AbsCourseVM
import io.stempedia.pictoblox.learn.CourseManager
import io.stempedia.pictoblox.web.PictoBloxWebActivity


class LessonTitleVM(val activity: LessonTitleActivity) : AbsCourseVM() {
    val title = ObservableField<String>()
    val isCompleted = ObservableBoolean(false)
    private lateinit var courseFlow: CourseFlow
    private var commManagerServiceImpl: CommManagerServiceImpl? = null

    override fun init(bundle: Bundle?, courseFlow: CourseFlow?, courseManager: CourseManager) {
        courseFlow?.also {
            this.courseFlow = it
            title.set(it.lesson.title)
            isCompleted.set(it.lesson.isCompleted)
        }
    }

    override fun onServiceConnected(commManagerService: CommManagerServiceImpl) {

    }

    fun onProgramClicked() {
        commManagerServiceImpl?.also { commManagerServiceImpl ->
            commManagerServiceImpl.courseManager.getLessonCompletionFile(courseFlow)
                .flatMapCompletable { commManagerServiceImpl.communicationHandler.loadCompletedLessonProject(it) }
                .subscribeWith(object : DisposableCompletableObserver() {

                    override fun onComplete() {
                        val intent = Intent(activity, PictoBloxWebActivity::class.java)
                        activity.startActivity(intent)
                        activity.finish()

                    }

                    override fun onError(e: Throwable) {
                        activity.showError(e.message?:"Unknown error")
                    }
                })
        }
    }


    fun onRestartClicked() {
        val intent = Intent(activity, LessonIntroActivity::class.java).apply {
            putExtra(COURSE_FLOW, courseFlow)
            putExtra(LESSON_FUNCTION_TAG, LESSON_FUNCTION_INTRO)
        }

        activity.startActivity(intent)
        activity.finish()
    }

    fun onStartClicked() {
        val intent = Intent(activity, LessonIntroActivity::class.java).apply {
            putExtra(COURSE_FLOW, courseFlow)
            putExtra(LESSON_FUNCTION_TAG, LESSON_FUNCTION_INTRO)
        }

        activity.startActivity(intent)
        activity.finish()
    }

}