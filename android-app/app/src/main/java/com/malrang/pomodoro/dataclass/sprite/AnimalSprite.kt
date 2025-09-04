package com.malrang.pomodoro.dataclass.sprite

/**
 * 화면에 표시되는 동물 스프라이트 하나를 나타내는 데이터 클래스입니다.
 * 이 클래스의 인스턴스는 앱이 재시작되면 사라지는 일시적인 상태를 가집니다.
 *
 * @property id 고유 식별자(UUID 등)로, 중복 생성을 방지합니다.
 * @property animalId 동물의 종류를 식별하는 ID입니다.
 * @property idleSheetRes 'IDLE' 상태일 때 사용할 스프라이트 시트 리소스 ID입니다.
 * @property idleCols 'IDLE' 상태 스프라이트 시트의 열 수입니다.
 * @property idleRows 'IDLE' 상태 스프라이트 시트의 행 수입니다.
 * @property jumpSheetRes 'JUMP' 상태일 때 사용할 스프라이트 시트 리소스 ID입니다.
 * @property jumpCols 'JUMP' 상태 스프라이트 시트의 열 수입니다.
 * @property jumpRows 'JUMP' 상태 스프라이트 시트의 행 수입니다.
 * @property spriteState 현재 스프라이트의 상태(IDLE 또는 JUMP)입니다.
 * @property frameDurationMs 각 프레임의 지속 시간(밀리초)입니다.
 * @property x 스프라이트의 현재 x 좌표입니다.
 * @property y 스프라이트의 현재 y 좌표입니다.
 * @property vx 스프라이트의 x축 방향 속도입니다.
 * @property vy 스프라이트의 y축 방향 속도입니다.
 * @property sizeDp 스프라이트의 크기(Dp)입니다.
 */
data class AnimalSprite(
    val id: String,
    val animalId: String,
    val idleSheetRes: Int,
    val idleCols: Int,
    val idleRows: Int,
    val jumpSheetRes: Int,
    val jumpCols: Int,
    val jumpRows: Int,
    val spriteState: SpriteState = SpriteState.IDLE,
    val frameDurationMs: Long = 120L,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val sizeDp: Float
)