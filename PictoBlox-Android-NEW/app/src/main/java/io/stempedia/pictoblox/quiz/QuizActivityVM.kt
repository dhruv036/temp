package io.stempedia.pictoblox.quiz

import android.content.Intent
import android.os.Bundle
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.viewpager2.widget.ViewPager2
import io.reactivex.observers.DisposableCompletableObserver
import io.reactivex.observers.DisposableSingleObserver
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.connectivity.CommManagerServiceImpl
import io.stempedia.pictoblox.firebase.COURSE_FLOW
import io.stempedia.pictoblox.firebase.CourseFlow
import io.stempedia.pictoblox.learn.AbsCourseVM
import io.stempedia.pictoblox.learn.CourseManager
import io.stempedia.pictoblox.learn.lessons.LESSON_FUNCTION_CONCLUSION
import io.stempedia.pictoblox.learn.lessons.LESSON_FUNCTION_TAG
import io.stempedia.pictoblox.learn.lessons.LessonIntroActivity
import io.stempedia.pictoblox.util.PictobloxLogger

class QuizActivityVM(val activity: QuizActivity) : AbsCourseVM() {
    val vpCallbacks = VPCallbacks(this)
    private var courseFlow: CourseFlow? = null
    private var selectedQuestionIndex: Int = 0
    private var selectedOption: OptionVM? = null

    private val afSubmit = 100
    private val afNext = 101
    private val afQuizComplete = 102

    private var isQuizCompleted = false
    private var totalQuizPointEarned = 0
    val showSolutionIcon = ObservableBoolean()
    val showSolution = ObservableBoolean()
    val solutionImage = ObservableField<String>()
    val solutionText = ObservableField<String>()
    private var itemVMList: List<QuestionVM>? = null

    private lateinit var courseManager: CourseManager

    override fun init(bundle: Bundle?, courseFlow: CourseFlow?, courseManager: CourseManager) {
        this.courseManager = courseManager
        this.courseFlow = courseFlow
        courseFlow?.also {
            fetchLessonQuiz(courseManager, it)
        }
    }

    override fun onServiceConnected(commManagerService: CommManagerServiceImpl) {

    }

    private fun fetchLessonQuiz(courseManager: CourseManager, courseFlow: CourseFlow) {
        activity.add(
            courseManager.getQuizOfLesson(courseFlow)
                .doOnSuccess {
                    isQuizCompleted = it.isCompleted
                    totalQuizPointEarned = it.totalPointsEarned
                }
                .map {
                    val itemVMList = mutableListOf<QuestionVM>()

                    it.questions.forEachIndexed { qi, question ->

                        val qVM = QuestionVM(this@QuizActivityVM, id = "")
                        val isQuestionAlreadyAttempted = question.attempt != null

                        if (isQuestionAlreadyAttempted) {
                            if (qi == it.questions.size - 1) {
                                qVM.actionText.set("Finish")
                                qVM.actionFunction = afQuizComplete
                            } else {
                                qVM.actionText.set("Next")
                                qVM.actionFunction = afNext
                            }
                        } else {
                            qVM.actionText.set("Submit")
                            qVM.actionFunction = afSubmit
                        }

                        qVM.questionImage = question.questionImage
                        qVM.questionText = "${qi + 1}. ${question.questionText}"
                        qVM.solutionText = question.solutionText
                        qVM.solutionImage = question.solutionImage
                        qVM.questionIndex = qi
                        qVM.id = question.id
                        qVM.correctAnswerPoint = question.correctAnswerPoints.toInt()
                        qVM.isLastQuestion = qi == (it.questions.size - 1)

                        question.options.forEachIndexed { oi, option ->

                            val optionVM = OptionVM(this@QuizActivityVM)

                            optionVM.isOptionAvailable = option.isAvailable
                            optionVM.isOptionSelectable.set(!isQuestionAlreadyAttempted)
                            optionVM.optionIndex = oi

                            optionVM.setDefaultBG()

                            optionVM.optionImage = option.image
                            optionVM.optionText = option.text
                            optionVM.isCorrect = option.isCorrect

                            optionVM.setDefaultBG()

                            if (isQuestionAlreadyAttempted) {

                                if (question.attempt!!.optionSelected.toInt() == oi) {
                                    optionVM.setSelectedBG()
                                }

                                if (option.isCorrect) {
                                    optionVM.setCorrectBG()
                                }

                            }

                            qVM.optionVMList.add(optionVM)

                        }

                        itemVMList.add(qVM)
                    }

                    itemVMList

                }
                .doOnSuccess {
                    this@QuizActivityVM.itemVMList = it
                }
                .subscribeWith(object : DisposableSingleObserver<List<QuestionVM>>() {

                    override fun onError(e: Throwable) {
                        PictobloxLogger.getInstance().logException(e)
                        activity.showError(e.message?:"Unknown error")
                    }


                    override fun onSuccess(t: List<QuestionVM>) {
                        activity.setQuestions(t)
                    }

                })
        )
    }


    fun onSubmitClicked(questionVM: QuestionVM) {
        when (questionVM.actionFunction) {
            afSubmit -> {
                selectedOption?.also {
                    submitAnswer(questionVM, it)
                }
            }

            afNext -> {
                switchToNextQuestion(questionVM)

            }

            afQuizComplete -> {
                finishQuiz()
            }
        }
    }

    private fun submitAnswer(questionVM: QuestionVM, optionVM: OptionVM) {
        courseFlow?.also { courseFlow ->
            val pointsEarned = if (optionVM.isCorrect) {
                questionVM.correctAnswerPoint
            } else {
                0
            }

            if (!isQuizCompleted) {
                isQuizCompleted = questionVM.isLastQuestion
            }

            totalQuizPointEarned += pointsEarned

            courseManager.setLessonQuizAnswer(
                courseFlow,
                questionVM.id,
                optionVM.isCorrect,
                optionVM.optionIndex,
                pointsEarned,
                isQuizCompleted,
                totalQuizPointEarned
            )
                .doOnSubscribe {
                    questionVM.optionVMList.find { it.isCorrect }?.setCorrectBG()
                }
                .subscribeWith(object : DisposableCompletableObserver() {

                    override fun onComplete() {

                        if (questionVM.isLastQuestion) {
                            questionVM.actionText.set("Finish")
                            questionVM.actionFunction = afQuizComplete

                        } else {
                            questionVM.actionText.set("Next")
                            questionVM.actionFunction = afNext
                        }

                        setCurrentQuestionSolutionFlag(questionVM)

                    }

                    override fun onError(e: Throwable) {
                        PictobloxLogger.getInstance().logException(e)
                        activity.showError(e.message?:"Unknown error")
                    }

                })


        }


    }

    private fun switchToNextQuestion(questionVM: QuestionVM) {
        activity.switchToQuestionIndex(questionVM.questionIndex)

    }

    private fun finishQuiz() {

        if (isQuizCompleted)

            courseFlow?.also { courseFlow ->

                courseManager.setLessonCompleted(courseFlow, totalQuizPointEarned)
                    .subscribeWith(object : DisposableCompletableObserver() {
                        override fun onComplete() {

                            val intent = Intent(activity, LessonIntroActivity::class.java).apply {
                                putExtra(COURSE_FLOW, courseFlow)
                                putExtra(LESSON_FUNCTION_TAG, LESSON_FUNCTION_CONCLUSION)
                            }

                            activity.startActivity(intent)
                            activity.finish()
                        }

                        override fun onError(e: Throwable) {
                            PictobloxLogger.getInstance().logException(e)
                            activity.showError(e.message?:"Unknown error")
                        }
                    })


            }

    }

    fun onItemClicked(optionVM: OptionVM) {
        selectedOption?.setDefaultBG()
        this.selectedOption = optionVM
    }

    inner class VPCallbacks(val vm: QuizActivityVM) : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            vm.onPageSelected(position)
        }
    }

    private fun onPageSelected(position: Int) {
        selectedQuestionIndex = position
        itemVMList?.also {
            setCurrentQuestionSolutionFlag(it[position])
            solutionImage.set(it[position].solutionImage)
            solutionText.set(it[position].solutionText)
        }
    }

    fun setCurrentQuestionSolutionFlag(questionVM: QuestionVM) {
        showSolutionIcon.set(questionVM.actionFunction == afNext || questionVM.actionFunction == afQuizComplete)
    }

    fun onShowSolutionClicked() {
        showSolution.set(!showSolution.get())
    }

}

class QuestionVM(
    val activityVM: QuizActivityVM,
    val actionText: ObservableField<String> = ObservableField(),
    var actionFunction: Int = 0,
    var questionIndex: Int = 0,
    var questionImage: String = "",
    var questionText: String = "",
    val optionVMList: MutableList<OptionVM> = mutableListOf(),
    var correctAnswerPoint: Int = 0,
    var id: String,
    var isLastQuestion: Boolean = false,
    var solutionImage: String = "",
    var solutionText: String = ""
) {


    fun onActionButtonClick() {
        activityVM.onSubmitClicked(this)
    }
}


class OptionVM(
    val activityVM: QuizActivityVM,
    val textOptionBackground: ObservableInt = ObservableInt(),
    val imageOptionBackground: ObservableInt = ObservableInt(),
    var optionIndex: Int = 0,
    var isOptionAvailable: Boolean = false,
    var isOptionSelectable: ObservableBoolean = ObservableBoolean(false),
    var optionImage: String = "",
    var optionText: String = "",
    var isCorrect: Boolean = false,
    var optionImageIndexAlphabets: String = when (optionIndex) {
        1 -> "A"
        2 -> "B"
        3 -> "C"
        else -> "D"
    }

) {


    fun onOptionClick() {
        setSelectedBG()
        activityVM.onItemClicked(this)
    }

    fun setDefaultBG() {
        imageOptionBackground.set(android.R.color.transparent)
        textOptionBackground.set(R.drawable.round_quiz_option_bg)
    }

    fun setSelectedBG() {
        imageOptionBackground.set(R.color.quiz_yellow)
        textOptionBackground.set(R.drawable.round_quiz_option_selected_bg)
    }

    fun setCorrectBG() {
        imageOptionBackground.set(R.color.quiz_green)
        textOptionBackground.set(R.drawable.round_quiz_option_correct_bg)
    }
}


/*
class OptionSelectionFlagsVM(
    val textOptionBackground: ObservableInt = ObservableInt(0),
    val imageOptionBackground: ObservableInt = ObservableInt(0),
    val isClickActive: ObservableBoolean = ObservableBoolean(),
    val questionIndex: Int,
    val optionIndex: Int
)*/

