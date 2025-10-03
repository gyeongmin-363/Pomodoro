package com.malrang.pomodoro.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malrang.pomodoro.dataclass.ui.Screen
import com.malrang.pomodoro.localRepo.PomodoroRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * 화면 전환(Navigation) 이벤트를 관리하는 뷰모델입니다.
 * 이제 PomodoroRepository를 주입받지만, 현재는 사용하지 않습니다.
 * 향후 앱의 전반적인 상태에 따라 내비게이션 로직이 필요할 때를 위해 구조를 유지합니다.
 */
class MainViewModel(
    private val repository: PomodoroRepository // <-- PermissionViewModel 대신 Repository를 주입받습니다.
) : ViewModel() {

    private val _navigationEvents = MutableSharedFlow<Screen>()
    val navigationEvents = _navigationEvents.asSharedFlow()

    // 👇 권한 확인 로직을 UI 레이어(PomodoroApp.kt)로 옮기기 위해 이 함수를 삭제합니다.
    // fun checkPermissionsAndNavigateIfNeeded(context: Context) { ... }

    fun navigateTo(screen: Screen) {
        viewModelScope.launch {
            _navigationEvents.emit(screen)
        }
    }
}