package io.gitp.ylfs.scraping.scraping_tl_job.jobs.mileage

import io.gitp.ylfs.entity.enums.Semester
import io.gitp.ylfs.scraping.scraping_tl_job.utils.execAndNullableMap
import io.gitp.ylfs.scraping.scraping_tl_job.utils.getStringOrNull
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection
import java.time.Year

enum class MajorProtectType { ONLY_MAJOR, ALSO_DUAL_MAJOR, UNPROTECT }

data class MileageInfo(
    val year: Year,
    val semester: Semester,

    val mainCode: String,
    val classCode: String,
    val subCode: String,

    val mileageLimit: Int,
    val appliedCnt: Int,
    val majorProtectType: MajorProtectType,

    val totalCapacity: Int,
    val majorCapacity: Int,
    val perMajorCapacity: List<Int>
) {
    init {
        require(perMajorCapacity.size == 6)
    }
}

class MileageInfoRespExtractor(
    private val db: Database
) {
    fun extract(year: Year, semester: Semester): List<MileageInfo> = transaction(db) {
        """
            select main_code, class_code, sub_code, http_resp_body
            from mlg_info_resp
        """.execAndNullableMap { rs ->
            val respBody: JsonObject? = rs.getStringOrNull("http_resp_body")?.let { Json.decodeFromString<JsonObject>(it) }
            val mainCpde: String = rs.getString("main_code")
            val classCode: String = rs.getString("class_code")
            val subCode: String = rs.getString("sub_code")

            if (respBody == null) return@execAndNullableMap null
            else return@execAndNullableMap MileageInfoTransformer.transform(year, semester, mainCpde, classCode, subCode, respBody)
        }.filterNotNull()

    }
}

object MileageInfoTransformer {
    private val parseIfMajorProtected = Regex("""(\d+)\((Y|N)\)""")
    fun parseTotalCapacity(respBody: JsonObject): Int = respBody["atnlcPercpCnt"]!!.jsonPrimitive.int
    fun parseMajorCapacity(respBody: JsonObject): Int = respBody["mjrprPercpCnt"]!!.jsonPrimitive.content.let { parseIfMajorProtected.find(it)!!.groupValues[1].toInt() }
    fun parseMileageLimit(respBody: JsonObject): Int = respBody["usePosblMaxMlgVal"]!!.jsonPrimitive.int
    fun parseAppliedCnt(respBody: JsonObject): Int = respBody["cnt"]!!.jsonPrimitive.int
    fun parseIfProtectSubMajor(respBody: JsonObject): Boolean = respBody["mjrprPercpCnt"]!!.jsonPrimitive.content.let {
        val yORn = parseIfMajorProtected.find(it)!!.groupValues[2]
        when (yORn) {
            "Y" -> true
            "N" -> false
            else -> throw IllegalStateException("fuck")
        }
    }

    fun parsePerMajorCapacity(respBody: JsonObject): List<Int> = (1..6).map { respBody["sy${it}PercpCnt"]!!.jsonPrimitive.int }

    fun transform(year: Year, semester: Semester, mainCode: String, classCode: String, subCode: String, respBody: JsonObject): MileageInfo? {
        val respBodyContent: JsonObject = respBody["dsSles251"]!!.jsonArray.firstOrNull()?.jsonObject ?: return null
        val majorProtectType = when {
            parseIfProtectSubMajor(respBodyContent) -> MajorProtectType.ALSO_DUAL_MAJOR
            (!parseIfProtectSubMajor(respBodyContent)) && (parseMajorCapacity(respBodyContent) == 0) -> MajorProtectType.UNPROTECT
            (!parseIfProtectSubMajor(respBodyContent)) && (parseMajorCapacity(respBodyContent) != 0) -> MajorProtectType.ONLY_MAJOR
            else -> throw IllegalStateException("unexpected major protect case")
        }
        return MileageInfo(
            year,
            semester,
            mainCode,
            classCode,
            subCode,
            parseMileageLimit(respBodyContent),
            parseAppliedCnt(respBodyContent),
            majorProtectType,
            parseTotalCapacity(respBodyContent),
            parseMajorCapacity(respBodyContent),
            parsePerMajorCapacity(respBodyContent)
        )

    }
}

class MileageLoader(
    private val connection: Connection
) {
    fun load(mileageInfoList: List<MileageInfo>, batchSize: Int) {
        createTmpTable()
        batchInsertToTmpTable(mileageInfoList, batchSize)
        copyFromTmpTableToMlgInfoTable()
        connection.exec("drop table tmp_mlg_info ")
    }


    private fun createTmpTable() = connection.exec(
        """
                CREATE TABLE tmp_mlg_info (
                     year                   YEAR NOT NULL,
                     semester               VARCHAR(7) NOT NULL,
                     main_code              CHAR(7) NOT NULL,
                     class_code             CHAR(2) NOT NULL,
                     sub_code               CHAR(2) NOT NULL,
            
                     mlg_limit              INT UNSIGNED NOT NULL,
            
                     applied_cnt            INT UNSIGNED NOT NULL,
                     total_capacity         INT UNSIGNED NOT NULL,
            
                     major_protect_type     VARCHAR(20)  NOT NULL CHECK ( major_protect_type IN ('ONLY_MAJOR', 'ALSO_DUAL_MAJOR', 'UNPROTECT')),
                     major_protect_capacity INT UNSIGNED NOT NULL,
            
                     first_grade_capacity   INT UNSIGNED NOT NULL,
                     second_grade_capacity  INT UNSIGNED NOT NULL,
                     third_grade_capacity   INT UNSIGNED NOT NULL,
                     fourth_grade_capacity  INT UNSIGNED NOT NULL,
                     fifth_grade_capacity   INT UNSIGNED NOT NULL,
                     sixth_grade_capacity   INT UNSIGNED NOT NULL,
            
                     UNIQUE (year, semester, main_code, class_code, sub_code)
                )
        """
    )

    private fun batchInsertToTmpTable(mileageInfoList: List<MileageInfo>, batchSize: Int) = connection.prepareStatement(
        """
                insert into tmp_mlg_info(
                    year, semester, main_code, class_code, sub_code,
                    mlg_limit, applied_cnt, total_capacity,
                    major_protect_type, major_protect_capacity,
                    first_grade_capacity, second_grade_capacity,
                    third_grade_capacity, fourth_grade_capacity,
                    fifth_grade_capacity, sixth_grade_capacity
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) 
    """
    ).use { pstmt ->
        mileageInfoList.forEachIndexed { idx, mlgInfo ->
            pstmt.setInt(1, mlgInfo.year.value)
            pstmt.setString(2, mlgInfo.semester.name)
            pstmt.setString(3, mlgInfo.mainCode)
            pstmt.setString(4, mlgInfo.classCode)
            pstmt.setString(5, mlgInfo.subCode)
            pstmt.setInt(6, mlgInfo.mileageLimit)
            pstmt.setInt(7, mlgInfo.appliedCnt)
            pstmt.setInt(8, mlgInfo.totalCapacity)
            pstmt.setString(9, mlgInfo.majorProtectType.name)
            pstmt.setInt(10, mlgInfo.majorCapacity)
            pstmt.setInt(11, mlgInfo.perMajorCapacity[0])
            pstmt.setInt(12, mlgInfo.perMajorCapacity[1])
            pstmt.setInt(13, mlgInfo.perMajorCapacity[2])
            pstmt.setInt(14, mlgInfo.perMajorCapacity[3])
            pstmt.setInt(15, mlgInfo.perMajorCapacity[4])
            pstmt.setInt(16, mlgInfo.perMajorCapacity[5])

            pstmt.addBatch()
            if (idx % batchSize == 0) pstmt.executeBatch()
        }
    }

    private fun copyFromTmpTableToMlgInfoTable() = connection.exec(
        """
                INSERT INTO mlg_info (
                    lecture_id,
                    mlg_limit,
                    applied_cnt,
                    total_capacity,
                    major_protect_type,
                    major_protect_capacity,
                    first_grade_capacity,
                    second_grade_capacity,
                    third_grade_capacity,
                    fourth_grade_capacity,
                    fifth_grade_capacity,
                    sixth_grade_capacity
                )
                SELECT 
                    lecture.lecture_id,
                    tmp.mlg_limit,
                    tmp.applied_cnt,
                    tmp.total_capacity,
                    tmp.major_protect_type,
                    tmp.major_protect_capacity,
                    tmp.first_grade_capacity,
                    tmp.second_grade_capacity,
                    tmp.third_grade_capacity,
                    tmp.fourth_grade_capacity,
                    tmp.fifth_grade_capacity,
                    tmp.sixth_grade_capacity
                FROM tmp_mlg_info tmp
                JOIN lecture ON 
                    lecture.year = tmp.year AND
                    lecture.semester = tmp.semester AND
                    lecture.main_code = tmp.main_code AND
                    lecture.class_code = tmp.class_code
    """
    )
}


// batch insert to it
// copy from temp table to mlg_rank
private fun Connection.exec(sql: String): Boolean = this.prepareStatement(sql).use { it.execute() }
