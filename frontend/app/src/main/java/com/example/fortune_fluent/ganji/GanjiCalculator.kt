package com.example.fortune_fluent.ganji

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

private val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")

// 2000-01-01은 무오(戊午)일. 갑자를 0으로 잡으면 무오는 54번 인덱스.
private const val BASE_DAY_INDEX = 54
private val BASE_DATE: java.time.LocalDate =
    ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, SEOUL).toLocalDate()

private val solarTermsCache = mutableMapOf<Int, List<SolarTermResult>>()

// --------------------------------------------------
// 기본 유틸
// --------------------------------------------------

private fun normalizeAngle(angle: Double): Double =
    ((angle % 360) + 360) % 360

private fun floorMod(value: Int, modulo: Int): Int =
    ((value % modulo) + modulo) % modulo

private fun toRadians(degrees: Double): Double =
    degrees * PI / 180

// --------------------------------------------------
// 60갑자
// --------------------------------------------------

fun getGanji(index: Int): GanjiResult {
    val normalized = floorMod(index, 60)
    val stemIndex = normalized % 10
    val branchIndex = normalized % 12

    return GanjiResult(
        stemIndex = stemIndex,
        branchIndex = branchIndex,
        stem = HEAVENLY_STEMS[stemIndex],
        branch = EARTHLY_BRANCHES[branchIndex],
        name = HEAVENLY_STEMS[stemIndex] + EARTHLY_BRANCHES[branchIndex]
    )
}

// --------------------------------------------------
// Julian Day
// --------------------------------------------------

fun toJulianDay(instant: Instant): Double =
    instant.toEpochMilli() / 86_400_000.0 + 2440587.5

fun julianCentury(jd: Double): Double =
    (jd - 2451545.0) / 36525

// --------------------------------------------------
// 태양 위치 계산
// --------------------------------------------------

fun sunMeanLongitude(t: Double): Double {
    val longitude = 280.46646 + 36000.76983 * t + 0.0003032 * t * t
    return normalizeAngle(longitude)
}

fun sunMeanAnomaly(t: Double): Double {
    val anomaly = 357.52911 + 35999.05029 * t - 0.0001537 * t * t
    return normalizeAngle(anomaly)
}

// 지구의 타원 공전에 따른 중심차 보정
fun sunEquationOfCenter(t: Double, m: Double): Double {
    val mRad = toRadians(m)
    return (
            (1.914602 - 0.004817 * t - 0.000014 * t * t) * sin(mRad) +
                    (0.019993 - 0.000101 * t) * sin(2 * mRad) +
                    0.000289 * sin(3 * mRad)
            )
}

// 진황경
fun sunTrueLongitude(t: Double): Double {
    val l0 = sunMeanLongitude(t)
    val m = sunMeanAnomaly(t)
    val c = sunEquationOfCenter(t, m)
    return normalizeAngle(l0 + c)
}

// 겉보기 황경
fun sunApparentLongitude(t: Double): Double {
    val trueLongitude = sunTrueLongitude(t)
    val omega = 125.04 - 1934.136 * t
    return normalizeAngle(
        trueLongitude - 0.00569 - 0.00478 * sin(toRadians(omega))
    )
}

// 날짜 -> 태양 황경
fun sunLongitude(instant: Instant): Double {
    val jd = toJulianDay(instant)
    val t = julianCentury(jd)
    return sunApparentLongitude(t)
}

// --------------------------------------------------
// 절기 계산
// --------------------------------------------------

private fun angleDifference(current: Double, target: Double): Double {
    var diff = current - target
    if (diff > 180) diff -= 360
    if (diff < -180) diff += 360
    return diff
}

private fun findSolarTermAround(approximateTime: Instant, targetLongitude: Double): Instant {
    var rangeDays = 1L

    while (rangeDays <= 32) {
        val start = approximateTime.minusSeconds(rangeDays * 86_400)
        val end = approximateTime.plusSeconds(rangeDays * 86_400)

        val leftDiff = angleDifference(sunLongitude(start), targetLongitude)
        val rightDiff = angleDifference(sunLongitude(end), targetLongitude)

        // 목표 황경을 이 구간에서 통과한다면
        if (leftDiff <= 0 && rightDiff >= 0) {
            return findSolarTermTime(start, end, targetLongitude)
        }

        // 못 찾았으면 탐색 범위를 2배 확장
        rangeDays *= 2
    }

    throw IllegalStateException("Could not find solar term $targetLongitude°")
}

// 특정 시간 범위 안에서 태양 황경이 targetLongitude가 되는 시각 탐색
private fun findSolarTermTime(start: Instant, end: Instant, targetLongitude: Double): Instant {
    var left = start.toEpochMilli()
    var right = end.toEpochMilli()

    val leftDiff = angleDifference(sunLongitude(Instant.ofEpochMilli(left)), targetLongitude)
    val rightDiff = angleDifference(sunLongitude(Instant.ofEpochMilli(right)), targetLongitude)

    if (!(leftDiff <= 0 && rightDiff >= 0)) {
        throw IllegalStateException("Solar term $targetLongitude° is not inside search range")
    }

    // 1초 이하까지 이분 탐색
    while (right - left > 1000) {
        val middle = (left + right) / 2
        val middleLongitude = sunLongitude(Instant.ofEpochMilli(middle))
        val diff = angleDifference(middleLongitude, targetLongitude)

        if (diff < 0) {
            left = middle
        } else {
            right = middle
        }
    }

    return Instant.ofEpochMilli((left + right) / 2)
}

// --------------------------------------------------
// 특정 연도의 24절기 계산
// --------------------------------------------------

private fun buildSolarTerms(year: Int): List<SolarTermResult> {
    val startOfYear = ZonedDateTime.of(year, 1, 1, 0, 0, 0, 0, SEOUL).toInstant()
    val startLongitude = sunLongitude(startOfYear)
    val sunDegreesPerDay = 360 / 365.2422

    return SOLAR_TERMS.map { term ->
        val degreeDifference = normalizeAngle(term.longitude - startLongitude)
        val approximateDays = degreeDifference / sunDegreesPerDay
        val approximateTime = startOfYear.plusSeconds((approximateDays * 86_400).toLong())
        val time = findSolarTermAround(approximateTime, term.longitude)

        SolarTermResult(name = term.name, longitude = term.longitude, instant = time)
    }
}

fun getSolarTerms(year: Int): List<SolarTermResult> {
    solarTermsCache[year]?.let { return it }
    val terms = buildSolarTerms(year)
    solarTermsCache[year] = terms
    return terms
}

fun getSolarTerm(year: Int, name: String): Instant {
    val term = getSolarTerms(year).find { it.name == name }
        ?: throw IllegalArgumentException("Unknown solar term: $name")
    return term.instant
}

// --------------------------------------------------
// KST 날짜 관련
// --------------------------------------------------

private fun kst(instant: Instant): ZonedDateTime = instant.atZone(SEOUL)

// 프론트(Composable)에서 KST 날짜를 만들 때 사용
fun createKstInstant(
    year: Int, month: Int, day: Int,
    hour: Int = 0, minute: Int = 0, second: Int = 0
): Instant = ZonedDateTime.of(year, month, day, hour, minute, second, 0, SEOUL).toInstant()

// --------------------------------------------------
// 일주
// --------------------------------------------------

fun getDayGanji(instant: Instant): GanjiResult {
    val currentDay = kst(instant).toLocalDate().toEpochDay()
    val baseDay = BASE_DATE.toEpochDay()
    val difference = (currentDay - baseDay).toInt()
    return getGanji(BASE_DAY_INDEX + difference)
}

// --------------------------------------------------
// 연주
// --------------------------------------------------

private fun getYearGanji(instant: Instant): GanjiResult {
    val year = kst(instant).year
    val ipchun = getSolarTerm(year, "입춘")

    // 입춘 이전이면 아직 전년도 간지년
    val effectiveYear = if (instant.isBefore(ipchun)) year - 1 else year

    // 1984 = 갑자년
    val index = floorMod(effectiveYear - 1984, 60)
    return getGanji(index)
}

// --------------------------------------------------
// 월주
// --------------------------------------------------

/*
명리 월의 경계가 되는 절(節)
寅=입춘 卯=경칩 辰=청명 巳=입하 午=망종 未=소서
申=입추 酉=백로 戌=한로 亥=입동 子=대설 丑=소한
*/
private val MONTH_BOUNDARIES = listOf(
    "입춘" to 0, "경칩" to 1, "청명" to 2, "입하" to 3,
    "망종" to 4, "소서" to 5, "입추" to 6, "백로" to 7,
    "한로" to 8, "입동" to 9, "대설" to 10
)

// 명리 월 index: 0=寅 ... 10=子 11=丑
private fun getSolarMonthIndex(instant: Instant): Int {
    val year = kst(instant).year
    val currentTerms = getSolarTerms(year)

    val xiaohan = getSolarTerm(year, "소한")

    // 1월 초, 소한 이전은 전년도 대설부터 이어진 子월
    if (instant.isBefore(xiaohan)) return 10

    // 소한 ~ 입춘 = 丑월
    val ipchun = getSolarTerm(year, "입춘")
    if (instant.isBefore(ipchun)) return 11

    // 입춘 이후 각 절입을 확인
    var currentMonth = 0
    for ((name, monthIndex) in MONTH_BOUNDARIES) {
        val term = currentTerms.find { it.name == name }
        if (term != null && !instant.isBefore(term.instant)) {
            currentMonth = monthIndex
        }
    }

    return currentMonth
}

private fun getMonthGanji(instant: Instant): GanjiResult {
    val yearGanji = getYearGanji(instant)
    val monthIndex = getSolarMonthIndex(instant)

    // 寅월의 지지 index는 2
    val branchIndex = (2 + monthIndex) % 12

    /*
    오호둔(五虎遁)
    甲/己年 -> 丙寅  乙/庚年 -> 戊寅  丙/辛年 -> 庚寅
    丁/壬年 -> 壬寅  戊/癸年 -> 甲寅
    공식: yearStemIndex * 2 + 2 + monthIndex
    */
    val stemIndex = floorMod(yearGanji.stemIndex * 2 + 2 + monthIndex, 10)

    return GanjiResult(
        stemIndex = stemIndex,
        branchIndex = branchIndex,
        stem = HEAVENLY_STEMS[stemIndex],
        branch = EARTHLY_BRANCHES[branchIndex],
        name = HEAVENLY_STEMS[stemIndex] + EARTHLY_BRANCHES[branchIndex]
    )
}

// --------------------------------------------------
// 시주
// --------------------------------------------------

private fun getHourGanji(instant: Instant): GanjiResult {
    val hour = kst(instant).hour

    /*
    子 23:00~00:59  丑 01:00~02:59  寅 03:00~04:59 ...
    */
    val branchIndex = floor((hour + 1) / 2.0).toInt() % 12

    /*
    LFT 야자시 규칙
    23:00~23:59: 일주는 당일 유지하지만, 시주의 천간 계산에는 다음 날의 일간을 사용.
    00:00~00:59: 이미 새 날짜의 일주 사용.
    */
    val referenceInstant = if (hour == 23) instant.plusSeconds(86_400) else instant
    val dayGanji = getDayGanji(referenceInstant)

    /*
    오서둔(五鼠遁)
    甲/己日 -> 甲子  乙/庚日 -> 丙子  丙/辛日 -> 戊子
    丁/壬日 -> 庚子  戊/癸日 -> 壬子
    */
    val stemIndex = floorMod(dayGanji.stemIndex * 2 + branchIndex, 10)

    return GanjiResult(
        stemIndex = stemIndex,
        branchIndex = branchIndex,
        stem = HEAVENLY_STEMS[stemIndex],
        branch = EARTHLY_BRANCHES[branchIndex],
        name = HEAVENLY_STEMS[stemIndex] + EARTHLY_BRANCHES[branchIndex]
    )
}

// --------------------------------------------------
// 최종 사주 계산
// --------------------------------------------------

fun calculateFourPillars(instant: Instant): FourPillarsResult = FourPillarsResult(
    year = getYearGanji(instant),
    month = getMonthGanji(instant),
    day = getDayGanji(instant),
    hour = getHourGanji(instant)
)