package io.gitp.ysfl.client.response

// TODO
data class MileageRank(
    val rank: Int,
    val mileage: Int,
    val ifSucceed: Boolean,
    val grade: Int,
    // val major: MajorType,
    // val totalCreditRatio: Fraction,
    val lastSemesterCreditRatio: Double,
    val appliedLectureNum: Int,
    val ifFirstRegister: Boolean,
    val ifGraduateApplied: Boolean,
    // val ifDisabled: Boolean

) {
}