package com.malrang.pomodoro.dataclass.ui

/**
 * 요청할 권한의 종류를 나타내는 열거형 클래스입니다.
 */
enum class PermissionType {
    /** 알림 권한 (Android 13 이상) */
    NOTIFICATION,
    /** 사용 정보 접근 권한 */
    USAGE_STATS,
    /** 다른 앱 위에 표시 권한 */
    OVERLAY
}

/**
 * 개별 권한의 정보를 담는 데이터 클래스입니다.
 *
 * @property type 권한의 종류.
 * @property title 사용자에게 보여줄 권한 이름.
 * @property description 권한이 필요한 이유에 대한 설명.
 * @property isGranted 현재 권한 허용 여부.
 */
data class PermissionInfo(
    val type: PermissionType,
    val title: String,
    val description: String,
    var isGranted: Boolean = false
)