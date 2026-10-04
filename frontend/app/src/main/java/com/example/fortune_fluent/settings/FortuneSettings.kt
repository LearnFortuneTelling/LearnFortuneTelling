package com.example.fortune_fluent.settings

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.fortune_fluent.ganji.GanjiScript

// --------------------------------------------------
// 옵션 정의
// --------------------------------------------------

enum class WeekdayStyle { ENGLISH, HANGUL, HANJA }

enum class TextScale(val label: String, val factor: Float) {
    SMALL("작게", 0.9f),
    NORMAL("보통", 1.0f),
    LARGE("크게", 1.15f)
}

enum class BackgroundOpacity(val label: String, val alpha: Float) {
    FULL("100%", 1.0f),
    MEDIUM("80%", 0.8f),
    LIGHT("60%", 0.6f)
}

/** 한 달 달력을 보여줄 수 있는 최소 위젯 크기 (셀 단위: 가로 x 세로) */
enum class WidgetGridSize(val columns: Int, val rows: Int) {
    GRID_4X5(4, 5),
    GRID_5X6(5, 6);

    val label: String get() = "${columns}×$rows"

    // AOSP 위젯 셀 → dp 근사식(70n - 30). 런처마다 조금씩 다릅니다.
    val minWidthDp: Int get() = columns * 70 - 30
    val minHeightDp: Int get() = rows * 70 - 30
}

enum class WidgetLayoutMode { MONTH, SINGLE_DAY }

/**
 * 위젯 실제 크기(dp)와 사용자가 고른 최소 크기로 레이아웃을 결정.
 * Glance의 SizeMode.Responsive / LocalSize.current 값을 그대로 넣으면 됩니다.
 */
fun resolveWidgetLayout(widthDp: Float, heightDp: Float, minSize: WidgetGridSize): WidgetLayoutMode =
    if (widthDp >= minSize.minWidthDp && heightDp >= minSize.minHeightDp) {
        WidgetLayoutMode.MONTH
    } else {
        WidgetLayoutMode.SINGLE_DAY
    }

// --------------------------------------------------
// 설정 모델
// --------------------------------------------------

/** 홈 달력과 위젯이 공통으로 갖는 표기 설정 */
data class DisplaySettings(
    val ganjiScript: GanjiScript = GanjiScript.HANGUL,
    val weekdayStyle: WeekdayStyle = WeekdayStyle.ENGLISH,
    val showLunar: Boolean = false,
    val showSolarTerm: Boolean = false
)

data class WidgetSettings(
    val display: DisplaySettings = DisplaySettings(),
    val minCalendarSize: WidgetGridSize = WidgetGridSize.GRID_4X5,
    val textScale: TextScale = TextScale.NORMAL,
    val backgroundOpacity: BackgroundOpacity = BackgroundOpacity.FULL,
    val showGridLines: Boolean = true
)

// --------------------------------------------------
// 저장소 (SharedPreferences) + Compose 상태
// 위젯(Glance)에서도 같은 SharedPreferences 파일을 읽으면 됩니다.
// --------------------------------------------------

class FortuneSettingsState(private val prefs: SharedPreferences) {

    var home: DisplaySettings by mutableStateOf(readDisplay(HOME_PREFIX))
        private set

    var widget: WidgetSettings by mutableStateOf(readWidget())
        private set

    fun updateHome(transform: (DisplaySettings) -> DisplaySettings) {
        val next = transform(home)
        home = next
        prefs.edit().putDisplay(HOME_PREFIX, next).apply()
    }

    fun updateWidget(transform: (WidgetSettings) -> WidgetSettings) {
        val next = transform(widget)
        widget = next
        prefs.edit()
            .putDisplay(WIDGET_PREFIX, next.display)
            .putString("$WIDGET_PREFIX.minSize", next.minCalendarSize.name)
            .putString("$WIDGET_PREFIX.textScale", next.textScale.name)
            .putString("$WIDGET_PREFIX.opacity", next.backgroundOpacity.name)
            .putBoolean("$WIDGET_PREFIX.grid", next.showGridLines)
            .apply()
    }

    private fun readDisplay(prefix: String) = DisplaySettings(
        ganjiScript = prefs.getEnum("$prefix.script", GanjiScript.HANGUL),
        weekdayStyle = prefs.getEnum("$prefix.weekday", WeekdayStyle.ENGLISH),
        showLunar = prefs.getBoolean("$prefix.lunar", false),
        showSolarTerm = prefs.getBoolean("$prefix.term", false)
    )

    private fun readWidget() = WidgetSettings(
        display = readDisplay(WIDGET_PREFIX),
        minCalendarSize = prefs.getEnum("$WIDGET_PREFIX.minSize", WidgetGridSize.GRID_4X5),
        textScale = prefs.getEnum("$WIDGET_PREFIX.textScale", TextScale.NORMAL),
        backgroundOpacity = prefs.getEnum("$WIDGET_PREFIX.opacity", BackgroundOpacity.FULL),
        showGridLines = prefs.getBoolean("$WIDGET_PREFIX.grid", true)
    )

    companion object {
        const val PREFS_NAME = "fortune_settings"
        private const val HOME_PREFIX = "home"
        private const val WIDGET_PREFIX = "widget"
    }
}

private fun SharedPreferences.Editor.putDisplay(prefix: String, d: DisplaySettings) = this
    .putString("$prefix.script", d.ganjiScript.name)
    .putString("$prefix.weekday", d.weekdayStyle.name)
    .putBoolean("$prefix.lunar", d.showLunar)
    .putBoolean("$prefix.term", d.showSolarTerm)

private inline fun <reified T : Enum<T>> SharedPreferences.getEnum(key: String, default: T): T {
    val name = getString(key, null) ?: return default
    return enumValues<T>().firstOrNull { it.name == name } ?: default
}
