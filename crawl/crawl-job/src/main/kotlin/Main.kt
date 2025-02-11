package io.gitp.ylfs.crawl.crawljob

import io.gitp.ylfs.entity.type.Semester
import java.time.Year

fun main(argsRaw: Array<String>) {
    val args: Args = parseArgs(argsRaw)
    println(args)
    crawlJob(args)

}

internal data class Args(
    val mysqlUsername: String,
    val mysqlPassword: String,
    val mysqlHost: String,
    val mysqlDatabase: String,

    val year: Year,
    val semester: Semester,

    val ifShowCredentialInLog: Boolean,
)

private fun parseArgs(args: Array<String>): Args {
    val usage = """
        
    (required fields)
    --m_user          : mysql user name
    --m_pass          : mysql user password
    --m_host          : mysql host
    --m_db            : mysql database
    --year            : year
    --semester        : semester ("FIRST" or "SECOND")
    
    
    (optional fields)
    --if_show_cred    : if show mysql credential in stdout log ("y" or "n")
    
    (example)
    --m_user root --m_host 43.202.5.149 --m_pass root_pass  --m_db crawl --year 2025 --semester FIRST    
    
    """.trimIndent()
    val optionalFlag = arrayOf("--show_cred")

    val requiredFlags: MutableMap<String, String?> = mutableMapOf(
        "--m_user" to null,
        "--m_host" to null,
        "--m_pass" to null,
        "--m_db" to null,

        "--year" to null,
        "--semester" to null
    )

    val optionalFlags: MutableMap<String, String?> = mutableMapOf(
        "--if_show_cred" to null
    )

    /* parse args */
    for (i in (args.indices step 2)) {
        val flag = args[i]
        val value = args[i + 1]

        if (requiredFlags.containsKey(flag)) {
            if (requiredFlags[flag] != null) error("flag(${flag}) duplicated\n" + usage)
            requiredFlags[flag] = value
        } else if (optionalFlags.containsKey(flag)) {
            if (requiredFlags[flag] != null) error("flag(${flag}) duplicated\n" + usage)
            optionalFlags[flag] = value
        } else error("flag named [${flag}] not expected\n" + usage)
    }

    optionalFlags["--if_show_cred"]
        ?.let { require(it in arrayOf("y", "n")) { """ --if_show_cred only accepts "y" or "n" """ + "\n" + usage } }


    /* return Args */
    return Args(
        mysqlUsername = requiredFlags["--m_user"]!!,
        mysqlPassword = requiredFlags["--m_pass"]!!,
        mysqlHost = requiredFlags["--m_host"]!!,
        mysqlDatabase = requiredFlags["--m_db"]!!,

        year = Year.parse(requiredFlags["--year"]!!),
        semester = Semester.valueOf(requiredFlags["--semester"]!!),
        ifShowCredentialInLog = optionalFlags["--if_show_cred"] == "y"
    )
}