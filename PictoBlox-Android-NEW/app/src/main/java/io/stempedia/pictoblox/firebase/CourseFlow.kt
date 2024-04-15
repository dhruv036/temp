package io.stempedia.pictoblox.firebase

import android.os.Parcel
import android.os.Parcelable

const val COURSE_FLOW = "parcelable_course"

class CourseFlow() : Parcelable {
    lateinit var course: Course
    lateinit var lesson: Lesson
    var nextLesson: Lesson? = null

    constructor(parcel: Parcel) : this() {
        parcel.readParcelable<Course>(Course::class.java.classLoader)?.also {
            course = it
        }
        parcel.readParcelable<Lesson>(Lesson::class.java.classLoader)?.also {
            lesson = it
        }

        nextLesson = parcel.readParcelable(Lesson::class.java.classLoader)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(course, flags)
        if (this::lesson.isInitialized) {
            parcel.writeParcelable(lesson, flags)
        }
        parcel.writeParcelable(nextLesson, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CourseFlow> {
        override fun createFromParcel(parcel: Parcel): CourseFlow {
            return CourseFlow(parcel)
        }

        override fun newArray(size: Int): Array<CourseFlow?> {
            return arrayOfNulls(size)
        }
    }


}

class Course(val id: String, val title: String, val courseId: String) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )


    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(title)
        parcel.writeString(courseId)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Course> {
        override fun createFromParcel(parcel: Parcel): Course {
            return Course(parcel)
        }

        override fun newArray(size: Int): Array<Course?> {
            return arrayOfNulls(size)
        }
    }

}

class Lesson(
    val id: String,
    val index: Int,
    val title: String,
    val isLast: Boolean,
    val isUnlocked: Boolean,
    val isCompleted: Boolean

) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeInt(index)
        parcel.writeString(title)
        parcel.writeByte(if (isLast) 1 else 0)
        parcel.writeByte(if (isUnlocked) 1 else 0)
        parcel.writeByte(if (isCompleted) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Lesson> {
        override fun createFromParcel(parcel: Parcel): Lesson {
            return Lesson(parcel)
        }

        override fun newArray(size: Int): Array<Lesson?> {
            return arrayOfNulls(size)
        }
    }

}
