package io.stempedia.pictoblox.learn.courseIntroConclusion

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.databinding.ActivityCourseIntroBinding
import io.stempedia.pictoblox.databinding.FragmentAssetViewerBinding
import io.stempedia.pictoblox.learn.AbsCourseActivity

const val INTRO_CONCLUSION_FLAG = "flag"
const val INTRO = 101
const val CONCLUSION = 102

class CourseIntroActivity : AbsCourseActivity() {
    private val vm = CourseIntroConclusionVM(this)
    private lateinit var mBinding: ActivityCourseIntroBinding
    private lateinit var adapter: CourseAssetsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_course_intro)
        mBinding.data = vm
        mBinding.vp2CourseIntroConclusion.registerOnPageChangeCallback(vm.vpCallbacks)
        adapter = CourseAssetsAdapter(layoutInflater)
        mBinding.vp2CourseIntroConclusion.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        mBinding.vp2CourseIntroConclusion.adapter = adapter
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        mBinding.vp2CourseIntroConclusion.unregisterOnPageChangeCallback(vm.vpCallbacks)
    }

    override fun getVM() = vm

    fun moveToPage(position: Int) {
        mBinding.vp2CourseIntroConclusion.setCurrentItem(position, true)
    }

    fun populateAssets(t: List<CourseAssetViewerVM>) {
        adapter.addData(t)
    }

    fun showError(s: String) {
        Toast.makeText(this, "Error: $s", Toast.LENGTH_LONG).show()
    }

    private class CourseAssetsAdapter(val inflater: LayoutInflater) : RecyclerView.Adapter<CourseAssetsVH>() {
        private val list = mutableListOf<CourseAssetViewerVM>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseAssetsVH {

            val view = inflater.inflate(R.layout.fragment_asset_viewer, parent, false)

            return CourseAssetsVH(view)
        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onBindViewHolder(holder: CourseAssetsVH, position: Int) {
            holder.setData(list[position])
        }

        fun addData(t: List<CourseAssetViewerVM>) {
            list.clear()
            list.addAll(t)
            notifyDataSetChanged()
        }


    }

    private class CourseAssetsVH(v: View) : RecyclerView.ViewHolder(v) {
        val mBinding = DataBindingUtil.bind<FragmentAssetViewerBinding>(v)

        fun setData(data: CourseAssetViewerVM) {
            mBinding?.data = data
        }
    }

}
