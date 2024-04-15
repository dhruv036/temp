package io.stempedia.pictoblox.learn

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.databinding.ActivityCourseDetailBinding

class CourseDetailActivity : AbsCourseActivity() {
    private lateinit var mBinding: ActivityCourseDetailBinding
    private val vm = CourseDetailVM(this)
    private var popupWindow: PopupWindow? = null

    override fun getVM() = vm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_course_detail)
        mBinding.data = vm

        setSupportActionBar(mBinding.tbCourseDetail)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        mBinding.tbCourseDetail.setNavigationOnClickListener { finish() }

        vm.onCreate()
    }

    override fun onResume() {
        super.onResume()
        vm.onResume()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        vm.onWindowFocusChanged(hasFocus)
    }


    fun showError(error: String) {
        Toast.makeText(this, "Error : $error", Toast.LENGTH_LONG).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        vm.onActivityResult(requestCode, resultCode, data)
    }

    fun hideLoginIncompleteDialog() {
        popupWindow?.dismiss()
        popupWindow = null
    }

    fun showLoginIncompleteDialog() {
        val accountIcon = mBinding.ivAccount

        popupWindow?.dismiss()

        val popupView =
            layoutInflater.inflate(
                R.layout.include_profile_incomplete_popup,
                null,
                false
            )

        val width = LinearLayout.LayoutParams.WRAP_CONTENT
        val height = LinearLayout.LayoutParams.WRAP_CONTENT
        val focusable = false
        popupWindow = PopupWindow(popupView, width, height, focusable)

        popupView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        val upIcon = popupView.findViewById<ImageView>(R.id.imageView34)

        val accountPos = IntArray(2)

        accountIcon.getLocationInWindow(accountPos)

        val x = (accountPos[0] + (upIcon.measuredWidth / 2) + (accountIcon.width / 2) - popupView.measuredWidth)
        val y = (accountPos[1] + accountIcon.measuredHeight)

        popupWindow?.animationStyle = R.style.style_incomplete_sign_up

        popupWindow?.showAtLocation(accountIcon, Gravity.NO_GRAVITY, x, y)

        Handler().postDelayed({
            popupWindow?.dismiss()
            popupWindow = null
        }, 3800)


    }

}

