create table crawl_job
(
    crawl_job_id   INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    start_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    end_datetime   TIMESTAMP
);

create table dpt_group_request
(
    dpt_group_request_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    crawl_job_id         INT UNSIGNED REFERENCES crawl_job (crawl_job_id),

    year                 YEAR,
    semester             VARCHAR(30),

    http_resp_body       TEXT
);

create table dpt_request
(
    dpt_request_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    crawl_job_id   INT UNSIGNED REFERENCES crawl_job (crawl_job_id),

    year           YEAR        NOT NULL,
    semester       VARCHAR(30) NOT NULL,
    dpt_group_id   VARCHAR(30) NOT NULL,

    http_resp_body MEDIUMTEXT
);

create table course_request
(
    course_request_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    crawl_job_id      INT UNSIGNED REFERENCES crawl_job (crawl_job_id),

    year              YEAR        NOT NULL,
    semester          VARCHAR(30) NOT NULL,

    dpt_group_id      VARCHAR(30) NOT NULL,
    dpt_id            VARCHAR(30) NOT NULL,

    http_resp_body    MEDIUMTEXT
);
create table mileage_request
(
    mileage_request_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    crawl_job_id       INT UNSIGNED REFERENCES crawl_job (crawl_job_id),

    year               YEAR        NOT NULL,
    semester           VARCHAR(30) NOT NULL,
    main_id            CHAR(7)     NOT NULL,
    class_id           CHAR(2)     NOT NULL,
    sub_id             CHAR(2)     NOT NULL,
    http_resp_body     MEDIUMTEXT
);