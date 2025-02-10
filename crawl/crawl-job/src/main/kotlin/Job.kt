package io.gitp.ylfs.crawl.crawljob

import io.gitp.ylfs.crawl.client.CourseClient
import io.gitp.ylfs.crawl.client.DptClient
import io.gitp.ylfs.crawl.client.DptGroupClient
import io.gitp.ylfs.crawl.client.MileageClient
import io.gitp.ylfs.crawl.payload.CoursePayload
import io.gitp.ylfs.crawl.payload.DptGroupPayload
import io.gitp.ylfs.crawl.payload.DptPayload
import io.gitp.ylfs.crawl.payload.MileagePayload
import io.gitp.ylfs.entity.type.LectureId

private typealias PairList<T, V> = List<Pair<T, V>>


val extractDptGroupId = Regex(""" "deptCd":"(?<dptId>\w+)" """, RegexOption.COMMENTS)
val extractDptId = Regex(""" "deptCd":"(?<dptId>\d+)" """, RegexOption.COMMENTS)
val extractCourseId = Regex(""" "subjtnbCorsePrcts":"([\dA-Z]{7})-(\d{2})-(\d{2})" """, RegexOption.COMMENTS)


/**
 * request to yonsei course search server and
 * persist raw json http response body to mysql
 */
internal fun crawlJob(arg: Args) {

    /* request DptGroup */

    val dptGroupResp: String = DptGroupClient
        .request(DptGroupPayload(arg.year, arg.semester))
        .get()
        .onFailure { TODO() }
        .getOrNull()!!

    val dptGroupIds: List<String> =
        extractDptGroupId.findAll(dptGroupResp).map { matchResult: MatchResult -> matchResult.destructured }.map { (dptId) -> dptId }.toList()

    /* request Dpt */

    // first: department group id, second: department response json
    val dptResp: PairList<String, String> = dptGroupIds
        .map { dptGroupId -> Pair(dptGroupId, DptClient.request(DptPayload(dptGroupId, arg.year, arg.semester))) }
        .map { (dptGroupId, reqFuture) -> Pair(dptGroupId, reqFuture.get().onFailure { TODO() }.getOrNull()!!) }

    val dptGroupIdAndDptId: PairList<String, String> = dptResp
        .flatMap { (dptGroupId, dptResp) ->
            val dptIds: List<String> = extractDptId.findAll(dptResp).map { it.destructured.component1() }.toList()
            dptIds.map { Pair(dptGroupId, it) }
        }
        // .onEach { println(it) }

    /* request course */

    // first: department id, second: course response json
    val courseResp: PairList<String, String> = dptGroupIdAndDptId
        .map { (dptGroupId, dptId) -> Pair(dptId, CourseClient.request(CoursePayload(dptGroupId, dptId, arg.year, arg.semester))) }
        .map { (dptId, reqFuture) -> Pair(dptId, reqFuture.get().onFailure { TODO() }.getOrNull()!!) }

    val dptIdAndCourseId: PairList<String, LectureId> = courseResp
        .flatMap { (dptId, courseResp) ->
            val courseIds = extractCourseId.findAll(courseResp).map { it.destructured }
                .map { (mainid, classid, subId) -> LectureId(mainid, classid, subId) }.toList()
            courseIds.map { Pair(dptId, it) }
        }
        // .onEach { println(it) }

    /* request mileage */

    val mileageResp: PairList<LectureId, String> = dptIdAndCourseId
        .map { (_, courseId) -> Pair(courseId, MileageClient.request(MileagePayload(courseId, arg.year.minusYears(1), arg.semester))) }
        .map { (courseId, reqFuture) -> Pair(courseId, reqFuture.get().onFailure { TODO() }.getOrNull()!!) }
        // .onEach { println(it) }
}