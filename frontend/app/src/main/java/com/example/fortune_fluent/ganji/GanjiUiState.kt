package com.example.fortune_fluent.ganji

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.Instant
import java.time.ZoneId

private val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")

class GanjiUiState(initial: Instant = Instant.now()) {

    var current: Instant by mutableStateOf(initial)
        private set

    val pillars: FourPillarsResult
        get() = calculateFourPillars(current)

    val hourOfDay: Int
        get() = current.atZone(SEOUL).hour

    fun goToPreviousDay() {
        current = current.minusSeconds(86_400)
    }

    fun goToNextDay() {
        current = current.plusSeconds(86_400)
    }

    fun setHour(hour: Int) {
        current = current.atZone(SEOUL).withHour(hour).toInstant()
    }
}