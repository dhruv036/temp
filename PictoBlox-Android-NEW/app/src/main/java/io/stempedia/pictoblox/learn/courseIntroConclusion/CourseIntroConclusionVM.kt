package io.stempedia.pictoblox.learn.courseIntroConclusion

import android.content.Intent
import android.os.Bundle
import androidx.databinding.ObservableBoolean
import androidx.viewpager2.widget.ViewPager2
import io.reactivex.observers.DisposableSingleObserver
import io.stempedia.pictoblox.account.AccountHelper
import io.stempedia.pictoblox.connectivity.CommManagerServiceImpl
import io.stempedia.pictoblox.firebase.COURSE_FLOW
import io.stempedia.pictoblox.firebase.CourseFlow
import io.stempedia.pictoblox.learn.AbsCourseVM
import io.stempedia.pictoblox.learn.CourseManager
import io.stempedia.pictoblox.learn.lessons.LessonsListActivity
import io.stempedia.pictoblox.util.PictobloxLogger

class CourseIntroConclusionVM(val activity: CourseIntroActivity) : AbsCourseVM() {

    private lateinit var courseFlow: CourseFlow
    //If its conclusion it will be false
    private var isIntro = false
    private val list = mutableListOf<CourseAssetViewerVM>()
    private var currentItem = 0
    val vpCallbacks = VPCallbacks(this)

    val isFirstPage = ObservableBoolean()
    val isLastPage = ObservableBoolean()


    override fun init(bundle: Bundle?, courseFlow: CourseFlow?, courseManager: CourseManager) {
        bundle?.also {
            this.isIntro = it.getInt(INTRO_CONCLUSION_FLAG) == INTRO
        }

        courseFlow?.also {
            this.courseFlow = it
            getSweepableAssetList(it.course.id, courseManager)
        }
    }

    override fun onServiceConnected(commManagerService: CommManagerServiceImpl) {

    }

    fun onPageSelected(position: Int) {
        PictobloxLogger.getInstance().logd("onPageSelected : $position")
        isFirstPage.set(position == 0)
        isLastPage.set(position == list.size - 1)

        if (isLastPage.get()) {
            updateCourseIntroSeenFlag(courseFlow.course.id)
        }
    }

    fun onRightClicked() {
        if (currentItem < list.size) {
            activity.moveToPage(++currentItem)
        }

        if (isLastPage.get()) {
            activity.startActivity(Intent(activity, LessonsListActivity::class.java).apply {
                putExtra(COURSE_FLOW, courseFlow)
            })

            activity.finish()
        }
    }

    fun onLeftClicked() {
        if (currentItem > 0) {
            activity.moveToPage(--currentItem)

        }

    }

    private fun getSweepableAssetList(documentId: String, courseManager: CourseManager) {
        if (isIntro) {
            activity.add(courseManager.getCourseIntros(documentId)
                .flatMap { courseManager.processCourseIntroConclusion(it.second, it.first) }
                .map { it.map { story -> run { CourseAssetViewerVM(story.asset, story.text.replace("\\n", "\n")) } } }
                .doAfterSuccess {
                    list.clear()
                    list.addAll(it)
                }
                .subscribeWith(object : DisposableSingleObserver<List<CourseAssetViewerVM>>() {
                    override fun onSuccess(t: List<CourseAssetViewerVM>) {
                        activity.populateAssets(t)
                    }

                    override fun onError(e: Throwable) {
                        activity.showError(e.message?:"Unknown error")
                        e.printStackTrace()
                    }

                })
            )

        } else {
            //commManagerServiceImpl.courseManager.getCourseConclusions(documentId)

        }
    }

    private fun updateCourseIntroSeenFlag(documentId: String) {
        val accountHelper = AccountHelper()
        accountHelper.getUserProgress2().update(mapOf("course_enrolled.${documentId}.is_story_fully_shown" to true))

    }

    inner class VPCallbacks(val vm: CourseIntroConclusionVM) : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            vm.onPageSelected(position)
        }
    }

}