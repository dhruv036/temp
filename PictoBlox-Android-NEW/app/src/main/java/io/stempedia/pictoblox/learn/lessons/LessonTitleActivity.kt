package io.stempedia.pictoblox.learn.lessons

import android.os.Bundle
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.databinding.ActivityLessonTitleBinding
import io.stempedia.pictoblox.learn.AbsCourseActivity
import io.stempedia.pictoblox.util.PictobloxLogger

class LessonTitleActivity : AbsCourseActivity() {
    private lateinit var mBinding: ActivityLessonTitleBinding
    private val vm = LessonTitleVM(this)

    override fun getVM() = vm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_lesson_title)
        mBinding.data = vm
        supportActionBar?.setDisplayShowTitleEnabled(false)
        PictobloxLogger.getInstance().logd("onCreate")
    }

    override fun onRestart() {
        super.onRestart()
        PictobloxLogger.getInstance().logd("onRestart")
    }

    override fun onResume() {
        super.onResume()
        PictobloxLogger.getInstance().logd("onResume")
    }

    override fun onPause() {
        super.onPause()
        PictobloxLogger.getInstance().logd("onPause")
    }

    override fun onStop() {
        super.onStop()
        PictobloxLogger.getInstance().logd("onStop")
    }

    fun showError(s: String) {
        Toast.makeText(this, "Error :$s", Toast.LENGTH_LONG).show()
    }

}
