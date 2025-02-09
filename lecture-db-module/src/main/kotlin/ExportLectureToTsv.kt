package io.gitp.ysfl.db

import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.io.path.Path
import kotlin.io.path.bufferedWriter


fun main() {

    val exportPath = Path("lectures.tsv").toAbsolutePath()

    val db: Database = Database.connect(
        url = "jdbc:mysql://43.202.5.149:3306/test_db",
        driver = "com.mysql.cj.jdbc.Driver",
        user = "root",
        password = "root_pass"
    )

    val lectureJsons: List<String> = transaction {
        LectureRequest.select(LectureRequest.httpRespBody).map { it[LectureRequest.httpRespBody] }
    }.filterNotNull()

    // val jsonContent: (JsonElement.(String) -> String) = { keyName -> this.jsonObject[keyName]!!.jsonPrimitive.content }

    val lectures: List<Map<String, String>> = lectureJsonsToMap(lectureJsons)
    val columns = lectures.first().keys.toList()
    val tsv = lectures.map { lecture: Map<String, String> ->
        columns.map { key -> lecture[key] }.joinToString("\t")
    }.toMutableList().apply { this.addFirst(columns.joinToString("\t")) }

    exportPath
        .also { println("writing to $it") }
        .bufferedWriter()
        .use { writer ->
            tsv.forEach { line -> writer.write(line + "\n") }
        }
}

fun lectureJsonsToMap(lectureJsons: List<String>): List<Map<String, String>> =
    lectureJsons
        .map { Json.decodeFromString<JsonObject>(it) }
        .flatMap { json -> json["dsSles251"]!!.jsonArray }
        .map { it.jsonObject }
        .map { json ->
            json.entries.associate { (key, value) -> Pair(key, value.jsonPrimitive.content) }
        }
