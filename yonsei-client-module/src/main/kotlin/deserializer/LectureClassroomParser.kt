package io.gitp.ysfl.client.deserializer

import io.gitp.ysfl.client.response.ClassroomUnion

internal class LectureClassroomParser {

    internal fun parseClassroom(input: String): List<ClassroomUnion> = lexerClassroom(tokenizeClassroom(input))


    private val physicalClassroomFuckingEdgeCase: Regex = Regex("""\((?<building>[가-힣0-9]+)\((?<address>[가-힣0-9]+)\)\)_?""")

    // @formatter:off
    internal fun tokenizeClassroom(classroomRaw: String): List<String> {

        // 체육과목의 강의실들중 댑부분이 (스포츠(다목적실))/(볼링장) 이런형태이다.
        // (스포츠-다목적실)/(볼링장) 이렇게 바꿔주자.
        val classroomRaw1 = physicalClassroomFuckingEdgeCase
            .findAll(classroomRaw)
            .map {
                val building = it.groups["building"]!!.value
                val address = it.groups["address"]!!.value
                "${building}-${address}"
            }
            .joinToString("/")
            .takeIf { it != "" }
            ?: classroomRaw

        // 토크나이징...
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

        var idx = 0
        val parsedClassroomList = mutableListOf<ClassroomUnion>()

        // @formatter:off
        while (idx < tokens.size - 1) {
            when (tokens[idx]) {
                "실시간온라인" -> ClassroomUnion.RealTimeOnline()
                "동영상", "동영상콘텐츠" -> {
                    if (tokens[idx + 1] != "중복수강불가") ClassroomUnion.Online(false)
                    else { idx++; ClassroomUnion.Online(true) }
                }
                else -> ClassroomUnion.OffLine.of(tokens[0])
            }.let {classroomParsed -> parsedClassroomList.add(classroomParsed) }
            idx++
        }
        // @formatter:on

        return parsedClassroomList
    }
}

