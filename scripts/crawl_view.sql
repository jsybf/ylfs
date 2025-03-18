USE crawl;

CREATE VIEW college_resp_raw_view AS
    SELECT crawl_job_id, jt.deptNm, jt.deptCd, jt.engDeptNm, jt.sysinstDivCd

    FROM college_resp,
        # @formatter:off
        JSON_TABLE(
                http_resp_body,
                '$.dsUnivCd[*]'
                COLUMNS (
                    engDeptNm VARCHAR(255) PATH '$.engDeptNm',
                    sysinstDivCd VARCHAR(10) PATH '$.sysinstDivCd',
                    deptNm VARCHAR(255) PATH '$.deptNm',
                    deptCd VARCHAR(10) PATH '$.deptCd'
                    )
            # @formatter:off
        ) AS jt;

CREATE VIEW college_resp_view AS
    SELECT
        crawl_job_id,
        deptNm as college_name,
        deptCd as collge_id
    FROM
        college_resp_raw_view;


CREATE VIEW dpt_resp_raw_view AS
    SELECT
        crawl_job_id,
        college_id,
        jt.deptNm,
        jt.deptCd,
        jt.engDeptNm,
        jt.sysinstDivCd

    FROM
        dpt_resp,
        # @formatter:off
        JSON_TABLE(
                cast(http_resp_body as JSON ),
                '$.dsFaclyCd[*]'
                COLUMNS (
                    engDeptNm VARCHAR(255) PATH '$.engDeptNm',
                    sysinstDivCd VARCHAR(10) PATH '$.sysinstDivCd',
                    deptNm VARCHAR(255) PATH '$.deptNm',
                    deptCd VARCHAR(10) PATH '$.deptCd'
                    )
        # @formatter:off
        ) AS jt;

CREATE VIEW dpt_resp_view AS
    SELECT
        crawl_job_id,
        college_id,
        deptNm as dpt_name,
        deptCd as dpt_id
    FROM
        dpt_resp_raw_view;



CREATE VIEW lecture_resp_raw_view AS
    SELECT crawl_job_id, jt.*
    FROM lecture_resp,
        # @formatter:off
        JSON_TABLE(http_resp_body, '$.dsSles251[*]' COLUMNS (
            # id 관련
            subjtnbCorsePrcts VARCHAR(50) PATH '$.subjtnbCorsePrcts', # full_id
            subjtnb VARCHAR(20) PATH '$.subjtnb', # main_id
            corseDvclsNo VARCHAR(10) PATH '$.corseDvclsNo', # class_id
            prctsCorseDvclsNo VARCHAR(10) PATH '$.prctsCorseDvclsNo', # sub_id
            usubjtnb VARCHAR(20) PATH '$.usubjtnb', # ???

            # 장소/위치
            lecrmNm VARCHAR(100) PATH '$.lecrmNm', # 장소
            lctreTimeNm VARCHAR(50) PATH '$.lctreTimeNm', # 시간표
            lctreTimeEngNm VARCHAR(50) PATH '$.lctreTimeEngNm', # 시간표 영어

            campsDivNm VARCHAR(50) PATH '$.campsDivNm', # 캠퍼스 이름
            campsBusnsCd VARCHAR(10) PATH '$.campsBusnsCd', #??? s1, s3, s7
            subjtClNm VARCHAR(50) PATH '$.subjtClNm', # 오프라인/온라인?

            # 학과 정보
            estblDeprtCd VARCHAR(10) PATH '$.estblDeprtCd', # 학과 코드
            estblDeprtNm VARCHAR(255) PATH '$.estblDeprtNm', # 학과 이름 학과코드, 학과이름 같음
            estblDeprtOrd INT PATH '$.estblDeprtOrd', # ???

            # 강의 정보
            cdt INT PATH '$.cdt', # 학점
            subjtUnitVal VARCHAR(50) PATH '$.subjtUnitVal', # 강의 단위

            subjtNm VARCHAR(50) PATH '$.subjtNm', # 이름
            subjtNm2 VARCHAR(50) PATH '$.subjtNm2', # 이름(좀더 자세)
            subjtEngNm VARCHAR(100) PATH '$.subjtEngNm', subjtSbtlNm VARCHAR(255) PATH '$.subjtSbtlNm', # ??? 부재목으로 예상
            subjtSbtlEngNm VARCHAR(255) PATH '$.subjtSbtlEngNm', # ???

            subsrtDivNm VARCHAR(10) PATH '$.subsrtDivNm', # 강의 타입(전선, 피교..)
            subsrtDivCd VARCHAR(10) PATH '$.subsrtDivCd', #  강의 타입 코드

            gradeEvlMthdDivNm VARCHAR(10) PATH '$.gradeEvlMthdDivNm', # 평가방식(절평, 상평) 이름
            gradeEvlMthdDivCd VARCHAR(10) PATH '$.gradeEvlMthdDivCd', # 평가방식 코드

            atntnMattrDesc TEXT PATH '$.atntnMattrDesc', # 강의 정보 비고
            # 교수
            cgprfNm VARCHAR(50) PATH '$.cgprfNm', #교수이름들
            cgprfEngNm VARCHAR(50) PATH '$.cgprfEngNm', # 교수 영어명
            cgprfNndsYn CHAR(1) PATH '$.cgprfNndsYn', # ???


            # 학년
            hy VARCHAR(255) PATH '$.hy', # 학년

            # 년도/학기
            syy INT PATH '$.syy', # 년도
            lessnSessnDivCd VARCHAR(10) PATH '$.lessnSessnDivCd', # 학기 코드
            lessnSessnDivNm VARCHAR(50) PATH '$.lessnSessnDivNm', # 학기 이름
            syySmtDivNm VARCHAR(20) PATH '$.syySmtDivNm',
            smtDivCd VARCHAR(10) PATH '$.smtDivCd', # semester

            # 언어관련
            srclnLctreLangDivCd VARCHAR(50) PATH '$.srclnLctreLangDivCd',
            srclnLctreYn CHAR(1) PATH '$.srclnLctreYn',
            srclnLctreLangDivNm VARCHAR(50) PATH '$.srclnLctreLangDivNm',

            # 시간표(Tm), 위치(Pl), 교수(Pr)이 변했는 지 여부로 추정. 사이트에서 파란색으로 뜨기도함
            subjtChngGudncDivCdTm VARCHAR(50) PATH '$.subjtChngGudncDivCdTm',
            subjtChngGudncDivCdPl VARCHAR(50) PATH '$.subjtChngGudncDivCdPl',
            subjtChngGudncDivCdPr VARCHAR(50) PATH '$.subjtChngGudncDivCdPr',

            # 폐강여부
            rmvlcYn CHAR(1) PATH '$.rmvlcYn',
            rmvlcYnNm VARCHAR(50) PATH '$.rmvlcYnNm', # null column

            # ?? (해석불가)
            lawscSubjcgpNm VARCHAR(255) PATH '$.lawscSubjcgpNm',
            lawscSubjcChrtzNm VARCHAR(255) PATH '$.lawscSubjcChrtzNm',
            lawscSubjcFldNm VARCHAR(255) PATH '$.lawscSubjcFldNm',
            coprtEstblYn CHAR(1) PATH '$.coprtEstblYn',
            experPrctsAmt INT PATH '$.experPrctsAmt',
            syllaUnregTrgetDivCd CHAR(1) PATH '$.syllaUnregTrgetDivCd',
            onppsPrttnAmt INT PATH '$.onppsPrttnAmt',
            rcognHrs INT PATH '$.rcognHrs',
            timtbDplctPermKindCd VARCHAR(10) PATH '$.timtbDplctPermKindCd',
            excstPercpFg CHAR(1) PATH '$.excstPercpFg',
            medcHyLisup VARCHAR(255) PATH '$.medcHyLisup',
            orgSysinstDivCd VARCHAR(10) PATH '$.orgSysinstDivCd',
            lecrmEngNm VARCHAR(100) PATH '$.lecrmEngNm',
            attflUuid VARCHAR(50) PATH '$.attflUuid',
            cmptPrctsAmt INT PATH '$.cmptPrctsAmt',
            tmtcYn CHAR(1) PATH '$.tmtcYn',
            sysinstDivCd VARCHAR(10) PATH '$.sysinstDivCd' )
        ) AS jt;
# @formatter:on

CREATE VIEW lecture_resp_view AS
    SELECT
        crawl_job_id,
        subjtnbCorsePrcts   AS full_id,
        subjtnb             AS main_id,
        corseDvclsNo        AS class_id,
        prctsCorseDvclsNo   AS sub_id,

        subjtNm2            AS course_name,

        estblDeprtNm        AS dpt_id,
        estblDeprtNm        AS dpt_name,

        hy                  AS grades,
        cdt                 AS credit,
        subjtUnitVal        AS course_unit_value,
        subsrtDivNm         AS course_type,
        gradeEvlMthdDivNm   AS grade_eval_method,
        gradeEvlMthdDivCd   AS grade_eval_method_id,
        atntnMattrDesc      AS describition,

        cgprfNm             AS professors,

        lecrmNm             AS locations,
        lctreTimeNm         AS schedules,

        lessnSessnDivNm     AS semester,
        syy                 AS course_year,

        campsDivNm          AS campus_name,

        srclnLctreLangDivCd AS language_code, # null: 한국어 10: 영어, 20

        rmvlcYn             AS if_closed
    FROM lecture_resp_raw_view;



CREATE VIEW mlg_rank_resp_raw_view AS
    SELECT
        crawl_job_id,

        main_id,
        class_id,
        sub_id,

        jt.ttCmpsjCdtRto,
        jt.jstbfSmtCmpsjAtnlcPosblCdt,
        jt.remrk,
        jt.aplySubjcCnt,
        jt.mlgRank,
        jt.mlgAppcsPrcesDivNm,
        jt.hy,
        jt.fratlcYn,
        jt.grdtnAplyYn,
        jt.jstbfSmtCmpsjCdtRto,
        jt.mjsbjYn,
        jt.ttCmpsjGrdtnCmpsjCdt,
        jt.mlgVal,
        jt.dsstdYn
    FROM mlg_rank_resp,
        # @formatter:off
        JSON_TABLE(
                http_resp_body,
                '$.dsSles440[*]'
                COLUMNS (
                    ttCmpsjCdtRto DECIMAL(6,4) PATH '$.ttCmpsjCdtRto', # 총이수학점비율 소수형
                    ttCmpsjGrdtnCmpsjCdt VARCHAR(10) PATH '$.ttCmpsjGrdtnCmpsjCdt', # 총이수학점비율 분수형
                    jstbfSmtCmpsjAtnlcPosblCdt VARCHAR(10) PATH '$.jstbfSmtCmpsjAtnlcPosblCdt', # 직전학기 이수학점비율 소수꼴
                    jstbfSmtCmpsjCdtRto DECIMAL(6,4) PATH '$.jstbfSmtCmpsjCdtRto', # 직전학기 이수학점비율 분수꼴
                    remrk TEXT PATH '$.remrk', # 비고
                    aplySubjcCnt INT PATH '$.aplySubjcCnt', # 신청과목수
                    mlgRank INT PATH '$.mlgRank', # 랭킹
                    mlgAppcsPrcesDivNm VARCHAR(10) PATH '$.mlgAppcsPrcesDivNm', # 성공여부
                    hy VARCHAR(10) PATH '$.hy', # 학년
                    fratlcYn VARCHAR(2) PATH '$.fratlcYn', # 초수강여부
                    grdtnAplyYn VARCHAR(2) PATH '$.grdtnAplyYn', # 졸업예정
                    mjsbjYn VARCHAR(10) PATH '$.mjsbjYn', # 전공자 보호 여부
                    mlgVal INT PATH '$.mlgVal', # 마일리지수
                    dsstdYn VARCHAR(2) PATH '$.dsstdYn'
                    )
        ) AS jt;
# @formatter:off

CREATE VIEW mlg_rank_resp_view AS
    SELECT
        crawl_job_id,

        main_id,
        class_id,
        sub_id,

        mlgRank as mlg_rank,
        mlgAppcsPrcesDivNm as if_succeed,
        hy as grade,

        # 평가우선순위에 따라
        mlgVal as mlg_num,
        dsstdYn as if_disabled_privileaged,
        mjsbjYn as if_major_protected,
        aplySubjcCnt as applied_course_count,
        grdtnAplyYn as if_graduate_planned,
        fratlcYn as if_firest_applied,
        ttCmpsjCdtRto as total_credit_decimal,
        jstbfSmtCmpsjCdtRto as last_semester_credit_decimal,

        remrk as remark,

        # 총이수학점비율, 직전학기 이수학점비율의 분수형
        ttCmpsjGrdtnCmpsjCdt as total_credit_fraction,
        jstbfSmtCmpsjAtnlcPosblCdt as last_semester_credit_fraction
    FROM
        mlg_rank_resp_raw_view;
