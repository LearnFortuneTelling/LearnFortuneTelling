package com.example.fortune_fluent.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.fortune_fluent.settings.FortuneSettingsState
import java.time.LocalDate
import java.time.YearMonth

private enum class Route { CALENDAR, DAY_DETAIL, SETTINGS }

@Composable
fun FortuneApp() {
    val context = LocalContext.current
    val settings = remember {
        FortuneSettingsState(
            context.applicationContext.getSharedPreferences(FortuneSettingsState.PREFS_NAME, Context.MODE_PRIVATE)
        )
    }
    val monthState = rememberSaveable(saver = MonthCalendarUiState.Saver) { MonthCalendarUiState() }
    var route by rememberSaveable { mutableStateOf(Route.CALENDAR) }
    var selectedEpochDay by rememberSaveable { mutableStateOf(LocalDate.now(SeoulZone).toEpochDay()) }

    BackHandler(enabled = route != Route.CALENDAR) { route = Route.CALENDAR }

    FortuneScaffold(
        selectedTab = if (route == Route.SETTINGS) AppTab.SETTINGS else AppTab.HOME,
        onTabSelected = { tab ->
            when (tab) {
                AppTab.HOME -> {
                    // 이미 달력이면 오늘이 있는 달로 복귀, 그 외 화면이면 달력으로
                    if (route == Route.CALENDAR) monthState.goToToday()
                    route = Route.CALENDAR
                }
                AppTab.SETTINGS -> route = Route.SETTINGS
                AppTab.LIST -> Unit   // 목록 보기: 아직 미구현
            }
        }
    ) {
        when (route) {
            Route.CALENDAR -> GanjiMonthCalendarScreen(
                state = monthState,
                settings = settings.home,
                onDayClick = { date ->
                    selectedEpochDay = date.toEpochDay()
                    route = Route.DAY_DETAIL
                }
            )

            Route.DAY_DETAIL -> DayDetailScreen(
                date = LocalDate.ofEpochDay(selectedEpochDay),
                script = settings.home.ganjiScript,
                onDateChange = { date ->
                    selectedEpochDay = date.toEpochDay()
                    monthState.goTo(YearMonth.from(date))   // 돌아왔을 때 해당 달이 보이도록
                }
            )

            Route.SETTINGS -> SettingsScreen(settings)
        }
    }
}
