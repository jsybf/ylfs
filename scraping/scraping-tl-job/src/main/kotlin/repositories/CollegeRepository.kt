package repositories

import io.gitp.ylfs.entity.enums.Semester
import io.gitp.ylfs.scraping.scraping_tl_job.jobs.college.CollegeDto
import io.gitp.ylfs.scraping.scraping_tl_job.tables.CollegeTbl
import io.gitp.ylfs.scraping.scraping_tl_job.tables.TermTbl
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Year

class CollegeRepository(
    private val db: Database,
    val termRepo: TermRepository
) {
    fun getIdOrNull(year: Year, semester: Semester, collegeCode: String): Int? = transaction(db) {
        return@transaction (CollegeTbl innerJoin TermTbl)
            .select(CollegeTbl.id)
            .where {
                (TermTbl.year eq year) and (TermTbl.semester eq semester) and (CollegeTbl.code eq collegeCode)
            }
            .limit(1)
            .singleOrNull()
            ?.get(CollegeTbl.id)
            ?.value
    }

    fun insertIfNotExists(collegeDto: CollegeDto): Int = transaction(db) {
        getIdOrNull(collegeDto.year, collegeDto.semester, collegeDto.code)?.let { id -> return@transaction id }

        val termId: Int = termRepo.getIdOrNull(collegeDto.year, collegeDto.semester)!!

        return@transaction CollegeTbl.insertAndGetId {
            it[CollegeTbl.termId] = termId
            it[CollegeTbl.name] = collegeDto.name
            it[CollegeTbl.code] = collegeDto.code
        }.value
    }
}