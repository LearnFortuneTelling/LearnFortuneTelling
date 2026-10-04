package com.example.fortune_fluent.ganji

val HEAVENLY_STEMS = listOf(
    "갑", "을", "병", "정", "무",
    "기", "경", "신", "임", "계"
)

val EARTHLY_BRANCHES = listOf(
    "자", "축", "인", "묘", "진", "사",
    "오", "미", "신", "유", "술", "해"
)

data class SolarTermDef(val name: String, val longitude: Double)

val SOLAR_TERMS = listOf(
    SolarTermDef("소한", 285.0),
    SolarTermDef("대한", 300.0),
    SolarTermDef("입춘", 315.0),
    SolarTermDef("우수", 330.0),
    SolarTermDef("경칩", 345.0),
    SolarTermDef("춘분", 0.0),
    SolarTermDef("청명", 15.0),
    SolarTermDef("곡우", 30.0),
    SolarTermDef("입하", 45.0),
    SolarTermDef("소만", 60.0),
    SolarTermDef("망종", 75.0),
    SolarTermDef("하지", 90.0),
    SolarTermDef("소서", 105.0),
    SolarTermDef("대서", 120.0),
    SolarTermDef("입추", 135.0),
    SolarTermDef("처서", 150.0),
    SolarTermDef("백로", 165.0),
    SolarTermDef("추분", 180.0),
    SolarTermDef("한로", 195.0),
    SolarTermDef("상강", 210.0),
    SolarTermDef("입동", 225.0),
    SolarTermDef("소설", 240.0),
    SolarTermDef("대설", 255.0),
    SolarTermDef("동지", 270.0)
)

data class GanjiResult(
    val stemIndex: Int,
    val branchIndex: Int,
    val stem: String,
    val branch: String,
    val name: String
)

data class FourPillarsResult(
    val year: GanjiResult,
    val month: GanjiResult,
    val day: GanjiResult,
    val hour: GanjiResult
)

data class SolarTermResult(
    val name: String,
    val longitude: Double,
    val instant: java.time.Instant
)