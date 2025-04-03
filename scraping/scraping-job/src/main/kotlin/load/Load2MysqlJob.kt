package io.gitp.yfls.scarping.job.file.load

import io.gitp.ylfs.entity.enums.Semester
import kotlinx.serialization.json.*
import org.duckdb.DuckDBConnection
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.sql.DriverManager
import java.time.Year
import kotlin.io.path.exists
import kotlin.io.path.readText

data class DataFilePaths(
    val collegeFile: Path,
    val dptFile: Path,
    val lectureFile: Path,
    val mlgInfoFile: Path,
    val mlgRankFile: Path
)

object Load2MysqlJob {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private fun DuckDBConnection.execLogged(sql: String): Unit = this.createStatement().use { conn ->
        logger.info("excuting duckdb query:\n{}", sql)
        conn.execute(sql)
    }

    private fun findDataFilePaths(baseDir: Path): DataFilePaths = DataFilePaths(
        collegeFile = baseDir.resolve("college-refined.json").also { require(it.exists()) { "can't find ${it}" } },
        dptFile = baseDir.resolve("dpt-refined.json").also { require(it.exists()) { "can't find ${it}" } },
        lectureFile = baseDir.resolve("lecture-refined.json").also { require(it.exists()) { "can't find ${it}" } },
        mlgInfoFile = baseDir.resolve("mlg-info-refined.json").also { require(it.exists()) { "can't find ${it}" } },
        mlgRankFile = baseDir.resolve("mlg-rank-refined.json").also { require(it.exists()) { "can't find ${it}" } },
    )

    /**
     * determine year and semester based on college-refined.json
     */
    private fun determineTerm(collegeFile: Path): Pair<Year, Semester> {
        val collegeSample = Json.parseToJsonElement(collegeFile.readText()).jsonArray.first().jsonObject
        return Pair(
            collegeSample["year"]!!.jsonPrimitive.int.let { Year.of(it) },
            collegeSample["semester"]!!.jsonPrimitive.content.let { Semester.valueOf(it) }
        )
    }

    fun run(
        inputDir: Path,
        mysqlHost: String,
        mysqlPort: String,
        mysqlDatabase: String,
        mysqlUser: String,
        mysqlPassword: String
    ) = (DriverManager.getConnection("jdbc:duckdb:") as DuckDBConnection).use { conn ->
        // find json files
        val dataFilePaths: DataFilePaths = findDataFilePaths(inputDir)
        /** json file to duckdb **/
        conn.execLogged(Load2DuckDBStatment.DDL.collegeTbl)
        conn.execLogged(Load2DuckDBStatment.LoadStatment.buildCollegeLoadSql(dataFilePaths.collegeFile))
        conn.execLogged(Load2DuckDBStatment.DDL.dptTbl)
        conn.execLogged(Load2DuckDBStatment.LoadStatment.buildDptLoadSql(dataFilePaths.dptFile))
        conn.execLogged(Load2DuckDBStatment.DDL.lectureTbl)
        conn.execLogged(Load2DuckDBStatment.LoadStatment.buildLectureLoadSql(dataFilePaths.lectureFile))
        conn.execLogged(Load2DuckDBStatment.DDL.mlgInfoTbl)
        conn.execLogged(Load2DuckDBStatment.LoadStatment.buildMlgInfoLoadSql(dataFilePaths.mlgInfoFile))
        conn.execLogged(Load2DuckDBStatment.DDL.mlgRankTbl)
        conn.execLogged(Load2DuckDBStatment.LoadStatment.buildMlgRankLoadSql(dataFilePaths.mlgRankFile))

        /** duckdb to mysql **/
        conn.execLogged(DuckDB2MysqlStatment.MysqlConnection.buildMysqlExtensionInstallAndLoad())
        conn.execLogged(DuckDB2MysqlStatment.MysqlConnection.buildMysqlAttach(mysqlHost, mysqlPort, mysqlDatabase, mysqlUser, mysqlPassword))

        val (year: Year, semester: Semester) = determineTerm(dataFilePaths.collegeFile)
        conn.execLogged(DuckDB2MysqlStatment.Load2Mysql.insertTerm(year, semester))
        conn.execLogged(DuckDB2MysqlStatment.Load2Mysql.insertCollege(year, semester))
        conn.execLogged(DuckDB2MysqlStatment.Load2Mysql.insertDepartment(year, semester))
        conn.execLogged(DuckDB2MysqlStatment.Load2Mysql.insertLecture(year, semester))
        conn.execLogged(DuckDB2MysqlStatment.Load2Mysql.insertSubclass(year, semester))
        conn.execLogged(DuckDB2MysqlStatment.Load2Mysql.insertMileageInfo(year, semester))
        conn.execLogged(DuckDB2MysqlStatment.Load2Mysql.insertMileageRank(year, semester))
    }

}


object Load2DuckDBStatment {
    object DDL {
        val collegeTbl = """
        CREATE TABLE college(
            year              INT           NOT NULL,
            semester          VARCHAR(6)    NOT NULL,
            college_code      CHAR(6)       NOT NULL,
            college_name      VARCHAR(100)   NOT NULL
        );
        """
        val dptTbl = """
        CREATE TABLE dpt(
            year              INT           NOT NULL,
            semester          VARCHAR(6)    NOT NULL,
            college_code      CHAR(6)       NOT NULL,
            dpt_code          CHAR(5)       NOT NULL,
            dpt_name          VARCHAR(100)   NOT NULL
        );
        """

        val lectureTbl = """
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
        val mlgInfoTbl = """
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
        val mlgRankTbl = """
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
    }

    object LoadStatment {
        fun buildDptLoadSql(dptRespFile: Path): String = """
        INSERT INTO dpt
        SELECT 
            year,
            semester,
            college_code,
            dpt_code,
            name
        FROM
            read_json('${dptRespFile}')
        ;
        """
            .also { assert(dptRespFile.isAbsolute) }

        fun buildCollegeLoadSql(collegeRespFile: Path): String = """
        INSERT INTO college
        SELECT 
            year,
            semester,    
            college_code,
            name,
        FROM read_json('${collegeRespFile}')
        ;
        """
            .also { assert(collegeRespFile.isAbsolute) }

        fun buildLectureLoadSql(lectureRespFile: Path) = """
        INSERT INTO lecture
        SELECT
            year,
            semester,
            main_code,
            class_code,
            sub_code,
            name,
            professors,
            grades,
            credit,
            grade_eval_method,
            lecture_type,
            language,
            loc_and_sched_list,
        FROM read_json('${lectureRespFile}')
        ;
    """
            .also { assert(lectureRespFile.isAbsolute) }

        fun buildMlgRankLoadSql(mlgRankRespFile: Path) = """
        INSERT INTO mlg_rank
        SELECT
            year,
            semester,
            main_code,
            class_code,
            sub_code,
            -- grade,
            if_success,
            mlg_rank,
            mlg_value,
            if_disabled,
            if_major_protected,
            applied_subject_cnt,
            if_grade_planned,
            if_first_apply,
            last_semester_ratio,
            last_semesterratio_frac,
            total_credit_ratio,
            total_credit_ratio_frac AS  total_credit_faction
        FROM read_json('${mlgRankRespFile}')
        ;
    """
            .also { assert(mlgRankRespFile.isAbsolute) }

        fun buildMlgInfoLoadSql(mlgInfoRespFile: Path) = """
        INSERT INTO mlg_info
        SELECT 
            year,
            semester,
            main_code,
            class_code,
            sub_code,
            mileage_limit as mlg_limit,
            major_protect_type,
            applied_cnt,
            total_capacity,
            major_capacity,
            per_major_capacity[1],
            per_major_capacity[2],
            per_major_capacity[3],
            per_major_capacity[4],
            per_major_capacity[5],
            per_major_capacity[6]
        FROM  read_json('${mlgInfoRespFile}')
        ;
    """
            .also { assert(mlgInfoRespFile.isAbsolute) }

    }
}

object DuckDB2MysqlStatment {
    object MysqlConnection {
        fun buildMysqlExtensionInstallAndLoad() = """
        INSTALL mysql;
        LOAD mysql;
    """

        fun buildMysqlAttach(
            mysqlHost: String,
            mysqlPort: String,
            mysqlDatabase: String,
            mysqlUser: String,
            mysqlPassword: String
        ) = """
        ATTACH 'host=${mysqlHost} port=${mysqlPort} user=${mysqlUser} password=${mysqlPassword} database=${mysqlDatabase}' AS mysql_db (TYPE mysql);
    """
    }

    object Load2Mysql {
        fun insertTerm(year: Year, semester: Semester) = """
        INSERT INTO mysql_db.term VALUES (${year}, '${semester}');
    """

        fun insertCollege(year: Year, semester: Semester) = """
        INSERT INTO mysql_db.college(year, semester, college_code, college_name)
            SELECT year, semester, college_code, college_name
            FROM college
            WHERE year = ${year} AND semester = '${semester}'
        ;
    """

        fun insertDepartment(year: Year, semester: Semester) = """
        INSERT INTO mysql_db.dpt(college_id, dpt_code, dpt_name)
            SELECT c.college_id, d.dpt_code, d.dpt_name
            FROM dpt AS d
            JOIN mysql_db.college AS c USING(year, semester, college_code)
            WHERE year = ${year} AND semester = '${semester}'
        ;
    """

        fun insertLecture(year: Year, semester: Semester) = """
        INSERT INTO mysql_db.lecture (
            year,
            semester,
            main_code,
            class_code,
            name,
            professor_list,
            grade_list,
            credit,
            grade_eval_method,
            language
        )
        WITH
            sub_code_partition AS (
                SELECT 
                    *,
                    row_number() OVER (PARTITION BY year, semester, main_code, class_code  ORDER BY sub_code) as sub_code_row
                FROM main.lecture
            ),
            duplicate_dropped AS (
                SELECT *
                FROM sub_code_partition
                WHERE sub_code_row = 1
            )
        SELECT
            year,
            semester,
            main_code,
            class_code,
            name,
            to_json(professors),
            to_json(grades),
            credit,
            grade_eval_method,
            language
        FROM duplicate_dropped
        WHERE year = ${year} AND semester = '${semester}'
        ;
    """

        fun insertSubclass(year: Year, semester: Semester) = """
        INSERT INTO mysql_db.subclass(
            lecture_id,
            sub_code,
            loc_sched,
            lecture_type_list
        )
        WITH
            subclass_group AS (
                select 
                    year,
                    semester,
                    main_code,
                    class_code,
                    sub_code,
                    loc_sched,
                    to_json(list_distinct(array_agg(lecture_type))) as lecture_type_list
                FROM 
                    lecture
                GROUP BY 
                    year, semester, main_code, class_code, sub_code, loc_sched
            )
        SELECT
            l.lecture_id,
            sg.sub_code,
            sg.loc_sched,
            sg.lecture_type_list
        FROM subclass_group AS sg
        JOIN mysql_db.lecture AS l USING (year, semester, main_code, class_code)
        where sg.year = ${year} AND sg.semester = '${semester}'
        ;
    """

        fun insertMileageInfo(year: Year, semester: Semester) = """
        INSERT INTO mysql_db.mlg_info(
            subclass_id,

            mlg_limit,
            major_protect_type,
            applied_cnt,

            total_capacity,
            major_protect_capacity,
            grade_1_capacity,
            grade_2_capacity,
            grade_3_capacity,
            grade_4_capacity,
            grade_5_capacity,
            grade_6_capacity
        )
        WITH
            mlg_info_partition AS (
                SELECT *,row_number() OVER (PARTITION BY year, semester, main_code, class_code, sub_code) as row_num
                FROM mlg_info
            ),
            mlg_info_distinct AS (
                select *
                FROM  mlg_info_partition
                WHERE row_num = 1
            )
        SELECT
            subclass_id,
            mlg_limit,
            major_protect_type,
            applied_cnt,
            total_capacity,
            major_protected_capacity,
            grade_1_capacity,
            grade_2_capacity,
            grade_3_capacity,
            grade_4_capacity,
            grade_5_capacity,
            grade_6_capacity
        FROM mlg_info_distinct
        JOIN mysql_db.lecture USING (year, semester, main_code, class_code)
        JOIN mysql_db.subclass USING (lecture_id, sub_code)
        WHERE year = ${year} AND semester = '${semester}'
        ;
    """

        fun insertMileageRank(year: Year, semester: Semester) = """
        INSERT INTO mysql_db.mlg_rank(
            subclass_id,
            if_succeed,

            mlg_rank,
            mlg_value,
            -- grade,

            if_disabled,
            if_major_protected,
            applied_lecture_cnt,
            if_graduate_planned,
            if_first_apply,
            last_term_credit,
            last_term_credit_faction,
            total_credit,
            total_credit_fraction
        )
        SELECT DISTINCT
            s.subclass_id,
            mr.if_succeed,

            mr.mlg_rank,
            mr.mlg_value,
            -- grade,

            mr.if_disabled,
            mr.if_major_protected,
            mr.applied_lecture_cnt,
            mr.if_graduate_planned,
            mr.if_first_apply,
            mr.last_term_credit,
            mr.last_term_credit_faction,
            mr.total_credit,
            mr.total_credit_fraction
        FROM mlg_rank as mr
        JOIN
            mysql_db.lecture as l USING (year, semester, main_code, class_code)
        JOIN
            mysql_db.subclass as s USING (lecture_id, sub_code)
        WHERE
            mr.year = ${year} AND mr.semester = '${semester}'
        ;
    """
    }

}

