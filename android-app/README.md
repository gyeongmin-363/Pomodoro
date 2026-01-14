# 📱 FocusConnect UI/UX & User Flow 상세 설계서 (v1.1)

본 문서는 **FocusConnect** 안드로이드 앱의 각 화면 구성 요소와 사용자의 상호작용 흐름을 상세히 정의합니다.

## 🎨 공통 디자인 원칙 (Notion Style)

- **Minimalism:** 불필요한 장식 제거, 오프화이트(`#FBFBFA`) 배경과 다크 차콜(`#37352F`) 텍스트의 조화.
- **Emoji-First:** 아이콘 대신 고해상도 이모지를 사용하여 직관적이고 감성적인 인터페이스 구축.
- **Consistency:** 모든 인터랙션은 부드러운 트랜지션(Fade-in, Slide)을 동반함.

## 1. Focus Screen: 타이머 및 워크 프리셋

사용자가 집중을 시작하고 관리하는 핵심 화면입니다.

### 🖼️ UI 구성 요소

1. **Work Preset Pager (상단):**
    - 가로 스와이프가 가능한 카드형 리스트.
    - 각 카드: `[이모지] [프리셋 명칭] [집중/휴식 시간]`.
    - 마지막 카드: `+` 버튼 (새 프리셋 추가).
2. **공부냥(Focus Cat) 영역 (중앙 상단):**
    - 현재 상태에 따른 Lottie 애니메이션 (대기/공부/휴식).
3. **Main Timer (중앙):**
    - `JetBrains Mono` 폰트의 대형 숫자 타이머.
    - 타이머 외곽을 감싸는 1px 두께의 가느다란 원형 진행 바.
4. **Control Panel (중앙 하단):**
    - 재생(Start) / 일시정지(Pause) / 건너뛰기(Skip) 버튼.
5. **Soundscape Dock (최하단):**
    - 백색소음 선택 버튼들 (빗소리, 카페 등) 및 볼륨 슬라이더.

### 🔄 User Flow

1. **프리셋 선택:** 상단 페이저에서 "영어 뽀모" 카드를 선택 -> 하단 타이머가 50:00으로 즉시 변경.
2. **집중 시작:** [Start] 클릭 -> 고양이가 공부 시작 -> Foreground Service 활성화 및 알림창 노출.
3. **사운드 조절:** 🌧️ 아이콘 터치 -> 백색소음 무한 루프 재생.

## 2. Social Screen: 다중 공부방 대시보드

참여 중인 여러 공부방의 상황을 실시간으로 확인하는 화면입니다.

### 🖼️ UI 구성 요소

1. **Room Selector (최상단):**
    - 인스타그램 스토리 스타일의 원형 아이콘 가로 리스트.
    - 접속 중인 방은 컬러풀한 테두리 효과.
2. **Room Info Card:**
    - 선택된 방 이름, 총 참여 인원, 누적 집중 시간 표시.
3. **Real-time Member Grid (중앙):**
    - 사용자 카드: `[상태 이모지] [닉네임] [사용 중인 Work] [실시간 타이머]`.
4. **Quick Reaction (하단 플로팅):**
    - 응원하기 이모지 퀵 메뉴 (🔥, 🙌, 👏).

## 3. Planner Screen: 독립형 체크리스트

타이머와 무관하게 할 일을 기록하고 관리하는 공간입니다.

### 🖼️ UI 구성 요소

1. **Date Navigation:** 이번 주 날짜가 표시되는 상단 캘린더 바.
2. **To-Do List:**
    - 노션 스타일의 체크박스 + 텍스트 라인.
3. **Inline Add:** 최하단 `+ 새로 만들기` 입력 줄.

## 4. Stats Screen: 데이터 분석 (신규)

나의 공부 습관을 시각적으로 분석하는 통계 특화 화면입니다.

### 🖼️ UI 구성 요소

1. **Heatmap (상단):**
    - 깃허브 잔디 스타일의 일별 집중도 기여도 차트.
2. **Subject Focus Pie Chart (중앙):**
    - 내가 설정한 Work 프리셋별 집중 시간 비율 시각화.
3. **Weekly Trend Line Chart (하단):**
    - 이번 주 요일별 총 집중 시간 변화 추이.
4. **Achievement Badge:** * '첫 100시간 달성', '7일 연속 집중' 등 획득한 이모지 배지 리스트.

### 🔄 User Flow

1. **기간 변경:** 주간/월간/전체 버튼을 통해 차트의 데이터 범위 변경.
2. **상세 확인:** 파이 차트의 특정 영역(예: 수학) 터치 -> 해당 과목의 누적 집중 횟수와 평균 시간 팝업 노출.

## 5. Settings Screen: 환경 설정 및 서버 (신규)

개인 설정과 서버 상태를 관리하는 화면입니다.

### 🖼️ UI 구성 요소

1. **Profile Section:** * 프로필 이모지 및 닉네임 수정 기능.
2. **Server & Sync (Koyeb/Uptime):** * **Koyeb Status:** 현재 백엔드 서버 가동 상태(실시간 API 응답 체크).
    - **UptimeRobot Info:** 마지막 핑(Ping) 시간 및 가동률 표시.
3. **Notification Settings:** * 뽀모도로 종료 알림음, 진동, 방해금지 모드 연동 설정.
4. **Account & Data:** * Supabase 계정 연동 상태 확인 및 데이터 수동 동기화 버튼.

### 🔄 User Flow

1. **서버 점검:** Koyeb 신호등이 빨간색일 경우 '서버 깨우기' 버튼 터치 -> 즉시 HTTP 요청 발송.
2. **환경 설정:** 알림음을 '빗소리'에서 '종소리'로 변경하여 집중 종료 시 감각적 피드백 조정.

## 6. 바텀 네비게이션 플로우 (Bottom Nav Flow)

- **Planner (1)**: 공부 전후 할 일 정리 및 체크.
- **Focus (2)**: 실제 집중 실행 (중심점).
- **Social (3)**: 실시간 자극 및 공유.
- **Stats (4)**: 성과 분석 및 동기 부여.
- **Settings (5)**: 앱 환경 최적화 및 서버 연결 확인.

**특이사항:** 타이머가 가동 중일 때 (1), (3), (4), (5) 화면으로 이동해도 상단 알림 영역에 미니 타이머가 지속적으로 표시되어 끊김 없는 UX를 제공함.

<img width="2816" height="1536" alt="Gemini_Generated_Image_he5bcmhe5bcmhe5b" src="https://github.com/user-attachments/assets/0b1b4b30-7870-4e2c-a53f-1de9b48114df" /><img width="2816" height="1536" alt="Gemini_Generated_Image_ivfbxfivfbxfivfb" src="https://github.com/user-attachments/assets/f56fb45a-9659-4a5d-a955-5e4a7b539608" />


