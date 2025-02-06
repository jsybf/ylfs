package io.gitp.ysfl.client

import io.gitp.ysfl.client.payload.DptGroupPayloadVo
import io.gitp.ysfl.client.payload.DptPayloadVo
import io.gitp.ysfl.client.response.DptGroupResp
import io.gitp.ysfl.client.response.DptResp
import java.time.Year
import java.util.concurrent.CompletableFuture


fun demo1() {
    val year = Year.of(2025)
    val semester = Semester.FIRST
    val dptGroupPayload = DptGroupPayloadVo(year, semester)

    YonseiClients.dptGroupClient
        .requestAndMap(dptGroupPayload.build()).get()
        .associate { dptGroup ->
            val dptPayload = DptPayloadVo(dptGroup.dptGroupId, year, semester)
            val dptList = YonseiClients.dptClient.requestAndMap(dptPayload.build())

            Pair(dptGroup, dptList)
        }
        .onEach { (dptGroup: DptGroupResp, dptList: CompletableFuture<List<DptResp>>) ->
            println(dptGroup)
            dptList.get().forEach { println("\t${it}") }
        }

}

fun main() {
    demo1()
}