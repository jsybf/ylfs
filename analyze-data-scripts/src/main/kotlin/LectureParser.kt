object LectureParser {
    /**
     * api(1/2)
     */
    fun parseSchedule(scheduleRaw: String): Map<Char, List<Int>> {

        val dayDelimiter = Regex("""[/,)(]""")
        val parseSchedule = Regex("""(?<day>[월화수목금토일])(?<times>(-?\d{1,2}\s*)+)""")

        val whiteSpaceSperated = scheduleRaw.replace(dayDelimiter, " ")

        val result: Map<Char, List<Int>> = parseSchedule
            .findAll(whiteSpaceSperated)
            .groupBy(
                keySelector = { it.groups["day"]!!.value[0].toChar() },
                valueTransform = { it.groups["times"]!!.value.trim().split(" ").map { n -> n.toInt() } }
            )
            .mapValues { (_, period: List<List<Int>>) -> period.flatten() }
        return result
    }

    /**
     * api(2/2) except for these 2 apis, every method is internal method.
     */
    fun parseClassroom(input: String): List<ClassroomUnion> = lexerClassroom(tokenizeClassroom(input))


    // @formatter:off
    internal fun tokenizeClassroom(classroomRaw: String): List<String> {

        // 체육과목의 강의실들중 댑부분이 (스포츠(다목적실))/(볼링장) 이런형태이다.
        // (스포츠-다목적실)/(볼링장) 이렇게 바꿔주자.
        val regex:Regex = Regex("""\((?<building>[가-힣0-9]+)\((?<address>[가-힣0-9]+)\)\)_?""")

        val classroomRaw1 = regex
            .findAll(classroomRaw)
            .map {
                val building = it.groups["building"]!!.value
                val address = it.groups["address"]!!.value
                "${building}-${address}"
            }
            .joinToString("/")
            .takeIf { it != "" }
            ?: classroomRaw

        val placeList = mutableListOf<String>()
        val curToken = StringBuilder()

        classroomRaw1.forEach {
            when (it) {
                '/', '(', ')' -> { placeList.add(curToken.toString());curToken.clear() }
                else -> { curToken.append(it) }
            }
        }
        placeList.add(curToken.toString())

        return placeList.filter { it != "" }
    }
    // @formatter:on

    internal fun lexerClassroom(tokens: List<String>): List<ClassroomUnion> {
        if (tokens.size == 1) {
            return when (tokens[0]) {
                "실시간온라인" -> listOf(ClassroomUnion.RealTimeOnline())
                "동영상", "동영상콘텐츠" -> listOf(ClassroomUnion.Online(true))
                else -> listOf(ClassroomUnion.OffLine.of(tokens[0]))
            }
        }

        var i = 0
        val parsedClassroomList = mutableListOf<ClassroomUnion>()

        // @formatter:off
        while (i < tokens.size - 1) {
            when (tokens[i]) {
                "실시간온라인" -> ClassroomUnion.RealTimeOnline()
                "동영상", "동영상콘텐츠" -> {
                    if (tokens[i + 1] != "중복수강불가") ClassroomUnion.Online(false)
                    else { i++; ClassroomUnion.Online(true) }
                }
                else -> ClassroomUnion.OffLine.of(tokens[0])
            }.let {classroomParsed -> parsedClassroomList.add(classroomParsed) }
            i++
        }
        // @formatter:on

        return parsedClassroomList
    }
}

sealed interface ClassroomUnion {
    data class RealTimeOnline(val dumpy: Nothing? = null) : ClassroomUnion
    data class Online(val duplicateCapability: Boolean) : ClassroomUnion

    data class OffLine(
        val building: String,
        val address: String?
    ) : ClassroomUnion {

        companion object {
            // order matters
            private val regexList = listOf(
                Regex("""(제[0-9]강의실)"""), // edge case 1
                Regex("""(KLI[0-9]F-?[0-9]?)"""), // edge case 2
                Regex("""^([A-Za-z\-0-9]+)$"""), // edge case 3 ex) IBS610
                Regex("""^([가-힣]+)$"""), // normal case 1  ex) 백양누리광장, 석산홀세미나실
                Regex("""^([가-힣]+[a-zA-Z]{0,2})([0-9]+-?[A-Z0-9]+)$"""), // normal case 2 ex) 대별B101, 삼312,외627-1
                Regex("""^([가-힣]+[0-9]{0,2})-([가-힣]+[0-9]{0,3})$"""), // normal case 2 ex) 대별B101, 삼312,외627-1
            )

            fun of(raw: String): ClassroomUnion.OffLine = regexList
                .firstNotNullOfOrNull { regex: Regex -> regex.find(raw) }
                ?.groupValues
                ?.let { matchGroups ->
                    when (matchGroups.size) {
                        2 -> OffLine(matchGroups[1], null)
                        3 -> OffLine(matchGroups[1], matchGroups[2])
                        else -> throw IllegalStateException("can't parse classRoom name ${raw}")
                    }
                }
                ?: throw IllegalStateException("can't parse classRoom name ${raw}")

        }
        // companion object ends
    }

}

