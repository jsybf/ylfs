package io.gitp.yfls.scarping.job.file

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import io.gitp.ylfs.entity.type.Semester
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import java.time.Year

class ScapeThenSaveFileCommand() : CliktCommand() {
    val year: Year by option("-y", "--year").int().convert { Year.of(it) }.required().help { "ex: 2023" }
    val semester: Semester by option("-s", "--semester").int().convert { Semester.codeof(it)!! }.required().help { "ex: 10 or 20" }
    val path: Path by option("-p", "--path").convert { Path.of(it) }.required().help { "files save base path. relative, absolte is both allowed" }
    val target: Int by option("-t", "--target").int().required().help { "scraping util certain level.  1: college,2: department,3: lecture 4: mileage" }

    override fun run() {
        println(
            """
            year: $year
            semester: $semester
            path: $path
            target: $target
        """.trimIndent()
        )

        runBlocking { job(year, semester, path) }
    }
}
