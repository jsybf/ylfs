package deserializer

// import org.junit.jupiter.api.Assertions.assertEquals
import io.gitp.ysfl.client.PairList
import io.gitp.ysfl.client.deserializer.LocationScheduleParser
import io.gitp.ysfl.client.response.LocationUnion
import io.gitp.ysfl.client.response.LocationUnion.OffLine
import io.gitp.ysfl.client.response.LocationUnion.Online
import io.gitp.ysfl.client.response.Schedule
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Paths
import java.time.DayOfWeek.*
import kotlin.io.path.readLines

class LocationScheduleParserTest {

    @Test
    fun just_test_location_and_schedule_parsing() {
        val testSet: PairList<Pair<String, String>, Map<Schedule, LocationUnion>> = listOf(
            Pair(
                Pair("동영상콘텐츠/공A328(공A328)", "화0/화5,6(화7)"),
                mapOf(
                    Schedule(TUESDAY, listOf(0)) to Online(true),
                    Schedule(TUESDAY, listOf(5, 6)) to OffLine("공A", "328"),
                    Schedule(TUESDAY, listOf(7)) to OffLine("공A", "328")
                )
            ),
            Pair(
                Pair("공A563/동영상(중복수강불가)", "화2,3/수3"),
                mapOf(
                    Schedule(TUESDAY, listOf(2, 3)) to OffLine("공A", "563"),
                    Schedule(WEDNESDAY, listOf(3)) to Online(false)
                )
            ),
            Pair(
                Pair("음211(음211)", "수7,8(금7,8)"),
                mapOf(
                    Schedule(WEDNESDAY, listOf(7, 8)) to OffLine("음", "211"),
                    Schedule(FRIDAY, listOf(7, 8)) to OffLine("음", "211"),
                )
            ),
            Pair(
                Pair("(체조장(체308))", "(수7,8)"),
                mapOf(
                    Schedule(WEDNESDAY, listOf(7, 8)) to OffLine("체조장", "체308"),
                )
            ),
            Pair(
                Pair("(공A213)/공A528", "(월8,9)/수4,5"),
                mapOf(
                    Schedule(MONDAY, listOf(8, 9)) to OffLine("공A", "213"),
                    Schedule(WEDNESDAY, listOf(4, 5)) to OffLine("공A", "528"),
                )
            ),
            Pair(
                Pair("(공D604)공D604", "(월10,11,수10,11)수9"),
                mapOf(
                    Schedule(MONDAY, listOf(10, 11)) to OffLine("공D", "604"),
                    Schedule(WEDNESDAY, listOf(10, 11)) to OffLine("공D", "604"),
                    Schedule(WEDNESDAY, listOf(9)) to OffLine("공D", "604"),
                )
            ),

            )
        testSet.forEach { (sample, expected) ->
            val actual: Map<Schedule, LocationUnion> = LocationScheduleParser.parse(sample.first, sample.second)
            assertEquals(expected, actual)
        }
    }

    @Test
    fun location_and_schedule_should_have_same_number_of_chunk() {
        getTestDataFromFile()
            .forEach {(loc, sched) -> assertDoesNotThrow { LocationScheduleParser.associateLocAndSched(loc, sched) } }
    }
}


/**
 * returns list of raw(before parsed) crawled location and schedule pair data
 * @return [Pair.first]: location [Pair.second]: schedule
 */
private fun getTestDataFromFile(testFilePath: Path = Paths.get("src/test/resources/schedule_location_set.tsv")): PairList<String, String> {
    return testFilePath
        .toAbsolutePath()
        .readLines()
        .map {
            val splited = it.split("\t")
            require(splited.size == 2)
            Pair(splited[0], splited[1])
        }

}


// private fun getTestData(): List<Pair<String, String>> {
//     val filePath: Path = Paths.get("/Users/gitp/gitp/dev/projects/ylfs/memo/schedule_location_set.tsv").toAbsolutePath()
// val readAllLines: MutableList<String> = Files.readAllLines(filePath)
//
// // split samples
// val locScheMap: List<Pair<String, String>> = readAllLines
//     .filter { it != "\t" }
//     .map {
//         val splited: List<String> = it.split("\t")
//         assert(splited.size == 2)
//         Pair(splited[0], splited[1])
//     }
//
// return locScheMap
// }
