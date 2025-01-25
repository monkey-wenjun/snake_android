package com.example.snake

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

class Snake(
    startX: Float,
    startY: Float,
    private val screenWidth: Float,
    private val screenHeight: Float,
    private val segmentSize: Float = 80f
) {
    private val segments = mutableListOf<PointF>()
    private var direction = Direction.RIGHT
    private var nextDirection = direction
    private val speed = segmentSize * 0.4f
    private val color = Color.rgb(
        Random.nextInt(128, 256),
        Random.nextInt(128, 256),
        Random.nextInt(128, 256)
    )
    
    private val paint = Paint().apply {
        color = this@Snake.color
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

    fun draw(canvas: Canvas) {
        segments.forEachIndexed { index, segment ->
            val alpha = 255 - (index * 255 / segments.size).coerceAtMost(200)
            paint.alpha = alpha
            canvas.drawCircle(segment.x, segment.y, segmentSize / 2, paint)
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

    fun getSegments(): List<PointF> = segments
} 