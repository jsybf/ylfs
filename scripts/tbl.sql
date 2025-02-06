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