package com.sekerpatlatma.game

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.sekerpatlatma.game.ui.GameActivity
import com.sekerpatlatma.game.ui.LevelSelectActivity
import com.sekerpatlatma.game.ui.SettingsActivity

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: GamePreferences
    private lateinit var tvTotalStars: TextView
    private lateinit var tvCurrentLevel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()
        setContentView(R.layout.activity_main)

        prefs = GamePreferences(this)
        
        setupViews()
        setupClickListeners()
        animateElements()
    }

    private fun setupViews() {
        tvTotalStars = findViewById(R.id.tvTotalStars)
        tvCurrentLevel = findViewById(R.id.tvCurrentLevel)
        
        updateStats()
    }

    private fun updateStats() {
        tvTotalStars.text = prefs.totalStars.toString()
        tvCurrentLevel.text = prefs.maxUnlockedLevel.toString()
    }

    private fun setupClickListeners() {
        findViewById<Button>(R.id.btnPlay).setOnClickListener {
            startGame(prefs.maxUnlockedLevel)
        }

        findViewById<Button>(R.id.btnLevels).setOnClickListener {
            startActivity(Intent(this, LevelSelectActivity::class.java))
        }

        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun startGame(level: Int) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("level", level)
        startActivity(intent)
    }

    private fun animateElements() {
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        fadeIn.duration = 500

        findViewById<View>(R.id.logoContainer).startAnimation(fadeIn)
        
        val slideUp = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
        slideUp.duration = 600
        slideUp.startOffset = 200
        
        findViewById<View>(R.id.buttonsContainer).startAnimation(slideUp)
    }

    override fun onResume() {
        super.onResume()
        updateStats()
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
