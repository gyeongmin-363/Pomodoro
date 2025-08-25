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
import com.malrang.pomodoro.dataclass.ui.BlockMode

class WarningOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: android.view.View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (overlayView == null) {
            // ✅ 인텐트로부터 차단 모드를 받아옵니다.
            val blockModeString = intent?.getStringExtra("BLOCK_MODE")
            val blockMode = runCatching { BlockMode.valueOf(blockModeString ?: "") }.getOrElse { BlockMode.PARTIAL }

            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater

            // ✅ 차단 모드에 따라 다른 레이아웃과 로직을 적용합니다.
            when (blockMode) {
                BlockMode.PARTIAL -> {
                    overlayView = inflater.inflate(R.layout.overlay_warning, null)
                    setupPartialBlockButtons()
                }
                BlockMode.FULL -> {
                    overlayView = inflater.inflate(R.layout.overlay_full_block, null)
                    setupFullBlockButton()
                }
                BlockMode.NONE -> {
                    stopSelf() // NONE 모드일 경우 즉시 서비스를 종료합니다.
                    return START_NOT_STICKY
                }
            }

            val params = WindowManager.LayoutParams(
                // ✅ 완전 차단 시에는 화면 전체를 덮도록 수정합니다.
                if (blockMode == BlockMode.FULL) WindowManager.LayoutParams.MATCH_PARENT else WindowManager.LayoutParams.MATCH_PARENT,
                if (blockMode == BlockMode.FULL) WindowManager.LayoutParams.MATCH_PARENT else WindowManager.LayoutParams.WRAP_CONTENT,
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

            windowManager.addView(overlayView, params)
        }
        return START_NOT_STICKY
    }

    // ✅ 부분 차단 버튼 설정
    private fun setupPartialBlockButtons() {
        val backButton = overlayView?.findViewById<Button>(R.id.btn_back_to_app)
        backButton?.setOnClickListener {
            bringAppToFront()
            stopSelf()
        }

        val continueButton = overlayView?.findViewById<Button>(R.id.btn_continue_using)
        continueButton?.setOnClickListener {
            sendBroadcast(Intent("com.malrang.pomodoro.ACTION_TEMP_PASS"))
            stopSelf()
        }
    }

    // ✅ 완전 차단 버튼 설정
    private fun setupFullBlockButton() {
        val backButton = overlayView?.findViewById<Button>(R.id.btn_back_to_app_full)
        backButton?.setOnClickListener {
            bringAppToFront()
            stopSelf()
        }
    }

    private fun bringAppToFront() {
        val mainActivityIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(mainActivityIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
    }
}