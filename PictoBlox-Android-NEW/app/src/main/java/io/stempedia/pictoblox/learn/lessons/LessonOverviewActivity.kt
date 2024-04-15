package io.stempedia.pictoblox.learn.lessons

import android.os.Bundle
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.databinding.ActivityLessonOverviewBinding
import io.stempedia.pictoblox.learn.AbsCourseActivity

class LessonOverviewActivity : AbsCourseActivity() {
    private lateinit var mBinding: ActivityLessonOverviewBinding
    private val vm = LessonOverviewVM(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_lesson_overview)
        mBinding.data = vm
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    override fun getVM() = vm

    override fun onPause() {
        super.onPause()
        vm.onPause()
    }

    fun showError(s: String) {
        Toast.makeText(this, "Error :$s", Toast.LENGTH_LONG).show()
    }


}
