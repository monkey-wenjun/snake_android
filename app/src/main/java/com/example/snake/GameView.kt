package com.example.snake

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager
import kotlin.random.Random

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    companion object {
        private const val TAG = "GameView"
        private const val FOOD_COUNT = 8
        private const val MIN_FOOD_DISTANCE = 150f
        private const val SEGMENT_SIZE = 80f
    }

    private var thread: GameThread? = null
    private lateinit var snake: Snake
    private val foodItems = mutableListOf<Food>()
    private lateinit var controlButton: ControlButton
    private var score = 0
    private val soundManager = SoundManager(context)
    private var isGamePaused = false
    private var gameStartTime = System.currentTimeMillis()
    private var lastDrawTime = 0L
    
    private val scorePaint = Paint().apply {
        color = Color.WHITE
        textSize = 50f
        textAlign = Paint.Align.RIGHT
    }

    private val timePaint = Paint().apply {
        color = Color.WHITE
        textSize = 50f
        textAlign = Paint.Align.CENTER
    }

    init {
        try {
            holder.addCallback(this)
            isFocusable = true
            isClickable = true
            isFocusableInTouchMode = true
            setBackgroundColor(Color.BLACK)
        } catch (e: Exception) {
            Log.e(TAG, "Error in init: ${e.message}")
            e.printStackTrace()
        }
    }

    fun pauseGame() {
        Log.d(TAG, "Pausing game")
        isGamePaused = true
        cleanupThread()
    }

    fun resumeGame() {
        Log.d(TAG, "Resuming game")
        isGamePaused = false
        if (holder.surface?.isValid == true) {
            startNewThread()
        }
    }

    private fun startNewThread() {
        Log.d(TAG, "Starting new game thread")
        cleanupThread()  // Ensure old thread is cleaned up
        thread = GameThread(holder, this).apply {
            setRunning(true)
            start()
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.d(TAG, "Surface created: width=$width, height=$height")
        
        try {
            // Initialize game objects if not already initialized
            if (!::snake.isInitialized) {
                snake = Snake(width / 2f, height / 2f, width.toFloat(), height.toFloat(), SEGMENT_SIZE)
                
                // Initialize control button
                val buttonRadius = height / 5f
                val margin = buttonRadius * 0.2f
                val buttonX = buttonRadius + margin
                val buttonY = height - (buttonRadius + margin)
                controlButton = ControlButton(buttonX, buttonY, buttonRadius)
                controlButton.setSnake(snake)
                
                initializeFood()
                
                // Reset game start time when initializing new game
                gameStartTime = System.currentTimeMillis()
            }
            
            resumeGame()
        } catch (e: Exception) {
            Log.e(TAG, "Error in surfaceCreated: ${e.message}", e)
        }
    }

    private fun initializeFood() {
        foodItems.clear()
        val snakeSegments = snake.getSegments()
        
        repeat(FOOD_COUNT) {
            var food: Food
            var validPosition: Boolean
            var attempts = 0
            
            do {
                food = Food(width, height, SEGMENT_SIZE)  // Use same size as snake segments
                validPosition = true
                
                // Check distance from snake
                for (segment in snakeSegments) {
                    if (distance(food.x, food.y, segment.x, segment.y) < MIN_FOOD_DISTANCE) {
                        validPosition = false
                        break
                    }
                }
                
                // Check distance from other food items
                for (existingFood in foodItems) {
                    if (distance(food.x, food.y, existingFood.x, existingFood.y) < MIN_FOOD_DISTANCE) {
                        validPosition = false
                        break
                    }
                }
                
                attempts++
                if (attempts > 100) break  // Prevent infinite loop
                
            } while (!validPosition)
            
            if (validPosition) {
                foodItems.add(food)
            }
        }
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.d(TAG, "Surface changed: width=$width, height=$height")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.d(TAG, "Surface destroyed")
        pauseGame()
    }

    override fun onDetachedFromWindow() {
        Log.d(TAG, "Detached from window")
        super.onDetachedFromWindow()
        cleanup()
    }

    private fun cleanupThread() {
        Log.d(TAG, "Cleaning up game thread")
        thread?.let { currentThread ->
            try {
                currentThread.setRunning(false)
                
                // Wait for the thread to die with timeout
                var retry = true
                var timeoutCount = 0
                while (retry && timeoutCount < 3) {
                    try {
                        currentThread.join(200)  // Increased timeout to 200ms
                        retry = false
                    } catch (e: InterruptedException) {
                        timeoutCount++
                        Log.w(TAG, "Thread join interrupted, attempt $timeoutCount")
                    }
                }
                
                if (retry) {
                    Log.w(TAG, "Thread cleanup timed out after $timeoutCount attempts")
                    // Force interrupt if join failed
                    currentThread.interrupt()
                } else {
                    Log.d(TAG, "Thread cleanup completed successfully")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up thread: ${e.message}")
                e.printStackTrace()
            } finally {
                thread = null
            }
        }
    }

    fun cleanup() {
        Log.d(TAG, "Cleaning up GameView resources")
        try {
            isGamePaused = true
            cleanupThread()
            soundManager.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup: ${e.message}")
        }
    }

    fun update() {
        if (isGamePaused) {
            return
        }

        try {
            synchronized(this) {
                // Store previous position to check if actually moved
                val prevHead = snake.getSegments().firstOrNull()?.let { PointF(it.x, it.y) }
                
                snake.move()
                
                // Only check collisions if snake actually moved
                val currentHead = snake.getSegments().firstOrNull()
                if (prevHead != null && currentHead != null) {
                    val dx = prevHead.x - currentHead.x
                    val dy = prevHead.y - currentHead.y
                    val moved = (dx != 0f || dy != 0f)
                    
                    if (moved) {
                        Log.d("SnakeGame", "蛇移动了！")
                        checkCollisions()
                    } else {
                        Log.d("SnakeGame", "蛇没有移动")
                    }
                }
                
                // Respawn food if there are too few
                while (foodItems.size < FOOD_COUNT) {
                    val food = Food(width, height, SEGMENT_SIZE)
                    if (isValidFoodPosition(food)) {
                        foodItems.add(food)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in update: ${e.message}")
            e.printStackTrace()
            try {
                pauseGame()
            } catch (e2: Exception) {
                Log.e(TAG, "Error while trying to pause game after error: ${e2.message}")
                e2.printStackTrace()
            }
        }
    }

    private fun isValidFoodPosition(food: Food): Boolean {
        // Check if food is too close to edges
        if (food.x < MIN_FOOD_DISTANCE || food.x > width - MIN_FOOD_DISTANCE ||
            food.y < MIN_FOOD_DISTANCE || food.y > height - MIN_FOOD_DISTANCE) {
            return false
        }
        
        // Check distance from snake
        for (segment in snake.getSegments()) {
            if (distance(food.x, food.y, segment.x, segment.y) < MIN_FOOD_DISTANCE) {
                return false
            }
        }
        
        // Check distance from other food items
        for (existingFood in foodItems) {
            if (distance(food.x, food.y, existingFood.x, existingFood.y) < MIN_FOOD_DISTANCE) {
                return false
            }
        }
        
        return true
    }

    override fun draw(canvas: Canvas) {
        if (canvas == null) {
            Log.e(TAG, "Canvas is null in draw")
            return
        }

        try {
            super.draw(canvas)
            
            // Draw game objects
            synchronized(this) {
                snake.draw(canvas)
                foodItems.forEach { it.draw(canvas) }
                controlButton.draw(canvas)
                
                // Draw score in top-right corner
                canvas.drawText("当前积分: $score", width - 20f, 60f, scorePaint)

                // Draw game time in top-center
                val gameTimeSeconds = ((System.currentTimeMillis() - gameStartTime) / 1000).toInt()
                val minutes = gameTimeSeconds / 60
                val seconds = gameTimeSeconds % 60
                val timeText = String.format("游戏时间: %02d:%02d", minutes, seconds)
                canvas.drawText(timeText, width / 2f, 60f, timePaint)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in draw: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun checkCollisions() {
        // Check food collisions
        val iterator = foodItems.iterator()
        while (iterator.hasNext()) {
            val food = iterator.next()
            if (snake.checkFoodCollision(food)) {
                val points = when (food.character) {
                    in 'A'..'Z' -> 30
                    in 'a'..'z' -> 20
                    in '0'..'9' -> 10
                    else -> 0
                }
                score += points
                Log.d("SnakeGame", "吃到食物了！字符: ${food.character}, 得分: $points")
                
                snake.grow()
                soundManager.playSound(food.character)
                iterator.remove()
                break
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isGamePaused) {
            return false
        }

        try {
            synchronized(this) {
                when (event.action) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                        controlButton.updateDirection(event.x, event.y)
                        postInvalidate()  // Use postInvalidate for thread safety
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        controlButton.resetDirection()
                        postInvalidate()
                        return true
                    }
                    else -> return super.onTouchEvent(event)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling touch event: ${e.message}")
            e.printStackTrace()
        }
        return super.onTouchEvent(event)
    }
} 