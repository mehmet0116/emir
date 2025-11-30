package com.sekerpatlatma.game.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sekerpatlatma.game.GamePreferences
import com.sekerpatlatma.game.R

class LevelSelectActivity : AppCompatActivity() {

    private lateinit var prefs: GamePreferences
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvTotalStars: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()
        setContentView(R.layout.activity_level_select)

        prefs = GamePreferences(this)

        setupViews()
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.recyclerViewLevels)
        tvTotalStars = findViewById(R.id.tvTotalStars)

        tvTotalStars.text = prefs.totalStars.toString()

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        recyclerView.layoutManager = GridLayoutManager(this, 5)
        recyclerView.adapter = LevelAdapter()
    }

    override fun onResume() {
        super.onResume()
        tvTotalStars.text = prefs.totalStars.toString()
        recyclerView.adapter?.notifyDataSetChanged()
    }

    private inner class LevelAdapter : RecyclerView.Adapter<LevelViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LevelViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_level, parent, false)
            return LevelViewHolder(view)
        }

        override fun onBindViewHolder(holder: LevelViewHolder, position: Int) {
            val levelNumber = position + 1
            val isUnlocked = levelNumber <= prefs.maxUnlockedLevel
            val stars = prefs.getLevelStars(levelNumber)

            holder.bind(levelNumber, isUnlocked, stars)
        }

        override fun getItemCount(): Int = 50
    }

    private inner class LevelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLevel: TextView = itemView.findViewById(R.id.tvLevelNumber)
        private val tvStars: TextView = itemView.findViewById(R.id.tvStars)
        private val lockIcon: TextView = itemView.findViewById(R.id.tvLockIcon)

        fun bind(level: Int, isUnlocked: Boolean, stars: Int) {
            if (isUnlocked) {
                tvLevel.text = level.toString()
                tvLevel.visibility = View.VISIBLE
                lockIcon.visibility = View.GONE
                
                val starText = "⭐".repeat(stars) + "☆".repeat(3 - stars)
                tvStars.text = starText
                tvStars.visibility = View.VISIBLE
                
                if (stars > 0) {
                    itemView.setBackgroundResource(R.drawable.level_completed_bg)
                } else {
                    itemView.setBackgroundResource(R.drawable.level_unlocked_bg)
                }
                
                itemView.setOnClickListener {
                    startGame(level)
                }
            } else {
                tvLevel.visibility = View.GONE
                tvStars.visibility = View.GONE
                lockIcon.visibility = View.VISIBLE
                itemView.setBackgroundResource(R.drawable.level_locked_bg)
                itemView.setOnClickListener(null)
            }
        }
    }

    private fun startGame(level: Int) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("level", level)
        startActivity(intent)
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
