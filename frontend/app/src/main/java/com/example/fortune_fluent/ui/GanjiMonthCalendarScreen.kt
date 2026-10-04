package com.example.fortune_fluent.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fortune_fluent.ganji.LunarConverter
import com.example.fortune_fluent.ganji.getDayGanji
import com.example.fortune_fluent.ganji.solarTermsOn
import com.example.fortune_fluent.ganji.text
import com.example.fortune_fluent.settings.DisplaySettings
import com.example.fortune_fluent.settings.WeekdayStyle
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

class MonthCalendarUiState(initial: YearMonth = YearMonth.now(SeoulZone)) {
    var month: YearMonth by mutableStateOf(initial)
        private set

    fun goToPreviousMonth() { month = month.minusMonths(1) }
    fun goToNextMonth() { month = month.plusMonths(1) }
    fun goToToday() { month = YearMonth.now(SeoulZone) }
    fun goTo(target: YearMonth) { month = target }

    companion object {
        // 화면 회전/프로세스 종료 후에도 보고 있던 달을 유지
        val Saver: Saver<MonthCalendarUiState, Int> = Saver(
            save = { it.month.year * 12 + (it.month.monthValue - 1) },
            restore = { MonthCalendarUiState(YearMonth.of(it / 12, it % 12 + 1)) }
        )
    }
}

private fun generateCalendarCells(yearMonth: YearMonth): List<LocalDate?> {
    val firstOfMonth = yearMonth.atDay(1)
    val leadingBlanks = firstOfMonth.dayOfWeek.value % 7   // 일요일 시작

    val cells = mutableListOf<LocalDate?>()
    repeat(leadingBlanks) { cells.add(null) }
    (1..yearMonth.lengthOfMonth()).forEach { cells.add(yearMonth.atDay(it)) }
    while (cells.size % 7 != 0) cells.add(null)
    return cells
}

@Composable
fun GanjiMonthCalendarScreen(
    state: MonthCalendarUiState,
    settings: DisplaySettings,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now(SeoulZone) }
    val cells = remember(state.month) { generateCalendarCells(state.month) }
    val monthLabel = state.month.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase()

    Column(
        modifier = modifier
            .fillMaxSize()
            // 좌우 스와이프로 월 이동 (버튼도 그대로 있음)
            .pointerInput(state) {
                val threshold = 64.dp.toPx()
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onHorizontalDrag = { _, delta -> total += delta },
                    onDragEnd = {
                        when {
                            total > threshold -> state.goToPreviousMonth()
                            total < -threshold -> state.goToNextMonth()
                        }
                    }
                )
            }
    ) {
        FortuneNavBar(
            title = monthLabel,
            subtitle = state.month.year.toString(),
            description = "${state.month.year}년 ${state.month.monthValue}월",
            previousLabel = "이전 달",
            nextLabel = "다음 달",
            onPrevious = state::goToPreviousMonth,
            onNext = state::goToNextMonth
        )

        WeekdayHeader(settings.weekdayStyle)

        // 5주든 6주든 남는 높이를 균등하게 나눠 스크롤 없이 한 화면에 표시
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(2.dp, BorderSubtle)
        ) {
            cells.chunked(7).forEach { week ->
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    week.forEach { date ->
                        DayCell(
                            date = date,
                            isToday = date == today,
                            settings = settings,
                            onClick = { date?.let(onDayClick) },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

private fun weekdayLabels(style: WeekdayStyle): List<String> = when (style) {
    WeekdayStyle.ENGLISH -> listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
    WeekdayStyle.HANGUL -> listOf("일", "월", "화", "수", "목", "금", "토")
    WeekdayStyle.HANJA -> listOf("日", "月", "火", "水", "木", "金", "土")
}

@Composable
private fun WeekdayHeader(style: WeekdayStyle) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clearAndSetSemantics { }   // 각 날짜 셀이 요일까지 읽어주므로 중복 낭독 방지
    ) {
        weekdayLabels(style).forEach { label ->
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = label,
                    color = TextSecondary,
                    fontSize = if (style == WeekdayStyle.HANJA) 14.sp else 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FortuneFont
                )
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate?,
    isToday: Boolean,
    settings: DisplaySettings,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cellModifier = modifier.border(BorderStroke(0.5.dp, BorderSubtle))

    if (date == null) {
        Box(cellModifier.clearAndSetSemantics { })
        return
    }

    val ganji = remember(date) { getDayGanji(date.atStartOfDay(SeoulZone).toInstant()) }
    val lunar = remember(date, settings.showLunar) {
        if (settings.showLunar) LunarConverter.toLunar(date) else null
    }
    val term = remember(date, settings.showSolarTerm) {
        if (settings.showSolarTerm) solarTermsOn(date).firstOrNull() else null
    }

    val description = buildString {
        val weekday = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREAN)
        append("${date.monthValue}월 ${date.dayOfMonth}일 $weekday, 일주 ${ganji.name}")
        lunar?.let { append(", ${it.spoken}") }
        term?.let { append(", ${it.name}") }
        if (isToday) append(", 오늘")
    }

    Column(
        modifier = cellModifier
            .accessibleClickable(description, onClickLabel = "상세 사주 보기", action = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            DayNumber(date.dayOfMonth, isToday)
            Spacer(Modifier.weight(1f))
            if (lunar != null) {
                Text(text = lunar.label, color = TextSecondary, fontSize = 9.sp, maxLines = 1)
            }
        }

        Spacer(Modifier.weight(1f))
        Text(
            text = ganji.text(settings.ganjiScript),
            color = Ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
        Spacer(Modifier.weight(1f))

        // 절기 표시를 켠 경우 모든 셀이 같은 높이를 확보해서 일주 위치가 흔들리지 않게 함
        if (settings.showSolarTerm) {
            Text(
                text = term?.name.orEmpty(),
                color = TextPrimary,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.heightIn(min = 12.dp)
            )
        }
    }
}

@Composable
private fun DayNumber(day: Int, isToday: Boolean) {
    val base = Modifier.defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
    Box(
        modifier = if (isToday) base.background(Ink, CircleShape).padding(horizontal = 3.dp) else base,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            color = if (isToday) Color.White else TextPrimary,
            fontSize = 11.sp,
            fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
