package io.stempedia.pictoblox.learn.lessons


class LessonItemVM(
    val activityViewModel: LessonsListVM,
    val lessonIndex: Int,
    val lessonResource: Int,
    val lessonRating: Long,
    val isLessonUnlocked: Boolean,
    val isLessonCompleted: Boolean,
    val lessonId: String,
    val lessonTitle: String,
    val isLast: Boolean

) {
    val lessonIndexString = lessonIndex.toString()
    var vh: LessonsListActivity.LessonViewHolder? = null

    fun setVH(vh: LessonsListActivity.LessonViewHolder) {
        this.vh = vh
    }

    fun onLessonClicked() {
        vh?.also {
            //activityViewModel.onLessonClicked(lessonId, lessonIndex, lessonTitle, isLessonUnlocked, isLessonCompleted, isLast)
            activityViewModel.onLessonClicked2(it.adapterPosition)

        }
    }
}