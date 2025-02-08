create table dpt_group
(
    dpt_group_id varchar(30) PRIMARY KEY,
    name         varchar(30) UNIQUE
);

create table dpt
(
    dpt_id       varchar(30) PRIMARY KEY,
    dpt_group_id varchar(30) REFERENCES dpt_group (dpt_group_id),
    name         varchar(30)

);

create table lecture_json
(
    lecture_json_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    dpt_id          varchar(30) REFERENCES dpt (dpt_id),
    json            TEXT
);

create table crawl_job
(
    crawl_job_id   INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    start_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    end_datetime   TIMESTAMP
);

create table dpt_group_request
(
    dpt_group_request INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    crawl_job_id      INT UNSIGNED REFERENCES crawl_job (crawl_job_id),

    year              YEAR,
    semester          VARCHAR(30),

    http_resp_body    TEXT
);

create table dpt_request
(
    dpt_request    INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    crawl_job_id   INT UNSIGNED REFERENCES crawl_job (crawl_job_id),

    year           YEAR        NOT NULL,
    semester       VARCHAR(30) NOT NULL,
    dpt_group_id   VARCHAR(30) NOT NULL,

    http_resp_body MEDIUMTEXT
);

create table lecture_request
(
    lecture_request INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    crawl_job_id    INT UNSIGNED REFERENCES crawl_job (crawl_job_id),

    year            YEAR        NOT NULL,
    semester        VARCHAR(30) NOT NULL,
    dpt_group_id    VARCHAR(30) NOT NULL,
    dpt_id          VARCHAR(30) NOT NULL,

    http_resp_body  MEDIUMTEXT
);
