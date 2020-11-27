package com.example.marquee

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.text.Layout
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.animation.LinearInterpolator
import android.widget.HorizontalScrollView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.Px


/**
 * MarqueeTextView
 */
class MarqueeTextView : HorizontalScrollView {

    companion object {
        const val DEFAULT_TEXT_SIZE = 14F
        const val DEFAULT_TEXT_COLOR = Color.RED
        const val DEFAULT_DURATION = 20000F
        const val DEFAULT_GAP_WIDTH = 80F
    }

    private var blankStr = " "
    private var blankWidth = 0F
    private var textAndBlankWidth = 0F

    private var animator: ObjectAnimator? = null
    private val textView: TextView

    private var gapWidth = DEFAULT_GAP_WIDTH.dpF

    private var _text: String = ""
    private var _textColor: Int = DEFAULT_TEXT_COLOR
    private var _duration: Float = DEFAULT_DURATION
    private var _textSize: Float = DEFAULT_TEXT_SIZE.spF

    var text: String
        get() = _text
        set(value) {
            _text = value
            updateTextWidth()
            start()
        }

    var textColor: Int
        @ColorInt
        get() = _textColor
        set(value) {
            _textColor = value
            start()
        }

    var duration: Float
        get() = _duration
        set(value) {
            _duration = value
            start()
        }

    var textSize: Float
        @Px
        get() = _textSize
        set(value) {
            _textSize = value
            updateTextWidth()
            start()
        }

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : super(
        context,
        attrs,
        defStyle
    ) {
        val arr = context.obtainStyledAttributes(attrs, R.styleable.MarqueeTextView)
        _textSize = arr.getDimension(R.styleable.MarqueeTextView_textSize, DEFAULT_TEXT_SIZE.spF)
        _textColor = arr.getColor(R.styleable.MarqueeTextView_textColor, DEFAULT_TEXT_COLOR)
        _text = arr.getString(R.styleable.MarqueeTextView_text) ?: ""
        _duration = arr.getFloat(R.styleable.MarqueeTextView_scroll_duration, DEFAULT_DURATION)
        gapWidth = arr.getDimension(R.styleable.MarqueeTextView_gap_width, DEFAULT_GAP_WIDTH.dpF)
        arr.recycle()
        isHorizontalScrollBarEnabled = false
        isHorizontalFadingEdgeEnabled = true
        //拦截事件
        setOnTouchListener { _, _ -> true }
        textView = TextView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL
            )
            isSingleLine = true
            setTextSize(TypedValue.COMPLEX_UNIT_PX, this@MarqueeTextView.textSize)
            setTextColor(this@MarqueeTextView.textColor)
        }
        updateTextWidth()
        addView(textView)
        post(this::start)
    }

    override fun getLeftFadingEdgeStrength(): Float {
        return if (animator?.isRunning == true) {
            0F
        } else {
            1F
        }
    }

    override fun getRightFadingEdgeStrength(): Float {
        return if (animator?.isRunning == true) {
            0F
        } else {
            1F
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator?.resume()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.pause()
    }

    private fun updateTextWidth() {
        blankWidth = Layout.getDesiredWidth(blankStr, textView.paint)
        val gapLength = (DEFAULT_GAP_WIDTH.dpF / blankWidth).toInt()
        blankStr = blankStr.repeat(gapLength)
        textAndBlankWidth = Layout.getDesiredWidth(text + blankStr, textView.paint)
    }

    /**
     * start
     */
    fun start() {
        animator?.cancel()
        animator = null
        textView.text = StringBuilder(text).append(blankStr).append(text)
        if (width != 0 && height != 0) {
            animator =
                ObjectAnimator.ofFloat(textView, "translationX", 0F, -textAndBlankWidth).let {
                    it.duration = duration.toLong()
                    it.interpolator = LinearInterpolator()
                    it.repeatCount = ValueAnimator.INFINITE
                    it.repeatMode = ValueAnimator.RESTART
                    it.startDelay = 1000
                    it
                }
            animator?.start()
        }
    }
}