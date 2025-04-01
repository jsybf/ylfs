package io.gitp.yfls.scarping.job.file

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.obj
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import io.gitp.yfls.scarping.job.file.request.job
import io.gitp.yfls.scarping.job.file.transform.TransformJob
import io.gitp.ylfs.entity.enums.Semester
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.time.Year
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

class CommandRoot() : CliktCommand("ingress") {
    override fun run() {
        currentContext.obj = object {}
    }
}

class ScapeThenSaveFileCommand() : CliktCommand("scrape") {
    private val logger = LoggerFactory.getLogger(this::class.java)

    val year: Year by option("-y", "--year").int().convert { Year.of(it) }.required().help { "ex: 2023" }
    val semester: Semester by option("-s", "--semester").int().convert { Semester.codeof(it)!! }.required().help { "ex: 10 or 20" }
    val path: Path by option("-p", "--path").convert { Path.of(it) }.required().help { "files save base path. relative, absolte is both allowed" }
    // val target: Int by option("-t", "--target").int().required().help { "scraping util certain level.  1: college,2: department,3: lecture 4: mileage" }

    override fun run() {
        logger.info("====param====")
        logger.info("year:{}", year)
        logger.info("semester:{}", semester)
        logger.info("path:{}", path)
        logger.info("=============")
        runBlocking { job(year, semester, path) }
    }
}

// --from <from_path> --to <to_path>
class TransformRawResponseFileCommand() : CliktCommand("transform") {
    private val logger = LoggerFactory.getLogger(object {}::class.java)

    private val inputDir: Path by option("--input_dir", "-i").convert { Path.of(it) }.required()
    private val outputDir: Path by option("--output_dir", "-o").convert { Path.of(it) }.required()


    private fun createIfNotExists(dir: Path) {
        when {
            dir.exists() && !dir.isDirectory() -> throw IllegalStateException("file exist at give path:${dir}")
            dir.exists() && dir.isDirectory() -> return
            !dir.exists() -> dir.createDirectories()
        }
    }

    override fun run() {
        val abseInputDir: Path = inputDir.toAbsolutePath().normalize()
        val absOutputDir: Path = outputDir.toAbsolutePath().normalize()

        createIfNotExists(abseInputDir)
        createIfNotExists(absOutputDir)

        logger.info("====param====")
        logger.info("inputDir:${abseInputDir}")
        logger.info("outputDir:${absOutputDir}")
        logger.info("=============")

        TransformJob.run(inputDir, outputDir)
    }
}

class LoadToMysqlCommand() : CliktCommand("load") {
    private val logger = LoggerFactory.getLogger(object {}::class.java)

    private val inputDir: Path by option("--input_dir", "-i").convert { Path.of(it) }.required()
    private val mysqlHost: String by option("--mysql_host", envvar = "YLFS_MYSQL_HOST").required()
    private val mysqlPort: String by option("--mysql_port", envvar = "YLFS_MYSQL_PORT").required()
    private val mysqlDatabase: String by option("--mysql_db", envvar = "YLFS_MYSQL_DB").required()
    private val mysqlUser: String by option("--mysql_user", envvar = "YLFS_MYSQL_USER").required()
    private val mysqlPassword: String by option("--mysql_user", envvar = "YLFS_MYSQL_USER").required()

    override fun run() {
        TODO()
    }
}