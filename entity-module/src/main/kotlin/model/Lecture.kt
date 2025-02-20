package io.gitp.ylfs.entity.model

import io.gitp.ylfs.entity.enums.GradeEvalMethod
import io.gitp.ylfs.entity.enums.Language
import io.gitp.ylfs.entity.enums.LectureType


data class Lecture(
    val term: Term,

    val mainCode: String,
    val classCode: String,

    val dptAndLectureType: Map<Dpt, LectureType>,

    val subclassLocScheds: List<SubclassLocSched>,

    val professors: List<String>,

    val name: String,
    val grades: List<Int>,
    val credit: Int,
    val gradeEvalMethod: GradeEvalMethod,
    val language: Language,
    val descriptions: String,
) {
    var dbId: Int? = null
}