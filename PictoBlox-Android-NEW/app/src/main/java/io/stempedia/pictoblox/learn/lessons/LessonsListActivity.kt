package io.stempedia.pictoblox.learn.lessons

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.databinding.ActivityLessonsBinding
import io.stempedia.pictoblox.databinding.RowLessonsBinding
import io.stempedia.pictoblox.learn.AbsCourseActivity

class LessonsListActivity : AbsCourseActivity() {
    private val lessonsListVM = LessonsListVM(this)
    lateinit var mBinding: ActivityLessonsBinding
    private val adapter = LessonAdapter()
    override fun getVM() = lessonsListVM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_lessons)
        mBinding.data = lessonsListVM
        mBinding.rvLessons.layoutManager = GridLayoutManager(this, 3)
        mBinding.rvLessons.adapter = adapter
        setSupportActionBar(mBinding.tbLessons)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        mBinding.tbLessons.setNavigationOnClickListener { finish() }
    }

    override fun onRestart() {
        super.onRestart()
        lessonsListVM.onRestart()
    }

    fun onPopulateLessons(itemVMList: List<LessonItemVM>) {
        adapter.setLessons(itemVMList)
    }

    fun showError(s: String) {
        Toast.makeText(this, "Error :$s", Toast.LENGTH_LONG).show()
    }


    inner class LessonAdapter : RecyclerView.Adapter<LessonViewHolder>() {
        private val list = mutableListOf<LessonItemVM>()


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonViewHolder {
            val v = layoutInflater.inflate(R.layout.row_lessons, parent, false)
            return LessonViewHolder(v)

        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onBindViewHolder(holder: LessonViewHolder, position: Int) {
            holder.setData(list[position])
        }

        fun setLessons(itemVMList: List<LessonItemVM>) {
            list.clear()
            list.addAll(itemVMList)
            notifyDataSetChanged()
        }

    }

    inner class LessonViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val mBinding = DataBindingUtil.bind<RowLessonsBinding>(v)

        fun setData(data: LessonItemVM) {
            data.vh = this
            mBinding?.data = data
        }

    }

}
