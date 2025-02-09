package io.gitp.ysfl.db.repository

import io.gitp.ysfl.client.Semester
import io.gitp.ysfl.client.response.DptGroup
import io.gitp.ysfl.db.DptGroupTbl
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Year

class DptGroupRepository(private val db: Database) {
    fun insert(dptGroup: DptGroup, year: Year, semester: Semester) = transaction(db) {
        DptGroupTbl.insert {
            it[name] = dptGroup.dptGroupName
            it[dptGroupId] = dptGroup.dptGroupId
            it[DptGroupTbl.semester] = semester.name
            it[DptGroupTbl.year] = year
        }
    }
}

// fun main() {
//     val db: Database = Database.connect(
//         url = "jdbc:mysql://43.202.5.149:3306/test_db",
//         driver = "com.mysql.cj.jdbc.Driver",
//         user = "root",
//         password = "root_pass"
//     )
//
//     val repo: DptGroupRepository = DptGroupRepository(db)
//
//     // val dptGroup = DptGroup("지역사회와세계", "s11000")
//     // repo.insert(dptGroup, Year.of(2025), Semester.SECOND)
//
//     transaction(db) {
//         DptGroupTbl.selectAll().map { it[DptGroupTbl.year] }.forEach { println(it) }
//
//     }
// }