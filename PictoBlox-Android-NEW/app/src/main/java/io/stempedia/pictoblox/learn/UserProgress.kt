package io.stempedia.pictoblox.learn

class UserProgress {

    var course_enrolled: Map<String, CourseProgress>? = null

    class CourseProgress {
        var completed_lessons = 0
        var total_lessons = 0
    }
}