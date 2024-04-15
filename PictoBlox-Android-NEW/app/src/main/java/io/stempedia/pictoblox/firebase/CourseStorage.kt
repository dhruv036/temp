package io.stempedia.pictoblox.firebase

import android.os.Parcel
import android.os.Parcelable
import io.reactivex.Completable
import io.reactivex.Single
import io.stempedia.pictoblox.connectivity.StorageHandler
import io.stempedia.pictoblox.connectivity.StorageType
import io.stempedia.pictoblox.learn.CourseContentTypes
import java.io.File
import java.io.FileOutputStream

class CourseStorage(val storageHandler: StorageHandler) {
    private val courseParentDir = storageHandler.getCourseDir()

    fun makeCourseDir(courseId: String) = File(courseParentDir, courseId).apply {
        if (!exists())
            mkdirs()
    }

    fun builder() = Builder(courseParentDir)

    inner class Builder(private val parent: File) {

        fun course(id: String): Course {
            return Course(parent, id)
        }
    }

    inner class Course(parent: File, id: String) : DirBuilder(parent, id) {

        fun intro() = IntroConclusionTypeBuilder(build(), "intro_assets")

        fun conclusion() = IntroConclusionTypeBuilder(build(), "conclusion_assets")

        fun lessons() = Lessons(build())

    }

    inner class Lessons(parent: File) : DirBuilder(parent, "lesson_assets") {

        fun intro() = IntroConclusionTypeBuilder(build(), "intro_assets")

        fun conclusion() = IntroConclusionTypeBuilder(build(), "conclusion_assets")

        fun overview() = OverviewTypeBuilder(build(), "task_overview_assets")

        fun step() = LessonContentTypeBuilder(build(), "task_steps_assets")

        fun completionFiles() = CompletedLessonFileBuilder(build(), "completed_lessons")

        fun quiz() = Quiz(build())

    }

    inner class Quiz(parent: File) : DirBuilder(parent, "quiz_assets") {
        fun question() = QuizQuestionBuilder(build(), "question_image")
        fun solution() = QuizSolutionBuilder(build(), "solution_image")
        fun option() = QuizOptionBuilder(build(), "option_image")

    }

    abstract class DirBuilder(val parent: File, val segment: String) {
        fun build() = File(parent, segment)
    }

    class IntroConclusionTypeBuilder(parent: File, id: String) : DirBuilder(parent, id) {
        fun build(step: Int): Asset {
            val file = File(build(), "$step.png")
            return Asset(file.absolutePath)
        }

        fun build(step: Int, index: Int): Asset {
            val file = File(build(), "${step}_${index}.png")
            return Asset(file.absolutePath)
        }

        inner class Asset(val imagePath: String)
    }

    class LessonContentTypeBuilder(parent: File, id: String) : DirBuilder(parent, id) {

        fun build(step: Int, index: Int, contentTypes: CourseContentTypes): Asset {

            val extension = if (contentTypes == CourseContentTypes.GIF) {
                "gif"
            } else {
                "png"
            }

            val file = File(build(), "${step}_${index}.$extension")
            return Asset(file.absolutePath)
        }

        fun build(step: Int): Asset2 {
            val file1 = File(build(), "${step}.sb3")
            val file2 = File(build(), "${step}.json")
            return Asset2(file1.absolutePath, file2.absolutePath)
        }

        inner class Asset(val gifPath: String)
        inner class Asset2(val startingSb3Path: String, val verificationJsonPath: String)
    }

    class OverviewTypeBuilder(parent: File, id: String) : DirBuilder(parent, id) {

        fun build(step: Int): Asset {
            val file1 = File(build(), "${step}.gif")
            val file2 = File(build(), "${step}.mp3")
            return Asset(file1.absolutePath, file2.absolutePath)
        }

        inner class Asset(val gifPath: String, val audioPath: String)

    }

    class CompletedLessonFileBuilder(parent: File, segment: String) : DirBuilder(parent, segment) {

        init {
            val lessonDir = build()
            if (!lessonDir.exists()) {
                lessonDir.mkdirs()
            }
        }

        fun build(step: Int): Asset {
            val file = File(build(), "${step}.sb3")
            return Asset(file.absolutePath)
        }

        inner class Asset(val sb3Path: String)
    }


    class QuizSolutionBuilder(parent: File, segment: String) : DirBuilder(parent, segment) {

        fun build(lessonIndex: Int, questionIndex: Int): Asset {
            val file = File(build(), "${lessonIndex}_${questionIndex}.png")
            return Asset(file.absolutePath)
        }

        inner class Asset(val solutionImage: String)
    }

    class QuizQuestionBuilder(parent: File, segment: String) : DirBuilder(parent, segment) {


        fun build(lessonIndex: Int, questionIndex: Int): Asset {
            val file = File(build(), "${lessonIndex}_${questionIndex}.png")
            return Asset(file.absolutePath)
        }

        inner class Asset(val questionImage: String)
    }

    class QuizOptionBuilder(parent: File, segment: String) : DirBuilder(parent, segment) {

        fun build(lessonIndex: Int, questionIndex: Int, optionIndex: Int): Asset {
            val file = File(build(), "${lessonIndex}_${questionIndex}_${optionIndex}.png")
            return Asset(file.absolutePath)
        }

        inner class Asset(val optionImage: String)
    }
}



