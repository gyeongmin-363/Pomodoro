package com.malrang.pomodoro.dataclass.ui

/**
 * 앱의 화면 종류를 나타내는 열거형 클래스입니다.
 */
enum class Screen {
    Main, /** 메인 화면 */
    Settings, /** 설정 화면 */
    Stats, /** 통계 */
    Whitelist, /** 화이트리스트 */
    Permission, /** 권한 설정 화면 */
    AccountSettings, /** 계정 설정 화면 */
    Background, /** 배경 설정 화면 */
    DailyDetail /** 상세 일간 통계*/
}