package io.gitp.yfls.scarping.job.file.load

import org.duckdb.DuckDBConnection
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.sql.DriverManager

object Load2MysqlJob {
    fun run(
        inputDir: Path,
        mysqlHost: String,
        mysqlPort: String,
        mysqlDatabase: String,
        mysqlUser: String,
        mysqlPassword: String
    ) {

    }
}


object Load2DuckDB {
    val collegeTableDDL = """
        CREATE TABLE college(
            year              INT           NOT NULL,
            semester          VARCHAR(6)    NOT NULL,
            college_code      CHAR(6)       NOT NULL,
            college_name      VARCHAR(100)   NOT NULL
        );
    """
    val dptTableDDL = """
        CREATE TABLE dpt(
            year              INT           NOT NULL,
            semester          VARCHAR(6)    NOT NULL,
            college_code      CHAR(6)       NOT NULL,
            dpt_code          CHAR(5)       NOT NULL,
            dpt_name          VARCHAR(100)   NOT NULL
        );
    """

    val lectureTableDDL = """
        CREATE TABLE lecture (
            year              INT           NOT NULL,
            semester          VARCHAR(6)    NOT NULL,
            main_code         CHAR(7)       NOT NULL,
            class_code        CHAR(2)       NOT NULL,
            sub_code          CHAR(2)       NOT NULL,
            name              VARCHAR(100)  NOT NULL,
            professors        VARCHAR[]     NOT NULL,
            grades            VARCHAR[]     NOT NULL,
            credit            DECIMAL(3, 1) NOT NULL,
            grade_eval_method VARCHAR(10)   NOT NULL,
            lecture_type      VARCHAR(10)   NOT NULL,
            language          VARCHAR(10)   NOT NULL CHECK ( language IN ('KOREAN', 'ENGLISH', 'ETC') ),
            loc_sched         JSON NOT NULL,
        );
    """
    val mlgInfoDDL = """
        CREATE TABLE mlg_info (
            year                     INT            NOT NULL,
            semester                 VARCHAR(6)     NOT NULL,
            main_code                CHAR(7)        NOT NULL,
            class_code               CHAR(2)        NOT NULL,
            sub_code                 CHAR(2)        NOT NULL,
        
            mlg_limit                BIGINT         NOT NULL,
            major_protect_type       VARCHAR(20)    NOT NULL,
            applied_cnt              INT            NOT NULL,
        
            total_capacity           INT            NOT NULL,
            major_protected_capacity INT            NOT NULL,
        
            grade_1_capacity         INT            NOT NULL,
            grade_2_capacity         INT            NOT NULL,
            grade_3_capacity         INT            NOT NULL,
            grade_4_capacity         INT            NOT NULL,
            grade_5_capacity         INT            NOT NULL,
            grade_6_capacity         INT            NOT NULL,
        );
    """

    val mlgRankDDL = """
        CREATE TABLE mlg_rank (
            year                     INT            NOT NULL,
            semester                 VARCHAR(6)     NOT NULL,
            main_code                CHAR(7)        NOT NULL,
            class_code               CHAR(2)        NOT NULL,
            sub_code                 CHAR(2)        NOT NULL,
            -- grade                    INT            NOT NULL,
            if_succeed               BOOLEAN        NOT NULL,
            mlg_rank                 BIGINT         NOT NULL,
            mlg_value                BIGINT         NOT NULL,
            if_disabled              BOOLEAN        NOT NULL,
            if_major_protected       VARCHAR(10)    NOT NULL CHECK ( if_major_protected IN ('MAJOR_PROTECTED', 'DUAL_MAJOR_PROTECTED', 'DUAL_MAJOR_NOT_PROTECTED', 'NOT_PROTECTED') ),
            applied_lecture_cnt      INT            NOT NULL,
            if_graduate_planned      BOOLEAN        NOT NULL,
            if_first_apply           BOOLEAN        NOT NULL,
            last_term_credit         DECIMAL(10, 2) NOT NULL,
            last_term_credit_faction VARCHAR(10)    NOT NULL,
            total_credit             DECIMAL(10, 2) NOT NULL,
            total_credit_fraction    VARCHAR(10)    NOT NULL,
        );
    """


    fun buildDptLoadSql(dptRespFile: Path): String = """
        INSERT INTO dpt
        SELECT 
            year         AS  year,
            semester     AS  semester,
            college_code AS  college_code,
            dpt_code     AS  dpt_code,
            name         AS  dpt_name
        FROM
            read_json('${dptRespFile}')
        ;
        """
        .trimIndent()
        .also { assert(dptRespFile.isAbsolute) }

    fun buildCollegeLoadSql(collegeRespFile: Path): String = """
        INSERT INTO college
        SELECT 
            year         AS  year,
            semester     AS  semester,
            college_code AS  college_code,
            name         AS  college_name
        FROM
            read_json('${collegeRespFile}')
        ;
        """
        .trimIndent()
        .also { println(it) }
        .also { assert(collegeRespFile.isAbsolute) }

    fun buildLectureLoadSql(lectureRespFile: Path) = """
        INSERT INTO lecture
        SELECT
            year               AS  year,
            semester           AS  semester,
            main_code          AS  main_code,
            class_code         AS  class_code,
            sub_code           AS  sub_code,
            name               AS  name,
            professors         AS  professors,
            grades             AS  grades,
            credit             AS  credit,
            grade_eval_method  AS  grade_eval_method,
            lecture_type       AS  lecture_type,
            language           AS  language,
            loc_and_sched_list AS  loc_sched,
        FROM 
            read_json('${lectureRespFile}')
        ;
    """.trimIndent()
        .also { assert(lectureRespFile.isAbsolute) }

    fun buildMlgRankLoadSql(mlgRankRespFile: Path) = """
        INSERT INTO mlg_rank
        SELECT
            year                    AS  year,
            semester                AS  semester,
            main_code               AS  main_code,
            class_code              AS  class_code,
            sub_code                AS  sub_code,
            -- grade                   AS  grade,
            if_success              AS  if_sucess,
            mlg_rank                AS  mlg_rank,
            mlg_value               AS  mlg_value,
            if_disabled             AS  if_disabled,
            if_major_protected      AS  if_major_protected,
            applied_subject_cnt     AS  applied_lecture_cnt,
            if_grade_planned        AS  if_graduated_planned,
            if_first_apply          AS  if_first_apply,
            last_semester_ratio     AS  tSemesterRatio,
            last_semesterratio_frac AS  tSemesterratioFrac,
            total_credit_ratio      AS  total_credit,
            total_credit_ratio_frac AS  total_credit_faction
        FROM
            read_json('${mlgRankRespFile}')
        ;
    """
        .also { assert(mlgRankRespFile.isAbsolute) }

    fun buildMlgInfoLoadSql(mlgInfoRespFile: Path) = """
        INSERT INTO mlg_info
        SELECT 
            year                  AS  year,
            semester              AS  semester,
            main_code             AS  main_code,
            class_code            AS  class_code,
            sub_code              AS  sub_code,
            mileage_limit         AS  mileage_limit,
            major_protect_type    AS  major_protect_type,
            applied_cnt           AS  applied_cnt,
            total_capacity        AS  total_capacity,
            major_capacity        AS  major_capacity,
            per_major_capacity[1] AS  grade_1_capacity,
            per_major_capacity[2] AS  grade_2_capacity,
            per_major_capacity[3] AS  grade_3_capacity,
            per_major_capacity[4] AS  grade_4_capacity,
            per_major_capacity[5] AS  grade_5_capacity,
            per_major_capacity[6] AS  grade_6_capacity
        FROM 
            read_json('${mlgInfoRespFile}')
        ;
    """
        .also { assert(mlgInfoRespFile.isAbsolute) }

}


private val logger = LoggerFactory.getLogger(object {}::class.java)
fun DuckDBConnection.execLogged(sql: String): Unit = this.createStatement().use { conn ->
    logger.info("excuting duckdb query:\n{}", sql)
    conn.execute(sql)
}

fun main() {
    val conn: DuckDBConnection = DriverManager.getConnection("jdbc:duckdb:") as DuckDBConnection

    val baseDir = Path.of("/Users/gitp/gitp/dev/projects/ylfs/data/refined/23-2")
    conn.execLogged(Load2DuckDB.collegeTableDDL)
    conn.execLogged(Load2DuckDB.buildCollegeLoadSql(baseDir.resolve("college-refined.json")))
    conn.execLogged(Load2DuckDB.dptTableDDL)
    conn.execLogged(Load2DuckDB.buildDptLoadSql(baseDir.resolve("dpt-refined.json")))
    conn.execLogged(Load2DuckDB.lectureTableDDL)
    conn.execLogged(Load2DuckDB.buildLectureLoadSql(baseDir.resolve("lecture-refined.json")))
    conn.execLogged(Load2DuckDB.mlgRankDDL)
    conn.execLogged(Load2DuckDB.buildMlgRankLoadSql(baseDir.resolve("mlg-rank-refined.json")))
    conn.execLogged(Load2DuckDB.mlgInfoDDL)
    conn.execLogged(Load2DuckDB.buildMlgInfoLoadSql(baseDir.resolve("mlg-info-refined.json")))



    conn.close()
}

