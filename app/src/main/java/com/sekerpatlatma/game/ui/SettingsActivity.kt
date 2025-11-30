package com.sekerpatlatma.game.ui

import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.switchmaterial.SwitchMaterial
import com.sekerpatlatma.game.GamePreferences
import com.sekerpatlatma.game.R

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: GamePreferences
    private lateinit var switchSound: SwitchMaterial
    private lateinit var switchMusic: SwitchMaterial
    private lateinit var switchAnimations: SwitchMaterial

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()
        setContentView(R.layout.activity_settings)

        prefs = GamePreferences(this)

        setupViews()
    }

    private fun setupViews() {
        switchSound = findViewById(R.id.switchSound)
        switchMusic = findViewById(R.id.switchMusic)
        switchAnimations = findViewById(R.id.switchAnimations)

        // Load saved settings
        switchSound.isChecked = prefs.soundEnabled
        switchMusic.isChecked = prefs.musicEnabled
        switchAnimations.isChecked = prefs.animationsEnabled

        // Setup listeners
        switchSound.setOnCheckedChangeListener { _, isChecked ->
            prefs.soundEnabled = isChecked
        }

        switchMusic.setOnCheckedChangeListener { _, isChecked ->
            prefs.musicEnabled = isChecked
        }

        switchAnimations.setOnCheckedChangeListener { _, isChecked ->
            prefs.animationsEnabled = isChecked
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.btnDone).setOnClickListener {
            finish()
        }
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
