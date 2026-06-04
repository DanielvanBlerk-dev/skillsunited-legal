package com.dkvb.skillswap

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class AvatarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var initials = "?"
    private var bgColor = Color.parseColor("#001F5B")

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val colors = listOf(
        "#001F5B", "#1976D2", "#0D47A1", "#1565C0",
        "#1E88E5", "#0288D1", "#01579B", "#283593"
    )

    fun setName(name: String) {
        initials = name.trim().split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it[0].uppercase() }
            .ifEmpty { "?" }

        // Pick a consistent color based on the name
        bgColor = Color.parseColor(colors[name.length % colors.size])
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(cx, cy)

        bgPaint.color = bgColor
        canvas.drawCircle(cx, cy, radius, bgPaint)

        textPaint.textSize = radius * 0.75f
        canvas.drawText(initials, cx, cy - (textPaint.descent() + textPaint.ascent()) / 2, textPaint)
    }
}