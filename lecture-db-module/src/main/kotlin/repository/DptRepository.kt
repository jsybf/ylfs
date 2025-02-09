package io.gitp.ysfl.db.repository

import io.gitp.ysfl.client.Semester
import io.gitp.ysfl.client.response.Dpt
import io.gitp.ysfl.client.response.DptGroup
import io.gitp.ysfl.db.DptTbl
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Year

class DptRepository(private val db: Database) {
    fun insert(dpt: Dpt, dptGroupId: String, year: Year, semester: Semester) = transaction(db) {
        DptTbl.insert {
            it[DptTbl.dptId] = dpt.dptId
            it[DptTbl.name] = dpt.dptName
            it[DptTbl.dptGroupId] = dptGroupId

            it[DptTbl.semester] = semester
            it[DptTbl.year] = year
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
//     val dptGroupRepo: DptGroupRepository = DptGroupRepository(db)
//     val dptRepo = DptRepository(db)
//
//
//     val dptGroup = DptGroup("dpt1", "s11000")
//     val dpt1 = Dpt("글쓰기", "30107")
//     val dpt2 = Dpt("기독의이해", "30106")
//     dptGroupRepo.insert(dptGroup, Year.of(2025), Semester.SECOND)
//     dptRepo.insert(dpt1, dptGroup.dptGroupId, Year.of(2025), Semester.SECOND)
// }