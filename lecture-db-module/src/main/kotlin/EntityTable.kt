package io.gitp.ysfl.db

import io.gitp.ysfl.client.Semester
import io.gitp.ysfl.db.custom_type.year
import org.jetbrains.exposed.sql.ColumnTransformer
import org.jetbrains.exposed.sql.Table
import java.time.DayOfWeek
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

object LectureTbl : Table("lecture") {
    val id = integer("lecture_id").autoIncrement()
    val dptId = varchar("dpt_id", 5) references DptTbl.dptId

    val mainId = char("main_id", 7)
    val classDivisionId = char("class_division_id", 2)
    val subId = char("sub_id", 2)

    val name = char("name", 100)

    override val primaryKey = PrimaryKey(id)

    init {
        index(isUnique = false, mainId, classDivisionId, subId)
    }
}

object LocationTbl : Table("location") {
    val id = integer("location_id").autoIncrement()
    val type = varchar("type", 16).check { it inList listOf("REAL_TIME_ONLINE", "ONLINE", "OFFLINE") }

    // OffLine columns
    val offlineBuilding = varchar("offline_building", 10).nullable()
    val offlineAddress = varchar("offline_address", 10).nullable()

    // Online columns
    val duplicateCapability = bool("duplicate_capability").nullable()
}

object ProfessorTbl : Table("professor") {
    val id = integer("professor_id").autoIncrement()
    val name = varchar("name", 30)

    override val primaryKey = PrimaryKey(id)
}

object ScheduleTbl : Table("schedule") {
    val id = integer("schedule_id").autoIncrement()
    val day = varchar("day", 9).check { it inList listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY") }
        .transform(DayOfWeekTransform())
    val period = integer("periods").check { it.greaterEq(-1) }
}


object LectureSchedLocTbl : Table("lecture_sched_loc") {
    val lectureId = integer("lecture_id") references LectureTbl.id
    val scheduleId = integer("schedule_id") references ScheduleTbl.id
    val locationId = integer("location_id") references LocationTbl.id

    // 인덱스
    init {
        index(isUnique = false, lectureId)
    }
}

// LectureProfessors 테이블
object LectureProfessors : Table() {
    val lectureId = integer("lecture_id") references LectureTbl.id
    val professorId = integer("professor_id") references ProfessorTbl.id

    // 복합 인덱스 설정
    init {
        index(isUnique = false, lectureId, professorId)
    }
}


private class YearTransform() : ColumnTransformer<String, Year> {
    override fun unwrap(value: Year): String = value.toString()

    override fun wrap(value: String): Year = Year.parse(value)
}

private class SemesterTransform() : ColumnTransformer<String, Semester> {
    override fun unwrap(value: Semester): String = value.name

    override fun wrap(value: String): Semester = Semester.valueOf(value)

}

private class DayOfWeekTransform() : ColumnTransformer<String, DayOfWeek> {
    override fun unwrap(value: DayOfWeek): String = value.name

    override fun wrap(value: String): DayOfWeek = DayOfWeek.valueOf(value)

}
