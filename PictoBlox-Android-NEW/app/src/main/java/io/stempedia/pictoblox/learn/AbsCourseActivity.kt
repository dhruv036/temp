package io.stempedia.pictoblox.learn

import android.os.Bundle
import io.stempedia.pictoblox.connectivity.CommManagerServiceImpl
import io.stempedia.pictoblox.firebase.COURSE_FLOW
import io.stempedia.pictoblox.firebase.CourseFlow
import io.stempedia.pictoblox.uiUtils.AbsActivity

abstract class AbsCourseActivity : AbsActivity() {

    abstract fun getVM(): AbsCourseVM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val courseFlow = intent?.getParcelableExtra<CourseFlow>(COURSE_FLOW)
        getVM().init(intent.extras, courseFlow, CourseManager(applicationContext))
    }

    override fun onPBServiceConnected(commManagerService: CommManagerServiceImpl) {
        getVM().onServiceConnected(commManagerService)
    }

    override fun onBeforeServiceGetsDisconnected(commManagerService: CommManagerServiceImpl) {

    }

    override fun onDeviceConnecting(name: String) {

    }

    override fun onDeviceConnected(name: String, address: String) {

    }

    override fun onDeviceDisconnected(name: String) {

    }

    override fun error(msg: String) {

    }
}

abstract class AbsCourseVM {
    abstract fun init(bundle: Bundle?, courseFlow: CourseFlow?, courseManager: CourseManager)
    abstract fun onServiceConnected(commManagerService: CommManagerServiceImpl)
}