package io.gitp.ylfs.crawl.client

import io.gitp.ylfs.crawl.payload.CoursePayload
import io.gitp.ylfs.crawl.payload.DptGroupPayload
import io.gitp.ylfs.crawl.payload.DptPayload
import io.gitp.ylfs.crawl.payload.MileagePayload


object DptGroupClient :
    YonseiClient<DptGroupPayload>("https://underwood1.yonsei.ac.kr/sch/sles/SlescsCtr/findSchSlesHandbList.do")

object DptClient :
    YonseiClient<DptPayload>("https://underwood1.yonsei.ac.kr/sch/sles/SlescsCtr/findSchSlesHandbList.do")

object CourseClient :
    YonseiClient<CoursePayload>("https://underwood1.yonsei.ac.kr/sch/sles/SlessyCtr/findAtnlcHandbList.do")

object MileageClient :
    YonseiClient<MileagePayload>("https://underwood1.yonsei.ac.kr/sch/sles/SlessyCtr/findMlgRankResltList.do")
