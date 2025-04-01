package io.gitp.yfls.scarping.job.file.load

import io.gitp.ylfs.entity.enums.Semester
import org.duckdb.DuckDBConnection
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.sql.DriverManager
import java.time.Year

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
        FROM read_json('${collegeRespFile}')
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
        FROM read_json('${lectureRespFile}')
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
        FROM read_json('${mlgRankRespFile}')
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
        FROM  read_json('${mlgInfoRespFile}')
        ;
    """
        .also { assert(mlgInfoRespFile.isAbsolute) }

}

object DuckDB2Mysql {
    fun buildMysqlSecret(
        mysqlHost: String,
        mysqlPort: String,
        mysqlDatabase: String,
        mysqlUser: String,
        mysqlPassword: String
    ) = """
        CREATE SECRET (
            TYPE mysql,
            HOST '${mysqlHost}',
            PORT ${mysqlPort},
            DATABASE ${mysqlDatabase},
            USER '${mysqlUser}',
            PASSWORD '${mysqlPassword}'
        );
    """.trimIndent()

    fun buildMysqlExtensionInstallAndLoad() = """
        INSTALL mysql;
        LOAD mysql;
    """.trimIndent()

    fun buildMysqlAttach(
        mysqlHost: String,
        mysqlPort: String,
        mysqlDatabase: String,
        mysqlUser: String,
        mysqlPassword: String
    ) = """
        ATTACH 'host=${mysqlHost} port=${mysqlPort} user=${mysqlUser} password=${mysqlPassword} database=${mysqlDatabase}' AS mysql_db (TYPE mysql);
        use mysql_db;
    """.trimIndent()

    fun insertTerm(year: Year, semester: Semester) = """
        INSERT INTO mysql_db.term VALUES (${year}, '${semester}');
    """.trimIndent()


    fun insertCollege(year: Year, semester: Semester) = """
        INSERT INTO mysql_db.college(year, semester, college_code, college_name)
            SELECT year, semester, college_code, college_name
            FROM college
            WHERE year = ${year} AND semester = '${semester}'
        ;
    """.trimIndent()

    fun insertDepartment(year: Year, semester: Semester) = """
        INSERT INTO mysql_db.college(year, semester, college_code, college_name)
            SELECT year, semester, college_code, college_name
            FROM college
            WHERE year = ${year} AND semester = '${semester}'
        ;
    """.trimIndent()

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
                    year,
                    semester,
                    main_code,
                    class_code,
                    name,
                    professors,
                    grades,
                    credit,
                    grade_eval_method,
                    language,
                    row_number() OVER (PARTITION BY year, semester, main_code, class_code  ORDER BY sub_code) as sub_code_row
                FROM lecture
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
            array_to_string(professors, ',') AS professor_list,
            array_to_string(grades, ',') AS grade_list,
            credit,
            grade_eval_method,
            language
        FROM duplicate_dropped
        WHERE year = ${year} AND semester = '${semester}'
        ;
    """.trimIndent()


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
    """.trimIndent()


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
        SELECT
            s.subclass_id,

            mi.mlg_limit,
            mi.major_protect_type,
            mi.applied_cnt,

            mi.total_capacity,
            mi.major_protected_capacity,
            mi.grade_1_capacity,
            mi.grade_2_capacity,
            mi.grade_3_capacity,
            mi.grade_4_capacity,
            mi.grade_5_capacity,
            mi.grade_6_capacity
        FROM mlg_info AS mi
        JOIN mysql_db.lecture AS l USING (year, semester, main_code, class_code)
        JOIN mysql_db.subclass AS s USING (lecture_id, sub_code)
        WHERE mi.year = ${year} AND mi.semester = '${semester}'
        ;
    """.trimIndent()

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
        SELECT
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
        FROM
            mlg_rank as mr
        JOIN
            mysql_db.lecture as l USING (year, semester, main_code, class_code)
        JOIN
            mysql_db.subclass as s USING (lecture_id, sub_code)
        WHERE
            r.year = ${year} AND r.semester = '${semester}'
        ;
    """.trimIndent()
}

private val logger = LoggerFactory.getLogger(object {}::class.java)
fun DuckDBConnection.execLogged(sql: String): Unit = this.createStatement().use { conn ->
    logger.info("excuting duckdb query:\n{}", sql)
    conn.execute(sql)
}

fun DuckDBConnection.execLoggedList(sqlList: List<String>): Unit = this.createStatement().use { conn ->
    sqlList.forEach { sql ->
        logger.info("excuting duckdb query:\n{}", sql)
        conn.execute(sql)
    }
}

fun main() {
    val conn: DuckDBConnection = DriverManager.getConnection("jdbc:duckdb:") as DuckDBConnection

    val baseDir = Path.of("/Users/gitp/gitp/dev/projects/ylfs/data/refined/23-2")
    val year = Year.of(2023)
    val semester = Semester.FIRST
    conn.execLoggedList(
        listOf(
            Load2DuckDB.collegeTableDDL,
            Load2DuckDB.buildCollegeLoadSql(baseDir.resolve("college-refined.json")),
            Load2DuckDB.dptTableDDL,
            Load2DuckDB.buildDptLoadSql(baseDir.resolve("dpt-refined.json")),
            Load2DuckDB.lectureTableDDL,
            Load2DuckDB.buildLectureLoadSql(baseDir.resolve("lecture-refined.json")),
            Load2DuckDB.mlgRankDDL,
            Load2DuckDB.buildMlgRankLoadSql(baseDir.resolve("mlg-rank-refined.json")),
            Load2DuckDB.mlgInfoDDL,
            Load2DuckDB.buildMlgInfoLoadSql(baseDir.resolve("mlg-info-refined.json")),
            DuckDB2Mysql.buildMysqlExtensionInstallAndLoad(),
            DuckDB2Mysql.buildMysqlAttach("3.39.233.95", "3306", "ylfs", "root", "root_pass"),
            DuckDB2Mysql.insertTerm(year, semester),
            DuckDB2Mysql.insertCollege(year, semester),
            DuckDB2Mysql.insertDepartment(year, semester),
            DuckDB2Mysql.insertLecture(year, semester),
            DuckDB2Mysql.insertSubclass(year, semester),
            DuckDB2Mysql.insertMileageInfo(year, semester),
            DuckDB2Mysql.insertMileageRank(year, semester),
        )
    )



    conn.close()
}

