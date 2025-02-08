package io.gitp.ysfl.client.deserializer

import io.gitp.ysfl.client.response.LocationUnion

internal class LectureLocationParser {

    internal fun parseLocation(input: String): List<LocationUnion> = locationLexer(tokenizeLocation(input))


    private val physicalLectureLocationRegex: Regex = Regex("""\((?<building>[가-힣0-9]+)\((?<address>[가-힣0-9]+)\)\)_?""")

    // @formatter:off
    internal fun tokenizeLocation(classroomRaw: String): List<String> {

        // 체육과목의 강의실들중 댑부분이 (스포츠(다목적실))/(볼링장) 이런형태이다.
        // (스포츠-다목적실)/(볼링장) 이렇게 바꿔주자.
        val classroomRaw1 = physicalLectureLocationRegex
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


    internal fun locationLexer(tokens: List<String>): List<LocationUnion> {
        if (tokens.size == 1) {
            return when (tokens[0]) {
                "실시간온라인" -> listOf(LocationUnion.RealTimeOnline())
                "동영상", "동영상콘텐츠" -> listOf(LocationUnion.Online(true))
                else -> listOf(LocationUnion.OffLine.of(tokens[0]))
            }
        }

        var idx = 0
        val parsedClassroomList = mutableListOf<LocationUnion>()

        // @formatter:off
        while (idx < tokens.size - 1) {
            when (tokens[idx]) {
                "실시간온라인" -> LocationUnion.RealTimeOnline()
                "동영상", "동영상콘텐츠" -> {
                    if (tokens[idx + 1] != "중복수강불가") LocationUnion.Online(false)
                    else { idx++; LocationUnion.Online(true) }
                }
                else -> LocationUnion.OffLine.of(tokens[0])
            }.let {classroomParsed -> parsedClassroomList.add(classroomParsed) }
            idx++
        }
        // @formatter:on

        return parsedClassroomList
    }
}

