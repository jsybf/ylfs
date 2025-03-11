package io.gitp.ylfs.scraping.scraping_tl_job.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object SubClassTbl : IntIdTable("subclass", "subclass_id") {
    val lectureId = reference("lecture_id", LectureTbl)
    val subCode = char("sub_code", 2)
}
