package io.stempedia.pictoblox.learn.lessons

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.databinding.ActivityLessonIntroBinding
import io.stempedia.pictoblox.databinding.RowLessonIntroBinding
import io.stempedia.pictoblox.learn.AbsCourseActivity

const val LESSON_FUNCTION_TAG = "fun_tag"
const val LESSON_FUNCTION_INTRO = "fun_intro"
const val LESSON_FUNCTION_CONCLUSION = "fun_conclusion"

class LessonIntroActivity : AbsCourseActivity() {
    private val vm = LessonIntroVM(this)
    private lateinit var mBinding: ActivityLessonIntroBinding
    private val adapter = LessonIntroAdapter()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_lesson_intro)
        mBinding.data = vm
        mBinding.vp2LessonIntroConclusion.registerOnPageChangeCallback(vm.vpCallbacks)
        mBinding.vp2LessonIntroConclusion.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        mBinding.vp2LessonIntroConclusion.adapter = adapter
    }


    override fun onDestroy() {
        super.onDestroy()
        mBinding.vp2LessonIntroConclusion.unregisterOnPageChangeCallback(vm.vpCallbacks)
    }

    override fun getVM() = vm

    fun moveToPage(position: Int) {
        mBinding.vp2LessonIntroConclusion?.also {
            it.setCurrentItem(position, true)
        }
    }

    fun populateAssets(t: List<LessonIntroItemVM>) {
        adapter.addData(t)
    }

    fun showError(s: String) {
        Toast.makeText(this, "Error :$s", Toast.LENGTH_LONG).show()
    }

    inner class LessonIntroAdapter : RecyclerView.Adapter<LessonIntroItemVH>() {
        private val list = mutableListOf<LessonIntroItemVM>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonIntroItemVH {

            val view = layoutInflater.inflate(R.layout.row_lesson_intro, parent, false)

            return LessonIntroItemVH(view)
        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onBindViewHolder(holder: LessonIntroItemVH, position: Int) {
            holder.setData(list[position])
        }

        fun addData(t: List<LessonIntroItemVM>) {
            list.clear()
            list.addAll(t)
            notifyDataSetChanged()
        }


    }

    inner class LessonIntroItemVH(v: View) : RecyclerView.ViewHolder(v) {
        val mBinding = DataBindingUtil.bind<RowLessonIntroBinding>(v)

        fun setData(data: LessonIntroItemVM) {
            mBinding?.data = data
        }
    }


}
