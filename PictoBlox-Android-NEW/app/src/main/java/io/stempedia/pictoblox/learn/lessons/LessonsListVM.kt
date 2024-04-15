package io.stempedia.pictoblox.learn.lessons

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import io.reactivex.observers.DisposableObserver
import io.reactivex.observers.DisposableSingleObserver
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.connectivity.CommManagerServiceImpl
import io.stempedia.pictoblox.firebase.COURSE_FLOW
import io.stempedia.pictoblox.firebase.CourseFlow
import io.stempedia.pictoblox.firebase.Lesson
import io.stempedia.pictoblox.learn.AbsCourseVM
import io.stempedia.pictoblox.learn.CourseManager
import io.stempedia.pictoblox.util.PictobloxLogger
import io.stempedia.pictoblox.util.SPManager

class LessonsListVM(val activity: LessonsListActivity) : AbsCourseVM() {
    val title = ObservableField<String>("")
    //private var isMuted = false
    var isMuted = ObservableBoolean(false)
    //val muteIcon = ObservableInt(R.drawable.ic_lesson_list_volume_mute)
    val totalScore = ObservableField<String>("100")
    private var courseFlow: CourseFlow? = null
    private var itemList: List<LessonItemVM>? = null
    private lateinit var courseManager: CourseManager
    private val spManager: SPManager by lazy(LazyThreadSafetyMode.NONE) {
        SPManager(activity)
    }

    override fun init(bundle: Bundle?, courseFlow: CourseFlow?, courseManager: CourseManager) {
        this.courseManager = courseManager
        courseFlow?.also {
            title.set(it.course.title)
            fetchLesson2(it, courseManager)
            fetchLessonsMeta(it, courseManager)
            this.courseFlow = it
        }

        isMuted.set(spManager.isLessonMusicMuted)
    }

    override fun onServiceConnected(commManagerService: CommManagerServiceImpl) {

    }

    fun onRestart() {
        courseFlow?.also { courseFlow ->
            fetchLesson2(courseFlow, courseManager)
            fetchLessonsMeta(courseFlow, courseManager)
        }
    }

    private fun fetchLesson2(courseFlow: CourseFlow, courseManager: CourseManager) {
        activity.add(
            courseManager.getAllLessonsForCourse(courseFlow)
                .map {
                    it.mapIndexed { index, story ->

                        val res = if (story.isUnlocked) {
                            getUnlockedIndex(story.index)
                        } else {
                            getLockedIndex(story.index)
                        }


                        LessonItemVM(
                            this,
                            story.index,
                            res,
                            story.rating,
                            story.isUnlocked,
                            story.isCompleted,
                            story.id,
                            story.title,
                            index == it.size - 1
                        )
                    }
                }
                .doOnNext {
                    this.itemList = it
                }
                .subscribeWith(object : DisposableObserver<List<LessonItemVM>>() {


                    override fun onError(e: Throwable) {
                        PictobloxLogger.getInstance().logException(e)
                        activity.showError(e.message ?: "Unknown error")
                    }

                    override fun onComplete() {

                    }

                    override fun onNext(t: List<LessonItemVM>) {
                        activity.onPopulateLessons(t)
                    }

                })
        )
    }

    private fun fetchLessonsMeta(courseFlow: CourseFlow, courseManager: CourseManager) {
        activity.add(
            courseManager.getLessonsMeta(courseFlow)
                .subscribeWith(object : DisposableSingleObserver<CourseManager.UserLessonsMetaStory>() {
                    override fun onSuccess(t: CourseManager.UserLessonsMetaStory) {
                        totalScore.set(t.pointsEarned.toString())
                    }

                    override fun onError(e: Throwable) {
                        PictobloxLogger.getInstance().logException(e)
                        activity.showError(e.message ?: "Unknown error")
                    }

                })

        )
    }

    fun onLessonClicked2(pos: Int) {
        itemList?.also { lessons ->
            val lesson = lessons[pos]
            if (lesson.isLessonUnlocked) {
                courseFlow?.also {

                    it.lesson = Lesson(
                        lesson.lessonId,
                        lesson.lessonIndex,
                        lesson.lessonTitle,
                        lesson.isLast,
                        lesson.isLessonUnlocked,
                        lesson.isLessonCompleted
                    )

                    if (pos < lessons.size) {
                        val nextLesson = lessons[pos + 1]
                        it.nextLesson = Lesson(
                            nextLesson.lessonId,
                            nextLesson.lessonIndex,
                            nextLesson.lessonTitle,
                            nextLesson.isLast,
                            nextLesson.isLessonUnlocked,
                            nextLesson.isLessonCompleted
                        )
                    }


                    val bundle = Bundle().apply {
                        putParcelable(COURSE_FLOW, courseFlow)
                    }

                    val intent = Intent(activity, LessonTitleActivity::class.java)
                    intent.putExtras(bundle)

                    activity.startActivity(intent)
                }

            } else {
                Toast.makeText(activity, "Complete previous lesson to start Lesson no :${lesson.lessonIndex}", Toast.LENGTH_LONG).show()
            }
        }
    }

    //TODO
    fun onHomeClicked() {
        activity.finish()
    }

    fun onMuteClicked() {
        isMuted.set(!isMuted.get())
        spManager.isLessonMusicMuted = isMuted.get()
    }

    enum class LessonStatus {
        LOCKED, ONGOING, COMPLETED
    }


    private fun getLockedIndex(index: Int): Int {
        return when (index) {
            1 -> R.drawable.ic_lesson_index_l_1
            2 -> R.drawable.ic_lesson_index_l_2
            3 -> R.drawable.ic_lesson_index_l_3
            4 -> R.drawable.ic_lesson_index_l_4
            5 -> R.drawable.ic_lesson_index_l_5
            else -> R.drawable.ic_lesson_index_l_1
        }
    }

    private fun getUnlockedIndex(index: Int): Int {
        return when (index) {
            1 -> R.drawable.ic_lesson_index_ul_1
            2 -> R.drawable.ic_lesson_index_ul_2
            3 -> R.drawable.ic_lesson_index_ul_3
            4 -> R.drawable.ic_lesson_index_ul_4
            5 -> R.drawable.ic_lesson_index_ul_5
            else -> R.drawable.ic_lesson_index_ul_1
        }
    }

}