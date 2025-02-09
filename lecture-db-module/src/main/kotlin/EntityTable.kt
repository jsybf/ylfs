package io.gitp.ysfl.db

import io.gitp.ysfl.db.custom_type.year
import org.jetbrains.exposed.sql.ColumnTransformer
import org.jetbrains.exposed.sql.Table
import java.time.Year


object DptGroupTbl : Table("dpt_group") {
    val name = varchar("name", 50)
    val dptGroupId = char("dpt_group_id", 6)

    val semester = char("semester", 6).check { it inList listOf("FIRST", "SECOND") }

    // val year = char("year", 4) // TODO: add custom year type
    val year = year("year").transform(YearTransform())

    override val primaryKey = PrimaryKey(dptGroupId)

}

object DptTbl

private class YearTransform() : ColumnTransformer<String, Year> {
    override fun unwrap(value: Year): String = value.toString()

    override fun wrap(value: String): Year = Year.parse(value)

}