package com.malrang.pomodoro

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.malrang.pomodoro.localRepo.PomodoroRepository
import com.malrang.pomodoro.localRepo.SoundPlayer
import com.malrang.pomodoro.localRepo.VibratorHelper
import com.malrang.pomodoro.ui.PomodoroApp
import com.malrang.pomodoro.ui.theme.PomodoroTheme
import com.malrang.pomodoro.viewmodel.PomodoroViewModel
import com.malrang.withpet.BackPressExit

/**
 * 앱의 메인 액티비티입니다.
 * 앱의 진입점 역할을 하며, [PomodoroApp] 컴포저블을 표시합니다.
 */
class MainActivity : ComponentActivity() {
    /**
     * 액티비티가 생성될 때 호출됩니다.
     * @param savedInstanceState 이전에 저장된 액티비티 상태입니다.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PomodoroTheme {
                val vm: PomodoroViewModel = viewModel(factory = PomodoroVMFactory(application))
                PomodoroApp(vm)
                BackPressExit() //뒤로가기 연속 두번 -> 앱 종료
            }
        }
    }
}

class PomodoroVMFactory(private val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val localDatastoreRepo = PomodoroRepository(app)
        val soundPlayer = SoundPlayer(app)
        val vibratorHelper = VibratorHelper(app)
        @Suppress("UNCHECKED_CAST")
        return PomodoroViewModel(localDatastoreRepo, soundPlayer, vibratorHelper) as T
    }
}
