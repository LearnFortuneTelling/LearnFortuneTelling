package com.example.fortune_fluent.widget

import android.content.Context
import android.content.SharedPreferences
import com.example.fortune_fluent.ganji.GanjiScript
import com.example.fortune_fluent.settings.BackgroundOpacity
import com.example.fortune_fluent.settings.DisplaySettings
import com.example.fortune_fluent.settings.FortuneSettingsState
import com.example.fortune_fluent.settings.TextScale
import com.example.fortune_fluent.settings.WeekdayStyle
import com.example.fortune_fluent.settings.WidgetGridSize
import com.example.fortune_fluent.settings.WidgetSettings

/**
 * 위젯(Glance)은 앱 프로세스와 분리되어 그려질 수 있어서, 화면이 들고 있는
 * FortuneSettingsState의 mutableStateOf 객체를 직접 공유할 수 없습니다.
 * 대신 앱과 완전히 같은 SharedPreferences 파일(FortuneSettingsState.PREFS_NAME)과
 * 똑같은 키 구조로 다시 읽어옵니다.
 *
 * 주의: 아래 키 이름("widget.script" 등)은 FortuneSettingsState.kt 안의
 * WIDGET_PREFIX + putDisplay()/readWidget() 로직과 한 글자도 달라지면 안 됩니다.
 * FortuneSettingsState.kt를 나중에 고치면 이 파일도 같이 고쳐야 해요.
 */
fun readWidgetSettingsFromPrefs(context: Context): WidgetSettings {
    val prefs = context.applicationContext
        .getSharedPreferences(FortuneSettingsState.PREFS_NAME, Context.MODE_PRIVATE)

    val prefix = "widget"

    return WidgetSettings(
        display = DisplaySettings(
            ganjiScript = prefs.getEnumOrDefault("$prefix.script", GanjiScript.HANGUL),
            weekdayStyle = prefs.getEnumOrDefault("$prefix.weekday", WeekdayStyle.ENGLISH),
            showLunar = prefs.getBoolean("$prefix.lunar", false),
            showSolarTerm = prefs.getBoolean("$prefix.term", false)
        ),
        minCalendarSize = prefs.getEnumOrDefault("$prefix.minSize", WidgetGridSize.GRID_4X5),
        textScale = prefs.getEnumOrDefault("$prefix.textScale", TextScale.NORMAL),
        backgroundOpacity = prefs.getEnumOrDefault("$prefix.opacity", BackgroundOpacity.FULL),
        showGridLines = prefs.getBoolean("$prefix.grid", true)
    )
}

private inline fun <reified T : Enum<T>> SharedPreferences.getEnumOrDefault(key: String, default: T): T {
    val name = getString(key, null) ?: return default
    return enumValues<T>().firstOrNull { it.name == name } ?: default
}
