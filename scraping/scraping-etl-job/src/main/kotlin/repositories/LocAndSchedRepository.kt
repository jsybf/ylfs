package io.gitp.ylfs.scraping.scraping_tl_job.repositories

import io.gitp.ylfs.entity.model.LocAndSched
import io.gitp.ylfs.scraping.scraping_tl_job.tables.LocAndSchedTbl
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction

class LocAndSchedRepository(
    private val db: Database
) {
    fun insert(locAndSchedList: List<LocAndSched>, subClassId: Int): Int = transaction(db) {
        LocAndSchedTbl.insertAndGetId {
            it[LocAndSchedTbl.subclassId] = subClassId
            it[LocAndSchedTbl.locAndSched] = locAndSchedList
        }.value
    }
}