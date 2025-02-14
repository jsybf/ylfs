package io.gitp.ylfs.crawl.client

import io.gitp.ylfs.crawl.payload.CollegePayload
import io.gitp.ylfs.crawl.payload.DptPayload
import io.gitp.ylfs.crawl.payload.LecturePayload
import io.gitp.ylfs.crawl.payload.MlgRankPayload


object CollegeClient :
    YonseiClient<CollegePayload>("https://underwood1.yonsei.ac.kr/sch/sles/SlescsCtr/findSchSlesHandbList.do")

object DptClient :
    YonseiClient<DptPayload>("https://underwood1.yonsei.ac.kr/sch/sles/SlescsCtr/findSchSlesHandbList.do")

object LectureClient :
    YonseiClient<LecturePayload>("https://underwood1.yonsei.ac.kr/sch/sles/SlessyCtr/findAtnlcHandbList.do")

object MlgRankClient :
    YonseiClient<MlgRankPayload>("https://underwood1.yonsei.ac.kr/sch/sles/SlessyCtr/findMlgRankResltList.do")
