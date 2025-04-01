import io.gitp.ylfs.crawl.client.*
import io.gitp.ylfs.crawl.payload.*
import io.gitp.ylfs.entity.model.LectureId
import io.gitp.ylfs.entity.enums.Semester
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.Year
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class YonseiClientTest {
    @Test
    @Tag("do_real_request")
    fun just_execute_CollegeClient() {
        // when
        val result: Result<String> = CollegeClient
            .request(CollegePayload(Year.of(2025), Semester.FIRST))
            .get()

        // then
        result.onSuccess { println(it) }
        assertTrue(result.isSuccess)
    }

    @Test
    @Tag("do_real_request")
    fun just_execute_DptClient() {
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
    fun just_execute_test_LectureClient() {
        // when
        val result: Result<String> = LectureClient
            .request(LecturePayload("s11000", "30105", Year.of(2025), Semester.FIRST))
            .get()

        // then
        result.onSuccess { println(it) }
        assertTrue(result.isSuccess)
    }

    @Test
    @Tag("do_real_request")
    fun just_execute_test_MlgRankClient() {
        // when
        val payloads = listOf(
            MlgRankPayload(
                LectureId("YCA1003", "01", "00"),
                Year.of(2023),
                Semester.FIRST
            ),
            MlgRankPayload(
                LectureId("ANT3208", "01", "00"),
                Year.of(2024),
                Semester.FIRST
            ),
            MlgRankPayload(
                LectureId("ECO3130", "03", "00"),
                Year.of(2024),
                Semester.FIRST
            )
        )

        payloads
            .map { payload -> MlgRankClient.request(payload).get() }
            .onEach { resp -> println("response: ${resp}") }
            .forEach { resp -> assertTrue(resp.isSuccess) }

    }

    @Test
    @Tag("do_real_request")
    fun just_execute_test_MlgInfoClient() {
        // when
        val payloads = listOf(
            MlgInfoPayload(
                LectureId("YCA1003", "01", "00"),
                Year.of(2023),
                Semester.FIRST
            ),
            MlgInfoPayload(
                LectureId("ANT3208", "01", "00"),
                Year.of(2024),
                Semester.FIRST
            ),
            MlgInfoPayload(
                LectureId("ECO3130", "03", "00"),
                Year.of(2024),
                Semester.FIRST
            )
        )

        payloads
            .map { payload -> MlgInfoClient.request(payload).get() }
            .onEach { resp -> println("response: ${resp}") }
            .forEach { resp -> assertTrue(resp.isSuccess) }

    }

    @Test
    @Tag("do_real_request")
    fun if_request_to_yonsei_server_failed_return_failed_with_custom_exception() {
        // when
        val invalidUrl = "https://underwood1.yonsei.ac.kr/sch/sles/SlescsCtr/findSchSlesHandbListFUCK.do"
        val client = YonseiClient<CollegePayload>(invalidUrl)

        // given
        val result: Result<String> = client
            .request(CollegePayload(Year.of(2025), Semester.FIRST))
            .get()

        // then
        assertTrue(result.isFailure)

        val exception: Throwable = result.exceptionOrNull()!!
        assertIs<YonseiRequestException>(exception)

        (exception as YonseiRequestException).let {
            assertEquals(CollegePayload(Year.of(2025), Semester.FIRST), it.requestPayload)
            assertEquals(invalidUrl, it.requestUrl)
        }
    }
}