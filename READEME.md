# MarqueeView
这是个快速且简便的实现跑马灯效果的View，目前Github上的大部分跑马灯的实现大概是两个方法。
- 1. 采用动画的形式，但是这样有几点缺陷，就是无法坐到前一个与后一个同时出现，而且也不能控制前后的间隔
- 2. 自定义View，重写绘制逻辑，手动实现跑马灯，这个方法虽然是最完美的解决方案，但是太过分复杂，比较耗时间。
-------------------- 
所以，回到最初的问题，实现一个跑马灯，需要一串文字滚动而已，这点完全能够通过动画来实现，剩下的还有一点便是需要前一串和后一串文字能够同时出现来避免穿帮，这一点在使用动画的实现上会有点难度，但是也不是不行。
实现了前后能同时显示，那肯定需要控制前后间隔，这个确实挺难处理的。
刚开始想到的是两个平行的View，但是同时控制两个View动画也是比较麻烦的。为什么不能是一个TextView放另外一段同样的文字，那间隔的设置完全能够通过设置空格来实现，比如：

**啊啊啊啊啊啊啊啊啊    啊啊啊啊啊啊啊啊啊**

然后类似一个窗口一样，每次都是扫一遍，这样实现的效果完全等价与跑马灯。
这样还剩下最后一个问题便是怎么让一个TextView的Text在超出本身大小后不截断或者省略。
第一个想到的便是ScrollView,于是使用HorizontalScrollView来尝试下：
继承HorizontalScrollView，添加一个TextView,然后设置一个比较长的文本，发现可行。
以下便是具体步骤：
0. 首先假设文本为“呜呜呜呜呜呜呜呜呜呜” 也就是10 * "呜"
1. 首先得到TextView的TextPaint，用于计算当前TextView样式下的空格rect大小，然后再用设置的GapWidth(两段前后间隔) / 单个空格的大小，这样得到来需要塞入的空格数（“ ”）。比如“首先假设文本为“呜呜呜呜呜呜呜呜呜呜””，设置的 gap / BlnakWidth 约等于 5,的变成“呜呜呜呜呜呜呜呜呜呜     呜呜呜呜呜呜呜呜呜呜”。
2. 对整个TextView进行translationX的动画，动画位移的距离应该等于“呜呜呜呜呜呜呜呜呜呜     ”的长度，这样当动画完成后，显示的是“呜呜呜呜呜呜呜呜呜呜”，这个时候重新开始动画。这样边实现了跑马灯的效果。

``` kotlin
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
```