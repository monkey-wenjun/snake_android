package com.example.snake

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.util.Log
import kotlin.math.atan2
import kotlin.math.sqrt

class ControlButton(private val x: Float, private val y: Float, private val radius: Float) {
    companion object {
        private const val TAG = "ControlButton"
    }

    private val paint = Paint().apply {
        color = Color.GRAY
        style = Paint.Style.FILL
        alpha = 128
        isAntiAlias = true
    }

    private val borderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
        alpha = 80
    }

    private val arrowPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        strokeWidth = 3f
        alpha = 120
    }

    private var snake: Snake? = null

    fun setSnake(snake: Snake) {
        this.snake = snake
        Log.d(TAG, "Snake reference set: $snake")
    }

    fun resetDirection() {
        snake?.setDirection(null)
        Log.d(TAG, "Direction reset")
    }

    fun draw(canvas: Canvas) {
        try {
            // Draw main circle
            canvas.drawCircle(x, y, radius, paint)
            canvas.drawCircle(x, y, radius, borderPaint)

            // Draw direction arrows
            val arrowSize = radius * 0.3f
            val arrowDist = radius * 0.5f
            
            // Draw arrows in all four directions
            canvas.drawPath(createArrowPath(x, y - arrowDist, arrowSize, Direction.UP), arrowPaint)
            canvas.drawPath(createArrowPath(x, y + arrowDist, arrowSize, Direction.DOWN), arrowPaint)
            canvas.drawPath(createArrowPath(x - arrowDist, y, arrowSize, Direction.LEFT), arrowPaint)
            canvas.drawPath(createArrowPath(x + arrowDist, y, arrowSize, Direction.RIGHT), arrowPaint)
        } catch (e: Exception) {
            Log.e(TAG, "Error drawing control button: ${e.message}", e)
        }
    }

    private fun createArrowPath(centerX: Float, centerY: Float, size: Float, direction: Direction): android.graphics.Path {
        val path = android.graphics.Path()
        
        when (direction) {
            Direction.UP -> {
                path.moveTo(centerX, centerY - size)
                path.lineTo(centerX - size, centerY + size)
                path.lineTo(centerX + size, centerY + size)
            }
            Direction.DOWN -> {
                path.moveTo(centerX, centerY + size)
                path.lineTo(centerX - size, centerY - size)
                path.lineTo(centerX + size, centerY - size)
            }
            Direction.LEFT -> {
                path.moveTo(centerX - size, centerY)
                path.lineTo(centerX + size, centerY - size)
                path.lineTo(centerX + size, centerY + size)
            }
            Direction.RIGHT -> {
                path.moveTo(centerX + size, centerY)
                path.lineTo(centerX - size, centerY - size)
                path.lineTo(centerX - size, centerY + size)
            }
        }
        path.close()
        return path
    }

    fun updateDirection(touchX: Float, touchY: Float) {
        if (!isPressed(touchX, touchY)) {
            return
        }

        try {
            // Calculate angle between touch point and button center
            val dx = touchX - x
            val dy = touchY - y
            val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
            
            // Determine direction based on angle
            val direction = when {
                angle in -45.0..45.0 -> Direction.RIGHT
                angle in 45.0..135.0 -> Direction.DOWN
                angle in -135.0..-45.0 -> Direction.UP
                else -> Direction.LEFT
            }

            Log.d(TAG, "Setting direction: $direction, angle: $angle")
            snake?.setDirection(direction)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating direction: ${e.message}", e)
        }
    }

    fun isPressed(touchX: Float, touchY: Float): Boolean {
        val dx = touchX - x
        val dy = touchY - y
        val distance = sqrt(dx * dx + dy * dy)
        val result = distance <= radius * 1.2f  // Slightly larger touch area
        if (result) {
            Log.d(TAG, "Button pressed at ($touchX, $touchY)")
        }
        return result
    }
} 