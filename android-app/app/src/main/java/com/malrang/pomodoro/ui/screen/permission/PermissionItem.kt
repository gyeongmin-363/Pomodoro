package com.malrang.pomodoro.ui.screen.permission

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malrang.pomodoro.dataclass.ui.PermissionInfo

@Composable
fun PermissionItem(permission: PermissionInfo, hasBeenAttempted: Boolean) {
    Card(modifier = Modifier.fillMaxWidth().border(2.dp, Color.White)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = permission.title, fontWeight = FontWeight.Bold)
                Text(text = permission.description)
            }
            Spacer(modifier = Modifier.width(16.dp))
            if (hasBeenAttempted) {
                Text(
                    text = if (permission.isGranted) "O" else "X",
                    color = if (permission.isGranted) Color(0xFF4CAF50) else Color(0xFFF44336),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}