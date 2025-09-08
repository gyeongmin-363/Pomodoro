package com.malrang.pomodoro.ui.screen.studyroom.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.malrang.pomodoro.dataclass.animalInfo.Animal
import com.malrang.pomodoro.networkRepo.StudyRoomMember
import com.malrang.pomodoro.ui.PixelArtConfirmDialog
import com.malrang.pomodoro.viewmodel.StudyRoomViewModel
import kotlin.collections.find

/**
 * 챌린지룸 내 내 정보(닉네임, 동물) 수정을 위한 다이얼로그. PixelArtConfirmDialog를 사용합니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMyInfoDialog(
    member: StudyRoomMember,
    collectedAnimals: Set<Animal>,
    viewModel: StudyRoomViewModel,
    onDismiss: () -> Unit
) {
    var nickname by remember { mutableStateOf(member.nickname) }
    // Find the initial Animal object from the member's animal ID
    val initialAnimal = remember(member.animal, collectedAnimals) {
        collectedAnimals.find { it.id == member.animal }
    }
    var selectedAnimal by remember { mutableStateOf(initialAnimal) }
    var expanded by remember { mutableStateOf(false) }

    PixelArtConfirmDialog(
        onDismissRequest = onDismiss,
        title = "내 정보 수정",
        confirmText = "수정",
        onConfirm = {
            member.study_room_id?.let {
                viewModel.updateMyInfoInRoom(
                    memberId = member.id,
                    studyRoomId = it,
                    newNickname = nickname,
                    newAnimalId = selectedAnimal?.id
                )
            }
            onDismiss()
        },
        confirmButtonEnabled = nickname.isNotBlank()
    ) {
        Column {
            OutlinedTextField(
                value = nickname,
                onValueChange = {
                    if (it.length <= 10) { // 10자 이하로 제한
                        nickname = it
                    }
                },
                label = { Text("닉네임 (${nickname.length}/10)") },
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
                    value = selectedAnimal?.displayName ?: "동물 선택 (선택사항)",
                    onValueChange = {},
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("선택 안함") },
                        onClick = {
                            selectedAnimal = null
                            expanded = false
                        }
                    )
                    collectedAnimals.forEach { animal ->
                        DropdownMenuItem(
                            text = { Text(animal.displayName) },
                            onClick = {
                                selectedAnimal = animal
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
