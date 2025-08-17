package com.malrang.pomodoro.data

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
 */
data class PomodoroUiState(
    val currentScreen: Screen = Screen.Main,
    val currentMode: Mode = Mode.STUDY,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val timeLeft: Int = 30 * 60,
    val cycleCount: Int = 1,

    // 도감(영구) + 이번 세션 스프라이트(비영구)
    val collectedAnimals: List<Animal> = emptyList(), // 도감(영구 저장분 + 이번에 새로 만난 것 반영된 메모리 상태)
    val activeSprites: List<AnimalSprite> = emptyList(), // 브레이크마다 추가, 앱 종료 시 초기화

    // 통계
    val totalSessions: Int = 0, // 총합(메모리용)
    val dailyStats: Map<String, DailyStat> = emptyMap(), // 일별(영구)
    val settings: Settings = Settings()
)

// 일별 기록
data class DailyStat(
    val date: String,          // "yyyy-MM-dd"
    val studySessions: Int     // 완료한 공부 세션 수
)

/**
 * 앱의 화면 종류를 나타내는 열거형 클래스입니다.
 */
enum class Screen {
    /** 메인 화면 */
    Main,
    /** 동물 획득 화면 */
    Animal,
    /** 동물 도감 화면 */
    Collection,
    /** 설정 화면 */
    Settings,
    /** 얜 뭐임? */
    Stats
}

/**
 * 타이머의 모드를 나타내는 열거형 클래스입니다.
 */
enum class Mode {
    /** 공부 모드 */
    STUDY,
    /** 휴식 모드 */
    BREAK
}
