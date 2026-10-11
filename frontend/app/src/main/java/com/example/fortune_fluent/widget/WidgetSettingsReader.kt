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
