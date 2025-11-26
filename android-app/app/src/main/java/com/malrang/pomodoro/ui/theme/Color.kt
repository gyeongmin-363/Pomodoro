package com.malrang.pomodoro.ui.theme

import androidx.compose.ui.graphics.Color

// --- Neo-Brutalism (뉴트로 팝) 스타일 팔레트 ---

// 1. 기본 흑백 & 외곽선
val NeoBlack = Color(0xFF000000)      // 메인 텍스트, 외곽선, 그림자 (완전 검정)
val NeoWhite = Color(0xFFFFFFFF)      // 카드 배경, 텍스트 (순백색)

// 2. 배경색 (레트로한 크림/노랑 느낌)
val NeoBackground = Color(0xFFFFFAE0) // 연한 레몬 크림색 (메인 배경)

// 3. 포인트 컬러 (고채도, 형광 느낌)
val NeoBlue = Color(0xFF3B82F6)       // 파랑 (Primary: DOING, 주요 버튼)
val NeoPink = Color(0xFFFF90E8)       // 핑크 (Secondary: TO DO, 포인트)
val NeoGreen = Color(0xFF23E478)      // 초록 (Tertiary: DONE, 성공)
val NeoYellow = Color(0xFFFFD700)     // 노랑 (Highlight: 경고, 강조)
val NeoRed = Color(0xFFFF4848)        // 빨강 (Error: 삭제, 에러)

// 4. 서브 컬러
val NeoGray = Color(0xFFF0F0F0)       // 비활성 배경, 연한 회색


// --- Light Theme Mapping (라이트 모드) ---
// 특징: 크림색 배경, 검은색 외곽선, 흰색 컨테이너

val primaryLight = NeoBlue
val onPrimaryLight = NeoWhite
val primaryContainerLight = NeoWhite
val onPrimaryContainerLight = NeoBlack

val secondaryLight = NeoPink
val onSecondaryLight = NeoBlack
val secondaryContainerLight = NeoWhite
val onSecondaryContainerLight = NeoBlack

val tertiaryLight = NeoGreen
val onTertiaryLight = NeoBlack
val tertiaryContainerLight = NeoWhite
val onTertiaryContainerLight = NeoBlack

val errorLight = NeoRed
val onErrorLight = NeoWhite
val errorContainerLight = NeoRed
val onErrorContainerLight = NeoWhite

val backgroundLight = NeoBackground
val onBackgroundLight = NeoBlack

val surfaceLight = NeoWhite
val onSurfaceLight = NeoBlack
val surfaceVariantLight = NeoWhite // 카드나 시트의 배경
val onSurfaceVariantLight = NeoBlack

val outlineLight = NeoBlack // 모든 테두리는 검정색


// --- Dark Theme Mapping (다크 모드) ---
// 특징: 아주 어두운 회색/검정 배경, 흰색 외곽선, 팝 컬러 포인트 유지

val primaryDark = NeoBlue
val onPrimaryDark = NeoWhite
val primaryContainerDark = NeoBlack // 컨테이너를 어둡게 눌러줌
val onPrimaryContainerDark = NeoBlue // 텍스트로 색상 강조

val secondaryDark = NeoPink
val onSecondaryDark = NeoBlack // 핑크 배경 위 텍스트는 검정이 가독성 좋음
val secondaryContainerDark = Color(0xFF333333) // 다크 그레이 컨테이너
val onSecondaryContainerDark = NeoPink

val tertiaryDark = NeoGreen
val onTertiaryDark = NeoBlack
val tertiaryContainerDark = Color(0xFF333333)
val onTertiaryContainerDark = NeoGreen

val errorDark = NeoRed
val onErrorDark = NeoBlack
val errorContainerDark = Color(0xFF521414) // 아주 어두운 붉은색
val onErrorContainerDark = NeoRed

val backgroundDark = Color(0xFF121212) // 눈이 편안한 아주 어두운 회색 (완전 검정보다 선호됨)
val onBackgroundDark = NeoWhite

val surfaceDark = Color(0xFF1E1E1E) // 배경보다 살짝 밝은 표면
val onSurfaceDark = NeoWhite
val surfaceVariantDark = Color(0xFF2C2C2C)
val onSurfaceVariantDark = NeoGray

val outlineDark = NeoWhite // 다크모드에서는 외곽선이 흰색이어야 잘 보임