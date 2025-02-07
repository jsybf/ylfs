package io.gitp.ysfl.client

import io.gitp.ysfl.client.payload.DptGroupPayloadVo
import io.gitp.ysfl.client.payload.DptPayloadVo
import io.gitp.ysfl.client.payload.LecturePayloadVo
import io.gitp.ysfl.client.payload.MileagePayloadVo
import io.gitp.ysfl.client.response.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.net.http.HttpResponse
import java.time.Year
import java.util.concurrent.CompletableFuture

fun dptGroupDemo() {
    val year = Year.of(2025)
    val semester = Semester.FIRST
    val dptGroupPayload = DptGroupPayloadVo(year, semester)

    val dptGroupClient: DptGroupClientV2 = DptGroupClientV2()

    dptGroupClient
        .request(dptGroupPayload)
        .get()
        .dptGroupList
        .forEach { println(it) }
}

fun dptDemo() {
    val year = Year.of(2025)
    val semester = Semester.FIRST
    val dptGroupPayload = DptGroupPayloadVo(year, semester)

    val dptGroupClient: DptGroupClientV2 = DptGroupClientV2()
    val dptClient: DptClientV2 = DptClientV2()

    dptGroupClient
        .request(dptGroupPayload)
        .get()
        .dptGroupList
        .map { dptGroup ->
            val dptRespFuture: CompletableFuture<DptResponse> = dptClient
                .request(DptPayloadVo(dptGroup.dptGroupId, year, semester))

            Pair(dptGroup, dptRespFuture)
        }
        .map { (dptGroup, dptRespFuture) -> Pair(dptGroup, dptRespFuture.get()) }
        .onEach { (dptGroup, dptResp) ->
            println(dptGroup)
            dptResp.dptList.forEach { println("\t$it") }
            dptResp.responseBody.let { println(it) }
        }

}
fun mileageClientDemo() {
    val mileageClient = YonseiClients.mileageClient

    val mileagePayloadVo: MileagePayloadVo = MileagePayloadVo(
        Year.of(2024),
        Semester.FIRST,
        LectureId(mainId = "ECO1101", classDivisionId = "01", subId = "00")
    )

    val get: HttpResponse<String> = mileageClient.request(mileagePayloadVo.build()).get()
    println(get.body())
}

fun main() {
    // dptGroupDemo()
    dptDemo()
}
