CREATE DATABASE IF NOT EXISTS ylfs;
USE ylfs;


DROP TABLE IF EXISTS mlg_rank;
DROP TABLE IF EXISTS mlg_info;
DROP TABLE IF EXISTS subclass;
DROP TABLE IF EXISTS dpt;
DROP TABLE IF EXISTS lecture;
DROP TABLE IF EXISTS college;
DROP TABLE IF EXISTS term;

CREATE TABLE term (
    year     YEAR       NOT NULL,
    semester VARCHAR(6) NOT NULL,
    PRIMARY KEY (year, semester)
);

CREATE TABLE college (
    college_id   INT UNSIGNED AUTO_INCREMENT,

    year         YEAR        NOT NULL,
    semester     VARCHAR(6)  NOT NULL,

    college_code VARCHAR(6)  NOT NULL,
    college_name VARCHAR(50) NOT NULL,

    PRIMARY KEY (college_id),
    FOREIGN KEY (year, semester) REFERENCES term (year, semester),
    CONSTRAINT UNIQUE (year, semester, college_code)
);

CREATE TABLE dpt (
    dpt_id     INT UNSIGNED AUTO_INCREMENT,

    college_id INT UNSIGNED NOT NULL,

    dpt_code   VARCHAR(5)   NOT NULL,
    dpt_name   VARCHAR(50)  NOT NULL,

    PRIMARY KEY (dpt_id),
    FOREIGN KEY (college_id) REFERENCES college (college_id),
    CONSTRAINT UNIQUE (college_id, dpt_code)
);


CREATE TABLE lecture (
    lecture_id        INT UNSIGNED AUTO_INCREMENT,

    year              YEAR          NOT NULL,
    semester          VARCHAR(6)    NOT NULL,

    main_code         CHAR(7)       NOT NULL,
    class_code        CHAR(2)       NOT NULL,

    name              VARCHAR(100)  NOT NULL,
    professor_list    JSON          NOT NULL,

    grade_list        JSON          NOT NULL,
    credit            DECIMAL(3, 1) NOT NULL,
    grade_eval_method VARCHAR(10)   NOT NULL,
    language          VARCHAR(10)   NOT NULL CHECK ( language IN ('KOREAN', 'ENGLISH', 'ETC') ),

    PRIMARY KEY (lecture_id),
    FOREIGN KEY (year, semester) REFERENCES term (year, semester),
    CONSTRAINT UNIQUE (year, semester, main_code, class_code)
);


CREATE TABLE subclass (
    subclass_id       INT UNSIGNED AUTO_INCREMENT,
    lecture_id        INT UNSIGNED NOT NULL,

    sub_code          CHAR(2)      NOT NULL,

    loc_sched         JSON         NOT NULL,
    lecture_type_list JSON         NOT NULL,
    PRIMARY KEY (subclass_id),
    FOREIGN KEY (lecture_id) REFERENCES lecture (lecture_id),
    UNIQUE (lecture_id, sub_code)
);

CREATE TABLE mlg_info (
    mlg_info_id            INT UNSIGNED AUTO_INCREMENT,
    subclass_id            INT UNSIGNED NOT NULL,

    mlg_limit              INT UNSIGNED NOT NULL,
    major_protect_type     VARCHAR(15)  NOT NULL CHECK ( major_protect_type IN ('ONLY_MAJOR', 'ALSO_DUAL_MAJOR', 'UNPROTECT')),
    applied_cnt            INT UNSIGNED NOT NULL,

    total_capacity         INT UNSIGNED NOT NULL,
    major_protect_capacity INT UNSIGNED NOT NULL,

    grade_1_capacity       INT UNSIGNED NOT NULL,
    grade_2_capacity       INT UNSIGNED NOT NULL,
    grade_3_capacity       INT UNSIGNED NOT NULL,
    grade_4_capacity       INT UNSIGNED NOT NULL,
    grade_5_capacity       INT UNSIGNED NOT NULL,
    grade_6_capacity       INT UNSIGNED NOT NULL,

    PRIMARY KEY (mlg_info_id),
    FOREIGN KEY (subclass_id) REFERENCES subclass (subclass_id),
    UNIQUE (subclass_id)
);

CREATE TABLE mlg_rank (
    mlg_rank_id              INT UNSIGNED AUTO_INCREMENT,
    subclass_id              INT UNSIGNED  NOT NULL,

    if_succeed               BOOLEAN       NOT NULL,
    mlg_rank                 INT UNSIGNED  NOT NULL,
    mlg_value                INT UNSIGNED  NOT NULL,

    -- grade                    INT UNSIGNED  NOT NULL,

    if_disabled              BOOLEAN       NOT NULL,
    if_major_protected       VARCHAR(24)   NOT NULL CHECK ( if_major_protected IN
                                                            ('MAJOR_PROTECTED', 'DUAL_MAJOR_PROTECTED', 'DUAL_MAJOR_NOT_PROTECTED',
                                                             'NOT_PROTECTED') ),
    applied_lecture_cnt      INT UNSIGNED  NOT NULL,
    if_graduate_planned      BOOLEAN       NOT NULL,
    if_first_apply           BOOLEAN       NOT NULL,
    last_term_credit         DECIMAL(5, 4) NOT NULL,
    last_term_credit_faction VARCHAR(10)   NOT NULL,
    total_credit             DECIMAL(5, 4) NOT NULL,
    total_credit_fraction    VARCHAR(10)   NOT NULL,

    PRIMARY KEY (mlg_rank_id),
    FOREIGN KEY (subclass_id) REFERENCES subclass (subclass_id)
);


