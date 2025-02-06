import duckdb.YonseiLectureDuckDBService

fun main() {
    val lectureDuckDBService = YonseiLectureDuckDBService(
        System.getenv("mysqlHost"),
        System.getenv("mysqlDatabase"),
        System.getenv("mysqlUser"),
        System.getenv("mysqlPassword")
    )

    val classrooms = lectureDuckDBService.retriveAll("select classroom from lecture")


    val physicalExcluded = classrooms.filterNotNull().filter {
        var parCnt = 0
        it.forEach { c ->
            if (c == '(') parCnt++
            else if (c == ')') parCnt--

            if (1 < parCnt) return@filter false
        }
        true
    }.also { println("size of physicalExclude: ${it.size}") }

    physicalExcluded
        .map { LectureParser.parseClassroom(it) }
        .onEach { println(it) }

}