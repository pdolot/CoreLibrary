package com.dolotdev.customViews.roundedView

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.View
import com.dolotdev.customViews.R
import com.dolotdev.utils.path.Path
import com.dolotdev.utils.view.Bound

open class SimpleRoundedView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    protected val viewStrokePaint: Paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    protected val viewPaint: Paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        setLayerType(LAYER_TYPE_HARDWARE, this)
    }

    var strokeColor: Int = Color.BLACK
        set(value) {
            field = value
            viewStrokePaint.color = value
            invalidate()
        }

    private var strokeOffset: Float = 0f

    var strokeWidth: Float = 0f
        set(value) {
            field = value
            strokeOffset = value / 2f
            viewStrokePaint.strokeWidth = value
            invalidate()
        }

    var bgColor: Int = Color.TRANSPARENT
        set(value) {
            field = value
            viewPaint.color = value
            invalidate()
        }

    var cornerRadius: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    var roundedCorners = ROUNDED_ALL
        set(value) {
            field = value
            binaryRoundedCorners = field.toString(2).padStart(4, '0')
        }

    private var binaryRoundedCorners = "0000"

    var shadowPadding = WITHOUT_SHADOW_PADDING
        set(value) {
            field = value
            binaryShadowPadding = field.toString(2).padStart(4, '0')
        }

    private var binaryShadowPadding = "0000"

    var shadowColor = Color.TRANSPARENT
        private set
    var shadowRadius = 0f
        private set
    var shadowDx = 0f
        private set
    var shadowDy = 0f
        private set


    private var viewBound = Bound()
    private var drawBound = Bound()
    private val viewPath = Path()

    init {
        initAttrs(context, attrs, defStyleAttr)
        if (shadowColor != Color.TRANSPARENT)
            viewPaint.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor)
    }

    private fun initAttrs(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.SimpleRoundedView, defStyleAttr, 0)
        shadowColor = a.getColor(R.styleable.SimpleRoundedView_cv_shadowColor, Color.TRANSPARENT)
        shadowRadius = a.getDimension(R.styleable.SimpleRoundedView_cv_shadowRadius, 0f)
        shadowDx = a.getInteger(R.styleable.SimpleRoundedView_cv_shadowDx, 0).toFloat()
        shadowDy = a.getInteger(R.styleable.SimpleRoundedView_cv_shadowDy, 0).toFloat()
        roundedCorners = a.getInteger(R.styleable.SimpleRoundedView_cv_roundedCorners, WITHOUT_ROUNDING)
        shadowPadding = a.getInteger(R.styleable.SimpleRoundedView_cv_includeShadowPadding, WITHOUT_SHADOW_PADDING)
        cornerRadius = a.getDimension(R.styleable.SimpleRoundedView_cv_cornerRadius, 0f)
        bgColor = a.getColor(R.styleable.SimpleRoundedView_cv_backgroundColor, Color.TRANSPARENT)
        strokeColor = a.getColor(R.styleable.SimpleRoundedView_cv_strokeColor, Color.TRANSPARENT)
        strokeWidth = a.getDimension(R.styleable.SimpleRoundedView_cv_strokeWidth, 0f)
        a.recycle()
    }

    fun setShadow(shadowColor: Int, shadowRadius: Float, shadowDx: Float, shadowDy: Float) {
        this.shadowColor = shadowColor
        this.shadowRadius = shadowRadius
        this.shadowDx = shadowDx
        this.shadowDy = shadowDy
        viewPaint.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewBound = Bound(0, 0, measuredWidth, measuredHeight)
        drawBound = Bound(paddingLeft, paddingTop, w - paddingRight, h - paddingBottom)
        if (includePaddingLeft())
            drawBound.left += getShadowPaddingLeft().toInt()
        if (includePaddingTop())
            drawBound.top += getShadowPaddingTop().toInt()
        if (includePaddingRight())
            drawBound.right -= getShadowPaddingRight().toInt()
        if (includePaddingBottom())
            drawBound.bottom -= getShadowPaddingBottom().toInt()
        getPath()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.let { drawView(it) }
    }

    open fun drawView(canvas: Canvas) {
        canvas.drawPath(viewPath, viewPaint)
        if (strokeWidth > 0f)
            canvas.drawPath(viewPath, viewStrokePaint)
    }

    private fun getPath() {
        val bound = Bound(
            drawBound.left + strokeOffset.toInt(),
            drawBound.top + strokeOffset.toInt(),
            drawBound.right - strokeOffset.toInt(),
            drawBound.bottom - strokeOffset.toInt())

        viewPath.reset()

        if (isRoundedTopLeft()) {
            viewPath.moveTo(bound.left.toFloat(), bound.top + cornerRadius)
            viewPath.arcTo(
                RectF(
                    bound.left.toFloat(), bound.top.toFloat(),
                    bound.left + cornerRadius * 2, bound.top + cornerRadius * 2
                ),
                180f, 90f
            )
        } else {
            viewPath.moveTo(bound.left.toFloat(), bound.top.toFloat())
        }

        if (isRoundedTopRight()) {
            viewPath.lineTo(bound.right - cornerRadius, bound.top.toFloat())
            viewPath.arcTo(
                RectF(
                    bound.right.toFloat() - cornerRadius * 2, bound.top.toFloat(),
                    bound.right.toFloat(), bound.top + cornerRadius * 2
                ),
                270f, 90f
            )
        } else {
            viewPath.lineTo(bound.right.toFloat(), bound.top.toFloat())
        }

        if (isRoundedBottomRight()) {
            viewPath.lineTo(bound.right.toFloat(), bound.bottom - cornerRadius)
            viewPath.arcTo(
                RectF(
                    bound.right.toFloat() - cornerRadius * 2, bound.bottom.toFloat() - cornerRadius * 2,
                    bound.right.toFloat(), bound.bottom.toFloat()
                ),
                0f, 90f
            )
        } else {
            viewPath.lineTo(bound.right.toFloat(), bound.bottom.toFloat())
        }

        if (isRoundedBottomLeft()) {
            viewPath.lineTo(bound.left + cornerRadius, bound.bottom.toFloat())
            viewPath.arcTo(
                RectF(
                    bound.left.toFloat(), bound.bottom.toFloat() - cornerRadius * 2,
                    bound.left.toFloat() + cornerRadius * 2, bound.bottom.toFloat()
                ),
                90f, 90f
            )
        } else {
            viewPath.lineTo(bound.left.toFloat(), bound.bottom.toFloat())
        }

        viewPath.close()
    }

    private fun isRoundedTopLeft() = binaryRoundedCorners[3] == '1'
    private fun isRoundedTopRight() = binaryRoundedCorners[2] == '1'
    private fun isRoundedBottomRight() = binaryRoundedCorners[1] == '1'
    private fun isRoundedBottomLeft() = binaryRoundedCorners[0] == '1'

    private fun includePaddingLeft() = binaryShadowPadding[3] == '1'
    private fun includePaddingTop() = binaryShadowPadding[2] == '1'
    private fun includePaddingRight() = binaryShadowPadding[1] == '1'
    private fun includePaddingBottom() = binaryShadowPadding[0] == '1'

    fun getShadowPaddingLeft() = shadowRadius * 1.25f - shadowDx
    fun getShadowPaddingRight() = shadowRadius * 1.25f + shadowDx
    fun getShadowPaddingTop() = shadowRadius * 1.25f - shadowDy
    fun getShadowPaddingBottom() = shadowRadius * 1.25f + shadowDy

    companion object {
        const val WITHOUT_ROUNDING = 0
        const val ROUNDED_TOP_LEFT: Int = 1
        const val ROUNDED_TOP_RIGHT: Int = 2
        const val ROUNDED_BOTTOM_RIGHT: Int = 4
        const val ROUNDED_BOTTOM_LEFT: Int = 8
        const val ROUNDED_ALL: Int = 15

        const val WITHOUT_SHADOW_PADDING = 0
        const val SHADOW_PADDING_LEFT: Int = 1
        const val SHADOW_PADDING_TOP: Int = 2
        const val SHADOW_PADDING_BOTTOM_RIGHT: Int = 4
        const val SHADOW_PADDING_BOTTOM: Int = 8
        const val SHADOW_PADDING_ALL: Int = 15
    }
}