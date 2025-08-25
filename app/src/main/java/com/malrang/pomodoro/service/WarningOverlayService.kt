package com.malrang.pomodoro.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Button
import com.malrang.pomodoro.MainActivity
import com.malrang.pomodoro.R

class WarningOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: android.view.View? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (overlayView == null) {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
            overlayView = inflater.inflate(R.layout.overlay_warning, null)

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            val backButton = overlayView?.findViewById<Button>(R.id.btn_back_to_app)
            backButton?.setOnClickListener {
                val mainActivityIntent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(mainActivityIntent)
                stopSelf()
            }

            // ✅ '앱 계속 사용' 버튼 리스너 추가
            val continueButton = overlayView?.findViewById<Button>(R.id.btn_continue_using)
            continueButton?.setOnClickListener {
                // "임시 허용" 신호를 감시 서비스에 보냄
                sendBroadcast(Intent("com.malrang.pomodoro.ACTION_TEMP_PASS"))
                // 오버레이 제거
                stopSelf()
            }


            windowManager.addView(overlayView, params)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
    }
}