package io.gitp.ysfl.db

import io.gitp.ysfl.client.Semester
import io.gitp.ysfl.client.YonseiClients
import io.gitp.ysfl.client.payload.DptGroupPayloadVo
import java.net.http.HttpResponse
import java.time.Year

fun main() {
    val resp: HttpResponse<String> = YonseiClients
        .dptGroupClient
        .request(DptGroupPayloadVo(Year.of(2025), Semester.FIRST).build())
        .get()

    println(resp.body())
}