package io.stempedia.pictoblox.quiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.databinding.ActivityQuizBinding
import io.stempedia.pictoblox.databinding.RowQuizQuestionBinding
import io.stempedia.pictoblox.learn.AbsCourseActivity

class QuizActivity : AbsCourseActivity() {
    val vm = QuizActivityVM(this)
    private lateinit var mBinding: ActivityQuizBinding
    private val adapter = QuizQuestionAdapter()

    override fun getVM() = vm

    override fun getRootView(layoutInflater: LayoutInflater): View? {
        if (mBinding == null) {
            mBinding = ActivityQuizBinding.inflate(layoutInflater)
        }
        return mBinding!!.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_quiz)
        mBinding.vp2QuizQuestions.registerOnPageChangeCallback(vm.vpCallbacks)
        mBinding.vp2QuizQuestions.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        mBinding.vp2QuizQuestions.adapter = adapter
        mBinding.vp2QuizQuestions.isUserInputEnabled = false
        mBinding.data = vm
    }

    fun setQuestions(t: List<QuestionVM>) {
        adapter.setQuestions(t)
    }

    fun switchToQuestionIndex(index: Int) {
        mBinding.vp2QuizQuestions.currentItem = index
    }

    fun showError(s: String) {
        Toast.makeText(this, "Error :$s", Toast.LENGTH_LONG).show()
    }

    inner class QuizQuestionAdapter : RecyclerView.Adapter<QuizViewHolder>() {
        private val dataList = mutableListOf<QuestionVM>()


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuizViewHolder {
            val v = layoutInflater.inflate(R.layout.row_quiz_question, parent, false)
            return QuizViewHolder(v)

        }

        override fun getItemCount(): Int {
            return dataList.size
        }

        override fun onBindViewHolder(holder: QuizViewHolder, position: Int) {
            holder.setData(dataList[position])
        }

        fun setQuestions(itemVMList: List<QuestionVM>) {
            dataList.clear()
            dataList.addAll(itemVMList)
            notifyDataSetChanged()
        }

    }

    inner class QuizViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val mBinding = DataBindingUtil.bind<RowQuizQuestionBinding>(v)

        fun setData(questionVM: QuestionVM) {
            mBinding?.data = questionVM

        }

    }
}
