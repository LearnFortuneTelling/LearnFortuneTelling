package com.example.fortune_fluent.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fortune_fluent.ganji.GanjiScript
import com.example.fortune_fluent.ganji.getGanji
import com.example.fortune_fluent.ganji.text
import com.example.fortune_fluent.settings.BackgroundOpacity
import com.example.fortune_fluent.settings.DisplaySettings
import com.example.fortune_fluent.settings.FortuneSettingsState
import com.example.fortune_fluent.settings.TextScale
import com.example.fortune_fluent.settings.WeekdayStyle
import com.example.fortune_fluent.settings.WidgetGridSize

private enum class SettingsTab(val title: String) {
    HOME("홈 달력"),
    WIDGET("위젯")
}

@Composable
fun SettingsScreen(settings: FortuneSettingsState, modifier: Modifier = Modifier) {
    var tab by rememberSaveable { mutableStateOf(SettingsTab.HOME) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SegmentedControl(
            options = SettingsTab.values().toList(),
            selected = tab,
            label = { it.title },
            onSelect = { tab = it }
        )

        when (tab) {
            SettingsTab.HOME -> HomeSettingsContent(settings)
            SettingsTab.WIDGET -> WidgetSettingsContent(settings)
        }
    }
}

// --------------------------------------------------
// 탭별 내용
// --------------------------------------------------

@Composable
private fun HomeSettingsContent(settings: FortuneSettingsState) {
    DisplaySettingsSection(
        display = settings.home,
        onChange = settings::updateHome
    )
}

@Composable
private fun WidgetSettingsContent(settings: FortuneSettingsState) {
    val widget = settings.widget

    SettingsGroup("크기") {
        SettingChoice(
            title = "달력 표시 크기",
            description = "위젯이 이 크기 이상이면 한 달 달력을, 더 작아지면 선택한 날짜 하나만 보여줘요.",
            options = WidgetGridSize.values().toList(),
            selected = widget.minCalendarSize,
            label = { it.label },
            onSelect = { v -> settings.updateWidget { it.copy(minCalendarSize = v) } }
        )
        HelperText("위젯 크기는 홈 화면에서 위젯을 길게 눌러 조절할 수 있어요.")
    }

    DisplaySettingsSection(
        display = widget.display,
        onChange = { transform -> settings.updateWidget { w -> w.copy(display = transform(w.display)) } }
    )

    SettingsGroup("디자인") {
        SettingChoice(
            title = "글자 크기",
            description = null,
            options = TextScale.values().toList(),
            selected = widget.textScale,
            label = { it.label },
            onSelect = { v -> settings.updateWidget { it.copy(textScale = v) } }
        )
        SettingChoice(
            title = "배경 투명도",
            description = null,
            options = BackgroundOpacity.values().toList(),
            selected = widget.backgroundOpacity,
            label = { it.label },
            onSelect = { v -> settings.updateWidget { it.copy(backgroundOpacity = v) } }
        )
        SettingSwitch(
            title = "격자선 표시",
            description = null,
            checked = widget.showGridLines,
            onCheckedChange = { v -> settings.updateWidget { it.copy(showGridLines = v) } }
        )
    }
}

/** 홈 달력 / 위젯이 공유하는 표기 설정 묶음 */
@Composable
private fun DisplaySettingsSection(
    display: DisplaySettings,
    onChange: ((DisplaySettings) -> DisplaySettings) -> Unit
) {
    val sample = remember { getGanji(54) }   // 무오 / 戊午

    SettingsGroup("표기") {
        SettingChoice(
            title = "일주 표기",
            description = "예) ${sample.text(GanjiScript.HANGUL)} ↔ ${sample.text(GanjiScript.HANJA)}",
            options = GanjiScript.values().toList(),
            selected = display.ganjiScript,
            label = { if (it == GanjiScript.HANGUL) "한글" else "한자" },
            onSelect = { v -> onChange { it.copy(ganjiScript = v) } }
        )
        SettingChoice(
            title = "요일 표기",
            description = "예) SUN ↔ 일 ↔ 日",
            options = WeekdayStyle.values().toList(),
            selected = display.weekdayStyle,
            label = {
                when (it) {
                    WeekdayStyle.ENGLISH -> "영문"
                    WeekdayStyle.HANGUL -> "한글"
                    WeekdayStyle.HANJA -> "한자"
                }
            },
            onSelect = { v -> onChange { it.copy(weekdayStyle = v) } }
        )
    }

    SettingsGroup("추가 정보") {
        SettingSwitch(
            title = "음력 표시",
            description = "날짜 옆에 음력을 작게 함께 보여줘요",
            checked = display.showLunar,
            onCheckedChange = { v -> onChange { it.copy(showLunar = v) } }
        )
        SettingSwitch(
            title = "절기 표시",
            description = "절기가 있는 날에 절기 이름을 보여줘요",
            checked = display.showSolarTerm,
            onCheckedChange = { v -> onChange { it.copy(showSolarTerm = v) } }
        )
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            color = TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FortuneFont,
            modifier = Modifier.padding(start = 4.dp).semantics { heading() }
        )
        content()
    }
}

@Composable
private fun HelperText(text: String) {
    Text(
        text = text,
        color = TextSecondary,
        fontSize = 13.sp,
        fontFamily = FortuneFont,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun <T> SettingChoice(
    title: String,
    description: String?,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fortuneSurface(RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium, fontFamily = FortuneFont)
        if (description != null) {
            Text(description, color = TextSecondary, fontSize = 13.sp, fontFamily = FortuneFont)
        }
        SegmentedControl(options, selected, label, onSelect)
    }
}

@Composable
private fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    val trackShape = RoundedCornerShape(24.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(trackShape)
            .background(BgBase)
            .border(1.dp, BorderSubtle, trackShape)
            .selectableGroup()
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(3.dp)
                    .clip(RoundedCornerShape(21.dp))
                    .background(if (isSelected) TextPrimary else Color.Transparent)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(option) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label(option),
                    color = if (isSelected) Color.White else TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    fontFamily = FortuneFont,
                    maxLines = 1
                )
            }
        }
    }
}

/** 켜기/끄기 항목 */
@Composable
private fun SettingSwitch(
    title: String,
    description: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fortuneSurface(shape)
            .clip(shape)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .heightIn(min = 56.dp)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium, fontFamily = FortuneFont)
            if (description != null) {
                Text(description, color = TextSecondary, fontSize = 13.sp, fontFamily = FortuneFont)
            }
        }
        FortuneSwitch(checked)
    }
}

@Composable
private fun FortuneSwitch(checked: Boolean) {
    val thumbOffset by animateDpAsState(targetValue = if (checked) 32.dp else 4.dp, label = "thumb")
    val trackShape = RoundedCornerShape(18.dp)

    Box(
        modifier = Modifier
            .size(width = 64.dp, height = 36.dp)
            .clip(trackShape)
            .background(if (checked) TextPrimary else BorderSubtle)
            .border(1.dp, IconMuted, trackShape)
            .clearAndSetSemantics { }   // 상태는 행의 toggleable 이 대신 전달
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset, y = 4.dp)
                .size(28.dp)
                .background(if (checked) BgBase else IconMuted, CircleShape)
        )
    }
}

private enum class SettingsTab(val title: String) {
    HOME("홈 달력"),
    WIDGET("위젯")
}

/**
 * Figma "Widget" 화면 기반 설정 화면.
 * - 미리 보기 / 테마 선택은 이번 범위에서 제외
 * - 상단 탭으로 "홈 달력"과 "위젯" 설정을 분리
 * - 위젯 탭에서 값을 바꾸면 홈 화면에 이미 놓인 위젯도 즉시 다시 그리도록
 *   GanjiGlanceWidget().updateAll(context)를 같이 호출함
 */
@Composable
fun SettingsScreen(settings: FortuneSettingsState, modifier: Modifier = Modifier) {
    var tab by rememberSaveable { mutableStateOf(SettingsTab.HOME) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SegmentedControl(
            options = SettingsTab.values().toList(),
            selected = tab,
            label = { it.title },
            onSelect = { tab = it }
        )

        when (tab) {
            SettingsTab.HOME -> HomeSettingsContent(settings)
            SettingsTab.WIDGET -> WidgetSettingsContent(settings)
        }
    }
}

@Composable
private fun HomeSettingsContent(settings: FortuneSettingsState) {
    DisplaySettingsSection(
        display = settings.home,
        onChange = settings::updateHome
    )
}

@Composable
private fun WidgetSettingsContent(settings: FortuneSettingsState) {
    val widget = settings.widget
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // settings.updateWidget{...}로 값을 바꾼 직후 위젯도 다시 그리게 하는 공용 래퍼.
    // 위젯 탭의 모든 변경은 반드시 이 함수를 거치게 해서 "저장은 됐는데 위젯 화면은
    // 그대로"인 상태가 생기지 않게 함.
    fun updateWidgetAndRefresh(transform: (com.example.fortune_fluent.settings.WidgetSettings) -> com.example.fortune_fluent.settings.WidgetSettings) {
        settings.updateWidget(transform)
        scope.launch { GanjiGlanceWidget().updateAll(context) }
    }

    SettingsGroup("크기") {
        SettingChoice(
            title = "달력 표시 크기",
            description = "위젯이 이 크기 이상이면 한 달 달력을, 더 작아지면 선택한 날짜 하나만 보여줘요.",
            options = WidgetGridSize.values().toList(),
            selected = widget.minCalendarSize,
            label = { it.label },
            onSelect = { v -> updateWidgetAndRefresh { it.copy(minCalendarSize = v) } }
        )
        HelperText("위젯 크기는 홈 화면에서 위젯을 길게 눌러 조절할 수 있어요.")
    }

    DisplaySettingsSection(
        display = widget.display,
        onChange = { transform -> updateWidgetAndRefresh { Dw -> w.copy(display = transform(w.display)) } }
    )

    SettingsGroup("디자인") {
        SettingChoice(
            title = "글자 크기",
            description = null,
            options = TextScale.values().toList(),
            selected = widget.textScale,
            label = { it.label },
            onSelect = { v -> updateWidgetAndRefresh { it.copy(textScale = v) } }
        )
        SettingChoice(
            title = "배경 투명도",
            description = null,
            options = BackgroundOpacity.values().toList(),
            selected = widget.backgroundOpacity,
            label = { it.label },
            onSelect = { v -> updateWidgetAndRefresh { it.copy(backgroundOpacity = v) } }
        )
        SettingSwitch(
            title = "격자선 표시",
            description = null,
            checked = widget.showGridLines,
            onCheckedChange = { v -> updateWidgetAndRefresh { it.copy(showGridLines = v) } }
        )
    }
}

/** 홈 달력 / 위젯이 공유하는 표기 설정 묶음 */
@Composable
private fun DisplaySettingsSection(
    display: DisplaySettings,
    onChange: ((DisplaySettings) -> DisplaySettings) -> Unit
) {
    val sample = remember { getGanji(54) }   // 무오 / 戊午

    SettingsGroup("표기") {
        SettingChoice(
            title = "일주 표기",
            description = "예) ${sample.text(GanjiScript.HANGUL)} ↔ ${sample.text(GanjiScript.HANJA)}",
            options = GanjiScript.values().toList(),
            selected = display.ganjiScript,
            label = { if (it == GanjiScript.HANGUL) "한글" else "한자" },
            onSelect = { v -> onChange { it.copy(ganjiScript = v) } }
        )
        SettingChoice(
            title = "요일 표기",
            description = "예) SUN ↔ 일 ↔ 日",
            options = WeekdayStyle.values().toList(),
            selected = display.weekdayStyle,
            label = {
                when (it) {
                    WeekdayStyle.ENGLISH -> "영문"
                    WeekdayStyle.HANGUL -> "한글"
                    WeekdayStyle.HANJA -> "한자"
                }
            },
            onSelect = { v -> onChange { it.copy(weekdayStyle = v) } }
        )
    }

    SettingsGroup("추가 정보") {
        SettingSwitch(
            title = "음력 표시",
            description = "날짜 옆에 음력을 작게 함께 보여줘요",
            checked = display.showLunar,
            onCheckedChange = { v -> onChange { it.copy(showLunar = v) } }
        )
        SettingSwitch(
            title = "절기 표시",
            description = "절기가 있는 날에 절기 이름을 보여줘요",
            checked = display.showSolarTerm,
            onCheckedChange = { v -> onChange { it.copy(showSolarTerm = v) } }
        )
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            color = TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FortuneFont,
            modifier = Modifier.padding(start = 4.dp).semantics { heading() }
        )
        content()
    }
}

@Composable
private fun HelperText(text: String) {
    Text(
        text = text,
        color = TextSecondary,
        fontSize = 13.sp,
        fontFamily = FortuneFont,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun <T> SettingChoice(
    title: String,
    description: String?,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fortuneSurface(RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium, fontFamily = FortuneFont)
        if (description != null) {
            Text(description, color = TextSecondary, fontSize = 13.sp, fontFamily = FortuneFont)
        }
        SegmentedControl(options, selected, label, onSelect)
    }
}

@Composable
private fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    val trackShape = RoundedCornerShape(24.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(trackShape)
            .background(BgBase)
            .border(1.dp, BorderSubtle, trackShape)
            .selectableGroup()
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(3.dp)
                    .clip(RoundedCornerShape(21.dp))
                    .background(if (isSelected) TextPrimary else Color.Transparent)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(option) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label(option),
                    color = if (isSelected) Color.White else TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    fontFamily = FortuneFont,
                    maxLines = 1
                )
            }
        }
    }
}

/** 켜기/끄기 항목 */
@Composable
private fun SettingSwitch(
    title: String,
    description: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fortuneSurface(shape)
            .clip(shape)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .heightIn(min = 56.dp)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium, fontFamily = FortuneFont)
            if (description != null) {
                Text(description, color = TextSecondary, fontSize = 13.sp, fontFamily = FortuneFont)
            }
        }
        FortuneSwitch(checked)
    }
}

@Composable
private fun FortuneSwitch(checked: Boolean) {
    val thumbOffset by animateDpAsState(targetValue = if (checked) 32.dp else 4.dp, label = "thumb")
    val trackShape = RoundedCornerShape(18.dp)

    Box(
        modifier = Modifier
            .size(width = 64.dp, height = 36.dp)
            .clip(trackShape)
            .background(if (checked) TextPrimary else BorderSubtle)
            .border(1.dp, IconMuted, trackShape)
            .clearAndSetSemantics { }   // 상태는 행의 toggleable 이 대신 전달
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset, y = 4.dp)
                .size(28.dp)
                .background(if (checked) BgBase else IconMuted, CircleShape)
        )
    }
}