package com.malrang.pomodoro.ui.theme

import androidx.compose.ui.graphics.Color

// --- Notion Style (미니멀리즘) 스타일 팔레트 ---

// 1. 기본 텍스트 & 외곽선
val NotionCharcoal = Color(0xFF37352F)  // 메인 텍스트, 아이콘 (다크 차콜)
val NotionWhite = Color(0xFFFFFFFF)    // 순백색
val NotionGray = Color(0xFF73726E)      // 보조 텍스트 (회색)

// 2. 배경색 (오프화이트)
val NotionBackground = Color(0xFFFBFBFA) // 노션 특유의 연한 회백색 배경
val NotionSurface = Color(0xFFFFFFFF)    // 카드 및 컨테이너 배경

// 3. 포인트 컬러 (노션 가이드 기반 차분한 색상)
val NotionBlue = Color(0xFF2383E2)       // Primary: 진행 중인 작업
val NotionPink = Color(0xFFD4457D)       // Secondary: 할 일
val NotionGreen = Color(0xFF0F7B6C)      // Tertiary: 완료
val NotionYellow = Color(0xFFDFAB01)     // Highlight: 경고
val NotionRed = Color(0xFFEB5757)        // Error: 삭제, 에러

// 4. 서브 컬러 및 테두리
val NotionBorder = Color(0xFFE9E9E7)     // 얇고 가느다란 테두리
val NotionSubtleGray = Color(0xFFF1F1EF)  // 비활성 배경 또는 호버 효과


// --- Light Theme Mapping (라이트 모드) ---
// 특징: 오프화이트 배경, 다크 차콜 텍스트, 아주 얇은 회색 테두리

val primaryLight = NotionBlue
val onPrimaryLight = NotionWhite
val primaryContainerLight = NotionSubtleGray
val onPrimaryContainerLight = NotionCharcoal

val secondaryLight = NotionPink
val onSecondaryLight = NotionWhite
val secondaryContainerLight = NotionSubtleGray
val onSecondaryContainerLight = NotionCharcoal

val tertiaryLight = NotionGreen
val onTertiaryLight = NotionWhite
val tertiaryContainerLight = NotionSubtleGray
val onTertiaryContainerLight = NotionCharcoal

val errorLight = NotionRed
val onErrorLight = NotionWhite
val errorContainerLight = Color(0xFFFFEBEC)
val onErrorContainerLight = NotionRed

val backgroundLight = NotionBackground
val onBackgroundLight = NotionCharcoal

val surfaceLight = NotionSurface
val onSurfaceLight = NotionCharcoal
val surfaceVariantLight = NotionSubtleGray // 카드나 시트의 배경
val onSurfaceVariantLight = NotionGray

val outlineLight = NotionBorder // 모든 테두리는 연한 회색으로 변경


// --- Dark Theme Mapping (다크 모드) ---
// 특징: 노션 다크모드 스타일의 짙은 회색 배경 유지

val primaryDark = Color(0xFF2EAADC)
val onPrimaryDark = Color(0xFF1A1A1A)
val primaryContainerDark = Color(0xFF193B4D)
val onPrimaryContainerDark = Color(0xFFB3E5FC)

val secondaryDark = NotionPink
val onSecondaryDark = Color(0xFF1A1A1A)
val secondaryContainerDark = Color(0xFF3E2631)
val onSecondaryContainerDark = Color(0xFFFFD1DC)

val tertiaryDark = NotionGreen
val onTertiaryDark = Color(0xFF1A1A1A)
val tertiaryContainerDark = Color(0xFF1C3A33)
val onTertiaryContainerDark = Color(0xFFB2DFDB)

val errorDark = NotionRed
val onErrorDark = Color(0xFF1A1A1A)
val errorContainerDark = Color(0xFF4C2323)
val onErrorContainerDark = Color(0xFFFFB4AB)

val backgroundDark = Color(0xFF191919) // 노션 다크모드 배경색
val onBackgroundDark = Color(0xFFE3E3E2)

val surfaceDark = Color(0xFF202020)
val onSurfaceDark = Color(0xFFE3E3E2)
val surfaceVariantDark = Color(0xFF2F2F2F)
val onSurfaceVariantDark = Color(0xFF9B9B9B)

val outlineDark = Color(0xFF3F3F3F) // 다크모드에서의 경계선