package io.stempedia.pictoblox.learn

class LessonsDataModel {
    var id: String? = null
    var max_task_point: Int = 0
    var title: String? = null
    var verificationLogic: String? = null
    var starting_sb3: String? = null
    var intro: List<IntroOrConclusion>? = null
    var conclusion: List<IntroOrConclusion>? = null
    var task_overview: TaskOverview? = null
    var task_steps: List<TaskStep>? = null
    var quiz: Quiz? = null
}

class IntroOrConclusion {
    var type: String? = null
}

class TaskOverview {
    var text_content: String? = null
}

class TaskStep {
    var size: Boolean = false
    var type: String? = null
    var text: String? = null
}

class Quiz {
    var correct_option: Int = -1
    var id: String? = null
    var points: Int = 0
    var question_text: String? = null
    var solution_text: String? = null
    var type: String? = null
    var options: List<QuizAnswerOption>? = null
}

class QuizAnswerOption {
    var image: String? = null
    var text: String? = null
}

enum class CourseContentTypes(val value: String) {
    IMG_AUDIO("IMG_AUDIO"),
    IMG("IMG"),
    GIF("GIF"),
    VIDEO("VIDEO"),
    TEXT("TEXT"),
    NONE(""),
    GIF_AUDIO("GIF_AUDIO");
}