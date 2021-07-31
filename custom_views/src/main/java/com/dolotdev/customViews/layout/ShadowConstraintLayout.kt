package com.dolotdev.customViews.layout

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.dolotdev.customViews.R
import com.dolotdev.customViews.roundedView.SimpleRoundedView

class ShadowConstraintLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    val shadowView = SimpleRoundedView(context)

    init {
        initAttrs(context, attrs, defStyleAttr)
        addView(shadowView)
    }


    private fun initAttrs(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.ShadowConstraintLayout, defStyleAttr, 0)
        val shadowColor = a.getColor(R.styleable.ShadowConstraintLayout_cv_shadowColor, Color.TRANSPARENT)
        val shadowRadius = a.getDimension(R.styleable.ShadowConstraintLayout_cv_shadowRadius, 0f)
        val shadowDx = a.getInteger(R.styleable.ShadowConstraintLayout_cv_shadowDx, 0).toFloat()
        val shadowDy = a.getInteger(R.styleable.ShadowConstraintLayout_cv_shadowDy, 0).toFloat()

        shadowView.setShadow(shadowColor, shadowRadius, shadowDx, shadowDy)
        shadowView.shadowPadding = SimpleRoundedView.SHADOW_PADDING_ALL
        shadowView.roundedCorners =
            a.getInteger(R.styleable.ShadowConstraintLayout_cv_roundedCorners, SimpleRoundedView.WITHOUT_ROUNDING)
        shadowView.cornerRadius = a.getDimension(R.styleable.ShadowConstraintLayout_cv_cornerRadius, 0f)
        shadowView.bgColor = a.getColor(R.styleable.ShadowConstraintLayout_cv_backgroundColor, Color.TRANSPARENT)
        shadowView.strokeColor = a.getColor(R.styleable.ShadowConstraintLayout_cv_strokeColor, Color.TRANSPARENT)
        shadowView.strokeWidth = a.getDimension(R.styleable.ShadowConstraintLayout_cv_strokeWidth, 0f)
        a.recycle()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        (parent as? ViewGroup)?.apply {
            clipChildren = false
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        shadowView.apply {
            layout(
                -getShadowPaddingLeft().toInt(),
                -getShadowPaddingTop().toInt(),
                right - left + getShadowPaddingRight().toInt(),
                bottom - top + getShadowPaddingBottom().toInt()
            )
        }
    }

    override fun drawChild(canvas: Canvas?, child: View?, drawingTime: Long): Boolean {

        return super.drawChild(canvas, child, drawingTime)
    }

}