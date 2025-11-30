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
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import com.sekerpatlatma.game.R
import com.sekerpatlatma.game.model.Candy
import com.sekerpatlatma.game.model.CandyType

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
    
    private var blurMaskFilter: BlurMaskFilter? = null
    
    private fun getShadowPaintWithBlur(): Paint {
        if (blurMaskFilter == null) {
            blurMaskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
        }
        shadowPaint.maskFilter = blurMaskFilter
        return shadowPaint
    }
    
    // Animation properties
    private var animatedCells = mutableMapOf<Pair<Int, Int>, CellAnimation>()
    
    data class CellAnimation(
        var scale: Float = 1f,
        var alpha: Float = 1f,
        var offsetY: Float = 0f,
        var rotation: Float = 0f
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
        
        // Draw board background
        val boardRect = RectF(
            startX - boardPadding / 2,
            startY - boardPadding / 2,
            startX + boardWidth - boardPadding * 1.5f,
            startY + boardWidth - boardPadding * 1.5f
        )
        canvas.drawRoundRect(boardRect, 24f, 24f, boardPaint)
        canvas.drawRoundRect(boardRect, 24f, 24f, boardBorderPaint)
        
        // Draw candies
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                val candy = board[row][col] ?: continue
                val anim = animatedCells[Pair(row, col)] ?: CellAnimation()
                
                val x = startX + col * (cellSize + cellGap)
                val y = startY + row * (cellSize + cellGap) + anim.offsetY
                
                canvas.save()
                
                val centerX = x + cellSize / 2
                val centerY = y + cellSize / 2
                canvas.scale(anim.scale, anim.scale, centerX, centerY)
                canvas.rotate(anim.rotation, centerX, centerY)
                
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
                
                // Draw selection highlight
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
        if (event.action == MotionEvent.ACTION_DOWN) {
            val boardWidth = cellSize * boardSize + cellGap * (boardSize - 1) + boardPadding * 2
            val startX = (width - boardWidth) / 2 + boardPadding
            val startY = (height - boardWidth) / 2 + boardPadding
            
            val touchX = event.x - startX
            val touchY = event.y - startY
            
            if (touchX >= 0 && touchY >= 0) {
                val col = (touchX / (cellSize + cellGap)).toInt()
                val row = (touchY / (cellSize + cellGap)).toInt()
                
                if (row in 0 until boardSize && col in 0 until boardSize) {
                    animateCandyPress(row, col)
                    onCandyClickListener?.invoke(row, col)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    fun setBoard(newBoard: Array<Array<Candy?>>) {
        board = newBoard
        invalidate()
    }

    fun setOnCandyClickListener(listener: (Int, Int) -> Unit) {
        onCandyClickListener = listener
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
        positions.forEach { (row, col) ->
            val key = Pair(row, col)
            animatedCells[key] = CellAnimation()
        }
        
        val animators = positions.mapIndexed { index, (row, col) ->
            val key = Pair(row, col)
            
            ValueAnimator.ofFloat(1f, 1.3f, 0f).apply {
                duration = 400
                startDelay = (index * 30).toLong()
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener {
                    val value = it.animatedValue as Float
                    animatedCells[key]?.let { anim ->
                        anim.scale = if (value > 1f) value else 1f
                        anim.alpha = if (value <= 1f) value else 1f
                        anim.rotation = (1f - value) * 45f
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
