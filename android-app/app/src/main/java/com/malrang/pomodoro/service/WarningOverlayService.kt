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
            val blockModeString = intent?.getStringExtra("BLOCK_MODE")
            val blockMode = runCatching { BlockMode.valueOf(blockModeString ?: "") }.getOrElse { BlockMode.PARTIAL }

            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater

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
                    stopSelf()
                    return START_NOT_STICKY
                }
            }

            // ✅ [수정 핵심] 부분 차단이어도 높이를 MATCH_PARENT로 설정하여 터치를 막고 배경을 그림
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                // FLAG_LAYOUT_NO_LIMITS를 추가하여 상태바까지 덮을 수 있게 함
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            windowManager.addView(overlayView, params)
        }
        return START_NOT_STICKY
    }

    private fun setupPartialBlockButtons() {
        val backButton = overlayView?.findViewById<Button>(R.id.btn_back_to_app)
        backButton?.setOnClickListener {
            bringAppToFront()
            stopSelf()
        }

        val continueButton = overlayView?.findViewById<Button>(R.id.btn_continue_using)
        continueButton?.setOnClickListener {
            // 임시 허용 브로드캐스트 발송
            sendBroadcast(Intent("com.malrang.pomodoro.ACTION_TEMP_PASS").apply {
                setPackage("com.malrang.pomodoro")
            })
            stopSelf()
        }
    }

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