# ✈️ 픽뽀 - 비행기와 함께하는 픽셀 뽀모도로 앱

## 🎯 한 문장 요약
**“정적 세계지도 기반, 전 세계 사용자 존재감을 점/비행기와 점선으로 시각화하고, 버튼 클릭으로 전체 지도 ↔ 내 위치 보기 시점을 전환하며, 내 위치 보기에서는 항상 내 비행기 위치를 따라가는 항공사 테마 뽀모도로 앱”**

---

## 1️⃣ 핵심 컨셉
- **목표:** 전 세계 사용자와 함께 집중하고 있다는 느낌 제공
- **테마:** 항공사
- **경험:**
    - **집중 세션 = 비행:** 출발 → 비행 → 착륙/휴식
    - **시각화:** 지도 위 비행기/점으로 사용자 존재감 표시

---

## 2️⃣ 지도 및 시각화
- **배경 지도:** 정적 세계지도 이미지 (Equirectangular projection, PNG/SVG)
- **좌표계:** 월드 좌표계 기반, 이미지 픽셀 비율로 점/비행기 위치 설정
- **표시 요소:**
    - **점:** 다른 사용자의 위치
    - **점선:** 비행 경로 또는 집중 세션 연결
    - **비행기 아이콘:** 진행 중인 내 위치 표시

---

## 3️⃣ 시점 전환 (버튼 기반)
- **두 가지 시점:**
    - **전체 지도 보기:** 세계지도 전체 표시, 모든 사용자 점/비행기 표시
    - **내 위치 보기:** 항상 내 비행기 위치를 화면 중앙으로 따라감 (줌 레벨 고정)
- **전환 방식:** 화면 버튼 클릭 (부드러운 애니메이션 적용: `animateFloatAsState` / `animateOffsetAsState`)
- **🚫 미사용:** 드래그/핀치 줌

---

## 4️⃣ 사용자 연결 방식 (간접적)
- **직접적 소셜 연결 없음 (채팅, 친구 추가 X)**
- **간접적 연결:**
    - 지도 상의 다른 사용자 점/비행기로 존재감 전달
    - 점선, 비행기 이동 등으로 “같이 집중하고 있다”는 느낌 유지

---

## 5️⃣ 애니메이션 및 UX
- **비행 경로 애니메이션:** 출발점 → 도착점 사이 점선 따라 비행기 아이콘 이동 (여러 사용자 동시 표시)
- **내 위치 보기:** 내 비행기 위치를 중심으로 지도 및 다른 요소들 함께 이동 (줌 고정)
- **전체 지도 보기:** 세계 지도 전체 비율 유지, 확대/축소 없음

---

## 6️⃣ 기술적 접근
- **UI:** Jetpack Compose Canvas 기반
- **렌더링:** 지도 이미지 위에 점, 점선, 비행기 아이콘 렌더링
- **변환:** `graphicsLayer` 또는 `drawWithContent` 사용
- **좌표 변환:** 월드 좌표 → 화면 좌표 (Offset/Scale 변경으로 시점 전환 구현)
- **육지 좌표 처리:** 미리 정의된 도시 리스트에서 랜덤 선택 (바다 위 위치 방지)

---

## 7️⃣ UX 핵심 포인트
- **시각화:** 전 세계 사용자의 동시 집중 상태 시각화
- **은유:** 집중 시간 = 비행 거리, 진행 상태 = 비행기 위치
- **단순 조작:** 버튼 기반 시점 전환으로 명확한 사용자 의도 반영
- **몰입감:** '내 위치 보기' 시 자동 카메라 추적으로 몰입감 강화
- **부담 없는 연결:** 경쟁 압박 없이 간접적으로 존재감 전달

---
---

## 🛠️ 개발 로드맵 및 체크리스트

### ✈️ Phase 1: 솔로 비행 (핵심 기능 - 혼자 뽀모도로)
> **목표:** 사용자 혼자 뽀모도로를 실행할 때, 지도 위에서 자신의 비행기가 출발지부터 도착지까지 비행하는 전체 과정을 구현합니다.

#### 1. 🎨 리소스 및 좌표계 준비
- [ ] **정적 세계지도 이미지:** `res/drawable`에 `world_map.png` (Equirectangular) 추가
- [ ] **비행기 아이콘:** `res/drawable`에 `ic_airplane.xml` (또는 .png) 추가
- [ ] **도시 좌표 리스트:** `com/malrang/pomodoro/dataclass/`에 `CityCoordinates.kt` 파일 생성
    - `data class City(val name: String, val latitude: Double, val longitude: Double)` 정의
    - `object CityList { val cities = listOf(...) }`에 주요 도시(육지) 좌표 하드코딩
- [ ] **좌표 변환 유틸리티:** `com/malrang/pomodoro/ui/utils/CoordinateUtils.kt` 생성
    - `fun latLngToOffset(latitude, longitude, mapWidth, mapHeight): Offset` 함수 구현 (위도/경도 → Canvas의 X, Y 픽셀 좌표)
    - `fun lerp(start: Offset, stop: Offset, fraction: Float): Offset` 선형 보간 함수 구현 (비행기 위치 계산용)

#### 2. ✈️ 비행 상태 관리 (ViewModel 및 UiState)
- [ ] **`TimerUiState.kt` 수정:**
    - `val departureCity: City? = null` (출발 공항)
    - `val arrivalCity: City? = null` (도착 공항)
    - `val flightProgress: Float = 0.0f` (비행 진행률, 0.0 ~ 1.0)
- [ ] **`TimerViewModel` (또는 `TimerService.kt`) 로직 수정:**
    - **(집중) 세션 시작 시:**
        - `CityList`에서 `departureCity`와 `arrivalCity` 랜덤 선택
        - `_uiState.update`로 `departureCity`, `arrivalCity` 설정 및 `flightProgress`를 `0.0f`로 리셋
    - **타이머 진행 중 (OnTick):**
        - `flightProgress` 계산: `1.0f - (남은 시간 / 총 집중 시간)`
        - `_uiState.update`로 `flightProgress` 실시간 업데이트
    - **세션 완료/스킵 시:**
        - `_uiState.update`로 `flightProgress`를 `1.0f` (도착)로 강제 설정

#### 3. 🖌️ 메인 화면 UI 수정 (Canvas 렌더링)
- [ ] **`PortraitMainScreen.kt` / `MainScreen.kt` 수정:**
    - 기존의 큰 타이머 텍스트, `CycleIndicator` 컴포저블 제거 또는 주석 처리
- [ ] **`Box` 및 `Canvas` 배치:**
    - `Box(modifier = Modifier.fillMaxSize())`를 최상위에 배치
    - `Canvas(modifier = Modifier.fillMaxSize())`를 `Box`의 맨 아래에 배치
- [ ] **`Canvas` 렌더링 (onDraw):**
    - `drawImage(worldMapImage)`로 배경 지도 그리기 (지도 원본 크기 `mapWidth`, `mapHeight` 기억)
    - `departureCity`, `arrivalCity`의 픽셀 좌표(Offset) 계산 (`latLngToOffset` 사용)
    - `PathEffect.dashPathEffect`를 사용해 출발-도착 간 점선(항로) 그리기
    - `flightProgress`를 이용해 현재 비행기 좌표 계산 (`lerp` 사용)
    - `drawImage(airplaneIcon, topLeft = currentPlaneOffset)`로 비행기 그리기

#### 4.  overlay UI 및 컨트롤러 재배치
- [ ] **`Box` 내 UI 재배치:**
    - `Canvas` **위**에 타이머 정보와 컨트롤 버튼이 오도록 배치
- [ ] **타이머 정보 오버레이:**
    - `timerUiState.timeLeft`을 표시하는 `Text`를 화면 상단/하단에 작은 크기로 다시 추가
    - (디자인) 출발지(ICN) -> 도착지(JFK) 형태의 텍스트 오버레이 추가
- [ ] **컨트롤 버튼 (`start`, `pause`, `skip` 등):**
    - 화면 하단에 `Row` 또는 `Column`으로 재배치

#### 5. 🎥 시점 전환 및 카메라 구현 (핵심)
- [ ] **시점 상태 관리:**
    - `MainViewModel` (또는 `TimerViewModel`)에 `val mapViewMode: StateFlow<MapViewMode>` 추가
    - `enum class MapViewMode { FULL, MY_LOCATION }` 정의
- [ ] **시점 전환 버튼 UI:**
    - `Box` 내부에 `IconToggleButton` (지도/비행기 아이콘) 추가, `onClick` 시 `viewModel.toggleMapViewMode()` 호출
- [ ] **`Canvas`에 `graphicsLayer` 적용:**
    - `Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { ... })`
- [ ] **애니메이션 상태 정의 (컴포저블 내):**
    - `val scale = animateFloatAsState(targetValue = if (mapViewMode == MapViewMode.FULL) 1.0f else 3.0f)` (값은 예시)
    - `val animatedOffset = animateOffsetAsState(targetValue = ...)`
- [ ] **`targetOffset` 계산 로직 (중요):**
    - `[FULL 모드 시]`: `Offset.Zero` (또는 지도를 중앙에 맞추는 고정값)
    - `[MY_LOCATION 모드 시]`: **(비행기가 화면 중앙에 오도록)**
        - `targetOffset = (screenCenter - currentPlaneOffset) * scale.value`
        - `currentPlaneOffset`은 `onDraw`에서 계산된 픽셀 좌표여야 함.
- [ ] **`graphicsLayer` 로직 구현:**
    - `scaleX = scale.value`, `scaleY = scale.value`
    - `translationX = animatedOffset.value.x`
    - `translationY = animatedOffset.value.y`
- [ ] **자동 추적 확인:**
    - `MY_LOCATION` 모드에서 비행기가 움직일 때(`flightProgress` 변경), 화면(Canvas)이 부드럽게 따라 움직이는지 확인

#### 6. 🔒 입력 제한 및 폴리싱
- [ ] **드래그/핀치 줌 비활성화:** `Canvas`의 `pointerInput`에서 `detectTransformGestures` (핀치 줌), `detectDragGestures` (드래그)를 **호출하지 않음**
- [ ] **화면 방향 대응:** `LandscapeMainScreen.kt`에도 동일한 `Canvas` 및 `graphicsLayer` 로직 적용

---

### 🌐 Phase 2: 함께 비행 (Supabase 실시간 공유)
> **목표:** (로그인 된) 사용자들이 자신의 비행 상태를 Supabase를 통해 실시간 공유하고, 다른 사용자들의 위치를 지도 위에 '점'으로 표시합니다.

#### 1. ☁️ Supabase 설정 및 연동
- [ ] **Supabase 프로젝트 생성:**
    - `pomodoro_flights` (예시) 테이블 생성
    - RLS(Row Level Security) 설정 (인증된 사용자만 읽기/쓰기 가능하도록)
- [ ] **`android-app`에 Supabase 클라이언트 의존성 추가** (build.gradle.kts)
- [ ] **`SupabaseProvider.kt` 확인:** 기존 Supabase 클라이언트가 `AuthViewModel` 외에서도 사용 가능한지 확인 (필요시 Hilt/DI 설정)
- [ ] **테이블 정의:** `pomodoro_flights`
    - `user_id` (uuid, primary key, foreign key to auth.users)
    - `nickname` (text)
    - `departure_city` (text)
    - `arrival_city` (text)
    - `flight_progress` (float4, 0.0 ~ 1.0)
    - `updated_at` (timestampz)

#### 2. 📡 데이터 모델 및 ViewModel
- [ ] **데이터 모델:** `networkRepo/Models.kt`에 `FlightStatus.kt` (테이블과 매칭) data class 정의 (`@Serializable` 어노테이션 포함)
- [ ] **`SharedFlightsViewModel` (가칭) 신규 생성:**
    - `SupabaseRepository` 의존성 주입
    - `val otherUsersFlights: StateFlow<List<FlightStatus>>` 상태 정의
- [ ] **`SupabaseRepository.kt` 수정:**
    - `fun upsertMyFlightStatus(status: FlightStatus)` 함수 추가 (내 상태 `upsert`)
    - `fun getFlightStatusChanges(): Flow<List<FlightStatus>>` 함수 추가 (실시간 구독)
        - `supabase.realtime.channel(...).postgresChangeFlow(...)` 사용

#### 3. 📤 내 상태 송신 (Upsert)
- [ ] **`TimerViewModel` 수정:**
    - `SupabaseRepository` 의존성 주입
    - **(집중) 세션 시작 시:** `repository.upsertMyFlightStatus(...)` 호출 (출발지, 도착지, progress 0.0)
    - **타이머 진행 중 (OnTick):** (성능 고려) 5~10초에 한 번씩 `repository.upsertMyFlightStatus(...)` 호출 (현재 `flightProgress` 업데이트)
    - **세션 완료/중지 시:** `repository.upsertMyFlightStatus(...)` 호출 (progress 1.0 또는 상태 삭제)

#### 4. 📥 다른 사용자 상태 수신 (Subscribe)
- [ ] **`SharedFlightsViewModel` 구현:**
    - `init { ... }` 블록에서 `repository.getFlightStatusChanges()`를 구독
    - (중요) `myUserId`와 비교하여 **'나'를 제외한** 다른 사용자 목록만 `_otherUsersFlights`에 업데이트

#### 5. 🌍 다른 사용자 렌더링 (Canvas 수정)
- [ ] **`MainScreen.kt` 수정:**
    - `SharedFlightsViewModel`에서 `otherUsersFlights` 상태를 수집(collect)
- [ ] **`Canvas` 렌더링 (onDraw) 수정:**
    - `otherUsersFlights` 리스트를 `onDraw` 스코프로 전달
    - `otherUsersFlights.forEach { flightStatus -> ... }` 루프 실행
    - 각 `flightStatus`의 `departureCity`, `arrivalCity` 이름으로 `CityList`에서 좌표 조회
    - `latLngToOffset`으로 픽셀 좌표 계산
    - (선택) 다른 사용자 항로(점선)를 연한 색으로 그리기
    - `flightStatus.flightProgress`로 `lerp`를 사용해 현재 위치 계산
    - `drawCircle(...)`을 사용해 해당 위치에 **'점'** 그리기
- [ ] **시점별 렌더링:**
    - `[FULL 모드 시]`: 모든 점(dot)을 그리기
    - `[MY_LOCATION 모드 시]`: (성능 최적화) 내 위치 주변의 점(dot)만 그리거나, `graphicsLayer`의 `scale`로 인해 자연스럽게 안 보이도록 처리
