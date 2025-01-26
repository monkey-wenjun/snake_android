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
    
    private val scorePaint = Paint().apply {
        color = Color.WHITE
        textSize = 50f
        textAlign = Paint.Align.RIGHT
    }

    init {
        holder.addCallback(this)
        isFocusable = true
        isClickable = true
        isFocusableInTouchMode = true
        setBackgroundColor(Color.BLACK)
    }

    fun pauseGame() {
        Log.d(TAG, "Pausing game")
        isGamePaused = true
        cleanupThread()
    }

    fun resumeGame() {
        Log.d(TAG, "Resuming game")
        if (!isGamePaused && holder.surface?.isValid == true) {
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
        
        // Initialize game objects if not already initialized
        if (!::snake.isInitialized) {
            snake = Snake(width / 2f, height / 2f, width.toFloat(), height.toFloat(), SEGMENT_SIZE)
            
            // Adjust control button position
            val buttonRadius = height / 5f
            val margin = buttonRadius * 0.2f
            val buttonX = buttonRadius + margin
            val buttonY = height - (buttonRadius + margin)
            controlButton = ControlButton(buttonX, buttonY, buttonRadius)
            
            initializeFood()
        }
        
        isGamePaused = false
        startNewThread()
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
                currentThread.interrupt()
                
                // Wait for the thread to die with timeout
                var retry = true
                var timeoutCount = 0
                while (retry && timeoutCount < 3) {
                    try {
                        currentThread.join(100)  // Wait up to 100ms
                        retry = false
                    } catch (e: InterruptedException) {
                        timeoutCount++
                    }
                }
                
                // Log warning if cleanup timed out
                if (retry) {
                    Log.w(TAG, "Thread cleanup timed out")
                } else {
                    Log.d(TAG, "Thread cleanup completed successfully")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up thread: ${e.message}")
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
        // Only move and check collisions if not paused
        if (!isGamePaused) {
            // Store previous position to check if actually moved
            val prevHead = snake.getSegments().firstOrNull()?.let { PointF(it.x, it.y) }
            
            snake.move()
            
            // Only check collisions if snake actually moved
            val currentHead = snake.getSegments().firstOrNull()
            if (prevHead != null && currentHead != null &&
                (prevHead.x != currentHead.x || prevHead.y != currentHead.y)) {
                checkCollisions()
            }
            
            // Respawn food if there are too few
            while (foodItems.size < FOOD_COUNT) {
                val food = Food(width, height, SEGMENT_SIZE)
                if (isValidFoodPosition(food)) {
                    foodItems.add(food)
                }
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
        super.draw(canvas)
        
        // Draw game objects
        snake.draw(canvas)
        foodItems.forEach { it.draw(canvas) }
        controlButton.draw(canvas)
        
        // Draw score in top-right corner with new text
        canvas.drawText("当前积分: $score", width - 20f, 60f, scorePaint)
    }

    private fun checkCollisions() {
        // Check food collisions
        val iterator = foodItems.iterator()
        while (iterator.hasNext()) {
            val food = iterator.next()
            if (snake.checkFoodCollision(food)) {
                // Add score first
                score += when (food.character) {
                    in 'A'..'Z' -> 30
                    in 'a'..'z' -> 20
                    in '0'..'9' -> 10
                    else -> 0
                }
                
                // Then grow the snake
                snake.grow()
                
                // Play sound only when actually colliding with food
                soundManager.playSound(food.character)
                
                // Remove the food
                iterator.remove()
                break  // Only handle one collision per update
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        Log.d(TAG, "Touch event: ${event.action}, at (${event.x}, ${event.y})")
        
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val x = event.x
                val y = event.y
                
                if (controlButton.isPressed(x, y)) {
                    Log.d(TAG, "Control button pressed at ($x, $y)")
                    
                    // Try each direction in sequence
                    val directions = listOf(Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT)
                    for (dir in directions) {
                        if (controlButton.isInDirection(x, y, dir)) {
                            Log.d(TAG, "Setting direction to $dir")
                            snake.setDirection(dir)
                            invalidate()
                            return true
                        }
                    }
                }
            }
        }
        
        return true
    }
} 