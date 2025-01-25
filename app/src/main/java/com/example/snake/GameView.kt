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
        private const val MIN_FOOD_DISTANCE = 150f  // Increased minimum distance
        private const val SEGMENT_SIZE = 80f  // Increased snake and food size
    }

    private var thread: GameThread? = null
    private lateinit var snake: Snake
    private val foodItems = mutableListOf<Food>()
    private lateinit var controlButton: ControlButton
    private var score = 0
    private val soundManager = SoundManager(context)
    
    private val scorePaint = Paint().apply {
        color = Color.WHITE
        textSize = 100f  // Increased score text size
        textAlign = Paint.Align.RIGHT
    }

    init {
        holder.addCallback(this)
        
        // Set window to full screen
        val window = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = window.defaultDisplay
        val metrics = context.resources.displayMetrics
        display.getRealMetrics(metrics)
        
        isFocusable = true
        isClickable = true
        isFocusableInTouchMode = true
        setBackgroundColor(Color.BLACK)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.d(TAG, "Surface created: width=$width, height=$height")
        
        // Initialize snake with screen dimensions and larger size
        snake = Snake(width / 2f, height / 2f, width.toFloat(), height.toFloat(), SEGMENT_SIZE)
        
        // Initialize control button
        val buttonRadius = height / 5f  // Keep current size
        val buttonX = width * 0.08f     // Move closer to left edge
        val buttonY = height * 0.75f    // Move closer to bottom
        
        controlButton = ControlButton(buttonX, buttonY, buttonRadius)
        
        // Initialize food items
        initializeFood()
        
        // Create new thread
        thread = GameThread(holder, this).apply {
            setRunning(true)
            start()
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
        var retry = true
        thread?.let { currentThread ->
            currentThread.setRunning(false)
            while (retry) {
                try {
                    currentThread.join(500) // Wait up to 500ms
                    retry = false
                    thread = null
                } catch (e: InterruptedException) {
                    Log.e(TAG, "Error stopping game thread: ${e.message}")
                }
            }
        }
        try {
            soundManager.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing sound manager: ${e.message}")
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Log.d(TAG, "Detached from window")
        thread?.let { currentThread ->
            currentThread.setRunning(false)
            try {
                currentThread.join(500) // Wait up to 500ms
                thread = null
            } catch (e: InterruptedException) {
                Log.e(TAG, "Error stopping game thread on detach: ${e.message}")
            }
        }
        try {
            soundManager.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing sound manager on detach: ${e.message}")
        }
    }

    fun update() {
        snake.move()
        checkCollisions()
        
        // Respawn food if there are too few
        if (foodItems.size < FOOD_COUNT) {
            val food = Food(width, height, SEGMENT_SIZE)
            if (isValidFoodPosition(food)) {
                foodItems.add(food)
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
        
        // Draw score in top-right corner
        canvas.drawText("Score: $score", width - 40f, 100f, scorePaint)
    }

    private fun checkCollisions() {
        // Check food collisions
        val iterator = foodItems.iterator()
        while (iterator.hasNext()) {
            val food = iterator.next()
            if (snake.checkFoodCollision(food)) {
                // Play sound for the character
                soundManager.playSound(food.character)
                
                // Add score
                score += when (food.character) {
                    in 'A'..'Z' -> 30
                    in 'a'..'z' -> 20
                    in '0'..'9' -> 10
                    else -> 0
                }
                snake.grow()
                iterator.remove()
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