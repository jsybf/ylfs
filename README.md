# what is this

# how to
## how to ingress data
(step1) build scraping jar which is kotlin cli program.
```bash
./gradlew scraping:scraping-job:fatJar
```
(step2)scrape(or crawl) data from [yonsei site](https://underwood1.yonsei.ac.kr/com/lgin/SsoCtr/initExtPageWork.do?link=handbStdntBusns). this command will crawl ajax response of [yonsei site](https://underwood1.yonsei.ac.kr/com/lgin/SsoCtr/initExtPageWork.do?link=handbStdntBusns) as json file.
```bash
java -jar jars/scraping-job.jar scrape --year 2023 --semester 20 --path ./data/23-2
java -jar jars/scraping-job.jar scrape --year 2024 --semester 10 --path ./data/24-1
java -jar jars/scraping-job.jar scrape --year 2024 --semester 20 --path ./data/24-2
java -jar jars/scraping-job.jar scrape --year 2025 --semester 10 --path ./data/25-1
```
expected result is like this
```
data
└── 23-2
    ├── college.json
    ├── dpt.json
    ├── lecture.json
    ├── mlg-info.json
    └── mlg-rank.json
```
(step3)we have to transform/parse fuckly dirty raw json of [yonsei site](https://underwood1.yonsei.ac.kr/com/lgin/SsoCtr/initExtPageWork.do?link=handbStdntBusns) ajax response.
```bash
java -jar jars/scraping-job.jar transform -i data/23-2 -o data/refined/23-2
java -jar jars/scraping-job.jar transform -i data/24-1 -o data/refined/24-1
java -jar jars/scraping-job.jar transform -i data/24-2 -o data/refined/24-2
java -jar jars/scraping-job.jar transform -i data/25-1 -o data/refined/25-1
```
expected result is like this
```
data
└── refined
    └── 23-2
        ├── college-refined.json
        ├── dpt-refined.json
        ├── lecture-refined.json
        ├── mlg-info-refined.json
        └── mlg-rank-refined.json
```
(step4)finally load to mysql.
```bash
# for example. you can specify below parameters to jar command instead of env.
export YLFS_MYSQL_HOST=127.0.0.1
export YLFS_MYSQL_PORT=3306
export YLFS_MYSQL_DB=ylfs
export YLFS_MYSQL_USER=root
export YLFS_MYSQL_PASSWORD=root_pass

# create tables in mysql. WARN this command will remove all data and create table
java -jar jars/scraping-job.jar mysql-init

# load data from json file to mysql
java -jar jars/scraping-job.jar load --input_dir ./data/refined/25-1
java -jar jars/scraping-job.jar load --input_dir ./data/refined/24-2
java -jar jars/scraping-job.jar load --input_dir ./data/refined/24-1
java -jar jars/scraping-job.jar load --input_dir ./data/refined/23-2
```

for mor info
```bash
java -jar jars/scraping-job.jar --help
java -jar jars/scraping-job.jar scrape --help
java -jar jars/scraping-job.jar transform --help
java -jar jars/scraping-job.jar load --help
```
