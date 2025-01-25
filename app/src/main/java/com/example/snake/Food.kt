package com.example.snake

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.random.Random

class Food(
    private val screenWidth: Int,
    private val screenHeight: Int,
    val size: Float = 80f  // Increased default size
) {
    var x: Float = 0f
    var y: Float = 0f
    var character: Char = 'A'
    private val color: Int
    
    private val paint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val textPaint = Paint().apply {
        textAlign = Paint.Align.CENTER
        textSize = size * 0.8f  // Larger text size relative to food size
        isAntiAlias = true
        color = Color.BLACK
        isFakeBoldText = true  // Make text bolder
    }

    init {
        // Generate brighter colors for better visibility
        color = Color.rgb(
            Random.nextInt(128, 256),
            Random.nextInt(128, 256),
            Random.nextInt(128, 256)
        )
        respawn()
    }

    fun respawn() {
        // Keep food away from edges
        val margin = size * 2
        x = Random.nextFloat() * (screenWidth - 2 * margin) + margin
        y = Random.nextFloat() * (screenHeight - 2 * margin) + margin
        
        // Generate random character (A-Z, a-z, 0-9)
        character = when (Random.nextInt(3)) {
            0 -> Random.nextInt(26).let { 'A' + it }  // Uppercase letters
            1 -> Random.nextInt(26).let { 'a' + it }  // Lowercase letters
            else -> Random.nextInt(10).let { '0' + it }  // Numbers
        }
    }

    fun draw(canvas: Canvas) {
        // Draw glow effect
        paint.color = color
        for (i in 4 downTo 0) {
            paint.alpha = 50 - i * 10
            canvas.drawCircle(x, y, size * (1 + i * 0.1f), paint)
        }
        
        // Draw main circle
        paint.alpha = 255
        canvas.drawCircle(x, y, size, paint)
        
        // Draw character with shadow for better visibility
        textPaint.setShadowLayer(size * 0.1f, 0f, 0f, Color.WHITE)
        canvas.drawText(
            character.toString(),
            x,
            y + size * 0.3f,  // Adjust Y position for better centering
            textPaint
        )
    }
} 