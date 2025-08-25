package com.malrang.pomodoro.dataclass.ui

import com.malrang.pomodoro.dataclass.animalInfo.Animal
import com.malrang.pomodoro.dataclass.sprite.AnimalSprite

/**
 * 포모도로 앱의 전체 UI 상태를 나타내는 데이터 클래스입니다.
 *
 * @property currentScreen 현재 화면을 나타냅니다.
 * @property isRunning 타이머가 실행 중인지 여부를 나타냅니다.
 * @property isPaused 타이머가 일시정지되었는지 여부를 나타냅니다.
 * @property currentMode 현재 타이머 모드(공부 또는 휴식)를 나타냅니다.
 * @property timeLeft 남은 시간을 초 단위로 나타냅니다.
 * @property cycleCount 완료한 포모도로 사이클 수를 나타냅니다.
 * @property collectedAnimals 수집한 동물 목록입니다.
 * @property totalSessions 완료한 총 공부 세션 수입니다.
 * @property settings 앱의 설정값을 담고 있습니다.
 * @property useGrassBackground 풀 배경화면 사용 여부를 나타냅니다.
 */
data class PomodoroUiState(
    val currentScreen: Screen = Screen.Main,
    val currentMode: Mode = Mode.STUDY,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val timeLeft: Int = 30 * 60,
    val cycleCount: Int = 1,

    // 도감(영구) + 이번 세션 스프라이트(비영구)
    val collectedAnimals: Set<Animal> = emptySet(),
    val activeSprites: List<AnimalSprite> = emptyList(),

    // 통계
    val totalSessions: Int = 0,
    val dailyStats: Map<String, DailyStat> = emptyMap(),

    val settings: Settings = Settings(),
    val workPresets: List<WorkPreset> = emptyList(), // 모든 Work 프리셋 목록
    val currentWorkId: String? = null,               // 현재 선택된 Work의 ID

    val isTimerStartedOnce: Boolean = false,
    val useGrassBackground: Boolean = true,
    val whitelistedApps: Set<String> = emptySet()
)