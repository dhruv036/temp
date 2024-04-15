package io.stempedia.pictoblox.learn

import com.google.firebase.Timestamp
import com.google.firebase.storage.StorageReference

class CourseDataModel {
    var accessibility: Accessibility? = null
    var catgories: List<String>? = null
    var description: String = ""
    var difficulty: String = ""
    var estimated_time: Int = 0
    var course_id: String = ""
    var no_of_lessons = 0
    var thumb_key: String = ""
    var title: String = ""
    var version: Version? = null
    var video_link: String = ""

    var documentId: String = ""
    var thumbReference: StorageReference? = null

    var intro_stories: List<CourseIntroAndConclusion >? = null
    var conclusion_stories: List<CourseIntroAndConclusion >? = null
}

class Accessibility {
    var isPaid = false
    var price = 0f
}

class Version {
    var name: String = ""
    var date: Timestamp? = null
    var status: String = ""
}

class CourseIntroAndConclusion {
    var media_type: String = ""
    var text_content: String = ""
    var video_link: String = ""
}