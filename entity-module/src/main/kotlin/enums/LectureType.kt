package io.gitp.ylfs.entity.enums

import kotlinx.serialization.Serializable

@Serializable
enum class LectureType(val displayName: String) {
    TYPE_1("교기"), TYPE_2("대교"), TYPE_3("필교"), TYPE_4("자율"), TYPE_5("일반"),
    TYPE_6("전기"), TYPE_7("전선"), TYPE_8("교직"), TYPE_9("전필"),
    TYPE_10("UICE"), TYPE_11("CC"), TYPE_12("ME"), TYPE_13("MB"), TYPE_14("MR"),
    TYPE_15("전공"), TYPE_16("청강"), TYPE_17("선택"), TYPE_18("공통"), TYPE_19("공기"),
    TYPE_20("학선"), TYPE_21("교필"), TYPE_22("전탐"), TYPE_23("교선"), TYPE_24("기교"),
    TYPE_25("선교"), TYPE_26("학기"), TYPE_27("학필"), TYPE_28("계기"), TYPE_NONE("None");

    companion object {
        fun parse(name: String?): LectureType {
            if (name == null) return TYPE_NONE
            return entries.find { it.displayName == name } ?: throw IllegalArgumentException("can't parse ${name}")
        }
    }
}