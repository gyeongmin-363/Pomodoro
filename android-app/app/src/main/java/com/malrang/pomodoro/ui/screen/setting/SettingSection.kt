package com.malrang.pomodoro.ui.screen.setting

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val shadowOffset = 4.dp

    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        // 섹션 타이틀 (하이라이트 박스 느낌)
        Box(
            modifier = Modifier
                .padding(start = 4.dp, bottom = 8.dp)
                .background(MaterialTheme.colorScheme.secondary, shape = RoundedCornerShape(4.dp))
                .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black, // 아주 굵게
                color = MaterialTheme.colorScheme.onSecondary
            )
        }

        // 컨텐츠 영역 (Hard Shadow Box)
        Box(modifier = Modifier.fillMaxWidth()) {
            // 그림자 (뒤에 깔리는 검은 박스)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    // 높이는 내부 컨텐츠에 따라 결정되도록 matchParentSize 사용 불가할 수 있으므로
                    // 여기서는 content가 그려진 후 크기를 따라가도록 matchParentSize를 써야 하지만,
                    // ColumnScope 문제로 인해 Surface 뒤에 그림자를 배치하는 방식 사용
                    .matchParentSize()
                    .offset(x = shadowOffset, y = shadowOffset)
                    .background(MaterialTheme.colorScheme.outline, shape)
            )

            // 실제 컨텐츠 (앞에 있는 흰 박스)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = shape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column {
                    content()
                }
            }
        }
    }
}