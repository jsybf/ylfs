package io.gitp.ylfs.entity.type

enum class Semester(val code: Int) {
    FIRST(10),
    SECOND(20);

    companion object {
        fun codeof(code: Int): Semester {
            for (entry in entries) {
                if (entry.code == code) {
                    return entry
                }
            }

            throw IllegalStateException("can't match input code(:$code)")
        }
    }
}
