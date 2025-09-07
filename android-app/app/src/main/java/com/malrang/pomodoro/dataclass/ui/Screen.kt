package com.malrang.pomodoro.dataclass.ui

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
    /** 통계 */
    Stats,
    /** 화이트리스트 */
    Whitelist,
    /** 권한 설정 화면 */
    Permission,
    StudyRoom,
    AccountSettings, // 계정 설정 화면 추가
    DeleteStudyRoom
}