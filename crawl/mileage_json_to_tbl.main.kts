@file:DependsOn("mysql:mysql-connector-java:8.0.33")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Statement


// https://github.com/Kotlin/obsolete-kotlin-jdbc/blob/master/src/main/kotlin/kotlin/jdbc/ResultSets.kt
fun <T> ResultSet.map(transform: (ResultSet) -> T): Iterable<T> {

    val rs = this

    val iterator = object : Iterator<T> {
        override fun hasNext(): Boolean = rs.next()

        override fun next(): T = transform(rs)
    }

    return object : Iterable<T> {
        override fun iterator(): Iterator<T> = iterator
    }
}

fun getSampleJson(conn: Connection): JsonObject {
    val stmt: Statement = conn.createStatement()

    /* select query */
    val courseSample = stmt.executeQuery(
        """
    SELECT year, semester, dpt_id, http_resp_body
    FROM course_request
    where crawl_job_id = 1
    LIMIT 1
"""
    )
        .map { resultSet -> resultSet.getString("http_resp_body") }
        .toList()
        .first()

    stmt.close()
    return Json.decodeFromString(courseSample)

}

fun findNullColumns(conn: Connection) = conn.createStatement().use { stmt ->
    val columns: Set<String> = getSampleJson(conn)["dsSles251"]!!
        .jsonArray
        .first()
        .let { jsonElement ->
            val json = jsonElement.jsonObject
            json.keys
        }

    fun query(columnName: String) = """
        SELECT DISTINCT ${columnName} FROM course_tbl
    """.trimIndent()

    columns.onEach { column ->
        val results = stmt.executeQuery(query(column)).map { resultSet -> resultSet.getString(1) }.toList()
        if (results.size == 1 && results.first() == null) {
            println("null column: ${column}")

        }

    }
}


/* boilerplat  */
val conn: Connection = DriverManager.getConnection(
    "jdbc:mysql://43.202.5.149/crawl",
    "root",
    "root_pass"
)

findNullColumns(conn)




conn.close()