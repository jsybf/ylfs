package duckdb

import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Statement

private fun connectMysqlDuckDB(mysqlHost: String, mysqlDatabase: String, mysqlUser: String, mysqlPassword: String): String =
    """
        INSTALL mysql;
        LOAD mysql;
        
        CREATE SECRET (
            TYPE MYSQL,
            HOST '${mysqlHost}',
            PORT 3306,
            DATABASE ${mysqlDatabase},
            USER '${mysqlUser}',
            PASSWORD '${mysqlPassword}'
        );
        ATTACH '' AS mysql_db (TYPE MYSQL);
    """

private val importMysqlLectureTableDuckDB =
    """
        SET VARIABLE json_struct = (SELECT json_structure(json(json)) AS info FROM mysql_db.lecture_json LIMIT 1);
        
        CREATE TABLE lecture AS
            WITH
                lecture_tmp1 AS (
                    SELECT json(json) AS info FROM mysql_db.lecture_json
                ),
                lecture_tmp2 AS (
                    SELECT
                        UNNEST(json_transform(info, getvariable('json_struct'))) AS info
                    FROM lecture_tmp1
                ),
                lecture_tmp3 AS (
                    SELECT
                        subjtNm as name,
                        cgprfNm as professors,
                        subjtnbCorsePrcts as code,
                        lctreTimeNm as schedule,
                        lecrmNm as classroom,
                        subjtClNm as view_type,
                        gradeEvlMthdDivNm as gradle_eval_type,
                        hy as avaiable_grade
                    FROM 
                        lecture_tmp2
                )
            SELECT *
            FROM lecture_tmp3;
    """

class YonseiLectureDuckDBService(
    val mysqlHost: String,
    val mysqlDatabase: String,
    val mysqlUser: String,
    val mysqlPassword: String,
    jdbcUrl: String = "jdbc:duckdb:"
) {
    val conn: Connection = DriverManager.getConnection(jdbcUrl)

    init {
        startAndConnectMyql()
    }

    fun startAndConnectMyql() {
        val stmt: Statement = conn.createStatement()

        stmt.execute(connectMysqlDuckDB(mysqlHost, mysqlDatabase, mysqlUser, mysqlPassword))
        stmt.execute(importMysqlLectureTableDuckDB)

        stmt.close()
    }

    fun retriveAll(query: String): List<String> {
        val stmt: Statement = conn.createStatement()
        val result: List<String> = stmt.executeQuery(query).map { it.getString(1) }
        stmt.close()
        return result
    }

    private inline fun <reified T> ResultSet.map(transform: ((ResultSet) -> T)): List<T> {
        val acc = mutableListOf<T>()
        while (this.next()) {
            acc.add(transform(this))
        }
        return acc
    }
}