import duckdb.YonseiLectureDuckDBService

fun main() {
    val lectureDuckDBService = YonseiLectureDuckDBService(
        System.getenv("mysqlHost"),
        System.getenv("mysqlDatabase"),
        System.getenv("mysqlUser"),
        System.getenv("mysqlPassword")
    )

    val classrooms = lectureDuckDBService.retriveAll("select classroom from lecture")

    classrooms.filterNotNull()
        .map { LectureParser.parseClassroom(it) }
        .onEach { println(it) }

}