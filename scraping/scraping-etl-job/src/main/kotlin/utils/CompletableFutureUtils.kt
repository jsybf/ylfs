package io.gitp.ylfs.scraping.scraping_tl_job.utils

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

fun <T> supplyAsync(executor: Executor, block: () -> T): CompletableFuture<T> = CompletableFuture.supplyAsync(block, executor)