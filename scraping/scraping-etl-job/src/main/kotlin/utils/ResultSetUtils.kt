package io.gitp.ylfs.scraping.scraping_tl_job.utils

import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.sql.ResultSet

fun <T : Any> String.execAndMap(transform: (ResultSet) -> T): List<T> {
    val result = mutableListOf<T>()
    TransactionManager.current().exec(this) { rs ->
        while (rs.next()) {
            result += transform(rs)
        }
    }
    return result
}

fun <T : Any> String.execAndNullableMap(transform: (ResultSet) -> T?): List<T?> {
    val result = mutableListOf<T?>()
    TransactionManager.current().exec(this) { rs ->
        while (rs.next()) {
            result += transform(rs)
        }
    }
    return result
}

internal fun ResultSet.getIntOrNull(columnName: String): Int? = this.getInt(columnName).let { if (this.wasNull()) null else it }
internal fun ResultSet.getStringOrNull(columnName: String): String? = this.getString(columnName)
