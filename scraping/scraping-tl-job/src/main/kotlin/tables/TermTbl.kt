package io.gitp.ylfs.scraping.scraping_tl_job.tables

import io.gitp.ylfs.entity.enums.Semester
import io.gitp.ylfs.scraping.scraping_tl_job.tables.types.year
import org.jetbrains.exposed.dao.id.IntIdTable

object TermTbl : IntIdTable("term", "term_id") {
    val semester = enumerationByName<Semester>("semester", 7)
    val year = year("year")
}
