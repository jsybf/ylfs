import duckdb.YonseiLectureDuckDBService

fun main() {
    val lectureDuckDBService = YonseiLectureDuckDBService(
        System.getenv("mysqlHost"),
        System.getenv("mysqlDatabase"),
        System.getenv("mysqlUser"),
        System.getenv("mysqlPassword")
    )
    val schedules = lectureDuckDBService.retriveAll("select schedule from lecture")

    schedules
        .filterNotNull()
        .map { LectureParser.parseSchedule(it) }
        .onEach { println(it) }

}