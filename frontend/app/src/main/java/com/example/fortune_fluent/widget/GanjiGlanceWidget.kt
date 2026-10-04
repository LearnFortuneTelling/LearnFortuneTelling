package com.example.fortune_fluent.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
// Glance 버전에 따라 ColorProvider 패키지가 다를 수 있어요.
// 만약 import가 안 잡히면 androidx.glance.unit.ColorProvider 로 바꿔보세요.
import androidx.glance.color.ColorProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.defaultWeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.fortune_fluent.MainActivity
import com.example.fortune_fluent.ganji.getDayGanji
import com.example.fortune_fluent.ganji.text
import com.example.fortune_fluent.settings.WidgetGridSize
import com.example.fortune_fluent.settings.WidgetLayoutMode
import com.example.fortune_fluent.settings.WidgetSettings
import com.example.fortune_fluent.settings.resolveWidgetLayout
import com.example.fortune_fluent.ui.BgBase
import com.example.fortune_fluent.ui.BorderSubtle
import com.example.fortune_fluent.ui.Ink
import com.example.fortune_fluent.ui.SeoulZone
import com.example.fortune_fluent.ui.TextPrimary
import java.time.LocalDate
import java.time.YearMonth

/**
 * SizeMode.Responsive에 넣는 버킷 3개:
 *  - 가장 작은 버킷(하루만 표시)
 *  - 설정의 WidgetGridSize.GRID_4X5 / GRID_5X6 (한 달 표시)
 * Glance가 실제 위젯 크기를 이 중 가장 가까운(작거나 같은) 값으로 스냅해서
 * LocalSize.current에 넣어줍니다.
 */
private val SingleDaySize = DpSize(130.dp, 120.dp)

class GanjiGlanceWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            SingleDaySize,
            DpSize(WidgetGridSize.GRID_4X5.minWidthDp.dp, WidgetGridSize.GRID_4X5.minHeightDp.dp),
            DpSize(WidgetGridSize.GRID_5X6.minWidthDp.dp, WidgetGridSize.GRID_5X6.minHeightDp.dp)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // 매번 그릴 때마다 SharedPreferences를 새로 읽어서, 설정 화면에서
        // 값을 바꾸고 돌아왔을 때 위젯도 최신 설정을 반영하게 함.
        provideContent {
            GanjiWidgetContent(context)
        }
    }
}

@Composable
private fun GanjiWidgetContent(context: Context) {
    val widget = readWidgetSettingsFromPrefs(context)
    val size = LocalSize.current
    val mode = resolveWidgetLayout(size.width.value, size.height.value, widget.minCalendarSize)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(16.dp)
            .background(ColorProvider(BgBase.copy(alpha = widget.backgroundOpacity.alpha)))
            .clickable(actionStartActivity<MainActivity>())
            .padding(6.dp)
    ) {
        when (mode) {
            WidgetLayoutMode.MONTH -> MonthGrid(widget)
            WidgetLayoutMode.SINGLE_DAY -> SingleDayView(widget)
        }
    }
}

// --------------------------------------------------
// 한 달 그리드 — 앱 화면의 generateCalendarCells()와 동일한 로직
// --------------------------------------------------

private fun generateWidgetCells(yearMonth: YearMonth): List<LocalDate?> {
    val firstOfMonth = yearMonth.atDay(1)
    val leadingBlanks = firstOfMonth.dayOfWeek.value % 7
    val cells = mutableListOf<LocalDate?>()
    repeat(leadingBlanks) { cells.add(null) }
    (1..yearMonth.lengthOfMonth()).forEach { cells.add(yearMonth.atDay(it)) }
    while (cells.size % 7 != 0) cells.add(null)
    return cells
}

@Composable
private fun MonthGrid(widget: WidgetSettings) {
    val month = remember { YearMonth.now(SeoulZone) }
    val cells = remember(month) { generateWidgetCells(month) }

    Column(modifier = GlanceModifier.fillMaxSize()) {
        cells.chunked(7).forEach { week ->
            Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                week.forEach { date ->
                    WidgetDayCell(
                        date = date,
                        widget = widget,
                        modifier = GlanceModifier.defaultWeight().fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetDayCell(date: LocalDate?, widget: WidgetSettings, modifier: GlanceModifier) {
    if (date == null) {
        Box(modifier = modifier) {}
        return
    }

    val ganji = remember(date) { getDayGanji(date.atStartOfDay(SeoulZone).toInstant()) }
    val numberSize = (9 * widget.textScale.factor).sp
    val ganjiSize = (12 * widget.textScale.factor).sp

    val cellContent: @Composable () -> Unit = {
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = TextStyle(color = ColorProvider(TextPrimary), fontSize = numberSize)
            )
            Text(
                text = ganji.text(widget.display.ganjiScript),
                style = TextStyle(
                    color = ColorProvider(Ink),
                    fontSize = ganjiSize,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }

    // 격자선 표시 설정: 바깥 Box를 테두리색으로, 1dp 패딩만큼만 보이게 해서
    // 안쪽 Box와의 틈이 "선"처럼 보이게 하는 방식 (Glance는 border 모디파이어가 없음)
    if (widget.showGridLines) {
        Box(modifier = modifier.background(ColorProvider(BorderSubtle)).padding(1.dp)) {
            Box(modifier = GlanceModifier.fillMaxSize().background(ColorProvider(BgBase))) {
                cellContent()
            }
        }
    } else {
        Box(modifier = modifier) { cellContent() }
    }
}

// --------------------------------------------------
// 위젯이 설정한 최소 크기보다 작을 때 — 오늘 하루만 크게
// --------------------------------------------------

@Composable
private fun SingleDayView(widget: WidgetSettings) {
    val today = remember { LocalDate.now(SeoulZone) }
    val ganji = remember { getDayGanji(today.atStartOfDay(SeoulZone).toInstant()) }

    Column(
        modifier = GlanceModifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${today.monthValue}.${today.dayOfMonth}",
            style = TextStyle(color = ColorProvider(TextPrimary), fontSize = (11 * widget.textScale.factor).sp)
        )
        Text(
            text = ganji.text(widget.display.ganjiScript),
            style = TextStyle(
                color = ColorProvider(Ink),
                fontSize = (22 * widget.textScale.factor).sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}
