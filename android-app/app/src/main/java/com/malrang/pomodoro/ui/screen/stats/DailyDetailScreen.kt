package com.malrang.pomodoro.ui.screen.stats

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malrang.pomodoro.dataclass.ui.DailyStat
import com.malrang.pomodoro.viewmodel.StatsViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyDetailScreen(
    dateString: String?, // "yyyy-MM-dd"
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

    var selectedTab by remember { mutableIntStateOf(0) } // 0: 상세기록, 1: 체크리스트, 2: 회고

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color(0xFF1E1E1E),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("${date.monthValue}월 ${date.dayOfMonth}일 상세 기록", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF2C2C2C)) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.List, contentDescription = "기록") },
                    label = { Text("시간 기록") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Check, contentDescription = "체크리스트") },
                    label = { Text("체크리스트") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Edit, contentDescription = "회고") },
                    label = { Text("회고") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> TimeRecordTab(dailyStat)
                1 -> ChecklistTab(dailyStat, statsViewModel)
                2 -> RetrospectTab(dailyStat) { newRetrospect ->
                    statsViewModel.saveRetrospect(dailyStat.date, newRetrospect)
                    scope.launch {
                        snackbarHostState.showSnackbar("회고가 저장되었습니다.")
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
            .verticalScroll(rememberScrollState())
    ) {
        Text("📊 Work별 상세 기록", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))

        val allWorks = (dailyStat.studyTimeByWork?.keys ?: emptySet()) + (dailyStat.breakTimeByWork?.keys ?: emptySet())

        if (allWorks.isEmpty()) {
            Text("기록된 활동이 없습니다.", color = Color.Gray)
        } else {
            allWorks.forEach { work ->
                val study = dailyStat.studyTimeByWork?.get(work) ?: 0
                val breaks = dailyStat.breakTimeByWork?.get(work) ?: 0
                if (study > 0 || breaks > 0) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text("📌 $work", color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("   📚 공부: ${study}분", color = Color.White)
                        Text("   ☕ 휴식: ${breaks}분", color = Color.White)
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
        Text("✅ 체크리스트", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))

        // 입력 필드
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newTaskText,
                onValueChange = { newTaskText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("할 일을 입력하세요", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFFFBBF24),
                    focusedBorderColor = Color(0xFFFBBF24),
                    unfocusedBorderColor = Color.Gray
                ),
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
            IconButton(
                onClick = {
                    if (newTaskText.isNotBlank()) {
                        viewModel.addChecklistItem(dailyStat.date, newTaskText)
                        newTaskText = ""
                        focusManager.clearFocus()
                    }
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "추가", tint = Color(0xFFFBBF24))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 체크리스트 목록
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            if (dailyStat.checklist.isEmpty()) {
                Text("등록된 체크리스트가 없습니다.", color = Color.Gray, modifier = Modifier.padding(top = 16.dp))
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
    // [변경] SwipeToDismissBoxState 사용
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
                TextButton(
                    onClick = {
                        onModify(task, editContextText)
                        showEditDialog = false
                    }
                ) { Text("수정") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("취소") }
            }
        )
    }

    // [변경] SwipeToDismissBox 사용
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color = Color.Red
            val alignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd)
                Alignment.CenterStart else Alignment.CenterEnd

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Color.White)
            }
        },
        content = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E))
                    .combinedClickable(
                        onClick = onToggle,
                        onLongClick = { showMenu = true }
                    )
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isDone) Icons.Default.Check else Icons.Default.List,
                    contentDescription = null,
                    tint = if (isDone) Color.Green else Color.Gray
                )
                Text(
                    text = task,
                    color = if (isDone) Color.White else Color.Gray,
                    modifier = Modifier.padding(start = 12.dp),
                    fontSize = 16.sp,
                    fontWeight = if (isDone) FontWeight.Normal else FontWeight.Bold
                )

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("수정") },
                        onClick = {
                            showMenu = false
                            editContextText = task
                            showEditDialog = true
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("삭제") },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                    )
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
        Text("📝 오늘의 회고", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFFFBBF24),
                focusedBorderColor = Color(0xFFFBBF24),
                unfocusedBorderColor = Color.Gray,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            placeholder = { Text("오늘 하루는 어땠나요?", color = Color.Gray) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                focusManager.clearFocus()
                onSave(text)
            },
            modifier = Modifier.align(Alignment.End),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBBF24))
        ) {
            Text("저장", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}