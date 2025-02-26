package io.gitp.ylfs.scraping.scraping_tl_job.jobs.college

import io.gitp.ylfs.entity.enums.Semester
import io.gitp.ylfs.scraping.scraping_tl_job.tables.CollegeTbl
import io.gitp.ylfs.scraping.scraping_tl_job.tables.TermTbl
import io.gitp.ylfs.scraping.scraping_tl_job.utils.execAndMap
import io.gitp.ylfs.scraping.scraping_tl_job.utils.getStringOrNull
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Year
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

data class TermDto(
    val year: Year,
    val semester: Semester
)

data class CollegeDto(
    val year: Year,
    val semester: Semester,
    val code: String,
    val name: String
)

data class CollegeRespDto(
    val year: Year,
    val semester: Semester,
    val resp: JsonObject
)


private class CollegeRespRepository(
    private val db: Database
) {
    fun findAll(): List<CollegeRespDto> = transaction(db) {
        """
           SELECT job.year, job.semester, college.http_resp_body
           FROM college_resp as college
               JOIN crawl_job as job USING (crawl_job_id)
        """.execAndMap {
            val yearStr: String = it.getStringOrNull("job.year")!!
            val semesterStr: String = it.getStringOrNull("job.semester")!!
            val respJsonStr: String = it.getStringOrNull("college.http_resp_body")!!

            CollegeRespDto(
                year = yearStr.slice(0..3).let { s -> Year.parse(s) },
                semester = Semester.valueOf(semesterStr),
                resp = Json.decodeFromString<JsonObject>(respJsonStr)
            )
        }
    }
}

private class CollegeRepository(
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
        val collegeId: Int? = getIdOrNull(collegeDto.year, collegeDto.semester, collegeDto.code)
        if (collegeId != null) return@transaction collegeId

        val termId: Int = termRepo.getIdOrNull(collegeDto.year, collegeDto.semester)!!

        return@transaction CollegeTbl.insertAndGetId {
            it[CollegeTbl.termId] = termId
            it[CollegeTbl.name] = collegeDto.name
            it[CollegeTbl.code] = collegeDto.code
        }.value
    }
}

private class TermRepository(
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
        val termId = getIdOrNull(termDto.year, termDto.semester)
        if (termId != null) return@transaction termId

        val insertedTermId = TermTbl.insertAndGetId {
            it[TermTbl.year] = termDto.year
            it[TermTbl.semester] = termDto.semester
        }.value

        return@transaction insertedTermId
    }
}

class CollegeRespTlJob(
    scrapingDB: Database,
    refinedDB: Database,
    private val threadPool: ExecutorService = Executors.newFixedThreadPool(40)
) {
    private val collegeRespRepo = CollegeRespRepository(scrapingDB)
    private val termRepo = TermRepository(refinedDB)
    private val collegeRepo = CollegeRepository(refinedDB, termRepo)

    fun execute() {
        val collegeResps: List<CollegeRespDto> = collegeRespRepo.findAll()

        val collegeDtos = collegeResps.flatMap { CollegeRespParser.toCollegeDtos(it) }
        val termDtos = collegeResps.map { TermDto(it.year, it.semester) }.distinct()

        // termDtos.map { termRepo.insertIfNotExists(it) }
        // collegeDtos.map { collegeRepo.insertIfNotExists(it) }
        termDtos
            .map { CompletableFuture.supplyAsync({ termRepo.insertIfNotExists(it) }, threadPool) }
            .map { it.join() }
        collegeDtos
            .map { CompletableFuture.supplyAsync({ collegeRepo.insertIfNotExists(it) }, threadPool) }
            .map { it.join() }
        // threadPool.awaitTermination(1, TimeUnit.SECONDS)
        // threadPool.shutdownNow().also { require(it.size == 0) }.onEach { println(it) }
        println("execute complete")
    }
}

/**
 * internal for test
 */
object CollegeRespParser {
    fun toCollegeDtos(collegeRespDto: CollegeRespDto): List<CollegeDto> {
        val collegeJsonArray = collegeRespDto.resp.jsonObject["dsUnivCd"]!!.jsonArray

        return collegeJsonArray.map { collegeJson ->
            val collegeName = collegeJson.jsonObject["deptNm"]!!.jsonPrimitive.content
            val collegeCode = collegeJson.jsonObject["deptCd"]!!.jsonPrimitive.content

            CollegeDto(
                year = collegeRespDto.year,
                semester = collegeRespDto.semester,
                name = collegeName,
                code = collegeCode
            )
        }
    }

}
