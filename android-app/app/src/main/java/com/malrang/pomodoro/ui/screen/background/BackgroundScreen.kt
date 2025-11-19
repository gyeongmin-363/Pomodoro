package com.malrang.pomodoro.ui.screen.background

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import com.malrang.pomodoro.viewmodel.BackgroundType
import com.malrang.pomodoro.viewmodel.SettingsViewModel
import java.io.File

@Composable
fun BackgroundScreen(
    settingsViewModel: SettingsViewModel
) {
    val uiState by settingsViewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        settingsViewModel.loadAvailableImages(context)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { settingsViewModel.addBackgroundImage(context, it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("배경 및 텍스트 설정", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        TabRow(
            selectedTabIndex = if (uiState.backgroundType == BackgroundType.COLOR) 0 else 1,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = uiState.backgroundType == BackgroundType.COLOR,
                onClick = { settingsViewModel.setBackgroundType(BackgroundType.COLOR) },
                text = { Text("색상") }
            )
            Tab(
                selected = uiState.backgroundType == BackgroundType.IMAGE,
                onClick = { settingsViewModel.setBackgroundType(BackgroundType.IMAGE) },
                text = { Text("이미지") }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.backgroundType == BackgroundType.COLOR) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                SimpleColorPicker(
                    label = "배경 색상",
                    colorInt = uiState.customBgColor,
                    onColorChanged = { newColor ->
                        settingsViewModel.updateCustomColors(newColor, uiState.customTextColor)
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))

                SimpleColorPicker(
                    label = "텍스트 색상",
                    colorInt = uiState.customTextColor,
                    onColorChanged = { newColor ->
                        settingsViewModel.updateCustomColors(uiState.customBgColor, newColor)
                    }
                )
            }
        } else {
            Column {
                SimpleColorPicker(
                    label = "텍스트 색상 (이미지 위)",
                    colorInt = uiState.customTextColor,
                    onColorChanged = { newColor ->
                        settingsViewModel.updateCustomColors(uiState.customBgColor, newColor)
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("저장된 이미지 (${uiState.availableImages.size}/20)", fontWeight = FontWeight.Bold)
                    TextButton(onClick = { launcher.launch("image/*") }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("이미지 추가")
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(uiState.availableImages) { path ->
                        ImageItem(
                            path = path,
                            isSelected = path == uiState.selectedImagePath,
                            onClick = { settingsViewModel.selectBackgroundImage(path) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ImageItem(
    path: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // [수정] 비동기 이미지 로더 사용 (Coil)
    val painter = rememberAsyncImagePainter(model = File(path))

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
    ) {
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color.White)
            }
        }
    }
}

@Composable
fun SimpleColorPicker(label: String, colorInt: Int, onColorChanged: (Int) -> Unit) {
    val color = Color(colorInt)
    var red by remember(colorInt) { mutableFloatStateOf(color.red) }
    var green by remember(colorInt) { mutableFloatStateOf(color.green) }
    var blue by remember(colorInt) { mutableFloatStateOf(color.blue) }

    Column {
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(red, green, blue))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("R", modifier = Modifier.width(20.dp))
                    Slider(
                        value = red,
                        onValueChange = { red = it },
                        onValueChangeFinished = { onColorChanged(Color(red, green, blue).toArgb()) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("G", modifier = Modifier.width(20.dp))
                    Slider(
                        value = green,
                        onValueChange = { green = it },
                        onValueChangeFinished = { onColorChanged(Color(red, green, blue).toArgb()) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("B", modifier = Modifier.width(20.dp))
                    Slider(
                        value = blue,
                        onValueChange = { blue = it },
                        onValueChangeFinished = { onColorChanged(Color(red, green, blue).toArgb()) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}