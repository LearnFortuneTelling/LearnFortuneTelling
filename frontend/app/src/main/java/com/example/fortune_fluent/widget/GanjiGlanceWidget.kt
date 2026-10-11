package com.example.fortune_fluent.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.currentState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider
import com.example.fortune_fluent.MainActivity
import com.example.fortune_fluent.R
import com.example.fortune_fluent.ganji.getDayGanji
import com.example.fortune_fluent.ganji.text
import com.example.fortune_fluent.settings.WeekdayStyle
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
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

private fun glanceColor(color: Color): androidx.glance.unit.ColorProvider =
    ColorProvider(day = color, night = color)

private val SingleDaySize = DpSize(130.dp, 120.dp)

private val MonthOffsetKey = intPreferencesKey("monthOffset")

class GanjiGlanceWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Responsive(
        setOf(
            SingleDaySize,
            DpSize(WidgetGridSize.GRID_4X5.minWidthDp.dp, WidgetGridSize.GRID_4X5.minHeightDp.dp),
            DpSize(WidgetGridSize.GRID_5X6.minWidthDp.dp, WidgetGridSize.GRID_5X6.minHeightDp.dp)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GanjiWidgetContent(context)
        }
    }
}

class ChangeMonthAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val delta = parameters[DeltaKey] ?: 0
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[MonthOffsetKey] = (prefs[MonthOffsetKey] ?: 0) + delta
        }
        GanjiGlanceWidget().update(context, glanceId)
    }

    companion object {
        val DeltaKey = ActionParameters.Key<Int>("delta")
    }
}

class ResetMonthAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[MonthOffsetKey] = 0
        }
        GanjiGlanceWidget().update(context, glanceId)
    }
}

@Composable
private fun GanjiWidgetContent(context: Context) {
    val widget = readWidgetSettingsFromPrefs(context)
    val size = LocalSize.current
    val mode = resolveWidgetLayout(size.width.value, size.height.value, widget.minCalendarSize)
    val offset = currentState<Preferences>()[MonthOffsetKey] ?: 0

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(16.dp)
            .background(glanceColor(BgBase.copy(alpha = widget.backgroundOpacity.alpha)))
            .clickable(actionStartActivity(MainActivity::class.java))
            .padding(6.dp)
    ) {
        when (mode) {
            WidgetLayoutMode.MONTH -> MonthGrid(widget, offset)
            WidgetLayoutMode.SINGLE_DAY -> SingleDayView(widget)
        }
    }
}

// --------------------------------------------------
// 한 달 그리드
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
private fun MonthGrid(widget: WidgetSettings, offset: Int) {
    val month = YearMonth.now(SeoulZone).plusMonths(offset.toLong())
    val cells = remember(month) { generateWidgetCells(month) }
    val today = LocalDate.now(SeoulZone)

    Column(modifier = GlanceModifier.fillMaxSize()) {
        MonthBar(month = month, offset = offset, widget = widget)
        WeekdayRow(widget = widget)

        cells.chunked(7).forEach { week ->
            Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                week.forEach { date ->
                    WidgetDayCell(
                        date = date,
                        widget = widget,
                        isToday = date != null && date == today,
                        // 주의: Row 안에서 defaultWeight()와 fillMaxSize()/fillMaxWidth()를
                        // 같이 쓰면 첫 칸이 가로 전체를 차지해 버립니다. 높이만 채우세요.
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthBar(month: YearMonth, offset: Int, widget: WidgetSettings) {
    val barSize = (16 * widget.textScale.factor).sp

    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "◀",
            modifier = GlanceModifier
                .padding(horizontal = 12.dp)
                .clickable(
                    actionRunCallback<ChangeMonthAction>(
                        actionParametersOf(ChangeMonthAction.DeltaKey to -1)
                    )
                ),
            style = TextStyle(color = glanceColor(TextPrimary), fontSize = barSize)
        )
        Row(
            modifier = GlanceModifier
                .defaultWeight()
                .clickable(actionRunCallback<ResetMonthAction>()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = month.month.getDisplayName(JTextStyle.SHORT, Locale.ENGLISH).uppercase(),
                style = TextStyle(color = glanceColor(TextPrimary), fontSize = barSize)
            )
            // 다른 달을 보고 있을 때만 "TODAY" 표시 (누르면 이번 달로 복귀)
            if (offset != 0) {
                Text(
                    text = "  TODAY",
                    style = TextStyle(
                        color = glanceColor(BorderSubtle),
                        fontSize = (9 * widget.textScale.factor).sp
                    )
                )
            }
        }
        Text(
            text = "▶",
            modifier = GlanceModifier
                .padding(horizontal = 12.dp)
                .clickable(
                    actionRunCallback<ChangeMonthAction>(
                        actionParametersOf(ChangeMonthAction.DeltaKey to 1)
                    )
                ),
            style = TextStyle(color = glanceColor(TextPrimary), fontSize = barSize)
        )
    }
}

@Composable
private fun WeekdayRow(widget: WidgetSettings) {
    val labels = if (widget.display.weekdayStyle == WeekdayStyle.ENGLISH) {
        listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
    } else {
        listOf("일", "월", "화", "수", "목", "금", "토")
    }

    Row(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 2.dp)) {
        labels.forEach { label ->
            Text(
                text = label,
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(
                    color = glanceColor(BorderSubtle),
                    fontSize = (8 * widget.textScale.factor).sp,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

@Composable
private fun WidgetDayCell(
    date: LocalDate?,
    widget: WidgetSettings,
    isToday: Boolean,
    modifier: GlanceModifier
) {
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
            if (isToday) {
                val circleSize = (16 * widget.textScale.factor).dp
                Box(
                    modifier = GlanceModifier.size(circleSize),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.widget_today_circle),
                        contentDescription = null,
                        modifier = GlanceModifier.fillMaxSize(),
                        colorFilter = ColorFilter.tint(glanceColor(Ink))
                    )
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = TextStyle(
                            color = glanceColor(BgBase),
                            fontSize = numberSize,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            } else {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = TextStyle(color = glanceColor(TextPrimary), fontSize = numberSize)
                )
            }
            Text(
                text = ganji.text(widget.display.ganjiScript),
                style = TextStyle(
                    color = glanceColor(Ink),
                    fontSize = ganjiSize,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }

    if (widget.showGridLines) {
        Box(modifier = modifier.background(glanceColor(BorderSubtle)).padding(1.dp)) {
            Box(modifier = GlanceModifier.fillMaxSize().background(glanceColor(BgBase))) {
                cellContent()
            }
        }
    } else {
        Box(modifier = modifier) { cellContent() }
    }
}

// --------------------------------------------------
// 위젯이 설정한 최소 크기보다 작을 때
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
            style = TextStyle(color = glanceColor(TextPrimary), fontSize = (11 * widget.textScale.factor).sp)
        )
        Text(
            text = ganji.text(widget.display.ganjiScript),
            style = TextStyle(
                color = glanceColor(Ink),
                fontSize = (22 * widget.textScale.factor).sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}