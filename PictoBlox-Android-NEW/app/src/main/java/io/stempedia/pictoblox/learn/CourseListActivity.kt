package io.stempedia.pictoblox.learn

import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.databinding.ActivityCourseListBinding
import io.stempedia.pictoblox.databinding.RowCourseListBinding

class CourseListActivity : AbsCourseActivity() {
    private val vm = CourseListVM(this)
    private val adapter = CourseListAdapter()
    private lateinit var mBinding: ActivityCourseListBinding
    private var popupWindow: PopupWindow? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_course_list)
        mBinding.data = vm
        mBinding.rvCourseList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        mBinding.rvCourseList.adapter = adapter

        setSupportActionBar(mBinding.tbProjectList)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        mBinding.tbProjectList.setNavigationOnClickListener { finish() }

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

    override fun getVM() = vm

    fun populateCourse(it: List<CourseListItemVM>) {
        adapter.setCourses(it)
    }

    fun showError(s: String) {
        Toast.makeText(this, "Error : $s", Toast.LENGTH_LONG).show()

    }

    fun showWIP() {
        Toast.makeText(this, "WIP: Coming soon", Toast.LENGTH_LONG).show()
    }


    inner class CourseListAdapter : RecyclerView.Adapter<CourseListViewHolder>() {
        private val courseList = mutableListOf<CourseListItemVM>()

        fun setCourses(list: List<CourseListItemVM>) {
            courseList.clear()
            courseList.addAll(list)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseListViewHolder {
            val view = layoutInflater.inflate(R.layout.row_course_list, parent, false)
            return CourseListViewHolder(view)
        }

        override fun getItemCount(): Int {
            return courseList.size
        }

        override fun onBindViewHolder(holder: CourseListViewHolder, position: Int) {
            holder.setVM(courseList[position])
        }

    }


    inner class CourseListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var mBinding = DataBindingUtil.bind<RowCourseListBinding>(view)!!

        fun setVM(vm: CourseListItemVM) {
            mBinding.data = vm
        }

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
