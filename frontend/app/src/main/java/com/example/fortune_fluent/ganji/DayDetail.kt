package com.example.fortune_fluent.ganji

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import android.icu.util.Calendar as IcuCalendar
import android.icu.util.ChineseCalendar
import android.icu.util.TimeZone as IcuTimeZone

private val KST: ZoneId = ZoneId.of("Asia/Seoul")

// --------------------------------------------------
// 음력
// --------------------------------------------------

data class LunarDate(val month: Int, val day: Int, val isLeap: Boolean) {
    /** 달력 셀용 짧은 표기: 8.9 / 윤8.9 */
    val label: String get() = "${if (isLeap) "윤" else ""}$month.$day"

    /** TalkBack / 상세 화면용 표기 */
    val spoken: String get() = "음력 ${if (isLeap) "윤" else ""}${month}월 ${day}일"
}

/**
 * 양력 → 음력 변환.
 */
object LunarConverter {
    fun toLunar(date: LocalDate): LunarDate? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return null
        return convert(date)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun convert(date: LocalDate): LunarDate {
        val calendar = ChineseCalendar(IcuTimeZone.getTimeZone("Asia/Seoul"))
        // 정오로 잡아 자정 경계에서 날짜가 밀리는 것을 방지
        calendar.timeInMillis = date.atTime(12, 0).atZone(KST).toInstant().toEpochMilli()
        return LunarDate(
            month = calendar.get(IcuCalendar.MONTH) + 1,
            day = calendar.get(IcuCalendar.DAY_OF_MONTH),
            isLeap = calendar.get(IcuCalendar.IS_LEAP_MONTH) == 1
        )
    }
}

// --------------------------------------------------
// 절기
// --------------------------------------------------

// 명리 월이 바뀌는 12절(節). 중기(우수·춘분 등)는 월주가 바뀌지 않음.
private val MONTH_START_TERMS = setOf(
    "소한", "입춘", "경칩", "청명", "입하", "망종",
    "소서", "입추", "백로", "한로", "입동", "대설"
)

/** 해당 양력 날짜(KST)에 걸린 24절기 */
fun solarTermsOn(date: LocalDate): List<SolarTermResult> =
    getSolarTerms(date.year).filter { it.instant.atZone(KST).toLocalDate() == date }

fun SolarTermResult.timeText(): String =
    instant.atZone(KST).format(DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT))

fun SolarTermResult.isMonthBoundary(): Boolean = name in MONTH_START_TERMS

// --------------------------------------------------
// 하루 상세
// --------------------------------------------------

/** 절입 시각을 기준으로 월주(입춘이면 연주도)가 바뀌는 날의 정보 */
data class MonthTermChange(
    val term: SolarTermResult,
    val monthBefore: GanjiResult,
    val monthAfter: GanjiResult,
    val yearBefore: GanjiResult,
    val yearAfter: GanjiResult
) {
    val yearChanged: Boolean get() = yearBefore.name != yearAfter.name
}

data class HourPillar(
    val titleHangul: String,
    val titleHanja: String,
    val range: String,
    val pillar: GanjiResult
)

data class DayDetail(
    val date: LocalDate,
    /** 그날 00:00(KST) 기준 연주 · 월주 · 일주 */
    val year: GanjiResult,
    val month: GanjiResult,
    val day: GanjiResult,
    val lunar: LunarDate?,
    val termsToday: List<SolarTermResult>,
    val termChange: MonthTermChange?,
    /** 조자시 ~ 야자시까지 13개 시주 */
    val hours: List<HourPillar>
)

fun buildDayDetail(date: LocalDate): DayDetail {
    val startOfDay = createKstInstant(date.year, date.monthValue, date.dayOfMonth)
    val pillars = calculateFourPillars(startOfDay)
    val termsToday = solarTermsOn(date)

    val termChange = termsToday.firstOrNull { it.isMonthBoundary() }?.let { term ->
        val before = calculateFourPillars(term.instant.minusSeconds(1))
        val after = calculateFourPillars(term.instant)
        MonthTermChange(
            term = term,
            monthBefore = before.month,
            monthAfter = after.month,
            yearBefore = before.year,
            yearAfter = after.year
        )
    }

    fun slot(hour: Int, hangul: String, hanja: String, range: String) = HourPillar(
        titleHangul = hangul,
        titleHanja = hanja,
        range = range,
        pillar = calculateFourPillars(
            createKstInstant(date.year, date.monthValue, date.dayOfMonth, hour)
        ).hour
    )

    val hours = buildList {
        add(slot(0, "조자시", "早子時", "00:00~00:59"))
        for (b in 1..11) {
            val start = 2 * b - 1
            add(
                slot(
                    hour = start,
                    hangul = "${EARTHLY_BRANCHES[b]}시",
                    hanja = "${EARTHLY_BRANCHES_HANJA[b]}時",
                    range = String.format(Locale.ROOT, "%02d:00~%02d:59", start, start + 1)
                )
            )
        }
        add(slot(23, "야자시", "夜子時", "23:00~23:59"))
    }

    return DayDetail(
        date = date,
        year = pillars.year,
        month = pillars.month,
        day = pillars.day,
        lunar = LunarConverter.toLunar(date),
        termsToday = termsToday,
        termChange = termChange,
        hours = hours
    )
}
