package io.stempedia.pictoblox.learn.lessons

import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Toast
import androidx.databinding.ObservableField
import io.reactivex.observers.DisposableCompletableObserver
import io.reactivex.observers.DisposableSingleObserver
import io.stempedia.pictoblox.connectivity.CommManagerServiceImpl
import io.stempedia.pictoblox.firebase.CourseFlow
import io.stempedia.pictoblox.learn.AbsCourseVM
import io.stempedia.pictoblox.learn.CourseManager
import io.stempedia.pictoblox.util.PictobloxLogger
import io.stempedia.pictoblox.util.SPManager
import io.stempedia.pictoblox.web.PictoBloxWebActivity
import java.io.File

class LessonOverviewVM(val activity: LessonOverviewActivity) : AbsCourseVM() {
    private var mediaPlayer: MediaPlayer? = null
    var overviewTitle = ObservableField<String>()
    var overviewImagePath = ObservableField<String>()
    private lateinit var courseFlow: CourseFlow
    private lateinit var story: CourseManager.LessonOverviewStory
    private lateinit var courseManager: CourseManager
    private var commManagerService: CommManagerServiceImpl? = null
    private val spManager: SPManager by lazy(LazyThreadSafetyMode.NONE) {
        SPManager(activity)
    }

    override fun init(bundle: Bundle?, courseFlow: CourseFlow?, courseManager: CourseManager) {
        this.courseManager = courseManager
        courseFlow?.also {
            this.courseFlow = it
            fetLessonOverview(courseManager, it)
        }
    }

    override fun onServiceConnected(commManagerService: CommManagerServiceImpl) {
        this.commManagerService = commManagerService
    }

    fun onPause() {
        try {
            mediaPlayer?.also {
                if (it.isPlaying) {
                    it.stop()
                    it.release()
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    fun onRightClicked() {
        activity.add(
            courseManager.prepareLessonStepForPictbloxWeb(courseFlow)
                .flatMapCompletable {
                    commManagerService?.communicationHandler?.loadCourse(File(it.first), courseFlow, it.third.toInt(), it.second)
                }

                .subscribeWith(object : DisposableCompletableObserver() {
                    override fun onComplete() {
                        val intent = Intent(activity, PictoBloxWebActivity::class.java)
                        activity.startActivity(intent)
                        activity.finish()
                    }

                    override fun onError(e: Throwable) {
                        e.printStackTrace()
                        //TODO
                    }

                })
        )

    }

    fun onLeftClicked() {
        activity.finish()

    }

    private fun fetLessonOverview(courseManager: CourseManager, courseFlow: CourseFlow) {
        activity.add(
            courseManager.getLessonOverview(courseFlow.course.id, courseFlow.lesson.id, courseFlow.lesson.index)
                .doOnSuccess {
                    this.story = it
                }
                .subscribeWith(object : DisposableSingleObserver<CourseManager.LessonOverviewStory>() {
                    override fun onSuccess(t: CourseManager.LessonOverviewStory) {
                        overviewTitle.set(t.text)
                        overviewImagePath.set(t.asset.gifPath)
                        if (!spManager.isLessonMusicMuted) {
                            playMusicAt(t.asset.audioPath)
                        }
                    }

                    override fun onError(e: Throwable) {
                        activity.showError(e.message?:"Unknown error")
                    }

                })
        )

    }

    private fun playMusicAt(audioPath: String) {

        try {
            mediaPlayer?.also {
                if (it.isPlaying) {
                    it.stop()
                    it.release()
                }
            }

            val attribute = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(attribute)
                setDataSource("file://${audioPath}")
                prepare() // might take long! (for buffering, etc)
                start()
            }

        } catch (e: Exception) {
            PictobloxLogger.getInstance().logException(e)
            activity.showError(e.message?:"Unknown error")
        }

    }


}