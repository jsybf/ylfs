import io.gitp.ylfs.crawl.client.*
import io.gitp.ylfs.crawl.payload.CoursePayload
import io.gitp.ylfs.crawl.payload.DptGroupPayload
import io.gitp.ylfs.crawl.payload.DptPayload
import io.gitp.ylfs.crawl.payload.MileagePayload
import io.gitp.ylfs.entity.type.LectureId
import io.gitp.ylfs.entity.type.Semester
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.Year
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class YonseiClientTest {
    @Test
    @Tag("do_real_request")
    fun DptGroupClient_just_execute_test() {
        // when
        val result: Result<String> = DptGroupClient
            .request(DptGroupPayload(Year.of(2025), Semester.FIRST))
            .get()

        // then
        result.onSuccess { println(it) }
        assertTrue(result.isSuccess)
    }

    @Test
    @Tag("do_real_request")
    fun DptClient_just_execute_test() {
        // when
        val result: Result<String> = DptClient
            .request(DptPayload("s11000", Year.of(2025), Semester.FIRST))
            .get()

        // then
        result.onSuccess { println(it) }
        assertTrue(result.isSuccess)
    }

    @Test
    @Tag("do_real_request")
    fun CourseClient_just_execute_test() {
        // when
        val result: Result<String> = CourseClient
            .request(CoursePayload("s11000", "30105", Year.of(2025), Semester.FIRST))
            .get()

        // then
        result.onSuccess { println(it) }
        assertTrue(result.isSuccess)
    }

    @Test
    @Tag("do_real_request")
    fun MileageClient_just_execute_test() {
        // when
        val coursePayloads = listOf(
            MileagePayload(
                LectureId("YCA1003", "01", "00"),
                Year.of(2023),
                Semester.FIRST
            ),
            MileagePayload(
                LectureId("ANT3208", "01", "00"),
                Year.of(2024),
                Semester.FIRST
            ),
            MileagePayload(
                LectureId("ECO3130", "03", "00"),
                Year.of(2024),
                Semester.FIRST
            )
        )

        coursePayloads
            .map { payload -> MileageClient.request(payload).get() }
            .onEach {resp -> println("response: ${resp}") }
            .forEach { resp -> assertTrue(resp.isSuccess) }

    }


    @Test
    @Tag("do_real_request")
    fun if_request_to_yonsei_server_failed_return_failed_with_custom_exception() {
        // when
        val invalidUrl = "https://underwood1.yonsei.ac.kr/sch/sles/SlescsCtr/findSchSlesHandbListFUCK.do"
        val client = YonseiClient<DptGroupPayload>(invalidUrl)

        // given
        val result: Result<String> = client
            .request(DptGroupPayload(Year.of(2025), Semester.FIRST))
            .get()

        // then
        assertTrue(result.isFailure)

        val exception: Throwable = result.exceptionOrNull()!!
        assertIs<YonseiRequestException>(exception)

        (exception as YonseiRequestException).let {
            assertEquals(DptGroupPayload(Year.of(2025), Semester.FIRST), it.requestPayload)
            assertEquals(invalidUrl, it.requestUrl)
        }
    }
}