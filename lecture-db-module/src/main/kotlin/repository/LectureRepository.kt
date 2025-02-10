package io.gitp.ysfl.db.repository

import io.gitp.ysfl.client.PairList
import io.gitp.ysfl.client.response.Lecture
import io.gitp.ysfl.client.response.LectureId
import io.gitp.ysfl.client.response.LocationUnion
import io.gitp.ysfl.client.response.Schedule
import io.gitp.ysfl.db.LectureSchedLocTbl
import io.gitp.ysfl.db.LectureTbl
import io.gitp.ysfl.db.LocationTbl
import io.gitp.ysfl.db.ScheduleTbl
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

class LectureRepository(
    private val db: Database
) {
    private val lectureSchedLocRepository = LectureSchedLocRepository(db)

    fun insert(lecture: Lecture, dptId: String): Pair<Int, PairList<List<Int>, Int>> {
        val lectureId = insertOnlyLecture(lecture.lectureId, lecture.name, dptId)
        val locId: PairList<List<Int>, Int> = lectureSchedLocRepository.insert(lecture.locationAndSchedule, lectureId)

        return Pair(lectureId, locId)
    }

    fun insertOnlyLecture(lectureId: LectureId, name: String, dptId: String): Int = transaction(db) {
        LectureTbl.insert {
            it[LectureTbl.dptId] = dptId

            it[LectureTbl.mainId] = lectureId.mainId
            it[LectureTbl.classDivisionId] = lectureId.classDivisionId
            it[LectureTbl.subId] = lectureId.subId

            it[LectureTbl.name] = name
        }[LectureTbl.id]
    }
}


class LectureSchedLocRepository(
    private val db: Database
) {
    fun insert(schedAndLoc: Map<Schedule, LocationUnion>, lectureId: Int): PairList<List<Int>, Int> = transaction(db) {
        val schedIdsAndLocId: PairList<List<Int>, Int> = schedAndLoc.map { (sched, loc) ->
            val schedIds: List<Int> = insertSchedule(sched)
            val locId: Int = insertLocation(loc)
            Pair(schedIds, locId)
        }

        val schedIdAndLocId: PairList<Int, Int> = schedIdsAndLocId
            .flatMap { (schedIds, locId) -> schedIds.map { Pair(it, locId) } }


        schedIdAndLocId
            .map { (schedId, locId) ->
                insertLectureSchedLocJunction(
                    lectureId = lectureId,
                    scheduleId = schedId,
                    locationid = locId
                )
            }

        return@transaction schedIdsAndLocId
    }

    fun insertLectureSchedLocJunction(lectureId: Int, scheduleId: Int, locationid: Int) {
        LectureSchedLocTbl.insert {
            it[LectureSchedLocTbl.lectureId] = lectureId
            it[LectureSchedLocTbl.scheduleId] = scheduleId
            it[LectureSchedLocTbl.locationId] = locationid
        }
    }

    fun insertLocation(location: LocationUnion): Int = transaction(db) {
        LocationTbl.insert {
            when (location) {
                is LocationUnion.OffLine -> {
                    it[LocationTbl.type] = "OFFLINE"
                    it[LocationTbl.offlineBuilding] = location.building
                    it[LocationTbl.offlineAddress] = location.address
                }
                is LocationUnion.RealTimeOnline -> {
                    it[LocationTbl.type] = "REAL_TIME_ONLINE"
                }
                is LocationUnion.Online -> {
                    it[LocationTbl.type] = "ONLINE"
                    it[LocationTbl.duplicateCapability] = location.duplicateCapability
                }
            }
        }[LocationTbl.id]
    }

    /**
     * [Schedule]즉 `DayOfweek, List<Int>`를 `DayOfWeek`, `Int`의 페어로 테이블에 저장
     * @return autoincrement id들 반환
     */
    fun insertSchedule(schedule: Schedule): List<Int> = transaction(db) {
        schedule.periods.map { period ->
            ScheduleTbl.insert {
                it[ScheduleTbl.day] = schedule.dayOfWeek
                it[ScheduleTbl.period] = period
            }[ScheduleTbl.id]
        }
    }
}
