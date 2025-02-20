package io.gitp.ylfs.scraping.scraping_req_job

import ch.qos.logback.classic.Level
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import io.gitp.ylfs.entity.type.Semester
import org.slf4j.LoggerFactory
import java.time.Year

private class CrawlJobCommand : CliktCommand() {
    init {
        context {
            helpFormatter = { MordantHelpFormatter(it, showRequiredTag = true) }
        }
    }

    override fun help(context: Context): String = """
    request data to yonsei lecture finding server.

    this cli can crawl the following data ${"\u0085"}
        - department group ${"\u0085"}
        - department ${"\u0085"}
        - course ${"\u0085"}
        - mileage ${"\u0085"}
    these data are hierarchical, where each of them depends on upper one.
    --depth option specifies request data target respecting this hierarchy.

    (example)${"\u0085"}
    `--depth 3` means request course data. then program sequentially requests deptartment group ->
    department -> course to get course data.
    """.trimIndent()

    private val requestDepthHelp = """
        set request target data. ${"\u0085"}
        1: DptGroup 2: Dpt 3: Course 4: Mileage ${"\u0085"}
        see help text for more info. ${"\u0085"}
    """.trimIndent()

    private val mysqlUsername by option("--m_user", help = "mysql username").required()
    private val mysqlPassword by option("--m_pass", help = "mysql user password").required()
    private val mysqlHost by option("--m_host", help = "mysql host").required()
    private val mysqlDatabase by option("--m_db", help = "mysql database name").required()
    private val requestYear by option("--year", help = "year to request. ex: 2023").convert { Year.parse(it) }.required()
    private val requestSemester by option("--semester", help = "semesterto request.'FIRST', 'SECOND' available").convert { Semester.valueOf(it) }
        .required()
    private val logLevel by option("--log_level", help = "'ERROR', 'WARN', 'INFO', 'DEBUG', 'TRACE', 'OFF' avaiable. default: 'DEBUG'")
        .convert { Level.toLevel(it) }.default(Level.DEBUG)
    private val requestDepth by option("--depth", help = requestDepthHelp).int().required()

    override fun run() {
        LoggerFactory.getLogger("com.zaxxer.hikari").apply {
            val logger = this as (ch.qos.logback.classic.Logger)
            logger.level = Level.INFO
        }
        // set logging level
        val logger = LoggerFactory.getLogger("io.gitp.ylfs") as (ch.qos.logback.classic.Logger)
        logger.level = logLevel

        val crawlJobRepo = CrawlJobRepository(
            mysqlHost,
            mysqlDatabase,
            mysqlUsername,
            mysqlPassword,
            requestYear,
            requestSemester
        )
        val crawlJob = CrawlJob(
            crawlJobRepo,
            requestYear,
            requestSemester,
            requestDepth,
            64
        )

        crawlJob.execute()

    }


}

fun main(args: Array<String>) = CrawlJobCommand().main(args)
