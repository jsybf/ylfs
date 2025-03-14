package io.gitp.ylfs.scraping.scraping_tl_job.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object DptTbl : IntIdTable("dpt", "dpt_id") {
    val collegeId = reference("college_id", CollegeTbl)
    val code = varchar("code", 5)
    val name = varchar("name", 50)
}
