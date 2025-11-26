package com.malrang.pomodoro.ui.screen.stats.month

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.malrang.pomodoro.R
import com.malrang.pomodoro.dataclass.ui.DailyStat
import java.time.LocalDate

@Composable
fun DailySummaryCard(
    date: LocalDate,
    stats: DailyStat?,
    displayedStudyTime: Int,
    onDetailClick: () -> Unit
) {
    // 1. 데이터 계산
    val checklistTotal = stats?.checklist?.size ?: 0
    val checklistDone = stats?.checklist?.values?.count { it } ?: 0
    val checklistProgress = if (checklistTotal > 0) checklistDone.toFloat() / checklistTotal else 0f
    val totalBreakTime = stats?.totalBreakTimeInMinutes ?: 0
    val retrospect = stats?.retrospect ?: ""

    val topCategories = stats?.studyTimeByWork?.entries
        ?.sortedByDescending { it.value }
        ?.take(3)
        ?: emptyList()

    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onDetailClick)
    ) {
        // Shadow
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 6.dp, y = 6.dp)
                .background(MaterialTheme.colorScheme.outline, shape)
        )

        // Card Body
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, shape)
                .border(2.dp, MaterialTheme.colorScheme.outline, shape)
                .padding(20.dp)
        ) {
            Column {
                // --- 상단: 날짜 및 더보기 ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${date.monthValue}월 ${date.dayOfMonth}일",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${date.dayOfWeek.name}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "상세보기",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.outline,
                    thickness = 2.dp
                )

                // --- 메인 통계: 공부 시간 & 휴식 시간 ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatsInfoBox(
                        modifier = Modifier.weight(1f),
                        icon = R.drawable.menu_book_24px,
                        label = "총 집중",
                        value = formatTimeSimple(displayedStudyTime),
                        accentColor = MaterialTheme.colorScheme.primary // Blue
                    )

                    StatsInfoBox(
                        modifier = Modifier.weight(1f),
                        icon = R.drawable.chair_24px,
                        label = "총 휴식",
                        value = formatTimeSimple(totalBreakTime),
                        accentColor = MaterialTheme.colorScheme.secondary // Pink
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // --- 체크리스트 (네오 스타일 프로그레스 바) ---
                if (checklistTotal > 0) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "할 일 달성률",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${(checklistProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        // Custom Neo Progress Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(checklistProgress)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.tertiary, CircleShape) // Green
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // --- 회고 (말풍선 스타일) ---
                if (retrospect.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp)) // Pink Bg
                            .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "💬 $retrospect",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsInfoBox(
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int,
    label: String,
    value: String,
    accentColor: Color
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = accentColor
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// 칩용 간소화된 시간 포맷 (예: 125분 -> 2h 5m)
private fun formatTimeSimple(minutes: Int): String {
    if (minutes == 0) return "0m"
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}