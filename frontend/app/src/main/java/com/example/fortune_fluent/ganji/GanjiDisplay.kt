package com.example.fortune_fluent.ganji

enum class GanjiScript { HANGUL, HANJA }

fun GanjiScript.other(): GanjiScript =
    if (this == GanjiScript.HANGUL) GanjiScript.HANJA else GanjiScript.HANGUL

val HEAVENLY_STEMS_HANJA = listOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
val EARTHLY_BRANCHES_HANJA = listOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")

// 천간: 갑을=목, 병정=화, 무기=토, 경신=금, 임계=수
private val STEM_ELEMENTS = listOf("목", "목", "화", "화", "토", "토", "금", "금", "수", "수")

// 지지: 자=수, 축=토, 인묘=목, 진=토, 사오=화, 미=토, 신유=금, 술=토, 해=수
private val BRANCH_ELEMENTS = listOf("수", "토", "목", "목", "토", "화", "화", "토", "금", "금", "토", "수")

private val ELEMENT_HANJA = mapOf("목" to "木", "화" to "火", "토" to "土", "금" to "金", "수" to "水")

fun GanjiResult.stemText(script: GanjiScript): String =
    if (script == GanjiScript.HANJA) HEAVENLY_STEMS_HANJA[stemIndex] else stem

fun GanjiResult.branchText(script: GanjiScript): String =
    if (script == GanjiScript.HANJA) EARTHLY_BRANCHES_HANJA[branchIndex] else branch

/** 갑자 / 甲子 처럼 선택한 표기로 두 글자를 반환 */
fun GanjiResult.text(script: GanjiScript): String = stemText(script) + branchText(script)

val GanjiResult.stemElement: String get() = STEM_ELEMENTS[stemIndex]
val GanjiResult.branchElement: String get() = BRANCH_ELEMENTS[branchIndex]

/** 천간의 음양 (짝수 index = 양) */
val GanjiResult.stemYinYang: String get() = if (stemIndex % 2 == 0) "양" else "음"

fun elementText(element: String, script: GanjiScript): String =
    if (script == GanjiScript.HANJA) ELEMENT_HANJA[element] ?: element else element
