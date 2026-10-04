package com.example.fortune_fluent.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fortune_fluent.ganji.DayDetail
import com.example.fortune_fluent.ganji.GanjiResult
import com.example.fortune_fluent.ganji.GanjiScript
import com.example.fortune_fluent.ganji.HourPillar
import com.example.fortune_fluent.ganji.branchElement
import com.example.fortune_fluent.ganji.branchText
import com.example.fortune_fluent.ganji.buildDayDetail
import com.example.fortune_fluent.ganji.elementText
import com.example.fortune_fluent.ganji.isMonthBoundary
import com.example.fortune_fluent.ganji.other
import com.example.fortune_fluent.ganji.stemElement
import com.example.fortune_fluent.ganji.stemText
import com.example.fortune_fluent.ganji.stemYinYang
import com.example.fortune_fluent.ganji.text
import com.example.fortune_fluent.ganji.timeText
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DayDetailScreen(
    date: LocalDate,
    script: GanjiScript,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val detail = remember(date) { buildDayDetail(date) }
    val weekday = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)

    Column(modifier = modifier.fillMaxSize()) {
        FortuneNavBar(
            title = "${date.monthValue}월 ${date.dayOfMonth}일 ($weekday)",
            subtitle = buildString {
                append("${date.year}년")
                detail.lunar?.let { append(" · ${it.spoken}") }
            },
            description = buildString {
                append("${date.year}년 ${date.monthValue}월 ${date.dayOfMonth}일 ")
                append(date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREAN))
                detail.lunar?.let { append(", ${it.spoken}") }
            },
            previousLabel = "전날",
            nextLabel = "다음 날",
            onPrevious = { onDateChange(date.minusDays(1)) },
            onNext = { onDateChange(date.plusDays(1)) }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 월주 → 일주
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PillarCard("월주", detail.month, script, Modifier.weight(1f))
                PillarCard("일주", detail.day, script, Modifier.weight(1f))
            }

            YearReference(detail.year, script)
            TermNotice(detail, script)
            HourSection(detail.hours, script)

            Text(
                text = "※ 절입 시각·야자시(23:00~23:59)는 현재 계산기 규칙을 따릅니다. " +
                    "야자시는 일주를 그날로 두고, 시주 천간만 다음 날 일간 기준으로 계산합니다.",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

// --------------------------------------------------
// 월주 / 일주 카드 : 천간 위, 지지 아래 (전통 사주표 배치)
// --------------------------------------------------

@Composable
private fun PillarCard(title: String, ganji: GanjiResult, script: GanjiScript, modifier: Modifier = Modifier) {
    val other = script.other()
    val spoken = "$title ${ganji.name}. 천간 ${ganji.stem} ${ganji.stemElement} ${ganji.stemYinYang}, " +
        "지지 ${ganji.branch} ${ganji.branchElement}"

    Column(
        modifier = modifier
            .fortuneSurface(RoundedCornerShape(20.dp))
            .clearAndSetSemantics { contentDescription = spoken }
            .padding(vertical = 16.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(8.dp))

        Glyph(
            main = ganji.stemText(script),
            caption = "${ganji.stemText(other)} · ${elementText(ganji.stemElement, other)} · ${ganji.stemYinYang}"
        )
        Box1dpLine()
        Glyph(
            main = ganji.branchText(script),
            caption = "${ganji.branchText(other)} · ${elementText(ganji.branchElement, other)}"
        )
    }
}

@Composable
private fun Glyph(main: String, caption: String) {
    Column(
        modifier = Modifier.padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = main, color = Ink, fontSize = 36.sp, fontWeight = FontWeight.Bold)
        Text(text = caption, color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Box1dpLine() {
    Spacer(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(BorderSubtle)
    )
}

// --------------------------------------------------
// 연주(참고) / 절기 안내
// --------------------------------------------------

@Composable
private fun YearReference(year: GanjiResult, script: GanjiScript) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fortuneSurface(RoundedCornerShape(28.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .clearAndSetSemantics { contentDescription = "연주 ${year.name}, 입춘 기준" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("연주", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text("입춘 기준", color = TextSecondary, fontSize = 12.sp)
        }
        Text(year.text(script), color = Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TermNotice(detail: DayDetail, script: GanjiScript) {
    if (detail.termsToday.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fortuneSurface(RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("오늘의 절기", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.semantics { heading() })

        detail.termsToday.forEach { term ->
            val kind = if (term.isMonthBoundary()) "절입" else "중기"
            Text("${term.name} ${term.timeText()} ($kind)", color = TextPrimary, fontSize = 14.sp)
        }

        detail.termChange?.let { change ->
            val monthLine = "${change.term.timeText()}부터 월주가 " +
                "${change.monthBefore.text(script)} → ${change.monthAfter.text(script)}(으)로 바뀝니다."
            Text(monthLine, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            if (change.yearChanged) {
                Text(
                    "연주도 ${change.yearBefore.text(script)} → ${change.yearAfter.text(script)}(으)로 바뀝니다.",
                    color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                )
            }
            Text("위 월주는 이 시각 이후 기준입니다. 카드의 월주는 00:00 기준입니다.",
                color = TextSecondary, fontSize = 12.sp)
        }
    }
}

// --------------------------------------------------
// 시주
// --------------------------------------------------

@Composable
private fun HourSection(hours: List<HourPillar>, script: GanjiScript) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.padding(horizontal = 4.dp)) {
            Text("시주", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { heading() })
            Text("태어난 시각에 따라 달라져요", color = TextSecondary, fontSize = 12.sp)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fortuneSurface(RoundedCornerShape(20.dp))
        ) {
            hours.forEachIndexed { index, slot ->
                if (index > 0) {
                    Spacer(
                        Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(BorderSubtle.copy(alpha = 0.6f))
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clearAndSetSemantics {
                            contentDescription = "${slot.titleHangul} ${slot.range}, 시주 ${slot.pillar.name}"
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = if (script == GanjiScript.HANJA) slot.titleHanja else slot.titleHangul,
                            color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium
                        )
                        Text(slot.range, color = TextSecondary, fontSize = 12.sp)
                    }
                    Text(
                        text = slot.pillar.text(script),
                        color = Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
