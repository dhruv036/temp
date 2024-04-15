package io.stempedia.pictoblox.learn.lessons

import android.content.Intent
import android.os.Bundle
import androidx.databinding.ObservableBoolean
import androidx.viewpager2.widget.ViewPager2
import io.reactivex.observers.DisposableSingleObserver
import io.stempedia.pictoblox.connectivity.CommManagerServiceImpl
import io.stempedia.pictoblox.firebase.COURSE_FLOW
import io.stempedia.pictoblox.firebase.CourseFlow
import io.stempedia.pictoblox.firebase.CourseStorage
import io.stempedia.pictoblox.learn.AbsCourseVM
import io.stempedia.pictoblox.learn.CourseManager
import io.stempedia.pictoblox.util.PictobloxLogger

class LessonIntroVM(val activity: LessonIntroActivity) : AbsCourseVM() {
    private var isIntro = true
    private val list = mutableListOf<LessonIntroItemVM>()
    private var currentItem = 0
    private lateinit var courseFlow: CourseFlow

    val isFirstPage = ObservableBoolean()
    //TODO this is not used at moment due to design
    val isLastPage = ObservableBoolean()

    val vpCallbacks = VPCallbacks(this)


    override fun init(bundle: Bundle?, courseFlow: CourseFlow?, courseManager: CourseManager) {
        bundle?.also {
            isIntro = it.getString(LESSON_FUNCTION_TAG) == LESSON_FUNCTION_INTRO
        }

        courseFlow?.also {
            this.courseFlow = it
            getSweepableAssetList(it, courseManager)

        }
    }

    override fun onServiceConnected(commManagerService: CommManagerServiceImpl) {

    }

    fun onPageSelected(position: Int) {
        PictobloxLogger.getInstance().logd("onPageSelected : $position")
        isFirstPage.set(position == 0)
        isLastPage.set(position == list.size - 1)

    }

    fun onRightClicked() {
        if (isLastPage.get()) {
            if (isIntro) {
                startOverview()

            } else {
                activity.finish()
            }

            return
        }

        if (currentItem < list.size) {
            activity.moveToPage(++currentItem)

        }
    }

    fun onLeftClicked() {
        if (currentItem > 0) {
            activity.moveToPage(--currentItem)

        }

    }


    private fun startOverview() {
        val intent = Intent(activity, LessonOverviewActivity::class.java).apply {
            putExtra(COURSE_FLOW, courseFlow)
        }

        activity.startActivity(intent)
        activity.finish()
    }

    private fun getSweepableAssetList(courseFlow: CourseFlow, courseManager: CourseManager) {
        activity.add(courseManager.getLessonIntrosConclusion(courseFlow, isIntro)
            .map { it.map { story -> run { LessonIntroItemVM(story.asset) } } }
            .doAfterSuccess {
                list.clear()
                list.addAll(it)
            }
            .subscribeWith(object : DisposableSingleObserver<List<LessonIntroItemVM>>() {
                override fun onSuccess(t: List<LessonIntroItemVM>) {
                    activity.populateAssets(t)
                }

                override fun onError(e: Throwable) {
                    PictobloxLogger.getInstance().logException(e)
                    activity.showError(e.message ?: "Unknown error")
                }

            })
        )
    }

    inner class VPCallbacks(val vm: LessonIntroVM) : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            vm.onPageSelected(position)
        }
    }

}

class LessonIntroItemVM(val asset: CourseStorage.IntroConclusionTypeBuilder.Asset) {

}
