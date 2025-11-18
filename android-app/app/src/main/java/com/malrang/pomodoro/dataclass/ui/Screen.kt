package com.malrang.pomodoro.dataclass.ui

/**
 * 앱의 화면 종류를 나타내는 열거형 클래스입니다.
 */
enum class Screen {
    /** 메인 화면 */
    Main,
    /** 설정 화면 */
    Settings,
    /** 통계 */
    Stats,
    /** 화이트리스트 */
    Whitelist,
    /** 권한 설정 화면 */
    Permission,
    AccountSettings, // 계정 설정 화면
    NicknameSetup, // 닉네임 설정 화면,
}