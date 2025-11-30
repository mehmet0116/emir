package com.sekerpatlatma.game.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.sekerpatlatma.game.GamePreferences
import com.sekerpatlatma.game.R
import com.sekerpatlatma.game.audio.SoundManager
import com.sekerpatlatma.game.engine.GameEngine
import com.sekerpatlatma.game.model.Level
import com.sekerpatlatma.game.view.GameBoardView

class GameActivity : AppCompatActivity() {

    private lateinit var prefs: GamePreferences
    private lateinit var soundManager: SoundManager
    private lateinit var gameEngine: GameEngine
    private lateinit var gameBoardView: GameBoardView
    
    private lateinit var tvLevel: TextView
    private lateinit var tvMoves: TextView
    private lateinit var tvScore: TextView
    private lateinit var tvTarget: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var star1: TextView
    private lateinit var star2: TextView
    private lateinit var star3: TextView
    
    private var currentLevel: Level? = null
    private val handler = Handler(Looper.getMainLooper())
    private val levels = Level.generateLevels()
    
    // Track which stars have been earned to avoid duplicate sounds
    private var starsEarned = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()
        setContentView(R.layout.activity_game)

        prefs = GamePreferences(this)
        soundManager = SoundManager(this)
        soundManager.setSoundEnabled(prefs.soundEnabled)

        setupViews()
        setupGameEngine()
        
        val levelNumber = intent.getIntExtra("level", 1)
        startLevel(levelNumber)
    }

    private fun setupViews() {
        gameBoardView = findViewById(R.id.gameBoardView)
        tvLevel = findViewById(R.id.tvLevel)
        tvMoves = findViewById(R.id.tvMoves)
        tvScore = findViewById(R.id.tvScore)
        tvTarget = findViewById(R.id.tvTarget)
        progressBar = findViewById(R.id.progressBar)
        star1 = findViewById(R.id.star1)
        star2 = findViewById(R.id.star2)
        star3 = findViewById(R.id.star3)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            showExitConfirmation()
        }

        findViewById<Button>(R.id.btnHint).setOnClickListener {
            showHint()
        }

        findViewById<Button>(R.id.btnShuffle).setOnClickListener {
            shuffleBoard()
        }

        gameBoardView.setOnCandyClickListener { row, col ->
            onCandyClicked(row, col)
        }
        
        gameBoardView.setOnCandySwipeListener { row1, col1, row2, col2 ->
            onCandySwiped(row1, col1, row2, col2)
        }
    }
    
    private fun onCandySwiped(row1: Int, col1: Int, row2: Int, col2: Int) {
        val success = gameEngine.swapCandies(row1, col1, row2, col2)
        if (!success) {
            if (prefs.soundEnabled) {
                soundManager.playSound(SoundManager.SoundType.INVALID)
            }
        }
    }

    private fun setupGameEngine() {
        gameEngine = GameEngine(
            onScoreChanged = { score -> 
                runOnUiThread { 
                    tvScore.text = score.toString()
                    updateProgress()
                    updateStars()
                }
            },
            onMovesChanged = { moves -> 
                runOnUiThread { 
                    tvMoves.text = moves.toString()
                }
            },
            onBoardChanged = { 
                runOnUiThread { 
                    gameBoardView.setBoard(gameEngine.currentBoard)
                }
            },
            onMatchFound = { positions -> 
                runOnUiThread {
                    if (prefs.soundEnabled) {
                        soundManager.playSound(SoundManager.SoundType.MATCH)
                    }
                    gameBoardView.animateMatchExplosion(positions) {
                        handler.postDelayed({
                            val hasMoreMatches = gameEngine.removeMatchesAndRefill()
                            if (!hasMoreMatches) {
                                gameBoardView.animateCandyDrop {}
                            }
                        }, 100)
                    }
                }
            },
            onCombo = { count -> 
                runOnUiThread {
                    showComboIndicator(count)
                }
            },
            onGameWon = { stars -> 
                runOnUiThread {
                    showWinDialog(stars)
                }
            },
            onGameLost = { 
                runOnUiThread {
                    showLoseDialog()
                }
            },
            onNoValidMoves = {
                runOnUiThread {
                    showShuffleNotice()
                }
            }
        )
    }

    private fun startLevel(levelNumber: Int) {
        currentLevel = levels.getOrNull(levelNumber - 1) ?: return
        currentLevel?.let { level ->
            gameEngine.startLevel(level)
            
            tvLevel.text = "Seviye ${level.number}"
            tvTarget.text = level.targetScore.toString()
            tvMoves.text = level.moves.toString()
            tvScore.text = "0"
            
            updateProgress()
            resetStars()
            
            gameBoardView.animateCandyDrop {}
        }
    }

    private fun onCandyClicked(row: Int, col: Int) {
        if (prefs.soundEnabled) {
            soundManager.playSound(SoundManager.SoundType.SELECT)
        }
        gameEngine.selectCandy(row, col)
    }

    private fun showHint() {
        if (prefs.soundEnabled) {
            soundManager.playSound(SoundManager.SoundType.HINT)
        }
        gameEngine.showHint()
        handler.postDelayed({
            gameEngine.clearHints()
        }, 2000)
    }

    private fun shuffleBoard() {
        if (prefs.soundEnabled) {
            soundManager.playSound(SoundManager.SoundType.SHUFFLE)
        }
        gameEngine.shuffleBoard()
    }

    private fun updateProgress() {
        currentLevel?.let { level ->
            val progress = (gameEngine.state.score.toFloat() / level.targetScore * 100).toInt()
            progressBar.progress = minOf(progress, 100)
        }
    }

    private fun updateStars() {
        currentLevel?.let { level ->
            val score = gameEngine.state.score
            
            if (score >= level.starThresholds[0] && !starsEarned.contains(1)) {
                star1.alpha = 1f
                if (prefs.soundEnabled) {
                    soundManager.playSound(SoundManager.SoundType.STAR)
                    animateStar(star1)
                }
                starsEarned.add(1)
            }
            if (score >= level.starThresholds[1] && !starsEarned.contains(2)) {
                star2.alpha = 1f
                if (prefs.soundEnabled) {
                    soundManager.playSound(SoundManager.SoundType.STAR)
                    animateStar(star2)
                }
                starsEarned.add(2)
            }
            if (score >= level.starThresholds[2] && !starsEarned.contains(3)) {
                star3.alpha = 1f
                if (prefs.soundEnabled) {
                    soundManager.playSound(SoundManager.SoundType.STAR)
                    animateStar(star3)
                }
                starsEarned.add(3)
            }
        }
    }

    private fun resetStars() {
        star1.alpha = 0.3f
        star2.alpha = 0.3f
        star3.alpha = 0.3f
        starsEarned.clear()
    }

    private fun animateStar(view: View) {
        val scaleUp = AnimationUtils.loadAnimation(this, R.anim.star_pop)
        view.startAnimation(scaleUp)
    }

    private fun showComboIndicator(count: Int) {
        val comboTexts = arrayOf("", "", "İYİ!", "SÜPER!", "HARİKA!", "MÜTHİŞ!", "İNANILMAZ!")
        val text = comboTexts.getOrElse(minOf(count, comboTexts.size - 1)) { "EFSANE!" }
        
        val comboView = findViewById<TextView>(R.id.tvCombo)
        comboView.text = "${count}x $text"
        comboView.visibility = View.VISIBLE
        
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        comboView.startAnimation(fadeIn)
        
        handler.postDelayed({
            comboView.visibility = View.GONE
        }, 1000)
    }

    private fun showShuffleNotice() {
        val dialog = AlertDialog.Builder(this, R.style.ModernDialogTheme)
            .setTitle("🔄 Karıştırılıyor")
            .setMessage("Geçerli hamle kalmadı!")
            .setCancelable(false)
            .create()
        
        dialog.show()
        handler.postDelayed({
            dialog.dismiss()
        }, 1500)
    }

    private fun showWinDialog(stars: Int) {
        if (prefs.soundEnabled) {
            soundManager.playSound(SoundManager.SoundType.WIN)
        }
        
        currentLevel?.let { level ->
            prefs.setLevelStars(level.number, stars)
            prefs.unlockNextLevel(level.number)
        }
        
        val bonus = gameEngine.calculateBonus()
        val finalScore = gameEngine.state.score + bonus
        
        val dialog = AlertDialog.Builder(this, R.style.ModernDialogTheme)
            .setTitle("🎉 TEBRİKLER! 🎉")
            .setMessage("Puan: $finalScore\n⭐".repeat(stars))
            .setPositiveButton("Sonraki Seviye") { _, _ ->
                currentLevel?.let { 
                    if (it.number < 50) {
                        startLevel(it.number + 1)
                    } else {
                        showAllCompleteDialog()
                    }
                }
            }
            .setNegativeButton("Tekrar Oyna") { _, _ ->
                currentLevel?.let { startLevel(it.number) }
            }
            .setNeutralButton("Ana Menü") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .create()
        
        dialog.show()
    }

    private fun showLoseDialog() {
        if (prefs.soundEnabled) {
            soundManager.playSound(SoundManager.SoundType.LOSE)
        }
        
        val dialog = AlertDialog.Builder(this, R.style.ModernDialogTheme)
            .setTitle("😢 Hamle Bitti!")
            .setMessage("Puan: ${gameEngine.state.score}\nÜzülme, tekrar deneyebilirsin!")
            .setPositiveButton("Tekrar Dene") { _, _ ->
                currentLevel?.let { startLevel(it.number) }
            }
            .setNegativeButton("Ana Menü") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .create()
        
        dialog.show()
    }

    private fun showAllCompleteDialog() {
        val dialog = AlertDialog.Builder(this, R.style.ModernDialogTheme)
            .setTitle("🏆 MUHTEŞEM! 🏆")
            .setMessage("Tüm seviyeleri tamamladın!\nSen bir şampiyonsun!")
            .setPositiveButton("Ana Menü") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .create()
        
        dialog.show()
    }

    private fun showExitConfirmation() {
        val dialog = AlertDialog.Builder(this, R.style.ModernDialogTheme)
            .setTitle("⚠️ Çıkmak İstiyor musun?")
            .setMessage("İlerlemeniz kaydedilmeyecek.")
            .setPositiveButton("Evet, Çık") { _, _ ->
                finish()
            }
            .setNegativeButton("Hayır, Devam Et", null)
            .create()
        
        dialog.show()
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onDestroy() {
        soundManager.release()
        super.onDestroy()
    }
}
