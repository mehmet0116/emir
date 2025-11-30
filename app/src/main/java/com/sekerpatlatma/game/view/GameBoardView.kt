package com.sekerpatlatma.game.view

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import com.sekerpatlatma.game.R
import com.sekerpatlatma.game.model.Candy
import com.sekerpatlatma.game.model.CandyType
import kotlin.math.abs
import kotlin.random.Random

class GameBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val boardSize = 8
    private var cellSize = 0f
    private var boardPadding = 20f
    private var cellGap = 6f
    
    private var board: Array<Array<Candy?>> = Array(boardSize) { arrayOfNulls(boardSize) }
    
    private var onCandyClickListener: ((Int, Int) -> Unit)? = null
    private var onCandySwipeListener: ((Int, Int, Int, Int) -> Unit)? = null
    
    // Swipe gesture tracking
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var startRow = -1
    private var startCol = -1
    private val minSwipeDistance = 50f
    private var isSwiping = false
    
    // Particle system for explosions
    private val particles = mutableListOf<Particle>()
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    data class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var size: Float,
        var color: Int,
        var alpha: Float,
        var life: Float,
        var maxLife: Float,
        var gravity: Float = 0.5f
    )
    
    // Paints
    private val boardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A1A2E")
        style = Paint.Style.FILL
    }
    
    private val boardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3D3D5C")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    
    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
        setShadowLayer(15f, 0f, 0f, Color.WHITE)
    }
    
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD700")
        style = Paint.Style.STROKE
        strokeWidth = 3f
        setShadowLayer(12f, 0f, 0f, Color.parseColor("#FFD700"))
    }
    
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#40000000")
    }
    
    // Glow paint for enhanced visuals
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    private var blurMaskFilter: BlurMaskFilter? = null
    private var glowBlurFilter: BlurMaskFilter? = null
    
    private fun getShadowPaintWithBlur(): Paint {
        if (blurMaskFilter == null) {
            blurMaskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
        }
        shadowPaint.maskFilter = blurMaskFilter
        return shadowPaint
    }
    
    private fun getGlowPaint(color: Int): Paint {
        if (glowBlurFilter == null) {
            glowBlurFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
        }
        glowPaint.color = color
        glowPaint.alpha = 100
        glowPaint.maskFilter = glowBlurFilter
        return glowPaint
    }
    
    // Animation properties
    private var animatedCells = mutableMapOf<Pair<Int, Int>, CellAnimation>()
    
    data class CellAnimation(
        var scale: Float = 1f,
        var alpha: Float = 1f,
        var offsetX: Float = 0f,
        var offsetY: Float = 0f,
        var rotation: Float = 0f,
        var glowIntensity: Float = 0f
    )

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val availableSize = minOf(w, h) - boardPadding * 2
        cellSize = (availableSize - cellGap * (boardSize - 1)) / boardSize
        textPaint.textSize = cellSize * 0.5f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val boardWidth = cellSize * boardSize + cellGap * (boardSize - 1) + boardPadding * 2
        val startX = (width - boardWidth) / 2 + boardPadding
        val startY = (height - boardWidth) / 2 + boardPadding
        
        // Draw board background with enhanced styling
        val boardRect = RectF(
            startX - boardPadding / 2,
            startY - boardPadding / 2,
            startX + boardWidth - boardPadding * 1.5f,
            startY + boardWidth - boardPadding * 1.5f
        )
        
        // Draw board glow effect (only apply software layer when needed for blur effects)
        canvas.drawRoundRect(boardRect, 24f, 24f, boardPaint)
        canvas.drawRoundRect(boardRect, 24f, 24f, boardBorderPaint)
        
        // Draw candies
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                val candy = board[row][col] ?: continue
                val anim = animatedCells[Pair(row, col)] ?: CellAnimation()
                
                val x = startX + col * (cellSize + cellGap) + anim.offsetX
                val y = startY + row * (cellSize + cellGap) + anim.offsetY
                
                canvas.save()
                
                val centerX = x + cellSize / 2
                val centerY = y + cellSize / 2
                canvas.scale(anim.scale, anim.scale, centerX, centerY)
                canvas.rotate(anim.rotation, centerX, centerY)
                
                // Draw glow effect for enhanced visuals
                if (anim.glowIntensity > 0) {
                    val glowColor = getCandyGlowColor(candy.type)
                    glowPaint.color = glowColor
                    glowPaint.alpha = (anim.glowIntensity * 150).toInt()
                    if (glowBlurFilter == null) {
                        glowBlurFilter = BlurMaskFilter(25f, BlurMaskFilter.Blur.NORMAL)
                    }
                    glowPaint.maskFilter = glowBlurFilter
                    val glowRect = RectF(x - 5f, y - 5f, x + cellSize + 5f, y + cellSize + 5f)
                    canvas.drawRoundRect(glowRect, 18f, 18f, glowPaint)
                }
                
                // Draw shadow
                canvas.drawRoundRect(
                    x + 4f, y + 6f,
                    x + cellSize - 4f, y + cellSize,
                    14f, 14f,
                    getShadowPaintWithBlur()
                )
                
                // Draw candy background with gradient
                drawCandyGradient(canvas, candy.type, x, y, cellSize, anim.alpha)
                
                // Draw emoji
                textPaint.alpha = (anim.alpha * 255).toInt()
                canvas.drawText(
                    candy.type.emoji,
                    centerX,
                    centerY + textPaint.textSize / 3,
                    textPaint
                )
                
                // Draw selection highlight with pulsing effect
                if (candy.isSelected) {
                    val selectRect = RectF(x + 2f, y + 2f, x + cellSize - 2f, y + cellSize - 2f)
                    canvas.drawRoundRect(selectRect, 14f, 14f, selectedPaint)
                }
                
                // Draw hint highlight
                if (candy.isHint) {
                    val hintRect = RectF(x + 2f, y + 2f, x + cellSize - 2f, y + cellSize - 2f)
                    canvas.drawRoundRect(hintRect, 14f, 14f, hintPaint)
                }
                
                canvas.restore()
            }
        }
        
        // Draw particles for explosion effects
        drawParticles(canvas)
    }
    
    private fun getCandyGlowColor(type: CandyType): Int {
        return when (type) {
            CandyType.RED -> Color.parseColor("#FF5252")
            CandyType.BLUE -> Color.parseColor("#00B0FF")
            CandyType.GREEN -> Color.parseColor("#00E676")
            CandyType.YELLOW -> Color.parseColor("#FFEA00")
            CandyType.PURPLE -> Color.parseColor("#E040FB")
            CandyType.ORANGE -> Color.parseColor("#FF9100")
        }
    }
    
    private fun drawParticles(canvas: Canvas) {
        if (particles.isEmpty()) return
        
        val iterator = particles.iterator()
        var hasActiveParticles = false
        
        while (iterator.hasNext()) {
            val particle = iterator.next()
            
            // Update particle position
            particle.x += particle.vx
            particle.y += particle.vy
            particle.vy += particle.gravity
            particle.life -= 1f / 60f
            particle.alpha = (particle.life / particle.maxLife).coerceIn(0f, 1f)
            particle.size *= 0.97f
            
            if (particle.life <= 0 || particle.alpha <= 0 || particle.size < 0.5f) {
                iterator.remove()
                continue
            }
            
            hasActiveParticles = true
            
            // Draw particle
            particlePaint.color = particle.color
            particlePaint.alpha = (particle.alpha * 255).toInt()
            canvas.drawCircle(particle.x, particle.y, particle.size, particlePaint)
        }
        
        // Continue animation if particles exist, using post instead of postInvalidateOnAnimation for better control
        if (hasActiveParticles) {
            postInvalidateOnAnimation()
        }
    }
    
    private fun spawnExplosionParticles(x: Float, y: Float, color: Int, count: Int = 12) {
        val colors = listOf(
            color,
            Color.WHITE,
            adjustColorBrightness(color, 1.3f),
            adjustColorBrightness(color, 0.7f)
        )
        
        for (i in 0 until count) {
            val angle = (i.toFloat() / count) * 2 * Math.PI + Random.nextFloat() * 0.5f
            val speed = 4f + Random.nextFloat() * 8f
            val particleColor = colors[Random.nextInt(colors.size)]
            
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = (Math.cos(angle) * speed).toFloat(),
                    vy = (Math.sin(angle) * speed).toFloat() - 2f,
                    size = 6f + Random.nextFloat() * 8f,
                    color = particleColor,
                    alpha = 1f,
                    life = 0.5f + Random.nextFloat() * 0.5f,
                    maxLife = 1f,
                    gravity = 0.3f
                )
            )
        }
        
        // Add sparkle particles
        for (i in 0 until 8) {
            particles.add(
                Particle(
                    x = x + (Random.nextFloat() - 0.5f) * cellSize,
                    y = y + (Random.nextFloat() - 0.5f) * cellSize,
                    vx = (Random.nextFloat() - 0.5f) * 3f,
                    vy = (Random.nextFloat() - 0.5f) * 3f - 1f,
                    size = 3f + Random.nextFloat() * 4f,
                    color = Color.WHITE,
                    alpha = 1f,
                    life = 0.3f + Random.nextFloat() * 0.4f,
                    maxLife = 0.7f,
                    gravity = 0.1f
                )
            )
        }
    }
    
    private fun adjustColorBrightness(color: Int, factor: Float): Int {
        val r = ((Color.red(color) * factor).toInt()).coerceIn(0, 255)
        val g = ((Color.green(color) * factor).toInt()).coerceIn(0, 255)
        val b = ((Color.blue(color) * factor).toInt()).coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    private fun drawCandyGradient(canvas: Canvas, type: CandyType, x: Float, y: Float, size: Float, alpha: Float) {
        val colors = when (type) {
            CandyType.RED -> intArrayOf(Color.parseColor("#FF5252"), Color.parseColor("#D32F2F"))
            CandyType.BLUE -> intArrayOf(Color.parseColor("#00B0FF"), Color.parseColor("#0091EA"))
            CandyType.GREEN -> intArrayOf(Color.parseColor("#00E676"), Color.parseColor("#00C853"))
            CandyType.YELLOW -> intArrayOf(Color.parseColor("#FFEA00"), Color.parseColor("#FFC400"))
            CandyType.PURPLE -> intArrayOf(Color.parseColor("#E040FB"), Color.parseColor("#AA00FF"))
            CandyType.ORANGE -> intArrayOf(Color.parseColor("#FF9100"), Color.parseColor("#FF6D00"))
        }
        
        val gradient = LinearGradient(
            x, y, x + size, y + size,
            colors[0], colors[1],
            Shader.TileMode.CLAMP
        )
        
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = gradient
            this.alpha = (alpha * 255).toInt()
        }
        
        // Draw main candy shape
        val rect = RectF(x + 2f, y + 2f, x + size - 2f, y + size - 2f)
        canvas.drawRoundRect(rect, 14f, 14f, paint)
        
        // Draw inner highlight
        val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#40FFFFFF")
        }
        val highlightRect = RectF(x + 6f, y + 6f, x + size - 6f, y + size / 2)
        canvas.drawRoundRect(highlightRect, 10f, 10f, highlightPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val boardWidth = cellSize * boardSize + cellGap * (boardSize - 1) + boardPadding * 2
        val boardStartX = (width - boardWidth) / 2 + boardPadding
        val boardStartY = (height - boardWidth) / 2 + boardPadding
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.x
                touchStartY = event.y
                isSwiping = false
                
                val touchX = event.x - boardStartX
                val touchY = event.y - boardStartY
                
                if (touchX >= 0 && touchY >= 0) {
                    startCol = (touchX / (cellSize + cellGap)).toInt()
                    startRow = (touchY / (cellSize + cellGap)).toInt()
                    
                    if (startRow in 0 until boardSize && startCol in 0 until boardSize) {
                        animateCandyPress(startRow, startCol)
                        return true
                    }
                }
                startRow = -1
                startCol = -1
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (startRow >= 0 && startCol >= 0 && !isSwiping) {
                    val deltaX = event.x - touchStartX
                    val deltaY = event.y - touchStartY
                    
                    if (abs(deltaX) > minSwipeDistance || abs(deltaY) > minSwipeDistance) {
                        isSwiping = true
                        
                        // Determine swipe direction
                        val endRow: Int
                        val endCol: Int
                        
                        if (abs(deltaX) > abs(deltaY)) {
                            // Horizontal swipe
                            endRow = startRow
                            endCol = if (deltaX > 0) startCol + 1 else startCol - 1
                        } else {
                            // Vertical swipe
                            endRow = if (deltaY > 0) startRow + 1 else startRow - 1
                            endCol = startCol
                        }
                        
                        // Validate target position
                        if (endRow in 0 until boardSize && endCol in 0 until boardSize) {
                            // Animate the swap visually
                            animateSwap(startRow, startCol, endRow, endCol)
                            onCandySwipeListener?.invoke(startRow, startCol, endRow, endCol)
                        }
                        
                        startRow = -1
                        startCol = -1
                        return true
                    }
                }
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!isSwiping && startRow >= 0 && startCol >= 0) {
                    // It was a tap, not a swipe
                    onCandyClickListener?.invoke(startRow, startCol)
                }
                startRow = -1
                startCol = -1
                isSwiping = false
            }
        }
        return true
    }
    
    private fun animateSwap(row1: Int, col1: Int, row2: Int, col2: Int) {
        val key1 = Pair(row1, col1)
        val key2 = Pair(row2, col2)
        
        animatedCells[key1] = CellAnimation()
        animatedCells[key2] = CellAnimation()
        
        val offsetX = (col2 - col1) * (cellSize + cellGap)
        val offsetY = (row2 - row1) * (cellSize + cellGap)
        
        // Animate first candy moving to second position
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 150
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                val progress = it.animatedValue as Float
                animatedCells[key1]?.offsetX = offsetX * progress
                animatedCells[key1]?.offsetY = offsetY * progress
                animatedCells[key2]?.offsetX = -offsetX * progress
                animatedCells[key2]?.offsetY = -offsetY * progress
                invalidate()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    animatedCells.remove(key1)
                    animatedCells.remove(key2)
                }
            })
            start()
        }
    }

    fun setBoard(newBoard: Array<Array<Candy?>>) {
        board = newBoard
        invalidate()
    }

    fun setOnCandyClickListener(listener: (Int, Int) -> Unit) {
        onCandyClickListener = listener
    }
    
    fun setOnCandySwipeListener(listener: (Int, Int, Int, Int) -> Unit) {
        onCandySwipeListener = listener
    }

    fun animateCandyPress(row: Int, col: Int) {
        val key = Pair(row, col)
        animatedCells[key] = CellAnimation()
        
        val scaleDown = ValueAnimator.ofFloat(1f, 0.9f).apply {
            duration = 100
            addUpdateListener {
                animatedCells[key]?.scale = it.animatedValue as Float
                invalidate()
            }
        }
        
        val scaleUp = ValueAnimator.ofFloat(0.9f, 1f).apply {
            duration = 150
            interpolator = OvershootInterpolator()
            addUpdateListener {
                animatedCells[key]?.scale = it.animatedValue as Float
                invalidate()
            }
        }
        
        AnimatorSet().apply {
            playSequentially(scaleDown, scaleUp)
            start()
        }
    }

    fun animateMatchExplosion(positions: List<Pair<Int, Int>>, onComplete: () -> Unit) {
        val boardWidth = cellSize * boardSize + cellGap * (boardSize - 1) + boardPadding * 2
        val boardStartX = (width - boardWidth) / 2 + boardPadding
        val boardStartY = (height - boardWidth) / 2 + boardPadding
        
        positions.forEach { (row, col) ->
            val key = Pair(row, col)
            animatedCells[key] = CellAnimation()
            
            // Spawn particles for each exploding candy
            val candy = board[row][col]
            if (candy != null) {
                val x = boardStartX + col * (cellSize + cellGap) + cellSize / 2
                val y = boardStartY + row * (cellSize + cellGap) + cellSize / 2
                val color = getCandyGlowColor(candy.type)
                spawnExplosionParticles(x, y, color, 15)
            }
        }
        
        val animators = positions.mapIndexed { index, (row, col) ->
            val key = Pair(row, col)
            
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 450
                startDelay = (index * 25).toLong()
                interpolator = AccelerateInterpolator()
                addUpdateListener {
                    val progress = it.animatedValue as Float
                    animatedCells[key]?.let { anim ->
                        // Scale up then down
                        anim.scale = if (progress < 0.3f) {
                            1f + progress * 1.5f
                        } else {
                            1.45f * (1f - (progress - 0.3f) / 0.7f)
                        }
                        // Fade out
                        anim.alpha = if (progress < 0.5f) 1f else 1f - (progress - 0.5f) * 2f
                        // Rotate
                        anim.rotation = progress * 180f
                        // Glow intensity
                        anim.glowIntensity = if (progress < 0.3f) progress * 3f else (1f - progress) * 1.5f
                    }
                    invalidate()
                }
            }
        }
        
        AnimatorSet().apply {
            playTogether(animators)
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    positions.forEach { animatedCells.remove(it) }
                    onComplete()
                }
            })
            start()
        }
    }

    fun animateCandyDrop(onComplete: () -> Unit) {
        val dropAnimations = mutableListOf<ValueAnimator>()
        
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (board[row][col] != null) {
                    val key = Pair(row, col)
                    animatedCells[key] = CellAnimation(offsetY = -cellSize * 2)
                    
                    val dropAnim = ValueAnimator.ofFloat(-cellSize * 2, 0f).apply {
                        duration = 300
                        startDelay = (col * 30).toLong()
                        interpolator = OvershootInterpolator(0.8f)
                        addUpdateListener {
                            animatedCells[key]?.offsetY = it.animatedValue as Float
                            invalidate()
                        }
                    }
                    dropAnimations.add(dropAnim)
                }
            }
        }
        
        if (dropAnimations.isEmpty()) {
            onComplete()
            return
        }
        
        AnimatorSet().apply {
            playTogether(dropAnimations)
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    animatedCells.clear()
                    onComplete()
                }
            })
            start()
        }
    }
}
