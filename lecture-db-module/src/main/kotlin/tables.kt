package io.gitp.ysfl.db

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object DptGroupTable : Table(name = "dpt_group") {
    val dptGroupId = varchar("dpt_group_id", 30)
    val name = varchar("name", 30).uniqueIndex()
}

object DptTable : Table(name = "dpt") {
    val dptId = varchar("dpt_id", 30)
    val dptGroupId = varchar("dpt_group_id", 30)
    val name = varchar("name", 30).uniqueIndex()
}

object LectureJsonTable : Table(name = "lecture_json") {
    val dptId = varchar("dpt_id", 30).uniqueIndex()
    val json = text("json")
    val lectureJsonId = integer("lecture_json_id")
}


object CrawlJob : Table("crawl_job") {
    val crawlJobId = integer("crawl_job_id").autoIncrement().uniqueIndex()
    val startDatetime = timestamp("start_datetime").defaultExpression(CurrentTimestamp)
    val endDatetime = timestamp("end_datetime").nullable()

    override val primaryKey = PrimaryKey(crawlJobId)
}

object DptGroupRequest : Table("dpt_group_request") {
    val dptGroupRequestId = integer("dpt_group_request").autoIncrement().uniqueIndex()
    val crawlJobId = integer("crawl_job_id").references(CrawlJob.crawlJobId, onDelete = ReferenceOption.CASCADE)

    val year = integer("year").nullable()
    val semester = varchar("semester", 30).nullable()

    val httpRespBody = text("http_resp_body").nullable()

    override val primaryKey = PrimaryKey(dptGroupRequestId)
}

object DptRequest : Table("dpt_request") {
    val dptRequestId = integer("dpt_request").autoIncrement().uniqueIndex()
    val crawlJobId = integer("crawl_job_id").references(CrawlJob.crawlJobId, onDelete = ReferenceOption.CASCADE)

    val year = integer("year")
    val semester = varchar("semester", 30)
    val dptGroupId = varchar("dpt_group_id", 30)

    val httpRespBody = text("http_resp_body").nullable()

    override val primaryKey = PrimaryKey(dptRequestId)
}

object LectureRequest : Table("lecture_request") {
    val lectureRequestId = integer("lecture_request").autoIncrement().uniqueIndex()
    val crawlJobId = integer("crawl_job_id").references(CrawlJob.crawlJobId, onDelete = ReferenceOption.CASCADE)

    val year = integer("year")
    val semester = varchar("semester", 30)
    val dptGroupId = varchar("dpt_group_id", 30)
    val dptId = varchar("dpt_id", 30)

    val httpRespBody = text("http_resp_body").nullable()

    override val primaryKey = PrimaryKey(lectureRequestId)
}

