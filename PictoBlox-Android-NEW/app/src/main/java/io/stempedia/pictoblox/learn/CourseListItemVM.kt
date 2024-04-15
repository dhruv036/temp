package io.stempedia.pictoblox.learn

class CourseListItemVM(
    val story: CourseManager.UserCourseStory,
    val courseListVM: CourseListVM
    /*val thumbReference: StorageReference,
    val isEnrolled: Boolean,
    val total_lessons: Int = 0,
    val completed_lessons: Int = 0,
    val courseFlow: CourseFlow*/
) {

    fun onCourseClicked() {
        if (story.status == "BETA") {
            courseListVM.onCourseClicked(story.courseFlow)

        } else {
            courseListVM.showWIP()
        }
    }

}