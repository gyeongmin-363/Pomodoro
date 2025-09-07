package com.malrang.pomodoro.ui.screen.studyroom

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.malrang.pomodoro.dataclass.animalInfo.Animal
import com.malrang.pomodoro.networkRepo.StudyRoom
import com.malrang.pomodoro.networkRepo.StudyRoomMember
import com.malrang.pomodoro.networkRepo.User
import com.malrang.pomodoro.viewmodel.StudyRoomViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinStudyRoomDialog(
    room: StudyRoom,
    currentUser: User,
    collectedAnimals: Set<Animal>,
    viewModel: StudyRoomViewModel,
    onDismiss: () -> Unit
) {
    var nickname by remember { mutableStateOf("") }
    var selectedAnimal by remember { mutableStateOf<Animal?>(null) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${room.name}\n프로필 설정") },
        text = {
            Column {
                OutlinedTextField(
                    value = nickname,
                    onValueChange = {
                        if (it.length <= 10) { // 10자 이하로 제한
                            nickname = it
                        }
                    },
                    label = { Text("닉네임 (${nickname.length}/10))") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true // 한 줄로 제한

                )
                Spacer(modifier = Modifier.height(16.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor(),
                        readOnly = true,
                        value = selectedAnimal?.name ?: "동물 선택 (선택사항)",
                        onValueChange = {},
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        collectedAnimals.forEach { animal ->
                            DropdownMenuItem(
                                text = { Text(animal.name) },
                                onClick = {
                                    selectedAnimal = animal
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val member = StudyRoomMember(
                        id = UUID.randomUUID().toString(),
                        study_room_id = room.id,
                        user_id = currentUser.id,
                        nickname = nickname,
                        animal = selectedAnimal?.id
                    )
                    viewModel.joinStudyRoom(member)
                },
                enabled = nickname.isNotBlank()
            ) {
                Text("참여하기")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("다음에 하기")
            }
        }
    )
}