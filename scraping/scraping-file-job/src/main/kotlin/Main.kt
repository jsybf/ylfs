package io.gitp.yfls.scarping.job.file

import com.github.ajalt.clikt.core.main
import io.gitp.yfls.scarping.job.file.request.ScapeThenSaveFileCommand

fun main(args: Array<String>) = ScapeThenSaveFileCommand().main(args)