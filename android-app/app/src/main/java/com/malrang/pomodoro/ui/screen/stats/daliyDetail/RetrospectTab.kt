package com.malrang.pomodoro.ui.screen.stats.daliyDetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malrang.pomodoro.dataclass.ui.DailyStat

@Composable
fun RetrospectTab(dailyStat: DailyStat, onSave: (String) -> Unit) {
    var text by remember(dailyStat.retrospect) { mutableStateOf(dailyStat.retrospect ?: "") }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Note Pad Style TextField
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Shadow
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = 6.dp, y = 6.dp)
                    .background(MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            )

            // Input Area
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .padding(4.dp)
            ) {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxSize(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    placeholder = {
                        Text(
                            "오늘 하루는 어땠나요?\n아쉬웠던 점이나 잘한 점을 기록해보세요.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Save Button (Neo Style)
        Button(
            onClick = {
                focusManager.clearFocus()
                onSave(text)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 6.dp,
                pressedElevation = 0.dp
            ),
            border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
        ) {
            Text(
                "회고 저장하기",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}