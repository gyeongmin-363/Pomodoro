package com.malrang.pomodoro.ui.screen.background

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import com.malrang.pomodoro.viewmodel.BackgroundType
import com.malrang.pomodoro.viewmodel.BackgroundViewModel
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BackgroundScreen(
    backgroundViewModel: BackgroundViewModel
) {
    val uiState by backgroundViewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 다이얼로그 상태 관리
    var showCustomColorDialog by remember { mutableStateOf(false) } // 커스텀 배경용 (배경+텍스트)
    var showImageTextColorDialog by remember { mutableStateOf(false) } // 이미지용 (텍스트+삭제)

    LaunchedEffect(Unit) {
        backgroundViewModel.loadAvailableImages(context)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { backgroundViewModel.addBackgroundImage(context, it) }
    }

    // 1. 커스텀 배경 설정 다이얼로그 (배경색 + 텍스트색)
    if (showCustomColorDialog) {
        AlertDialog(
            onDismissRequest = { showCustomColorDialog = false },
            title = { Text("커스텀 배경 설정") },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                ) {
                    SimpleColorPicker(
                        label = "배경 색상",
                        colorInt = uiState.customBgColor,
                        onColorChanged = { newColor ->
                            backgroundViewModel.updateCustomColors(newColor, uiState.customTextColor)
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    SimpleColorPicker(
                        label = "텍스트 색상",
                        colorInt = uiState.customTextColor,
                        onColorChanged = { newColor ->
                            backgroundViewModel.updateCustomColors(uiState.customBgColor, newColor)
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showCustomColorDialog = false }) {
                    Text("확인")
                }
            }
        )
    }

    // 2. [변경] 이미지 설정 다이얼로그 (텍스트색 + 삭제)
    if (showImageTextColorDialog) {
        AlertDialog(
            onDismissRequest = { showImageTextColorDialog = false },
            title = { Text("이미지 설정") },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                ) {
                    Text("이미지 위 텍스트 색상", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    SimpleColorPicker(
                        label = "텍스트 색상",
                        colorInt = uiState.customTextColor,
                        onColorChanged = { newColor ->
                            backgroundViewModel.updateCustomColors(uiState.customBgColor, newColor)
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // [추가] 이미지 삭제 버튼
                    OutlinedButton(
                        onClick = {
                            uiState.selectedImagePath?.let { path ->
                                backgroundViewModel.deleteBackgroundImage(context, path)
                            }
                            showImageTextColorDialog = false
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("이 이미지 삭제")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showImageTextColorDialog = false }) {
                    Text("닫기")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "배경 및 텍스트 설정",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "길게 눌러서 색상 설정 및 삭제",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            // 1. 커스텀 색상 배경 아이템
            item {
                SolidColorItem(
                    color = uiState.customBgColor,
                    isSelected = uiState.backgroundType == BackgroundType.COLOR,
                    onClick = {
                        backgroundViewModel.setBackgroundType(BackgroundType.COLOR)
                    },
                    onLongClick = {
                        backgroundViewModel.setBackgroundType(BackgroundType.COLOR)
                        showCustomColorDialog = true
                    }
                )
            }

            // 2. 이미지 업로드 아이템
            item {
                UploadImageItem(
                    onClick = { launcher.launch("image/*") }
                )
            }

            // 3. 저장된 이미지 아이템들
            items(uiState.availableImages) { path ->
                ImageItem(
                    path = path,
                    isSelected = (uiState.backgroundType == BackgroundType.IMAGE && path == uiState.selectedImagePath),
                    onClick = {
                        backgroundViewModel.selectBackgroundImage(path)
                    },
                    onLongClick = {
                        backgroundViewModel.selectBackgroundImage(path)
                        showImageTextColorDialog = true
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SolidColorItem(
    color: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(color))
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = if (Color(color).luminance() > 0.5f) Color.Black else Color.White,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "커스텀 색상 배경",
                color = if (Color(color).luminance() > 0.5f) Color.Black else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

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
fun UploadImageItem(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp,
                color = Color.Gray.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "이미지 업로드",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageItem(
    path: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
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
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
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
                    Text(
                        text = "R ${(red * 255).roundToInt()}",
                        modifier = Modifier.width(50.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Slider(
                        value = red,
                        onValueChange = { red = it },
                        onValueChangeFinished = { onColorChanged(Color(red, green, blue).toArgb()) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "G ${(green * 255).roundToInt()}",
                        modifier = Modifier.width(50.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Slider(
                        value = green,
                        onValueChange = { green = it },
                        onValueChangeFinished = { onColorChanged(Color(red, green, blue).toArgb()) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "B ${(blue * 255).roundToInt()}",
                        modifier = Modifier.width(50.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
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