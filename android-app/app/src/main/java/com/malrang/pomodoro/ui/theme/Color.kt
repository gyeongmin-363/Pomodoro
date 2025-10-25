package com.malrang.pomodoro.ui.theme

import androidx.compose.ui.graphics.Color

// 1. 모던 팔레트 정의 (Muted Blue & Gray)

// 기본 테마 색상 (차분한 파란색 계열)
val MutedBlue = Color(0xFF5A8DEE) // Primary
val MutedBlueLight = Color(0xFF8AAFFF) // Lighter variant for dark mode primary
val MutedBlueDark = Color(0xFF0060BA) // Darker variant

// 중립 배경색 (가이드라인 준수)
val OffWhite = Color(0xFFFAFAFA) // 라이트 모드 배경 (거의 흰색)
val DarkNavy = Color(0xFF1B263B) // 다크 모드 배경 (어두운 네이비)

// 중립 표면 및 텍스트 색상
val LightGray = Color(0xFFE0E0E0) // 라이트 모드 표면/경계선
val MediumGray = Color(0xFF9E9E9E) // 비활성/보조 텍스트
val DarkGray = Color(0xFF424242) // 다크 모드 표면
val White = Color(0xFFFFFFFF) // 라이트 모드 텍스트
val Black = Color(0xFF000000) // 다크 모드 텍스트


// 2. 기존 레트로 색상 (Pink80 등) 제거
/*
val Pink80 = Color(0xFFEFB8C8)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink40 = Color(0xFF7D5260)
val PurpleGrey40 = Color(0xFF625b71)
*/

// 3. (참고) Material 3 색상 이름에 맞춰 재정의 (선택 사항이지만 권장)
// 이 색상들은 Theme.kt의 ColorScheme에서 사용됩니다.

val primaryLight = MutedBlue
val onPrimaryLight = Color.White
val primaryContainerLight = Color(0xFFD8E2FF)
val onPrimaryContainerLight = Color(0xFF001A40)
val secondaryLight = Color(0xFF565E71)
val onSecondaryLight = Color.White
val secondaryContainerLight = Color(0xFFDAE2F9)
val onSecondaryContainerLight = Color(0xFF131C2C)
val tertiaryLight = Color(0xFF705574)
val onTertiaryLight = Color.White
val tertiaryContainerLight = Color(0xFFFAD7FB)
val onTertiaryContainerLight = Color(0xFF29132E)
val errorLight = Color(0xFFBA1A1A)
val onErrorLight = Color.White
val errorContainerLight = Color(0xFFFFDAD6)
val onErrorContainerLight = Color(0xFF410002)
val backgroundLight = OffWhite // 가이드라인 적용
val onBackgroundLight = Color(0xFF1A1C1E)
val surfaceLight = OffWhite // 가이드라인 적용
val onSurfaceLight = Color(0xFF1A1C1E)
val surfaceVariantLight = LightGray // 가이드라인 적용
val onSurfaceVariantLight = Color(0xFF44474F)
val outlineLight = MediumGray // 가이드라인 적용


val primaryDark = MutedBlueLight // 다크 모드에서는 더 밝은 파란색 사용
val onPrimaryDark = Color(0xFF002F67)
val primaryContainerDark = Color(0xFF004590)
val onPrimaryContainerDark = Color(0xFFD8E2FF)
val secondaryDark = Color(0xFFBEC6DC)
val onSecondaryDark = Color(0xFF283141)
val secondaryContainerDark = Color(0xFF3E4759)
val onSecondaryContainerDark = Color(0xFFDAE2F9)
val tertiaryDark = Color(0xFFDDBCE0)
val onTertiaryDark = Color(0xFF3F2844)
val tertiaryContainerDark = Color(0xFF573E5C)
val onTertiaryContainerDark = Color(0xFFFAD7FB)
val errorDark = Color(0xFFFFB4AB)
val onErrorDark = Color(0xFF690005)
val errorContainerDark = Color(0xFF93000A)
val onErrorContainerDark = Color(0xFFFFDAD6)
val backgroundDark = DarkNavy // 가이드라인 적용
val onBackgroundDark = Color(0xFFE3E2E6)
val surfaceDark = DarkNavy // 가이드라인 적용
val onSurfaceDark = Color(0xFFE3E2E6)
val surfaceVariantDark = DarkGray // 가이드라인 적용
val onSurfaceVariantDark = Color(0xFFC4C6D0)
val outlineDark = MediumGray // 가이드라인 적용