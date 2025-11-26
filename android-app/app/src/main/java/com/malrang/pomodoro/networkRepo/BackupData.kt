package com.malrang.pomodoro.networkRepo

import androidx.annotation.Keep
import com.malrang.pomodoro.dataclass.ui.DailyStat
import com.malrang.pomodoro.dataclass.ui.Settings
import com.malrang.pomodoro.dataclass.ui.WorkPreset
import kotlinx.serialization.Serializable

/**
 * [BackupData]
 * - 앱의 모든 데이터를 하나의 JSON 파일로 묶기 위한 최상위 래퍼(Wrapper) 클래스입니다.
 * - Room DB 테이블이 아니며, 오직 파일 생성 및 파싱 용도로 사용됩니다.
 */
@Keep
@Serializable
data class BackupData(
    // 나중에 데이터 구조가 바뀔 것을 대비한 버전 관리용 필드 (현재는 1)
    val version: Int = 1,

    // 백업이 생성된 시각 (Unix Timestamp)
    val backupTimestamp: Long = System.currentTimeMillis(),

    // --- 실제 백업할 데이터들 ---
    val workPresets: List<WorkPreset>,
    val dailyStats: List<DailyStat>
)