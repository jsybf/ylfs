package io.gitp.yfls.scarping.job.file

import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands

fun main(args: Array<String>) = CommandRoot()
    .subcommands(ScapeThenSaveFileCommand(), TransformRawResponseFileCommand())
    .main(args)