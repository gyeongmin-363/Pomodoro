// UserScreen.kt
package com.malrang.pomodoro.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.malrang.pomodoro.viewmodel.PomodoroViewModel

@Composable
fun UserScreen(viewModel: PomodoroViewModel) {
    val users by viewModel.users.collectAsState()

    var text by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("User Name") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            if (text.isNotBlank()) {
//                viewModel.addUser(text)
                text = ""
            }
        }) {
            Text("Add User")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { viewModel.loadUsers() }) {
            Text("Load Users")
        }
        Spacer(modifier = Modifier.height(16.dp))
        users.forEach { user ->
            Text("${user.id} - ${user.name}")
        }
    }
}
