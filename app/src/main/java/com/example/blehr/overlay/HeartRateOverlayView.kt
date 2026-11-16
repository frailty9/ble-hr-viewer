package com.example.blehr.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.example.blehr.HeartRateUiState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * One place for the overlay's look and touch behavior.
 * Service only creates this view, asks for its window size, and adds it to WindowManager.
 */
@SuppressLint("ViewConstructor", "ClickableViewAccessibility", "SetTextI18n")
class HeartRateOverlayView(
    context: Context,
    private val onClose: () -> Unit,
    private val onDrag: (Int, Int) -> Unit,
) : LinearLayout(context) {

    companion object {
        // Adjust the pill's size/layout here.
        const val WINDOW_WIDTH_DP = 52
        const val WINDOW_MARGIN_DP = 10
        const val INITIAL_TOP_DP = 88
        const val CORNER_RADIUS_DP = 45
        const val BORDER_WIDTH_DP = 1
        const val BORDER_ALPHA_FACTOR = 0.45f
        const val PADDING_HORIZONTAL_DP = 8
        const val PADDING_VERTICAL_DP = 4
        const val ICON_SIZE_DP = 16
        const val ICON_GAP_DP = 4
        const val TEXT_SIZE_SP = 17f
        const val ELEVATION_DP = 4

        // Gesture rhythm.
        const val LONG_PRESS_MS = 600L
        const val DRAG_SLOP_DP = 4
    }

    val windowWidthPx: Int = dp(WINDOW_WIDTH_DP)
    val windowMarginPx: Int = dp(WINDOW_MARGIN_DP)
    val initialTopPx: Int = dp(INITIAL_TOP_DP)
    val fallbackWindowHeightPx: Int =
        dp(max(ICON_SIZE_DP, 22) + PADDING_VERTICAL_DP * 2)

    private val heart = StaticHeartView(context)
    private val bpmText = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE_SP)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        includeFontPadding = false
        maxLines = 1
        gravity = Gravity.CENTER
        text = "--"
        setTextColor(OverlayColors.DefaultFont)
    }
    private val handler = Handler(Looper.getMainLooper())
    private val longPressRunnable = Runnable { onClose() }

    private var appearance = OverlayAppearance.Default
    private var state = HeartRateUiState()
    private var downRawX = 0f
    private var downRawY = 0f
    private var lastRawX = 0f
    private var lastRawY = 0f
    private var dragging = false

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(
            dp(PADDING_HORIZONTAL_DP),
            dp(PADDING_VERTICAL_DP),
            dp(PADDING_HORIZONTAL_DP),
            dp(PADDING_VERTICAL_DP),
        )
        elevation = dp(ELEVATION_DP).toFloat()
        isClickable = true

        addView(
            heart,
            LayoutParams(dp(ICON_SIZE_DP), dp(ICON_SIZE_DP)),
        )
        addView(
            bpmText,
            LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(ICON_GAP_DP)
            },
        )

        applyAppearance(OverlayAppearance.Default)
        setOnTouchListener { _, event -> handleTouch(event) }
    }

    fun bindState(state: HeartRateUiState) {
        this.state = state
        render()
    }

    fun bindAppearance(appearance: OverlayAppearance) {
        this.appearance = appearance
        applyAppearance(appearance)
        render()
    }

    private fun render() {
        val sample = state.sample
        bpmText.text = if (sample != null) sample.bpm.toString() else "--"
        val font = withAlpha(appearance.fontColor, appearance.fontAlpha)
        bpmText.setTextColor(font)
        heart.setColor(font)
    }

    private fun applyAppearance(appearance: OverlayAppearance) {
        background = GradientDrawable().apply {
            cornerRadius = dp(CORNER_RADIUS_DP).toFloat()
            setColor(withAlpha(appearance.backgroundColor, appearance.backgroundAlpha))
            setStroke(
                dp(BORDER_WIDTH_DP),
                withAlpha(0xFFFFFF, appearance.backgroundAlpha * BORDER_ALPHA_FACTOR),
            )
        }
    }

    private fun withAlpha(color: Int, alpha: Float): Int {
        val a = (alpha.coerceIn(0f, 1f) * 255f).roundToInt()
        return (a shl 24) or (color and 0x00FFFFFF)
    }

    private fun handleTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = event.rawX
                downRawY = event.rawY
                lastRawX = downRawX
                lastRawY = downRawY
                dragging = false
                handler.postDelayed(longPressRunnable, LONG_PRESS_MS)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!dragging &&
                    (abs(event.rawX - downRawX) > dp(DRAG_SLOP_DP) ||
                        abs(event.rawY - downRawY) > dp(DRAG_SLOP_DP))
                ) {
                    dragging = true
                    handler.removeCallbacks(longPressRunnable)
                }
                if (dragging) {
                    val dx = (event.rawX - lastRawX).toInt()
                    val dy = (event.rawY - lastRawY).toInt()
                    lastRawX = event.rawX
                    lastRawY = event.rawY
                    if (dx != 0 || dy != 0) {
                        onDrag(dx, dy)
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                handler.removeCallbacks(longPressRunnable)
                if (!dragging) {
                    // A tap intentionally does nothing beyond accessibility feedback.
                    performClick()
                }
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPressRunnable)
                return true
            }
        }
        return false
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(longPressRunnable)
        super.onDetachedFromWindow()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}

private class StaticHeartView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val path = Path()
    private var color = OverlayColors.DefaultFont

    fun setColor(color: Int) {
        this.color = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        paint.color = color

        val centerX = width / 2f
        val centerY = height / 2f
        val base = min(width, height) / 38f
        path.reset()
        for (index in 0..71) {
            val angle = index * (2.0 * PI / 72.0)
            val x = 16.0 * sin(angle).pow(3)
            val y = 13.0 * cos(angle) -
                5.0 * cos(2.0 * angle) -
                2.0 * cos(3.0 * angle) -
                cos(4.0 * angle)
            val px = centerX + (x * base).toFloat()
            val py = centerY - (y * base).toFloat()
            if (index == 0) {
                path.moveTo(px, py)
            } else {
                path.lineTo(px, py)
            }
        }
        path.close()
        canvas.drawPath(path, paint)
    }
}
