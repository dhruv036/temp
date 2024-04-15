package io.stempedia.pictoblox.learn

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.databinding.ActivityCourseContentRetriverBinding

class CourseContentRetrieverActivity : AbsCourseActivity() {
    private val vm = CourseContentRetrieverVM(this)
    private lateinit var mbinding: ActivityCourseContentRetriverBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mbinding = DataBindingUtil.setContentView(this, R.layout.activity_course_content_retriver)
        mbinding.data = vm
    }


    override fun getVM() = vm
}
