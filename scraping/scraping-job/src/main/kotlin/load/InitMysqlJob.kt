package io.gitp.yfls.scarping.job.file.load

import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager

object InitMysqlJob {
    private fun readResourceFile(path: Path): String = path
        .also { require(path.isAbsolute) { "path of resource file should be absoulte" } }
        .also { println(path.toString()) }
        .toString()
        .let { object {}::class.java.getResource(it).readText() }

    fun run(
        mysqlHost: String,
        mysqlPort: String,
        mysqlUser: String,
        mysqlPassword: String
    ) {
        Class.forName("com.mysql.cj.jdbc.Driver")
        DriverManager.getConnection("jdbc:mysql://@${mysqlHost}:${mysqlPort}?user=${mysqlUser}&password=${mysqlPassword}&allowMultiQueries=true").use { conn: Connection ->
            val ddl: String = readResourceFile(Path.of("/ddl.sql"))
            conn.createStatement().execute(ddl)
        }
    }
}
