package com.example.snake

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList

class Snake(
    startX: Float,
    startY: Float,
    private val screenWidth: Float,
    private val screenHeight: Float,
    private val segmentSize: Float = 80f
) {
    private val segments = CopyOnWriteArrayList<PointF>()
    private var direction = Direction.RIGHT
    private var nextDirection = direction
    private val speed = segmentSize * 0.25f
    private val color = Color.rgb(
        Random.nextInt(128, 256),
        Random.nextInt(128, 256),
        Random.nextInt(128, 256)
    )
    
    private val bodyPaint = Paint().apply {
        color = this@Snake.color
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val eyePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val pupilPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    init {
        setStartPosition(startX, startY)
    }

    fun setStartPosition(x: Float, y: Float) {
        segments.clear()
        for (i in 0..4) {
            segments.add(PointF(x - i * segmentSize, y))
        }
    }

    fun setDirection(newDirection: Direction) {
        when (newDirection) {
            Direction.UP -> if (direction != Direction.DOWN) nextDirection = newDirection
            Direction.DOWN -> if (direction != Direction.UP) nextDirection = newDirection
            Direction.LEFT -> if (direction != Direction.RIGHT) nextDirection = newDirection
            Direction.RIGHT -> if (direction != Direction.LEFT) nextDirection = newDirection
        }
    }

    fun move() {
        direction = nextDirection
        val head = segments.first()
        val newHead = PointF(head.x, head.y)

        when (direction) {
            Direction.UP -> newHead.y -= speed
            Direction.DOWN -> newHead.y += speed
            Direction.LEFT -> newHead.x -= speed
            Direction.RIGHT -> newHead.x += speed
        }

        if (newHead.x < 0) newHead.x = screenWidth
        if (newHead.x > screenWidth) newHead.x = 0f
        if (newHead.y < 0) newHead.y = screenHeight
        if (newHead.y > screenHeight) newHead.y = 0f

        segments.add(0, newHead)
        segments.removeAt(segments.size - 1)
    }

    fun grow() {
        val tail = segments.last()
        repeat(2) {
            segments.add(PointF(tail.x, tail.y))
        }
    }

    private fun drawEyes(canvas: Canvas, head: PointF) {
        val eyeRadius = segmentSize / 6
        val pupilRadius = eyeRadius / 2
        val eyeDistance = segmentSize / 3
        
        // Calculate eye positions based on direction
        val eyePositions = when (direction) {
            Direction.RIGHT -> {
                val x = head.x + segmentSize / 4
                EyePositions(
                    x, head.y - eyeDistance/2,  // left eye
                    x, head.y + eyeDistance/2   // right eye
                )
            }
            Direction.LEFT -> {
                val x = head.x - segmentSize / 4
                EyePositions(
                    x, head.y - eyeDistance/2,  // left eye
                    x, head.y + eyeDistance/2   // right eye
                )
            }
            Direction.UP -> {
                val y = head.y - segmentSize / 4
                EyePositions(
                    head.x - eyeDistance/2, y,  // left eye
                    head.x + eyeDistance/2, y   // right eye
                )
            }
            Direction.DOWN -> {
                val y = head.y + segmentSize / 4
                EyePositions(
                    head.x - eyeDistance/2, y,  // left eye
                    head.x + eyeDistance/2, y   // right eye
                )
            }
        }
        
        // Draw eyes (white part)
        canvas.drawCircle(eyePositions.leftEyeX, eyePositions.leftEyeY, eyeRadius, eyePaint)
        canvas.drawCircle(eyePositions.rightEyeX, eyePositions.rightEyeY, eyeRadius, eyePaint)
        
        // Draw pupils (black part)
        // Add slight offset to pupils based on direction to give more character
        val pupilOffset = eyeRadius / 3
        val pupilPositions = when (direction) {
            Direction.RIGHT -> PupilPositions(
                eyePositions.leftEyeX + pupilOffset, eyePositions.leftEyeY,
                eyePositions.rightEyeX + pupilOffset, eyePositions.rightEyeY
            )
            Direction.LEFT -> PupilPositions(
                eyePositions.leftEyeX - pupilOffset, eyePositions.leftEyeY,
                eyePositions.rightEyeX - pupilOffset, eyePositions.rightEyeY
            )
            Direction.UP -> PupilPositions(
                eyePositions.leftEyeX, eyePositions.leftEyeY - pupilOffset,
                eyePositions.rightEyeX, eyePositions.rightEyeY - pupilOffset
            )
            Direction.DOWN -> PupilPositions(
                eyePositions.leftEyeX, eyePositions.leftEyeY + pupilOffset,
                eyePositions.rightEyeX, eyePositions.rightEyeY + pupilOffset
            )
        }
        
        canvas.drawCircle(pupilPositions.leftPupilX, pupilPositions.leftPupilY, pupilRadius, pupilPaint)
        canvas.drawCircle(pupilPositions.rightPupilX, pupilPositions.rightPupilY, pupilRadius, pupilPaint)
    }

    private data class EyePositions(
        val leftEyeX: Float,
        val leftEyeY: Float,
        val rightEyeX: Float,
        val rightEyeY: Float
    )

    private data class PupilPositions(
        val leftPupilX: Float,
        val leftPupilY: Float,
        val rightPupilX: Float,
        val rightPupilY: Float
    )

    fun draw(canvas: Canvas) {
        // Create a snapshot of the segments to avoid concurrent modification
        val currentSegments = segments.toList()
        
        // Draw body segments with gradient alpha
        currentSegments.forEachIndexed { index, segment ->
            val alpha = 255 - (index * 255 / currentSegments.size).coerceAtMost(200)
            bodyPaint.alpha = alpha
            canvas.drawCircle(segment.x, segment.y, segmentSize / 2, bodyPaint)
        }
        
        // Draw eyes on the head
        if (currentSegments.isNotEmpty()) {
            drawEyes(canvas, currentSegments.first())
        }
    }

    fun checkFoodCollision(food: Food): Boolean {
        val head = segments.first()
        val distance = sqrt(
            (head.x - food.x) * (head.x - food.x) +
            (head.y - food.y) * (head.y - food.y)
        )
        return distance < (segmentSize + food.size) * 0.6f
    }

    fun getSegments(): List<PointF> = segments.toList() // Return a copy of the segments list
} 