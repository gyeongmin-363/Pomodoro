package com.malrang.pomodoro.dataclass.ui

/**
 * 앱의 설정을 나타내는 데이터 클래스입니다.
 *
 * @property studyTime 공부 시간(분)입니다.
 * @property breakTime 휴식 시간(분)입니다.
 * @property soundEnabled 알림음 사용 여부입니다.
 * @property vibrationEnabled 진동 사용 여부입니다.
 */
data class Settings(
    val studyTime: Int = 30,
    val breakTime: Int = 5,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val autoStart: Boolean = true   // ✅ 기본값은 자동 시작

)
