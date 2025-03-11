package io.gitp.ylfs.scraping.scraping_tl_job.tables

import io.gitp.ylfs.entity.model.LocAndSched
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.json.json

object LocAndSchedTbl : IntIdTable("loc_sched", "loc_sched_id") {
    val subclassId = reference("subclass_id", SubClassTbl)
    val locAndSched = json<List<LocAndSched>>("loc_and_sched", Json.Default)
}
