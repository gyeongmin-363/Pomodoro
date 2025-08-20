package com.malrang.pomodoro

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.malrang.pomodoro.localRepo.PomodoroRepository
import com.malrang.pomodoro.localRepo.SoundPlayer
import com.malrang.pomodoro.localRepo.VibratorHelper
import com.malrang.pomodoro.service.TimerServiceProvider
import com.malrang.pomodoro.ui.PomodoroApp
import com.malrang.pomodoro.ui.theme.PomodoroTheme
import com.malrang.pomodoro.viewmodel.PomodoroViewModel
import com.malrang.withpet.BackPressExit

/**
 * 앱의 메인 액티비티입니다.
 * 앱의 진입점 역할을 하며, [PomodoroApp] 컴포저블을 표시합니다.
 */
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // 권한 요청 결과 처리 (필요 시)
    }

    /**
     * 액티비티가 생성될 때 호출됩니다.
     * @param savedInstanceState 이전에 저장된 액티비티 상태입니다.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        askNotificationPermission()
        setContent {
            PomodoroTheme {
                val vm: PomodoroViewModel = viewModel(factory = PomodoroVMFactory(application))
                PomodoroApp(vm)
                BackPressExit() //뒤로가기 연속 두번 -> 앱 종료
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

class PomodoroVMFactory(private val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PomodoroViewModel::class.java)) {
            val localDatastoreRepo = PomodoroRepository(app)
            val soundPlayer = SoundPlayer(app)
            val vibratorHelper = VibratorHelper(app)
            val timerServiceProvider = TimerServiceProvider(app) // TimerServiceProvider 인스턴스 생성

            @Suppress("UNCHECKED_CAST")
            // ViewModel 생성자에 모든 의존성을 전달합니다.
            return PomodoroViewModel(
                repo = localDatastoreRepo,
                soundPlayer = soundPlayer,
                vibratorHelper = vibratorHelper,
                timerService = timerServiceProvider // 생성한 인스턴스 전달
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}