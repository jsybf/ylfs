package io.gitp.ylfs.scraping.scraping_tl_job.tables

import io.gitp.ylfs.entity.enums.LectureType
import org.jetbrains.exposed.dao.id.IntIdTable

object DptLectureTbl : IntIdTable("dpt_lecture", "dpt_lecture_id") {
    val lectureId = reference("lecture_id", LectureTbl)
    val dptId = reference("dpt_id", DptTbl)
    val lectureType = enumerationByName<LectureType>("lecture_type", 7)
}
