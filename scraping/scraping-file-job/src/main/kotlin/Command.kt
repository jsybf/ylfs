package io.gitp.yfls.scarping.job.file

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import java.nio.file.Path
import java.time.Year

enum class Semester(val num: Int) {
    FIRST(1), SECOND(2);

    companion object {
        fun byNum(num: Int): Semester? = entries.find { it.num == num }
    }
}

class ScapeThenSaveFileCommand() : CliktCommand() {
    // crawl text --year 2024 --semester 1 --path ./data/24-1 --target 4
    val year: Year by option("-y", "--year").int().convert { Year.of(it) }.required().help { "ex: 2023" }
    val semester: Semester by option("-s", "--semester").int().convert { Semester.byNum(it)!! }.required().help { "ex: 1 or 2" }
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
    }
}
