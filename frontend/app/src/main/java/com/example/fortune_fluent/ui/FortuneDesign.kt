package com.example.fortune_fluent.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.fortune_fluent.R
import java.time.ZoneId

// --------------------------------------------------
// 디자인 토큰 (Figma: bg-base / border-subtle / text-primary / icon-muted)
// --------------------------------------------------

internal val BgBase = Color(0xFFF2F2F2)
internal val BorderSubtle = Color(0xFFBFBFBF)
internal val IconMuted = Color(0xFF8C8C8C)
internal val TextPrimary = Color(0xFF404040)
internal val Ink = Color(0xFF0D0D0D)

internal val TextSecondary = Color(0xFF595959)

internal val AccentActive = Color(0xFFB388E0)
internal val FortuneFont: FontFamily = FontFamily.Default

internal val SeoulZone: ZoneId = ZoneId.of("Asia/Seoul")

// --------------------------------------------------
// 공통 Modifier
// --------------------------------------------------

internal fun Modifier.fortuneSurface(shape: Shape = RoundedCornerShape(20.dp)): Modifier =
    this.shadow(6.dp, shape).background(BgBase, shape)

internal fun Modifier.accessibleClickable(
    description: String,
    onClickLabel: String? = null,
    action: () -> Unit
): Modifier = this
    .clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = action)
    .clearAndSetSemantics {
        contentDescription = description
        role = Role.Button
        onClick(label = onClickLabel) { action(); true }
    }

// --------------------------------------------------
// 스캐폴드: 헤더 + 본문 + 푸터 (두 Figma 화면이 공유)
// --------------------------------------------------

enum class AppTab { LIST, HOME, SETTINGS }

@Composable
fun FortuneScaffold(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgBase)
            .systemBarsPadding()
    ) {
        FortuneHeader()
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) { content() }
        FortuneFooter(selected = selectedTab, onSelect = onTabSelected)
    }
}

@Composable
private fun FortuneHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(BgBase)
    ) {
        // 좌측 프로필 자리 — 아직 미구현 (Figma: 40dp 원)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
                .size(40.dp)
                .shadow(4.dp, CircleShape)
                .background(IconMuted, CircleShape)
                .clearAndSetSemantics { }
        )

        Image(
            painter = painterResource(R.drawable.ic_logo),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .height(64.dp)
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(BorderSubtle)
        )
    }
}

@Composable
private fun FortuneFooter(selected: AppTab, onSelect: (AppTab) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(BgBase)) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(BorderSubtle))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 좌측: 목록 보기 — 아직 미구현
            FooterIcon(Icons.Filled.Menu, "목록 보기", selected == AppTab.LIST) { onSelect(AppTab.LIST) }

            HomeButton(selected == AppTab.HOME) { onSelect(AppTab.HOME) }

            FooterIcon(Icons.Filled.Settings, "설정", selected == AppTab.SETTINGS) { onSelect(AppTab.SETTINGS) }
        }
    }
}

@Composable
private fun FooterIcon(icon: ImageVector, description: String, isSelected: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(56.dp).semantics { selected = isSelected }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (isSelected) AccentActive else IconMuted,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun HomeButton(isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clickable(onClickLabel = "오늘 달력으로 이동", role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "홈"; selected = isSelected },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .shadow(4.dp, CircleShape)
                .background(Color.White, CircleShape)
                .border(1.dp, BorderSubtle, CircleShape)
        )
    }
}

// --------------------------------------------------
// ◀ 제목 ▶ 네비게이터 (월 이동 / 날짜 이동에 공용)
// --------------------------------------------------

@Composable
fun FortuneNavBar(
    title: String,
    subtitle: String?,
    description: String,
    previousLabel: String,
    nextLabel: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .zIndex(1f)
            .shadow(6.dp)
            .background(BgBase),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavArrow("◀", previousLabel, onPrevious)

        Column(
            modifier = Modifier
                .weight(1f)
                .clearAndSetSemantics { heading(); contentDescription = description },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (subtitle != null) {
                Text(text = subtitle, color = TextSecondary, fontSize = 12.sp, fontFamily = FortuneFont)
            }
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FortuneFont,
                maxLines = 1
            )
        }

        NavArrow("▶", nextLabel, onNext)
    }
}

@Composable
private fun NavArrow(glyph: String, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .accessibleClickable(description, action = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = glyph, color = TextPrimary, fontSize = 20.sp, fontFamily = FortuneFont)
    }
}
