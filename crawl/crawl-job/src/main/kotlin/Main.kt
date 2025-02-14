package io.gitp.ylfs.crawl.crawljob

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME
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

class CrawlJobCommand() : CliktCommand() {

    val requestDepthHelp = """
        set request target data. ${"\u0085"}
        1: DptGroup 2: Dpt 3: Course 4: Mileage ${"\u0085"}
        see help text for more info. ${"\u0085"}
    """.trimIndent()

    val mysqlUsername by option("--m_user", help = "mysql username").required()
    val mysqlPassword by option("--m_pass", help = "mysql user password").required()
    val mysqlHost by option("--m_host", help = "mysql host").required()
    val mysqlDatabase by option("--m_db", help = "mysql database name").required()
    val requestYear by option("--year", help = "year to request. ex: 2023").convert { Year.parse(it) }.required()
    val requestSemester by option("--semester", help = "semesterto request.'FIRST', 'SECOND' available").convert { Semester.valueOf(it) }.required()
    val logLevel by option("--log_level", help = "'ERROR', 'WARN', 'INFO', 'DEBUG', 'TRACE', 'OFF' avaiable. default: 'DEBUG'")
        .convert { Level.toLevel(it) }.default(Level.DEBUG)
    val requestDepth by option("--depth", help = requestDepthHelp).int().required()


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


    override fun run() {
        // set logging level
        val logger = LoggerFactory.getLogger(ROOT_LOGGER_NAME) as (ch.qos.logback.classic.Logger)
        logger.level = logLevel

        crawlJob(
            mysqlUsername,
            mysqlPassword,
            mysqlHost,
            mysqlDatabase,
            requestYear,
            requestSemester,
            requestDepth
        )

    }

}

fun main(args: Array<String>) = CrawlJobCommand().main(args)