package io.gitp.ylfs.parse_load_job.tables.types

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.IDateColumnType
import org.jetbrains.exposed.sql.Table
import java.time.Year


internal class YearColumnType : ColumnType<Year>(), IDateColumnType {
    override fun sqlType(): String = "YEAR"
    override val hasTimePart: Boolean = false

    override fun notNullValueToDB(value: Year): Any {
        // println("!notNullValueToDB called")
        return "$value"
    }

    override fun nonNullValueToString(value: Year): String {
        // println("!nonNullValueToString called")
        return "'$value'"
    }

    override fun nonNullValueAsDefaultString(value: Year): String {
        // println("!nonNullValueAsDefaultString called")
        throw NotImplementedError("fuck")
    }

    // override fun valueToDB(value: Year?): Any? {
    //     println("valueToDB called")
    //     return super.valueToDB(value)
    // }
    //
    // override fun valueToString(value: Year?): String {
    //     println("valueToString called")
    //     return super.valueToString(value)
    // }
    //
    // override fun valueAsDefaultString(value: Year?): String {
    //     println("valueAsDefaultString called")
    //     return super.valueAsDefaultString(value)
    // }
    //
    // override fun readObject(rs: ResultSet, index: Int): Any? {
    //     println("readObject called")
    //     return super.readObject(rs, index)
    // }
    //
    // override fun setParameter(stmt: PreparedStatementApi, index: Int, value: Any?) {
    //     println("setParameter called")
    //     super.setParameter(stmt, index, value)
    // }
    //
    // override fun validateValueBeforeUpdate(value: Year?) {
    //     println("validateValueBeforeUpdate called")
    //     super.validateValueBeforeUpdate(value)
    // }
    //
    // override fun parameterMarker(value: Year?): String {
    //     println("parameterMarker called")
    //     super.validateValueBeforeUpdate(value)
    //     return super.parameterMarker(value)
    // }
    //
    override fun valueFromDB(value: Any): Year = when (value) {
        is java.sql.Date -> value.toLocalDate().year.let { Year.of(it) }
        else -> error("Retrieved unexpected value of type ${value::class.simpleName}")
    }
}

internal fun Table.year(name: String): Column<Year> = registerColumn(name, YearColumnType())
