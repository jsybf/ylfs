package io.gitp.ysfl.client.payload

/**
 * 연세 학사요람검색사이트 ajax요청payload를 모방하는 역활
 *
 * 단과대, 학과, 강의 들과 같은 api들이 있다.
 * 각각의 api들의 payload들을 분석해보면 3가지로 분류가능
 *      - defaultPayload: 각 api들별 변하지 않는 값
 *      - commonDefaultPayload: 모든 api들이 공유하는 값
 *      - payload: 각 api들별로 기입해야하는 값
 * 이 추상클래스를 구현하려면
 *      - defaultPayload에 api에서 변하지 않는값
 *      - payload: 검색하고 싶은 매개변수들 넣으면 됨
 */
abstract class AbstractPayloadVo() {
    abstract val defaultPayload: Map<String, String>
    abstract fun getPayloadMap(): Map<String, String>

    private val commonDefaultPayload: Map<String, String> = mapOf(
        "_menuId" to "MTA5MzM2MTI3MjkzMTI2NzYwMDA%3D",
        "_menuNm" to "",
        "_pgmId" to "NDE0MDA4NTU1NjY%3D",
        "%40d%23" to "%40d1%23",
        "%40d1%23" to "dmCond",
        "%40d1%23tp" to "dm"
    )

    // for debugging perpuse
    fun buildPayloadMap(): Map<String, String> = this.getPayloadMap() + defaultPayload + commonDefaultPayload

    fun buildPayloadStr(): String = buildPayloadMap()
        .map { (key, value) -> "${key}=${value}" }
        .reduce { acc: String, s: String -> "${acc}&${s}" }
}