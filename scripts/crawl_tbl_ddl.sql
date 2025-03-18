CREATE DATABASE IF NOT EXISTS crawl;


USE crawl;

CREATE TABLE crawl_job (
    crawl_job_id   INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    year           year                                NOT NULL,
    semester       varchar(6)                          NOT NULL CHECK (semester IN ('FIRST', 'SECOND')),
    start_datetime timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
    end_datetime   timestamp                           NULL
);

CREATE TABLE college_resp (
    college_resp_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    crawl_job_id    INT UNSIGNED REFERENCES crawl_job (crawl_job_id),

    http_resp_body  JSON NOT NULL
);

# department request
CREATE TABLE dpt_resp (
    dpt_resp_id    INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    crawl_job_id   INT UNSIGNED REFERENCES crawl_job (crawl_job_id),

    college_code   VARCHAR(6) NOT NULL,

    http_resp_body JSON       NOT NULL
);

CREATE TABLE lecture_resp (
    lecture_resp_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    crawl_job_id    INT UNSIGNED REFERENCES crawl_job (crawl_job_id),

    college_code    VARCHAR(6) NOT NULL,
    dpt_code        VARCHAR(6) NOT NULL,

    http_resp_body  JSON        NOT NULL
);


# mileage rank request
CREATE TABLE mlg_rank_resp (
    mlg_rank_resp_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    crawl_job_id     INT UNSIGNED REFERENCES crawl_job (crawl_job_id),

    main_code CHAR(7) NOT NULL,
    class_code CHAR(2) NOT NULL,
    sub_code CHAR(2) NOT NULL,

    http_resp_body   JSON    NOT NULL
);

# mileage info request
CREATE TABLE mlg_info_resp (
    mlg_info_resp_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    crawl_job_id     INT UNSIGNED REFERENCES crawl_job (crawl_job_id),

    main_code CHAR(7) NOT NULL,
    class_code CHAR(2) NOT NULL,
    sub_code CHAR(2) NOT NULL,

    http_resp_body   JSON    NOT NULL
);
