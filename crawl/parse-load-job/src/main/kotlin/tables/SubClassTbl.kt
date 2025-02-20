package io.gitp.ylfs.parse_load_job.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object SubClassTbl : IntIdTable("subclass", "subclass_id") {
    val lectureId = reference("lecture_id", LectureTbl)
    val subId = char("sub_id", 2)
}
