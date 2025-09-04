package com.malrang.pomodoro.localRepo

import android.content.Context
import android.media.MediaPlayer
import com.malrang.pomodoro.R

class SoundPlayer(private val context: Context) {
    fun playSound() {
        val mediaPlayer = MediaPlayer.create(context, R.raw.notification_sound)
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener { mp -> mp.release() }
    }
}
