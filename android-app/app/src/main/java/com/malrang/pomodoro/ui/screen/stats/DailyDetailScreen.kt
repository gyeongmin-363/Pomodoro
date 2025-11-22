package com.malrang.pomodoro.ui.screen.stats

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malrang.pomodoro.dataclass.ui.DailyStat
import com.malrang.pomodoro.viewmodel.StatsViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyDetailScreen(
    dateString: String?,
    statsViewModel: StatsViewModel,
    onNavigateBack: () -> Unit
) {
    val date = try {
        LocalDate.parse(dateString)
    } catch (e: Exception) {
        LocalDate.now()
    }

    val uiState by statsViewModel.uiState.collectAsState()
    val dailyStat = uiState.dailyStats[date.toString()] ?: DailyStat(date.toString())
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("${date.monthValue}월 ${date.dayOfMonth}일 기록", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // 상단 탭 (BottomBar -> TabRow 변경)
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("시간 기록") },
                    icon = { Icon(Icons.Default.List, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("체크리스트") },
                    icon = { Icon(Icons.Default.Check, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("회고") },
                    icon = { Icon(Icons.Default.Edit, contentDescription = null) }
                )
            }

            // 탭 콘텐츠
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> TimeRecordTab(dailyStat)
                    1 -> ChecklistTab(dailyStat, statsViewModel)
                    2 -> RetrospectTab(dailyStat) { newRetrospect ->
                        statsViewModel.saveRetrospect(dailyStat.date, newRetrospect)
                        scope.launch { snackbarHostState.showSnackbar("회고가 저장되었습니다.") }
                    }
                }
            }
        }
    }
}

@Composable
fun TimeRecordTab(dailyStat: DailyStat) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val allWorks = (dailyStat.studyTimeByWork?.keys ?: emptySet()) + (dailyStat.breakTimeByWork?.keys ?: emptySet())

        if (allWorks.isEmpty()) {
            EmptyStateMessage("기록된 활동이 없습니다.")
        } else {
            allWorks.forEach { work ->
                val study = dailyStat.studyTimeByWork?.get(work) ?: 0
                val breaks = dailyStat.breakTimeByWork?.get(work) ?: 0

                if (study > 0 || breaks > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = work,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("📚 공부 시간", style = MaterialTheme.typography.bodyMedium)
                                Text("${study}분", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("☕ 휴식 시간", style = MaterialTheme.typography.bodyMedium)
                                Text("${breaks}분", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChecklistTab(dailyStat: DailyStat, viewModel: StatsViewModel) {
    var newTaskText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 입력 필드 개선
        OutlinedTextField(
            value = newTaskText,
            onValueChange = { newTaskText = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("할 일을 입력하세요") },
            trailingIcon = {
                IconButton(
                    onClick = {
                        if (newTaskText.isNotBlank()) {
                            viewModel.addChecklistItem(dailyStat.date, newTaskText)
                            newTaskText = ""
                            focusManager.clearFocus()
                        }
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "추가", tint = MaterialTheme.colorScheme.primary)
                }
            },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (newTaskText.isNotBlank()) {
                    viewModel.addChecklistItem(dailyStat.date, newTaskText)
                    newTaskText = ""
                    focusManager.clearFocus()
                }
            })
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (dailyStat.checklist.isEmpty()) {
                EmptyStateMessage("등록된 체크리스트가 없습니다.")
            } else {
                dailyStat.checklist.forEach { (task, isDone) ->
                    ChecklistItemRow(
                        task = task,
                        isDone = isDone,
                        onToggle = { viewModel.toggleChecklistItem(dailyStat.date, task) },
                        onDelete = { viewModel.deleteChecklistItem(dailyStat.date, task) },
                        onModify = { old, new -> viewModel.modifyChecklistItem(dailyStat.date, old, new) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChecklistItemRow(
    task: String,
    isDone: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onModify: (String, String) -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            when (it) {
                SwipeToDismissBoxValue.StartToEnd, SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                else -> false
            }
        }
    )

    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editContextText by remember { mutableStateOf(task) }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("할 일 수정") },
            text = {
                OutlinedTextField(
                    value = editContextText,
                    onValueChange = { editContextText = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = { onModify(task, editContextText); showEditDialog = false }) {
                    Text("수정")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("취소") }
            }
        )
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color = MaterialTheme.colorScheme.errorContainer
            val alignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                Icon(Icons.Default.Delete, contentDescription = "삭제", tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        },
        content = {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .combinedClickable(
                            onClick = onToggle,
                            onLongClick = { showMenu = true }
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isDone) Icons.Default.Check else Icons.Default.List,
                        contentDescription = null,
                        tint = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = task,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isDone) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isDone) FontWeight.Normal else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("수정") },
                            onClick = { showMenu = false; editContextText = task; showEditDialog = true },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("삭제") },
                            onClick = { showMenu = false; onDelete() },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                        )
                    }
                }
            }
        }
    )
}

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
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = RoundedCornerShape(16.dp)
        ) {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxSize(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                placeholder = {
                    Text(
                        "오늘 하루는 어땠나요?\n아쉬웠던 점이나 잘한 점을 기록해보세요.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                focusManager.clearFocus()
                onSave(text)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("회고 저장하기")
        }
    }
}

@Composable
fun EmptyStateMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}