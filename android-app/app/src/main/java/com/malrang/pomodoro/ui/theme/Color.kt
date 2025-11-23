package com.malrang.pomodoro.ui.theme

import androidx.compose.ui.graphics.Color

// 1. 토스(Toss) 스타일 팔레트 정의
// Toss Signature: Vivid Blue & Cool Gray System

// 브랜드 컬러 (선명하고 쨍한 파란색)
val TossBlue = Color(0xFF3182F6)      // Primary: 토스 시그니처 블루
val TossBlueDark = Color(0xFF1B64DA)  // 클릭 시, 혹은 다크모드 포인트
val TossBlueLight = Color(0xFFE8F3FF) // 아주 연한 블루 (배경 강조용)

// 배경색 (쿨 그레이 & 딥 다크)
val TossBackgroundLight = Color(0xFFF2F4F6) // 토스 특유의 밝은 회색 배경
val TossBackgroundDark = Color(0xFF101012)  // 깊은 검정/회색 (OLED 최적화 느낌)

// 표면 색상 (카드, 팝업 등)
val TossWhite = Color(0xFFFFFFFF)       // 기본 카드 배경 (순백색)
val TossSurfaceDark = Color(0xFF202022) // 다크모드 카드 배경 (살짝 밝은 검정)

// 텍스트 및 아이콘 색상 (가독성 위계)
val TossBlack = Color(0xFF191F28)     // 메인 텍스트 (완전 검정이 아닌 짙은 차콜)
val TossDarkGray = Color(0xFF333D4B)  // 서브 텍스트 1
val TossGray = Color(0xFF8B95A1)      // 서브 텍스트 2 / 아이콘
val TossLightGray = Color(0xFFB0B8C1) // 비활성 / 라인

val ErrorRed = Color(0xFFF04452)      // 토스 스타일 에러 레드


// --- Light Theme Mapping (라이트 모드) ---

val primaryLight = TossBlue
val onPrimaryLight = Color.White
val primaryContainerLight = TossBlueLight // 연한 하늘색 배경
val onPrimaryContainerLight = Color(0xFF002F6C)

val secondaryLight = TossGray // 보조 요소는 튀지 않는 회색으로
val onSecondaryLight = Color.White
val secondaryContainerLight = Color(0xFFFFFFFF) // 흰색 카드 느낌
val onSecondaryContainerLight = TossBlack

val tertiaryLight = TossDarkGray // 포인트 요소
val onTertiaryLight = Color.White
val tertiaryContainerLight = Color(0xFFE5E8EB)
val onTertiaryContainerLight = TossBlack

val errorLight = ErrorRed
val onErrorLight = Color.White
val errorContainerLight = Color(0xFFFFE5E5)
val onErrorContainerLight = Color(0xFFD60000)

// [핵심] 배경은 회색, 카드는 흰색으로 설정하여 입체감 부여
val backgroundLight = TossBackgroundLight
val onBackgroundLight = TossBlack
val surfaceLight = TossWhite
val onSurfaceLight = TossBlack
val surfaceVariantLight = TossBackgroundLight // 입력 필드 배경 등
val onSurfaceVariantLight = TossDarkGray
val outlineLight = TossLightGray


// --- Dark Theme Mapping (다크 모드) ---

val primaryDark = TossBlue // 다크모드에서도 시인성 좋은 블루 유지
val onPrimaryDark = Color.White
val primaryContainerDark = Color(0xFF003E8A)
val onPrimaryContainerDark = Color(0xFFD4E3FF)

val secondaryDark = TossGray
val onSecondaryDark = Color(0xFF101012)
val secondaryContainerDark = Color(0xFF2C2C35)
val onSecondaryContainerDark = Color(0xFFE5E8EB)

val tertiaryDark = TossBlueDark
val onTertiaryDark = Color.White
val tertiaryContainerDark = Color(0xFF003E8A)
val onTertiaryContainerDark = Color(0xFFD4E3FF)

val errorDark = Color(0xFFFF6B6B)
val onErrorDark = Color(0xFF420000)
val errorContainerDark = Color(0xFF8C0009)
val onErrorContainerDark = Color(0xFFFFDAD6)

val backgroundDark = TossBackgroundDark
val onBackgroundDark = Color(0xFFE5E8EB) // 거의 흰색에 가까운 회색
val surfaceDark = TossSurfaceDark // 약간 밝은 검정
val onSurfaceDark = Color(0xFFE5E8EB)
val surfaceVariantDark = Color(0xFF2C2C35)
val onSurfaceVariantDark = TossGray
val outlineDark = Color(0xFF4E5968)