package com.malrang.pomodoro.dataclass.ui

/**
 * 앱의 설정을 나타내는 데이터 클래스입니다.
 *
 * @property studyTime 공부 시간(분)입니다.
 * @property shortBreakTime 휴식 시간(분)입니다.
 * @property soundEnabled 알림음 사용 여부입니다.
 * @property vibrationEnabled 진동 사용 여부입니다.
 */
data class Settings(
    val studyTime: Int = 30,
    val shortBreakTime: Int = 5,
    val longBreakTime: Int = 15, // 새로운 속성: 긴 휴식 시간
    val longBreakInterval: Int = 4, // 새로운 속성: 긴 휴식까지의 공부 세션 횟수
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val autoStart: Boolean = true   // ✅ 기본값은 자동 시작
)
