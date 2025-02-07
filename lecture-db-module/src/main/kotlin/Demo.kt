package io.gitp.ysfl.db

import io.gitp.ysfl.client.response.LectureResp
import io.gitp.ysfl.db.LectureJsonTable.json
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun main() {
    Database.connect(
        url = "jdbc:mysql://43.202.5.149:3306/test_db",
        driver = "com.mysql.cj.jdbc.Driver",
        user = "root",
        password = "root_pass"
    )
    transaction {
        LectureJsonTable
            .selectAll()
            .map { it[json] }
            .map { Json.decodeFromString<LectureResp>(it) }
            .filter { 1 < it.classrooms.size }
            .filter { 1 < it.schedules.size }
            .filter { it.schedules.size == it.classrooms.size }
            .forEach { println("${it.classrooms}\t${it.schedules}\t${it.name}") }

    }
}