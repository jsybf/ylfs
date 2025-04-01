package io.gitp.yfls.scarping.job.file.transform

import io.gitp.yfls.scarping.job.file.request.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

data class ResponseParseModule(
    val inputFile: Path,
    val outputFile: Path,
    val parser: (raw: String) -> String
)


object TransformJob {
    private val logger = LoggerFactory.getLogger(object {}::class.java)
    private val responseParseList = listOf(
        ResponseParseModule(
            inputFile = Path.of("mlg-info.json"),
            outputFile = Path.of("mlg-info-refined.json"),
            parser = { raw: String ->
                Json.decodeFromString<List<MlgInfoResponse>>(raw)
                    .mapNotNull { transformMlgInfoResp(it) }
                    .let { mlgInfoList: List<MlgInfo> -> Json.encodeToString<List<MlgInfo>>(mlgInfoList) }
            }
        ),
        ResponseParseModule(
            inputFile = Path.of("mlg-rank.json"),
            outputFile = Path.of("mlg-rank-refined.json"),
            parser = { raw: String ->
                Json.decodeFromString<List<MlgRankResponse>>(raw)
                    .flatMap { transformMlgRankResp(it) }
                    .let { mlgInfoList: List<MlgRankVo> -> Json.encodeToString<List<MlgRankVo>>(mlgInfoList) }
            }
        ),
        ResponseParseModule(
            inputFile = Path.of("lecture.json"),
            outputFile = Path.of("lecture-refined.json"),
            parser = { raw: String ->
                Json.decodeFromString<List<LectureResponse>>(raw)
                    .flatMap { transfromLectureResp(it) }
                    .let { lectureList: List<LectureVo> -> Json.encodeToString<List<LectureVo>>(lectureList) }
            }
        ),
        ResponseParseModule(
            inputFile = Path.of("dpt.json"),
            outputFile = Path.of("dpt-refined.json"),
            parser = { raw: String ->
                Json.decodeFromString<List<DptResponse>>(raw)
                    .flatMap { transformDpt(it) }
                    .let { lectureList: List<DptVo> -> Json.encodeToString<List<DptVo>>(lectureList) }
            }
        ),
        ResponseParseModule(
            inputFile = Path.of("college.json"),
            outputFile = Path.of("college-refined.json"),
            parser = { raw: String ->
                Json.decodeFromString<CollegeResponse>(raw)
                    .let { transformCollege(it) }
                    .let { lectureList: List<CollegeVo> -> Json.encodeToString<List<CollegeVo>>(lectureList) }
            }
        )
    )

    /**
     * return absolute path
     */
    private fun getAvaiableModules(baseDir: Path): List<ResponseParseModule> = responseParseList
        .filter { parseModule: ResponseParseModule -> baseDir.resolve(parseModule.inputFile).exists() }


    fun run(inputDir: Path, outputDir: Path) =
        getAvaiableModules(inputDir)
            .forEach { module ->
                val inputFile = inputDir.resolve(module.inputFile)
                val outputFile = outputDir.resolve(module.outputFile)
                logger.info("transforming ${inputFile} to ${outputFile}")

                inputFile.readText()
                    .let { inputJsonText: String -> module.parser(inputJsonText) }
                    .let { parsedJsonText: String -> outputFile.writeText(parsedJsonText) }
            }
}
