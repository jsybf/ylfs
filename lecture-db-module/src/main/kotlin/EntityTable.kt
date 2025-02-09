package io.gitp.ysfl.db

import io.gitp.ysfl.client.Semester
import io.gitp.ysfl.db.custom_type.year
import org.jetbrains.exposed.sql.ColumnTransformer
import org.jetbrains.exposed.sql.Table
import java.time.Year


object DptGroupTbl : Table("dpt_group") {
    val dptGroupId = char("dpt_group_id", 6)
    val name = varchar("name", 50)

    val semester = char("semester", 6).check { it inList listOf("FIRST", "SECOND") }.transform(SemesterTransform())
    val year = year("year").transform(YearTransform())

    override val primaryKey = PrimaryKey(dptGroupId)

}

object DptTbl : Table("dpt") {
    val dptId = char("dpt_id", 5)
    val dptGroupId = reference("dpt_group_id", DptGroupTbl.dptGroupId)
    val name = varchar("name", 50)

    val semester = char("semester", 6).check { it inList listOf("FIRST", "SECOND") }.transform(SemesterTransform())
    val year = year("year").transform(YearTransform())

    override val primaryKey = PrimaryKey(dptGroupId)

}

private class YearTransform() : ColumnTransformer<String, Year> {
    override fun unwrap(value: Year): String = value.toString()

    override fun wrap(value: String): Year = Year.parse(value)
}

private class SemesterTransform() : ColumnTransformer<String, Semester> {
    override fun unwrap(value: Semester): String = value.name

    override fun wrap(value: String): Semester = Semester.valueOf(value)

}