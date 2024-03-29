package com.swapnil.googlenavigationapp.views

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
fun CustomMarkerBitmap(index: Int): Bitmap {
    return remember {
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 60f
            textAlign = Paint.Align.CENTER
        }
        val circlePaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.FILL
        }
        val bounds = Rect()
        textPaint.getTextBounds(index.toString(), 0, index.toString().length, bounds)
        val width = bounds.width().toFloat()
        val height = bounds.height().toFloat()
        val circleRadius = maxOf(width, height) / 2 * 1.7f // Increase circle radius a bit for padding
        val bitmap = Bitmap.createBitmap((circleRadius * 2).toInt(), (circleRadius * 2).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawCircle(circleRadius, circleRadius, circleRadius, circlePaint) // Draw circle
        canvas.drawText(index.toString(), circleRadius, circleRadius + height / 2, textPaint) // Draw text
        bitmap
    }
}