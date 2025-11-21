package com.malrang.pomodoro.ui.screen.stats

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
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

    var selectedTab by remember { mutableStateOf(0) } // 0: 상세기록, 1: 체크리스트, 2: 회고

    // [추가] 스낵바 상태 및 코루틴 스코프 (피드백 표시용)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color(0xFF1E1E1E), // 어두운 배경
        // [추가] 스낵바 호스트 설정
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
                1 -> ChecklistTab(dailyStat)
                2 -> RetrospectTab(dailyStat) { newRetrospect ->
                    // [변경] 저장 로직 및 피드백
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
fun ChecklistTab(dailyStat: DailyStat) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("✅ 체크리스트", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))

        if (dailyStat.checklist.isEmpty()) {
            Text("등록된 체크리스트가 없습니다.", color = Color.Gray)
        } else {
            dailyStat.checklist.forEach { (task, isDone) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
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
                        modifier = Modifier.padding(start = 8.dp),
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun RetrospectTab(dailyStat: DailyStat, onSave: (String) -> Unit) {
    var text by remember(dailyStat.retrospect) { mutableStateOf(dailyStat.retrospect ?: "") }

    // [추가] 키보드 제어를 위한 FocusManager
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
                // [추가] 저장 시 키보드 숨기기 및 콜백 호출
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