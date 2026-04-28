package com.sojakothy.music.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sojakothy.music.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Immediately go to MainActivity; splash theme handles the visual
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
