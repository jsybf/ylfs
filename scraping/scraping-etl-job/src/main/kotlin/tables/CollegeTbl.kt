package io.gitp.ylfs.scraping.scraping_tl_job.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object CollegeTbl : IntIdTable("college", "college_id") {
    val termId = reference("term_id", TermTbl)
    val code = varchar("code", 6)
    val name = varchar("name", 50)
}
