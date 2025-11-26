package com.malrang.pomodoro.ui.screen.background

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
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
    var showCustomColorDialog by remember { mutableStateOf(false) }
    var showImageTextColorDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        backgroundViewModel.loadAvailableImages(context)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { backgroundViewModel.addBackgroundImage(context, it) }
    }

    // --- 1. 커스텀 배경 설정 다이얼로그 ---
    if (showCustomColorDialog) {
        AlertDialog(
            onDismissRequest = { showCustomColorDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text("커스텀 배경 설정", fontWeight = FontWeight.Bold)
            },
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
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 2.dp)
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
                NeoButton(text = "확인", onClick = { showCustomColorDialog = false })
            }
        )
    }

    // --- 2. 이미지 설정 다이얼로그 ---
    if (showImageTextColorDialog) {
        AlertDialog(
            onDismissRequest = { showImageTextColorDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("이미지 설정", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                ) {
                    Text(
                        "이미지 위 텍스트 색상",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SimpleColorPicker(
                        label = "텍스트 색상",
                        colorInt = uiState.customTextColor,
                        onColorChanged = { newColor ->
                            backgroundViewModel.updateCustomColors(uiState.customBgColor, newColor)
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 2.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // 삭제 버튼 (네오 스타일 Error 버튼)
                    Button(
                        onClick = {
                            uiState.selectedImagePath?.let { path ->
                                backgroundViewModel.deleteBackgroundImage(context, path)
                            }
                            showImageTextColorDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("이 이미지 삭제", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                NeoButton(text = "닫기", onClick = { showImageTextColorDialog = false })
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // NeoBackground
            .padding(16.dp)
    ) {
        // 타이틀 영역
        Box(
            modifier = Modifier
                .padding(bottom = 8.dp)
                .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(4.dp))
                .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "배경 및 텍스트 설정",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSecondary
            )
        }

        Text(
            text = "길게 눌러서 색상 설정 및 삭제",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 24.dp, start = 4.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp), // 간격 넓힘
            verticalArrangement = Arrangement.spacedBy(16.dp),
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

            // 하단 여백 추가
            item { Spacer(modifier = Modifier.height(50.dp)) }
            item { Spacer(modifier = Modifier.height(50.dp)) }
        }
    }
}

// --- 공통: 네오 브루탈리즘 컨테이너 ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NeoGridContainer(
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    val shadowOffset = if (isSelected) 6.dp else 4.dp
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val borderWidth = if (isSelected) 3.dp else 2.dp

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        // Shadow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = shadowOffset, y = shadowOffset)
                .background(MaterialTheme.colorScheme.outline, shape)
        )

        // Main Card
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface, shape)
                .border(borderWidth, borderColor, shape)
                .clip(shape)
        ) {
            content()
        }

        // Selection Badge
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 8.dp, y = (-8).dp) // 카드를 벗어나게 배치
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun SolidColorItem(
    color: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    NeoGridContainer(
        isSelected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 색상 미리보기 원
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(color), CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "색상 설정",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun UploadImageItem(
    onClick: () -> Unit
) {
    NeoGridContainer(
        isSelected = false,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, // 파란색 강조
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "이미지 추가",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ImageItem(
    path: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val painter = rememberAsyncImagePainter(model = File(path))

    NeoGridContainer(
        isSelected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick
    ) {
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 선택 시 이미지가 약간 어두워지게 처리 (선택됨을 명확히 하기 위해)
        if (isSelected) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))
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
        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            // 현재 색상 미리보기 박스
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(Color(red, green, blue), RoundedCornerShape(8.dp))
                    .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(16.dp))

            // 슬라이더 영역
            Column(Modifier.weight(1f)) {
                ColorSlider(label = "R", value = red, onValueChange = {
                    red = it
                    onColorChanged(Color(red, green, blue).toArgb())
                }, color = Color.Red)

                ColorSlider(label = "G", value = green, onValueChange = {
                    green = it
                    onColorChanged(Color(red, green, blue).toArgb())
                }, color = Color.Green)

                ColorSlider(label = "B", value = blue, onValueChange = {
                    blue = it
                    onColorChanged(Color(red, green, blue).toArgb())
                }, color = Color.Blue)
            }
        }
    }
}

@Composable
fun ColorSlider(label: String, value: Float, onValueChange: (Float) -> Unit, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            modifier = Modifier.width(20.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f).height(20.dp),
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = color.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
fun NeoButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 0.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}