package repositories

import io.gitp.ylfs.entity.enums.Semester
import io.gitp.ylfs.scraping.scraping_tl_job.jobs.dpt.DptDto
import io.gitp.ylfs.scraping.scraping_tl_job.tables.CollegeTbl
import io.gitp.ylfs.scraping.scraping_tl_job.tables.DptTbl
import io.gitp.ylfs.scraping.scraping_tl_job.tables.TermTbl
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Year

class DptRepository(
    private val db: Database,
    private val termRepo: TermRepository = TermRepository(db),
    private val collegeRepo: CollegeRepository = CollegeRepository(db, termRepo)
) {
    fun getIdOrNull(year: Year, semester: Semester, dptCode: String): Int? = transaction(db) {
        return@transaction (DptTbl innerJoin CollegeTbl innerJoin  TermTbl)
            .select(DptTbl.id)
            .where {
                (TermTbl.year eq year) and (TermTbl.semester eq semester) and (DptTbl.code eq dptCode)
            }
            .limit(1)
            .singleOrNull()
            ?.get(DptTbl.id)
            ?.value
    }

    fun insertIfNotExists(dpt: DptDto): Int = transaction(db) {
        getIdOrNull(dpt.year, dpt.semester, dpt.code)?.let { id -> return@transaction id }

        val collegeId: Int = collegeRepo.getIdOrNull(dpt.year, dpt.semester, dpt.collegeCode)!!

        return@transaction DptTbl.insertAndGetId {
            it[DptTbl.code] = dpt.code
            it[DptTbl.name] = dpt.name
            it[DptTbl.collegeId] = collegeId
        }.value
    }
}