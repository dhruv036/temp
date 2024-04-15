package io.stempedia.pictoblox.firebase

import android.os.Parcel
import android.os.Parcelable

class CourseFlow2 {

    lateinit var course: Course
    lateinit var lesson: Lesson
    var nextLesson: Lesson? = null

    class Course(
        val id: String,
        val title: String,
        val courseId: String
    )

    class Lesson(
        val id: String,
        val index: Int,
        val title: String,
        val isLast: Boolean,
        val isUnlocked: Boolean,
        val isCompleted: Boolean
    )
}