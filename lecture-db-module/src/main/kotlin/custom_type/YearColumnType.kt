package io.gitp.ysfl.db.custom_type

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.IDateColumnType
import org.jetbrains.exposed.sql.StringColumnType
import org.jetbrains.exposed.sql.Table

class YearColumnType : StringColumnType(), IDateColumnType {
    override fun sqlType(): String = "YEAR"
    override val hasTimePart: Boolean = false

    override fun valueFromDB(value: Any): String = when (value) {
        is java.sql.Date -> value.toString().substringBefore('-')
        else -> error("Retrieved unexpected value of type ${value::class.simpleName}")
    }

}

fun Table.year(name: String): Column<String> = registerColumn(name, YearColumnType())