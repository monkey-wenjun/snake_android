package com.example.snake

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.util.Log
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.PI
import kotlin.math.sqrt

class ControlButton(private val x: Float, private val y: Float, private val radius: Float) {
    companion object {
        private const val TAG = "ControlButton"
    }

    private val paint = Paint().apply {
        color = Color.GRAY
        style = Paint.Style.FILL
        alpha = 60  // More transparent
    }

    private val borderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f  // Even thinner border
        alpha = 80  // More transparent border
    }

    private val arrowPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        strokeWidth = 3f  // Even thinner arrows
        alpha = 120  // More transparent arrows
    }

    fun draw(canvas: Canvas) {
        // Draw main circle
        canvas.drawCircle(x, y, radius, paint)
        canvas.drawCircle(x, y, radius, borderPaint)

        // Draw direction arrows
        val arrowSize = radius * 0.3f  // Smaller arrows
        val arrowDist = radius * 0.5f  // Arrows closer to center
        
        // Up arrow
        canvas.drawPath(createArrowPath(x, y - arrowDist, arrowSize, Direction.UP), arrowPaint)
        
        // Down arrow
        canvas.drawPath(createArrowPath(x, y + arrowDist, arrowSize, Direction.DOWN), arrowPaint)
        
        // Left arrow
        canvas.drawPath(createArrowPath(x - arrowDist, y, arrowSize, Direction.LEFT), arrowPaint)
        
        // Right arrow
        canvas.drawPath(createArrowPath(x + arrowDist, y, arrowSize, Direction.RIGHT), arrowPaint)
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

    fun isPressed(touchX: Float, touchY: Float): Boolean {
        val distance = sqrt((touchX - x) * (touchX - x) + (touchY - y) * (touchY - y))
        val isPressed = distance <= radius * 1.2f  // Slightly reduced touch area
        Log.d(TAG, "isPressed: touch=($touchX, $touchY), distance=$distance, radius=$radius, result=$isPressed")
        return isPressed
    }

    fun isInDirection(touchX: Float, touchY: Float, direction: Direction): Boolean {
        val dx = touchX - x
        val dy = touchY - y
        
        // Simple direction detection based on relative position
        val result = when (direction) {
            Direction.UP -> dy < 0 && abs(dy) > abs(dx)
            Direction.DOWN -> dy > 0 && abs(dy) > abs(dx)
            Direction.LEFT -> dx < 0 && abs(dx) > abs(dy)
            Direction.RIGHT -> dx > 0 && abs(dx) > abs(dy)
        }
        
        Log.d(TAG, "isInDirection: dir=$direction, dx=$dx, dy=$dy, result=$result")
        return result
    }
} 