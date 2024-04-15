package io.stempedia.pictoblox.learn

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.*
import com.google.firebase.storage.FileDownloadTask
import com.google.firebase.storage.StorageReference
import com.google.gson.GsonBuilder
import io.reactivex.*
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Cancellable
import io.stempedia.pictoblox.account.AccountHelper
import io.stempedia.pictoblox.connectivity.StorageHandler
import io.stempedia.pictoblox.firebase.Course
import io.stempedia.pictoblox.firebase.CourseFlow
import io.stempedia.pictoblox.firebase.CourseStorage
import io.stempedia.pictoblox.firebase.Lesson
import io.stempedia.pictoblox.util.PictobloxLogger
import io.stempedia.pictoblox.util.SPManager
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import kotlin.Pair as Pair1

class CourseManager(val context: Context) {
    private val accountHelper = AccountHelper()
    private val storageHandler = StorageHandler(context, SPManager(context))
    private val courseStorage = CourseStorage(storageHandler)

    fun getQuizOfLesson(courseFlow: CourseFlow): Single<UserQuizStory> {

        return Single.create { emitter ->

            val quizData = accountHelper.getLessons(courseFlow.course.id).document(courseFlow.lesson.id).get(Source.CACHE)
            val userData = accountHelper.getUserProgress2().collection("courses").document(courseFlow.course.id).get(Source.CACHE)

            Tasks.whenAll(quizData, userData)
                .addOnSuccessListener {

                    val questions = mutableListOf<QuizQuestion>()

                    val quizAssetBuilder = courseStorage.builder()
                        .course(courseFlow.course.id)
                        .lessons()
                        .quiz()

                    var isQuizCompleted = false
                    var totalQuizPointsEarned = 0

                    userData.result?.get("lessons.${courseFlow.lesson.id}.quiz")?.also {
                        (it as Map<String, Any>).also { map ->
                            isQuizCompleted = map["is_completed"] as Boolean
                            totalQuizPointsEarned = (map["total_points_earned"] as Long).toInt()

                        }
                    }


                    quizData.result?.get("quiz")?.also { quiz ->

                        (quiz as List<Map<String, Any>>).forEachIndexed { questionIndex, question ->

                            //Start with getting question id
                            val q = QuizQuestion()
                            q.id = question["id"] as String

                            //Get user progress on the question
                            userData.result?.get("lessons.${courseFlow.lesson.id}.quiz.${q.id}")?.also {

                                (it as Map<String, Any>).also { map ->
                                    q.attempt = QuizQuestionAttempt(
                                        map["option_selected"] as Long,
                                        map["is_correct"] as Boolean,
                                        map["points_earned"] as Long
                                    )
                                }


                            } ?: run {
                                q.attempt = null
                            }

                            //Question detail
                            q.correctAnswerPoints = question["points"] as Long
                            q.questionText = question["question_text"] as String
                            q.solutionText = question["solution_text"] as String
                            q.correctOptionIndex = question["correct_option"] as Long

                            val qi = question["question_image"] as String
                            val si = question["solution_image"] as String

                            q.questionImage = if (qi.isNotEmpty()) {
                                quizAssetBuilder.question().build(courseFlow.lesson.index, questionIndex + 1).questionImage

                            } else {
                                ""
                            }

                            q.solutionImage = if (si.isNotEmpty()) {
                                quizAssetBuilder.question().build(courseFlow.lesson.index, questionIndex + 1).questionImage

                            } else {
                                ""
                            }

                            //option list
                            (question["option"] as List<Map<String, Any>>).forEachIndexed { i, option ->

                                if (option["text"] != null) {
                                    q.options[i].text = option["text"] as String

                                } else if (option["image"] != null) {
                                    q.options[i].image = quizAssetBuilder
                                        .option()
                                        .build(courseFlow.lesson.index, questionIndex + 1, i + 1)
                                        .optionImage
                                }

                                q.options[i].isCorrect = i == q.correctOptionIndex.toInt()

                                q.options[i].isAvailable = true
                            }

                            questions.add(q)
                        }
                    }

                    emitter.onSuccess(UserQuizStory(isQuizCompleted, totalQuizPointsEarned, questions))

                }


                .addOnFailureListener {
                    emitter.onError(it)
                }


        }


    }

    fun getLessonCompletionFile(courseFlow: CourseFlow): Single<File> {
        return Single.create { emitter ->
            emitter.onSuccess(
                File(
                    courseStorage.builder()
                        .course(courseFlow.course.id)
                        .lessons()
                        .completionFiles()
                        .build(courseFlow.lesson.index)
                        .sb3Path
                )
            )
        }
    }

    fun saveCompletedLessonFile(courseFlow: CourseFlow, byteArray: ByteArray): Completable {
        return getLessonCompletionFile(courseFlow)
            .flatMapCompletable {
                storageHandler.saveFile(it, byteArray)
            }
    }

    //TODO we'll have to figure out hoe to do this without doing the API call.
    fun setLessonTaskRetry(attempts: Int, courseFlow: CourseFlow): Completable {
        return Completable.create { emitter ->

            val ref = accountHelper.getUserProgressOfCourse(courseFlow.course.id)

            ref.get(Source.CACHE)
                .addOnSuccessListener { snapshot ->
                    snapshot?.get("lessons")?.also {

                        (it as Map<String, Map<String, Any>?>)[courseFlow.lesson.id]?.also { map ->

                            if (!(map["is_completed"] as Boolean)) {
                                ref.update(mapOf("lessons.${courseFlow.lesson.id}.attempts" to attempts))
                            }
                        }
                    }

                    emitter.onComplete()

                }
                .addOnFailureListener {
                    emitter.onError(it)
                }
        }
    }

    fun setLessonTaskCompleted(attempts: Int, courseFlow: CourseFlow): Completable {

        return Completable.create { emitter ->
            accountHelper.getUserProgress(courseFlow.course.id).get(Source.CACHE)
                .addOnSuccessListener { snapShot ->

                    val isCompleted = snapShot.get("lessons.${courseFlow.lesson.id}.tasks.is_completed") as Boolean

                    if (!isCompleted) {
                        val rating = when {
                            attempts < 2 -> 3
                            attempts < 4 -> 2
                            else -> 1
                        }

                        val map = mapOf(
                            "lessons.${courseFlow.lesson.id}.tasks.attempts" to attempts,
                            "lessons.${courseFlow.lesson.id}.tasks.is_completed" to true,
                            "lessons.${courseFlow.lesson.id}.tasks.points_earned" to rating
                        )


                        accountHelper.getUserProgressOfCourse(courseFlow.course.id).update(map)
                            .addOnSuccessListener {
                                emitter.onComplete()
                            }
                            .addOnFailureListener {
                                emitter.onError(it)
                            }


                    }
                }
                .addOnFailureListener {
                    emitter.onError(it)
                }
        }

    }

    fun setLessonCompleted(courseFlow: CourseFlow, totalQuizPointEarned: Int): Completable {
        return Single.create<DocumentSnapshot> { emitter ->
            accountHelper.getUserProgress(courseFlow.course.id).get(Source.CACHE)
                .addOnFailureListener {
                    emitter.onError(it)
                }
                .addOnSuccessListener {
                    emitter.onSuccess(it)
                }


        }.flatMapCompletable { userProgressDoc ->
            Completable.create { emitter ->

                val taskList = mutableListOf<Task<Void>>()

                // completed lesson count update at course level
                taskList.add(
                    accountHelper.getUserProgress2().update(
                        mapOf(
                            "course_enrolled.${courseFlow.course.id}.completed_lessons" to FieldValue.increment(
                                1
                            )
                        )
                    )
                )

                if (courseFlow.lesson.isLast) {//If this lesson is the last lesson
                    taskList.add(
                        accountHelper.getUserProgress2().update(mapOf("course_enrolled.${courseFlow.course.id}.status" to "COMPLETED"))
                    )
                }

                val lessonRating: Long = userProgressDoc.get("lessons.${courseFlow.lesson.id}.tasks.points_earned")?.let {
                    it as Long
                } ?: kotlin.run {
                    0L
                }


                //Lesson flag updates
                val totalPointsEarned = lessonRating + totalQuizPointEarned

                val currentLessonMap = mapOf(
                    "lessons" to mapOf(
                        courseFlow.lesson.id to mapOf(
                            "is_completed" to true,
                            "total_points_earned" to totalPointsEarned
                        )
                    )
                )

                taskList.add(accountHelper.getUserProgressOfCourse(courseFlow.course.id).set(currentLessonMap, SetOptions.merge()))


                //Next lesson flag updates
                courseFlow.nextLesson?.also {
                    if (!it.isUnlocked) {

                        val map1 = mapOf(
                            "lessons" to mapOf(
                                courseFlow.lesson.id to mapOf(
                                    "is_completed" to true,
                                    "total_points_earned" to 0
                                ),

                                it.id to mapOf(
                                    "final_sb3" to "",
                                    "tasks" to mapOf(
                                        "attempts" to 0,
                                        "points_earned" to 0,
                                        "is_completed" to false
                                    ),
                                    "quiz" to mapOf(
                                        "total_points_earned" to 0,
                                        "is_completed" to false
                                    )
                                )
                            )
                        )

                        taskList.add(accountHelper.getUserProgressOfCourse(courseFlow.course.id).set(map1, SetOptions.merge()))

                    }

                }

                Tasks.whenAll(taskList)
                    .addOnSuccessListener {
                        emitter.onComplete()
                    }
                    .addOnFailureListener {
                        emitter.onError(it)
                    }


            }
        }
    }

    fun setLessonQuizAnswer(
        courseFlow: CourseFlow,
        questionId: String,
        isCorrect: Boolean,
        optionSelected: Int,
        pointsEarned: Int,
        isQuizCompleted: Boolean,
        totalPointsEarned: Int
    ): Completable {

        return Completable.create { emitter ->

            val taskList = mutableListOf<Task<Void>>()

            val quizFlags = mapOf(
                "lessons" to mapOf(
                    courseFlow.lesson.id to mapOf(
                        "quiz" to mapOf(
                            "is_completed" to isQuizCompleted,
                            "total_points_earned" to totalPointsEarned,
                            questionId to mapOf(
                                "is_correct" to isCorrect,
                                "option_selected" to optionSelected,
                                "points_earned" to pointsEarned
                            )
                        )
                    )
                )
            )

            taskList.add(accountHelper.getUserProgress2().collection("courses").document(courseFlow.course.id).set(quizFlags, SetOptions.merge()))


            Tasks.whenAll(taskList)
                .addOnSuccessListener {
                    emitter.onComplete()
                }
                .addOnFailureListener {
                    emitter.onError(it)
                }


        }
    }


/*
    fun registerLessonCompleted(attempts: Int, courseFlow: CourseFlow): Completable {

        return getAllLessonsForCourse(courseFlow).flatMapCompletable { lessonStories ->

            Completable.create { emitter ->

                val index = lessonStories.indexOfFirst { it.id == courseFlow.lesson.id }

                if (index == -1) {
                    emitter.onError(NoSuchElementException("${courseFlow.lesson.id} does not exists"))
                    return@create
                }

                val story = lessonStories[index]
                val taskList = mutableListOf<Task<Void>>()

                if (story.isUnlocked && !story.isCompleted) {

                    val rating = when {
                        attempts < 2 -> 3
                        attempts < 4 -> 2
                        else -> 1
                    }

                    val map = mapOf(
                        "lessons.${courseFlow.lesson.id}.tasks.attempts" to attempts,
                        "lessons.${courseFlow.lesson.id}.tasks.is_completed" to true,
                        "lessons.${courseFlow.lesson.id}.tasks.points_earned" to rating
                    )


                    taskList.add(accountHelper.getUserProgressOfCourse(courseFlow.course.id).update(map))
                }

                if (index < lessonStories.size - 1) {
                    val nextLesson = lessonStories[index + 1]
                    if (!nextLesson.isUnlocked) {

                        val map1 = mapOf(
                            "lessons" to mapOf(
                                nextLesson.id to mapOf(
                                    "final_sb3" to "",
                                    "tasks" to mapOf(
                                        "attempts" to 0,
                                        "points_earned" to "",
                                        "is_completed" to false
                                    ),
                                    "quiz" to mapOf(
                                        "total_points_earned" to 0,
                                        "is_completed" to false
                                    )
                                )
                            )
                        )

                        taskList.add(accountHelper.getUserProgressOfCourse(courseFlow.course.id).set(map1, SetOptions.merge()))

                    }
                }

                Tasks.whenAll(taskList)
                    .addOnSuccessListener {
                        emitter.onComplete()
                    }
                    .addOnFailureListener {
                        emitter.onError(it)
                    }


            }
        }
    }
*/

    fun getLessonsMeta(courseFlow: CourseFlow): Single<UserLessonsMetaStory> {
        return Single.create { emitter ->
            val userProgressTask = accountHelper.getUserProgress2().get(Source.CACHE)

            userProgressTask
                .addOnSuccessListener { snapshot ->

                    snapshot.get("course_enrolled.${courseFlow.course.id}.total_lesson_score")?.also {
                        val score = it as Int
                        emitter.onSuccess(UserLessonsMetaStory(score))

                    } ?: kotlin.run {
                        emitter.onSuccess(UserLessonsMetaStory(0))
                    }

                }
                .addOnFailureListener {
                    emitter.onError(it)
                }
        }
    }

    private fun getLessons(courseFlow: CourseFlow) = Observable.create<QuerySnapshot> { emitter ->

        val registration = accountHelper.getCourse(courseFlow.course.id).collection("lessons")
            .addSnapshotListener { snapshot, firebaseFirestoreException ->

                if (firebaseFirestoreException != null) {
                    emitter.onError(firebaseFirestoreException)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    emitter.onNext(snapshot)
                }
            }

        emitter.setCancellable {
            registration.remove()
        }
    }


    private fun getUserProgressOfLessons(courseFlow: CourseFlow) = Observable.create<DocumentSnapshot> { emitter ->

        val registration = accountHelper.getUserProgress(courseFlow.course.id)
            .addSnapshotListener { snapshot, firebaseFirestoreException ->

                if (firebaseFirestoreException != null) {
                    emitter.onError(firebaseFirestoreException)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    emitter.onNext(snapshot)
                }
            }

        emitter.setCancellable {
            registration.remove()
        }
    }


    fun getAllLessonsForCourse(courseFlow: CourseFlow): Observable<List<UserLessonStory>> {

        return Observable.combineLatest(
            getLessons(courseFlow),
            getUserProgressOfLessons(courseFlow),
            BiFunction<QuerySnapshot, DocumentSnapshot, List<UserLessonStory>> { t1, t2 ->
                val itemVMList = mutableListOf<UserLessonStory>()

                t1.forEachIndexed { index, lesson ->
                    kotlin.run {

                        val title = lesson["title"] as String

                        t2.get("lessons.${lesson.id}")?.also {

                            val isCompleted = t2.get("lessons.${lesson.id}.tasks.is_completed") as Boolean
                            val lessonRating = t2.get("lessons.${lesson.id}.tasks.points_earned") as Long
                            val attempts = t2.get("lessons.${lesson.id}.tasks.attempts") as Long

                            itemVMList.add(
                                UserLessonStory(lesson.id, index + 1, lessonRating ?: 0, true, isCompleted ?: false, attempts ?: 0, title)
                            )

                        } ?: kotlin.run {
                            itemVMList.add(
                                UserLessonStory(lesson.id, index + 1, 0, false, false, 0, title)
                            )
                        }
                    }
                }

                itemVMList
            })


/*        return Single.create<List<UserLessonStory>> { emitter ->

            val lessonsTask = accountHelper.getCourse(courseFlow.course.id).collection("lessons").get(Source.CACHE)
            val userLessonProgressTask = accountHelper.getUserProgress(courseFlow.course.id).get(Source.CACHE)


            Tasks.whenAll(lessonsTask, userLessonProgressTask)
                .addOnSuccessListener {
                    val itemVMList = mutableListOf<UserLessonStory>()

                    lessonsTask.result?.forEachIndexed { index, lesson ->
                        kotlin.run {
                            val title = lesson["title"] as String

                            userLessonProgressTask.result?.get("lessons.${lesson.id}")?.also {

                                val isCompleted = userLessonProgressTask.result?.get("lessons.${lesson.id}.tasks.is_completed") as Boolean?
                                val lessonRating = userLessonProgressTask.result?.get("lessons.${lesson.id}.tasks.points_earned") as Long?
                                val attempts = userLessonProgressTask.result?.get("lessons.${lesson.id}.tasks.attempts") as Long?

                                itemVMList.add(
                                    UserLessonStory(lesson.id, index + 1, lessonRating ?: 0, true, isCompleted ?: false, attempts ?: 0, title)
                                )

                            } ?: kotlin.run {
                                itemVMList.add(
                                    UserLessonStory(lesson.id, index + 1, 0, false, false, 0, title)
                                )
                            }
                        }
                    }

                    emitter.onSuccess(itemVMList)


                }
                .addOnFailureListener {
                    emitter.onError(it)
                }
        }*/
    }

/*
    fun getUserLessonOfCourse(courseId: String, lesson: Lesson): Single<UserLessonStory> {
        return Single.create<UserLessonStory> { emitter ->

            val lessonsTask = accountHelper.getCourse(courseId).collection("lessons").document(lesson.id).get()
            val userProgressTask = accountHelper.getUserProgress(courseId).get()

            Tasks.whenAll(lessonsTask, userProgressTask)
                .addOnSuccessListener {

                    val userProgress = userProgressTask.result?.get("lessons")?.let { it as Map<String, Map<String, Any>?> }

                    lessonsTask.result?.also { snapshot ->

                        val title = snapshot["title"] as String

                        val vm = userProgress?.get(lesson.id)?.also {

                            val isCompleted = it["is_completed"] as Boolean
                            val lessonRating = it["task_points_earned"] as Long
                            val attempts = it["attempts"] as Long

                            UserLessonStory(lesson.id, 0, lessonRating, true, isCompleted, attempts, title)

                        } ?: kotlin.run {
                            UserLessonStory(lesson.id, 0, 0, false, false, 0, title)
                        }
                    }


                    emitter.onSuccess(itemVMList)


                }
                .addOnFailureListener {
                    emitter.onError(it)
                }
        }
    }
*/


    fun checkIfOfflineAccessIsPossible(courseFlow: CourseFlow, spManager: SPManager): Completable {
        return Completable.create { emitter ->
            kotlin.run {
                val course = accountHelper.getCourse(courseFlow.course.id).get(Source.CACHE)
                val courseIntro = accountHelper.getCourse(courseFlow.course.id).collection("intro_stories").get(Source.CACHE)
                val lessons = accountHelper.getCourse(courseFlow.course.id).collection("lessons").get(Source.CACHE)

                Tasks.whenAll(course, courseIntro, lessons)
                    .addOnSuccessListener {
                        if (course.result != null && courseIntro.result != null && lessons.result != null) {
                            val courseDir = courseStorage.builder().course(courseFlow.course.id).build()

                            if (spManager.checkCourseAssetEntryExist(courseFlow.course.id) && courseDir.exists() && courseDir.listFiles()?.isNotEmpty() == true) {
                                emitter.onComplete()

                            } else {
                                emitter.onError(Exception(""))
                            }
                        } else {
                            emitter.onError(Exception(""))
                        }
                    }
                    .addOnFailureListener {
                        emitter.onError(Exception(""))
                    }
            }
        }
    }

    fun fetchCourseContentForOfflineAccess(courseFlow: CourseFlow): Completable {

        return Completable.create { emitter ->
            run {

                val course = accountHelper.getCourse(courseFlow.course.id).get(Source.SERVER)
                val courseIntro = accountHelper.getCourse(courseFlow.course.id).collection("intro_stories").get(Source.SERVER)
                val lessons = accountHelper.getCourse(courseFlow.course.id).collection("lessons").get(Source.SERVER)

                accountHelper.getUserProgress2().get()

                Tasks.whenAll(course, courseIntro, lessons)
                    .addOnSuccessListener {
                        if (course.result != null && courseIntro.result != null && lessons.result != null) {
                            emitter.onComplete()

                        } else {
                            emitter.onError(java.lang.Exception())
                        }
                    }
                    .addOnFailureListener {
                        emitter.onError(java.lang.Exception())
                    }
            }
        }
    }

    fun getCourses2(): Observable<List<UserCourseStory>> {
        return Observable.zip(
            getAllCourses2(),
            getUserProgress2(),
            BiFunction<List<DocumentSnapshot>, Map<String, Map<String, Any>>, List<UserCourseStory>> { t1, t2 ->

                val list = mutableListOf<UserCourseStory>()
                //val map = t2.get("course_enrolled")?.let { map -> map as Map<String, Map<String, Any>> }

                t1.forEach {
                    val thumb = accountHelper.getCourseThumb(it["course_id"] as String, it["thumb_key"] as String)
                    val courseFlow = CourseFlow().apply {
                        course = Course(it.id, it["title"] as String, it["course_id"] as String)
                    }

                    val subTitle = it["sub_title"] as String
                    val status = it["version.status"] as String


                    val story = t2[it.id]?.let { map ->
                        UserCourseStory(thumb, true, map["total_lessons"] as Long, map["completed_lessons"] as Long, subTitle, courseFlow, status)

                    } ?: kotlin.run {
                        UserCourseStory(thumb, false, 0, 0, subTitle, courseFlow, status)

                    }

                    list.add(story)
                }

                return@BiFunction list
            }

        )

    }

    private fun getAllCourses2(): Observable<List<DocumentSnapshot>> {

        return Observable.create { emitter ->

            val query = accountHelper.getAllCourses()

            val reg = query.addSnapshotListener { p1, p2 ->
                if (p2 != null) {
                    emitter.onError(p2)
                    return@addSnapshotListener
                }

                p1?.documents?.also {
                    emitter.onNext(it)
                }
            }

            emitter.setCancellable {
                reg.remove()
            }

        }
    }

    private fun getUserProgress2(): Observable<Map<String, Map<String, Any>>> {
        return Observable.create { emitter ->

            if (!accountHelper.isLoggedIn()) {
                emitter.onNext(emptyMap())
                return@create
            }

            val query = accountHelper.getUserProgress2()

            val reg = query.addSnapshotListener { p1, p2 ->
                if (p2 != null) {
                    p2.printStackTrace()
                    //emitter.onError(p2)
                    emitter.onNext(emptyMap())
                    return@addSnapshotListener
                }

                p1?.also {

                    it.get("course_enrolled")?.let { map ->
                        emitter.onNext(map as Map<String, Map<String, Any>>)
                    } ?: kotlin.run {
                        emitter.onNext(emptyMap())
                    }


                }
            }

            emitter.setCancellable {
                reg.remove()
            }
        }
    }

    fun getCourseDetail(courseFlow: CourseFlow): Single<CourseDetailStory> {
        return Single.create { emitter ->
            accountHelper.getCourse(courseFlow.course.id)
                .get()
                .addOnSuccessListener {
                    try {

                        emitter.onSuccess(
                            CourseDetailStory(
                                accountHelper.getCourseThumb(it["course_id"] as String, it["thumb_for_detail_key"] as String),
                                it["title"] as String,
                                it["sub_title"] as String,
                                it["difficulty"] as String,
                                it["no_of_lessons"] as Long,
                                it["duration"] as String,
                                it["video_link"] as String,
                                it["description"] as String
                            )
                        )

                    } catch (e: java.lang.Exception) {
                        emitter.onError(e)
                    }

                }.addOnFailureListener {
                    emitter.onError(it)
                }
        }
    }

    fun fetchCourseAssets(documentId: String, courseId: String): Observable<Int> {
        return Observable.create(CourseObservableOnSubscribe(accountHelper, courseStorage, documentId, courseId))
    }

    fun enrollUserInACourse(noOfLessons: Long, courseFlow: CourseFlow): Completable {

        return Completable.create { emitter ->
            run {
                val courseProgress = mapOf(
                    "completed_lessons" to 0,
                    "total_lessons" to noOfLessons,
                    "is_story_fully_shown" to false,
                    "status" to "ONGOING"
                )

                //FYI, even though this is called at course detail level, the lesson in courseFLow will be filled with first lesson of the course by CourseDatailVM

                val firstLessonFlag = mapOf<String, Any>(
                    courseFlow.lesson.id to mapOf(
                        "final_sb3" to "",
                        "tasks" to mapOf(
                            "attempts" to 0,
                            "points_earned" to 0,
                            "is_completed" to false
                        ),
                        "quiz" to mapOf(
                            "total_points_earned" to 0,
                            "is_completed" to false
                        )
                    )
                )

                val courseLessonProgress = mapOf(
                    "course_id" to courseFlow.course.courseId,
                    "lessons" to firstLessonFlag
                )

                val courseProgressParent = mapOf("course_enrolled" to mapOf(courseFlow.course.id to courseProgress))

                val t1 = accountHelper.getUserProgress2().set(courseProgressParent, SetOptions.merge())

                val t2 = accountHelper.getUserProgressOfCourse(courseFlow.course.id).set(courseLessonProgress)

                Tasks.whenAll(t1, t2)
                    .addOnSuccessListener {
                        emitter.onComplete()
                    }
                    .addOnFailureListener {
                        emitter.onError(it)
                    }
            }
        }

    }

    fun processCourseIntroConclusion(fileBuilder: CourseStorage.IntroConclusionTypeBuilder, task: Task<QuerySnapshot>)
            : Single<List<CourseIntroConclusionStory>> {
        return Single.create<List<CourseIntroConclusionStory>> { emitter ->
            kotlin.run {
                task.addOnSuccessListener {

                    val list = mutableListOf<CourseIntroConclusionStory>()

                    try {
                        it.forEachIndexed { index, queryDocumentSnapshot ->
                            run {

                                val id = queryDocumentSnapshot.id
                                val type = CourseContentTypes.valueOf(queryDocumentSnapshot.getString("media_type")!!)
                                val text = queryDocumentSnapshot.getString("text_content")!!

                                val asset = fileBuilder.build(index + 1)

                                val story = CourseIntroConclusionStory(text, asset)

                                list.add(story)

                            }
                        }

                        emitter.onSuccess(list)

                    } catch (ex: Exception) {
                        emitter.onError(ex)
                    }
                }

                task.addOnFailureListener {
                    emitter.onError(it)
                }
            }
        }
    }

/*    fun processLessonIntroConclusion(fileBuilder: CourseStorage.IntroConclusionTypeBuilder, task: Task<DocumentSnapshot>, stepIndex: Int)
            : Single<List<LessonIntroConclusionStory>> {
        return Single.create<List<LessonIntroConclusionStory>> { emitter ->
            kotlin.run {
                task.addOnSuccessListener {

                    val list = mutableListOf<LessonIntroConclusionStory>()

                    try {
                        run {

                            task.result?.also {

                                val introArray = it.get("intro") as ArrayList<HashMap<String, String>>

                                //String parameter value is irrelevant it can contain only one type 'IMG'
                                introArray.forEachIndexed { i, string ->
                                    run {
                                        val asset = fileBuilder.build(stepIndex, i + 1)

                                        val story = LessonIntroConclusionStory(asset)

                                        list.add(story)
                                    }
                                }
                            }
                        }

                        emitter.onSuccess(list)

                    } catch (ex: Exception) {
                        emitter.onError(ex)
                    }
                }

                task.addOnFailureListener {
                    emitter.onError(it)
                }
            }
        }
    }*/

    fun getCourseIntros(documentId: String): Single<Pair1<Task<QuerySnapshot>, CourseStorage.IntroConclusionTypeBuilder>> = Single.create {

        try {
            val builder = courseStorage.builder()
                .course(documentId)
                .intro()

            it.onSuccess(
                Pair1(
                    accountHelper.getCourse(documentId)
                        .collection("intro_stories")
                        .get(Source.CACHE),
                    builder
                )
            )

        } catch (e: Exception) {
            it.onError(e)
        }
    }

    /*  fun getLessonIntros(documentId: String, lessonId: String): Single<Pair1<Task<DocumentSnapshot>, CourseStorage.IntroConclusionTypeBuilder>> =
          Single.create {

              try {
                  val builder = courseStorage.builder()
                      .course(documentId)
                      .lessons()
                      .intro()


                  it.onSuccess(
                      Pair1(
                          accountHelper.getLessons(documentId)
                              .document(lessonId)
                              .get(*//*Source.CACHE*//*),
                        builder
                    )
                )

            } catch (e: Exception) {
                it.onError(e)
            }
        }
*/
    fun getLessonIntrosConclusion(courseFlow: CourseFlow, isIntro: Boolean): Single<List<LessonIntroConclusionStory>> {
        return Single.create<List<LessonIntroConclusionStory>> { emitter ->
            kotlin.run {

                val lessons = courseStorage.builder()
                    .course(courseFlow.course.id)
                    .lessons()


                val builder = if (isIntro) {
                    lessons.intro()
                } else {
                    lessons.conclusion()
                }

                val tag = if (isIntro) {
                    "intro"
                } else {
                    "conclusion"
                }

                accountHelper.getLessons(courseFlow.course.id)
                    .document(courseFlow.lesson.id)
                    .get(Source.CACHE)
                    .addOnSuccessListener {

                        val list = mutableListOf<LessonIntroConclusionStory>()

                        try {
                            val itemArray = it.get(tag) as ArrayList<HashMap<String, String>>

                            //String parameter value is irrelevant it can contain only one type 'IMG'
                            itemArray.forEachIndexed { i, string ->
                                run {
                                    val asset = builder.build(courseFlow.lesson.index, i + 1)

                                    val story = LessonIntroConclusionStory(asset)

                                    list.add(story)
                                }
                            }



                            emitter.onSuccess(list)

                        } catch (ex: Exception) {
                            emitter.onError(ex)
                        }
                    }
                    .addOnFailureListener {
                        emitter.onError(it)
                    }
            }
        }
    }

    fun getLessonOverview(documentId: String, lessonId: String, lessonIndex: Int): Single<LessonOverviewStory> {
        return Single.create(object : SingleOnSubscribe<LessonOverviewStory> {
            override fun subscribe(emitter: SingleEmitter<LessonOverviewStory>) {

                accountHelper.getLessons(documentId)
                    .document(lessonId)
                    .get(Source.CACHE)
                    .addOnSuccessListener {

                        val map = it.get("task_overview")?.let { overview -> run { overview as HashMap<String, String> } }


                        map?.get("text_content")?.also { textContent ->
                            kotlin.run {

                                val asset = courseStorage.builder()
                                    .course(documentId)
                                    .lessons()
                                    .overview()
                                    .build(lessonIndex)

                                emitter.onSuccess(LessonOverviewStory(textContent, asset))
                            }
                        } ?: kotlin.run {
                            emitter.onError(java.lang.Exception("overview tag has error"))
                        }


                    }
                    .addOnFailureListener {
                        emitter.onError(it)
                    }
            }

        })
    }

    fun prepareLessonStepForPictbloxWeb(courseFlow: CourseFlow): Single<Triple<String, String, Long>> {

        return Single.create<Triple<String, String, Long>> { emitter ->
            run {

                val userProgressTask = accountHelper.getUserProgress(courseFlow.course.id).get(Source.CACHE)

                val lessonTask = accountHelper.getLessons(courseFlow.course.id)
                    .document(courseFlow.lesson.id)
                    .get(Source.CACHE)


                Tasks.whenAll(userProgressTask, lessonTask)
                    .addOnSuccessListener {
                        run {

                            var attempts = 0L

                            userProgressTask.result?.get("lessons.${courseFlow.lesson.id}.tasks.attempts")?.let {
                                attempts = it as Long
                            }


                            val model = LessonStepsForPictobloxWeb()
                            model.name = "ABCD"


                            val builder = courseStorage.builder()
                                .course(courseFlow.course.id)
                                .lessons()
                                .step()



                            lessonTask.result?.get("task_steps")?.also { steps ->

                                (steps as ArrayList<Map<String, Any>>).forEachIndexed { i, map ->
                                    run {

                                        val type = CourseContentTypes.valueOf(map["type"] as String)

                                        val step = LessonStep()
                                        //step.image = "step1"
                                        step.title = map["text"] as String
                                        step.imagePath = builder.build(courseFlow.lesson.index, i + 1, type).gifPath
                                        step.isFullScreen = map["size"] as Boolean
                                        model.steps.add(step)
                                    }
                                }

                            }

                            val asset2 = builder.build(courseFlow.lesson.index)


                            val modelJson = GsonBuilder().create().toJson(model)
                            var completeJson = ""

                            FileInputStream(File(asset2.verificationJsonPath)).use {
                                val obj = JSONObject(modelJson)
                                obj.put("verificationLogic", JSONObject(String(it.readBytes())))
                                completeJson = obj.toString()

                            }

                            //val tempPath = "/data/user/0/io.stempedia.pictoblox/files/cached/CachedSb3File.sb3"


                            emitter.onSuccess(Triple(asset2.startingSb3Path, completeJson, attempts))

                        }

                    }
                    .addOnFailureListener {
                        emitter.onError(it)
                        it.printStackTrace()
                    }
            }
        }


    }

    /**
     * Fills lesson detain in courseflow, so nothing is returned.
     */
    fun getFirstLessonOfCourse(courseFlow: CourseFlow): Completable {
        return Completable.create { emitter ->
            kotlin.run {
                accountHelper.getCourse(courseFlow.course.id).collection("lessons").get(Source.SERVER)
                    .addOnSuccessListener {
                        try {
                            it.documents.first().also { doc ->
                                courseFlow.lesson = Lesson(doc.id, 1, doc["title"] as String, it.documents.size == 1, false, false)
                                emitter.onComplete()
                            }

                        } catch (e: java.lang.Exception) {
                            emitter.onError(java.lang.Exception("Failed to retrive course Data, please check your connection"))
                        }
                    }

                    .addOnFailureListener {
                        emitter.onError(it)
                    }
            }

        }
    }

    class CourseObservableOnSubscribe(
        private val accountHelper: AccountHelper,
        courseStorage: CourseStorage,
        documentId: String,
        private val courseId: String
    ) :
        ObservableOnSubscribe<Int>, Cancellable {
        private val courseDir = courseStorage.makeCourseDir(documentId)
        private val courseZipFile = File(courseDir, "assets.zip")
        private val fileDownloadTask: FileDownloadTask by lazy {
            accountHelper.getCourseAssets(courseId).getFile(courseZipFile)
        }
        private var isZipExtractionInProgress = false
        private var cancelZipExtraction = false

        override fun subscribe(emitter: ObservableEmitter<Int>) {
            emitter.setCancellable(this)
            fileDownloadTask.addOnSuccessListener {
                //just to be safe if rounding leave it at 99%
                emitter.onNext(100)
                extractAssetsFromZip()

                emitter.onComplete()
            }

            fileDownloadTask.addOnFailureListener {
                emitter.onError(it)
            }

            fileDownloadTask.addOnProgressListener {
                val percentage = (it.bytesTransferred * 100.0f) / it.totalByteCount
                emitter.onNext(percentage.toInt())
            }
        }

        override fun cancel() {
            if (fileDownloadTask.isInProgress) {
                fileDownloadTask.cancel()

            } else if (isZipExtractionInProgress) {
                cancelZipExtraction = true
            }
        }

        private fun extractAssetsFromZip() {
            isZipExtractionInProgress = true

            ZipInputStream(courseZipFile.inputStream()).use { inputStream ->
                var zipEntry = inputStream.nextEntry

                val buffer = ByteArray(1024)

                while (zipEntry != null && !cancelZipExtraction) {

                    PictobloxLogger.getInstance().logd("Unzip____ ${zipEntry.name}")

                    val entry = File(courseDir, zipEntry.name)

                    if (zipEntry.isDirectory) {

                        if (!entry.exists()) {
                            entry.mkdirs()
                        }

                    } else {
                        entry.parentFile?.also {
                            if (!it.exists())
                                it.mkdirs()
                        }

                        BufferedOutputStream(FileOutputStream(entry)).use {

                            var read = inputStream.read(buffer)
                            while (read != -1) {
                                it.write(buffer, 0, read)
                                read = inputStream.read(buffer)
                            }
                        }
                    }

                    inputStream.closeEntry()

                    zipEntry = inputStream.nextEntry
                }

            }

            isZipExtractionInProgress = false
        }
    }

    data class CourseIntroConclusionStory(val text: String, val asset: CourseStorage.IntroConclusionTypeBuilder.Asset)
    data class LessonOverviewStory(val text: String, val asset: CourseStorage.OverviewTypeBuilder.Asset)
    data class LessonIntroConclusionStory(val asset: CourseStorage.IntroConclusionTypeBuilder.Asset)
    data class UserCourseStory(
        val thumbReference: StorageReference,
        val isEnrolled: Boolean,
        val totalLessons: Long = 0,
        val completedLessons: Long = 0,
        val courseSubTitle: String = "",
        val courseFlow: CourseFlow,
        val status: String = ""
    )

    data class UserLessonsMetaStory(val pointsEarned: Int)

    data class UserLessonStory(
        val id: String,
        val index: Int,
        val rating: Long,
        val isUnlocked: Boolean,
        val isCompleted: Boolean,
        val attempts: Long,
        val title: String
    )

    data class CourseDetailStory(
        val thumbReference: StorageReference,
        val title: String = "",
        val subTitle: String = "",
        val difficulty: String = "",
        val noOfLessons: Long = 0,
        val duration: String = "",
        val videoLink: String = "",
        val description: String = ""
    )

    data class UserQuizStory(
        val isCompleted: Boolean,
        val totalPointsEarned: Int,
        val questions: List<QuizQuestion>
    )

    data class QuizQuestion(
        var id: String = "",
        var options: List<Option> = listOf(Option(), Option(), Option(), Option()),
        var questionText: String = "",
        var questionImage: String = "",
        var solutionText: String = "",
        var solutionImage: String = "",
        var correctOptionIndex: Long = 0,
        var correctAnswerPoints: Long = 0,
        var attempt: QuizQuestionAttempt? = null
    )

    data class QuizQuestionAttempt(val optionSelected: Long, val isCorrect: Boolean, val pointEarned: Long)

    data class Option(var isAvailable: Boolean = false, var text: String = "", var image: String = "", var isCorrect: Boolean = false)

    inner class LessonStepsForPictobloxWeb {
        var name = ""
        var verificationLogic = ""
        var steps = mutableListOf<LessonStep>()
    }

    inner class LessonStep {
        var title = ""
        //var image = ""
        var imagePath = ""
        var isFullScreen = false
    }

}