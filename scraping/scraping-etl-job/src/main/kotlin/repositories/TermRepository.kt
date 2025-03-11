package repositories

import io.gitp.ylfs.entity.enums.Semester
import io.gitp.ylfs.scraping.scraping_tl_job.jobs.college.TermDto
import io.gitp.ylfs.scraping.scraping_tl_job.tables.TermTbl
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Year

class TermRepository(
    private val db: Database
) {
    fun getIdOrNull(year: Year, semester: Semester): Int? = transaction(db) {
        return@transaction TermTbl
            .select(TermTbl.id)
            .where { (TermTbl.year eq year) and (TermTbl.semester eq semester) }
            .limit(1)
            .singleOrNull()
            ?.get(TermTbl.id)
            ?.value
    }

    fun insertIfNotExists(termDto: TermDto): Int = transaction(db) {
        getIdOrNull(termDto.year, termDto.semester)?.let { id -> return@transaction id }

        return@transaction TermTbl.insertAndGetId {
            it[TermTbl.year] = termDto.year
            it[TermTbl.semester] = termDto.semester
        }.value
    }
}