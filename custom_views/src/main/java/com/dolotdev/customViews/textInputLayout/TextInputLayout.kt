package com.dolotdev.customViews.textInputLayout

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.*
import android.text.method.PasswordTransformationMethod
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.children
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.TextViewCompat
import com.dolotdev.customViews.R
import com.dolotdev.customViews.roundedView.RoundedView
import com.dolotdev.customViews.textInputLayout.action.TextEventListener
import com.dolotdev.utils.extension.isHeightWrapContent
import com.dolotdev.utils.extension.layout
import com.dolotdev.utils.extension.pxToSp
import com.dolotdev.utils.view.Bound
import com.dolotdev.utils.view.Size
import kotlin.math.max
import kotlin.math.min

class TextInputLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr), TextWatcher,
    TextInputEditText.TextInputEditTextListener, TextEventListener {

    internal var textInputLayoutType: TextInputLayoutType = TextInputLayoutType.FILLED
    internal var hintBaseline: TextBaseline = TextBaseline.INSIDE

    // listeners
    private var clickListener: ClickListener? = null

    // views
    internal var input: TextInputEditText? = null
    internal var hintTextView: TextView? = null
    private var roundedView: RoundedView = RoundedView(context).apply {
        id = View.generateViewId()
        setPadding(0, 0, 0, 0)
    }

    internal var helperTextView: TextView? = null
    internal var letterCounterTextView: TextView? = null

    internal var suffixTextView: TextView? = null
    internal var prefixTextView: TextView? = null

    private var leftIcon: ImageView? = null
    private var rightIcon: ImageView? = null

    // view bounds
    private var drawBound = Bound()
    private var roundedViewBound = Bound()
    private var innerViewsBound = Bound()
    private var inputBound = Bound()
    private var hintBound = Bound()
    private var hintMinimizedBound = Bound()
    private var suffixBound = Bound()
    private var prefixBound = Bound()
    private var leftIconBound = Bound()
    private var rightIconBound = Bound()
    private var helperTextBound = Bound()
    private var letterCounterBound = Bound()

    // drawables
    internal var leftIconResId: Int = 0
    internal var rightIconResId: Int = 0
    internal var leftIconDrawable: Drawable? = null
    internal var rightIconDrawable: Drawable? = null

    // colors
    internal var leftIconColor: Int = 0
    internal var rightIconColor: Int = 0
    internal var activeColor: Int = 0
    internal var inactiveColor: Int = 0
    internal var errorColor: Int = 0
    internal var successColor: Int = 0

    // input padding
    private var inputPaddingTop = 0
    private var inputPaddingStart = 0
    private var inputPaddingBottom = 0
    private var inputPaddingEnd = 0

    // view inner padding
    internal var innerPaddingTop = 0
    internal var innerPaddingStart = 0
    internal var innerPaddingBottom = 0
    internal var innerPaddingEnd = 0

    // sizes
    private var inputTextViewSize: Size = Size()
    private var minHintTextViewSize: Size = Size()
    private var hintTextViewSize: Size = Size()
    private var prefixTextViewSize: Size = Size()
    private var suffixTextViewSize: Size = Size()
    private var helperTextViewSize: Size = Size()
    private var letterCounterTextViewSize: Size = Size()
    private var textHeight = 0
    private var iconSize: Size = Size()

    // text sizes
    private var inputTextSize = 0f
    private var hintTextSize = 0f

    // flags
    private val editTextExist: Boolean
        get() = input != null

    private val bottomTextsEnabled: Boolean
        get() = helperTextView != null || letterCounterTextView != null

    private val leftIconExist: Boolean
        get() = leftIconDrawable != null || leftIconResId != 0

    private val rightIconExist: Boolean
        get() = rightIconDrawable != null || rightIconResId != 0

    internal var helperTextEnabled: Boolean = false
    internal var letterCounterEnabled: Boolean = false

    private val isHintMinimized
        get() = !input?.text.isNullOrBlank()

    internal var hideRightIconOnRemoveFocus = false
    internal var enableDefaultRightIconBehaviorForInputText = false
    internal var enableDefaultRightIconBehaviorForInputPassword = true
    private var isPasswordVisible = false
    internal var isEditable = true

    // strings
    var hint: String = ""
        set(value) {
            field = value
            hintTextView?.text = field
        }
    var text: String = ""
        set(value) {
            field = value
            input?.setText(field)
        }
        get() = input?.text ?: ""

    var prefix: String? = null
        set(value) {
            field = value
            prefixTextView?.text = value
        }
    var suffix: String? = null
        set(value) {
            field = value
            suffixTextView?.text = value
        }
    var helperText: String? = null
        set(value) {
            field = value
            helperTextView?.text = value
        }

    // text appearances
    private var hintTextAppearance: Int = 0
    private var letterCounterTextAppearance: Int = 0
    private var helperTextAppearance: Int = 0
    private var suffixTextAppearance: Int = 0
    private var prefixTextAppearance: Int = 0

    internal var inputMaxLength: Int = 0
    private var inputType: Int = InputType.TYPE_NULL
    private var animationDuration: Long = 200L

    private var isViewAnimating = false

    internal var viewsVerticalSpace = 2
    internal var viewsHorizontalSpace = 4

    private var visibleOnIcon = R.drawable.ic_visibility_on
    private var visibleOffIcon = R.drawable.ic_visibility_off
    private var clearTextIcon = R.drawable.ic_clear_text

    // rounded view attrs
    internal var borders: Int = 15
    internal var roundedCorners: Int = 15
    internal var bgColor: Int = Color.TRANSPARENT
    internal var strokeColor: Int = Color.BLACK
    internal var strokeWidth: Float = 3f
    internal var strokeLineCap: Paint.Cap = Paint.Cap.BUTT
    internal var strokeLineJoin: Paint.Join = Paint.Join.MITER
    internal var strokeLineMiter: Float = 4f
    internal var roundedViewStrokeColor: Int = 0
    internal var outlineCornerRadius = 0

    init {
        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.TextInputLayout,
            defStyleAttr,
            R.style.TextInputLayoutStyle
        )

        hint = a.getString(R.styleable.TextInputLayout_cv_hint) ?: ""
        isEditable = a.getBoolean(R.styleable.TextInputLayout_cv_editable, true)

        leftIconResId = a.getResourceId(R.styleable.TextInputLayout_cv_leftIconDrawable, 0)
        rightIconResId = a.getResourceId(R.styleable.TextInputLayout_cv_rightIconDrawable, 0)
        leftIconColor = a.getColor(R.styleable.TextInputLayout_cv_leftIconColor, 0)
        rightIconColor = a.getColor(R.styleable.TextInputLayout_cv_rightIconColor, 0)
        activeColor = a.getColor(R.styleable.TextInputLayout_cv_activeColor, 0)
        inactiveColor = a.getColor(R.styleable.TextInputLayout_cv_inactiveColor, 0)
        successColor = a.getColor(R.styleable.TextInputLayout_cv_successColor, 0)
        errorColor = a.getColor(R.styleable.TextInputLayout_cv_errorColor, 0)

        prefix = a.getString(R.styleable.TextInputLayout_cv_prefix)
        suffix = a.getString(R.styleable.TextInputLayout_cv_suffix)

        hintTextAppearance = a.getResourceId(
            R.styleable.TextInputLayout_cv_hintTextAppearance,
            R.style.HintTextAppearance
        )
        letterCounterTextAppearance = a.getResourceId(
            R.styleable.TextInputLayout_cv_letterCountTextAppearance,
            R.style.LetterCountTextAppearance
        )
        helperTextAppearance = a.getResourceId(
            R.styleable.TextInputLayout_cv_helperTextAppearance,
            R.style.HelperTextAppearance
        )
        suffixTextAppearance = a.getResourceId(
            R.styleable.TextInputLayout_cv_suffixTextAppearance,
            R.style.SuffixTextAppearance
        )
        prefixTextAppearance = a.getResourceId(
            R.styleable.TextInputLayout_cv_prefixTextAppearance,
            R.style.PrefixTextAppearance
        )

        letterCounterEnabled =
            a.getBoolean(R.styleable.TextInputLayout_cv_letterCounterEnabled, false)
        helperTextEnabled = a.getBoolean(R.styleable.TextInputLayout_cv_helperTextEnabled, false)

        hideRightIconOnRemoveFocus =
            a.getBoolean(R.styleable.TextInputLayout_cv_hideRightIconOnRemoveFocus, false)
        enableDefaultRightIconBehaviorForInputText =
            a.getBoolean(
                R.styleable.TextInputLayout_cv_enableDefaultRightIconBehaviorForInputText,
                false
            )
        enableDefaultRightIconBehaviorForInputPassword =
            a.getBoolean(
                R.styleable.TextInputLayout_cv_enableDefaultRightIconBehaviorForInputPassword,
                true
            )

        helperText = a.getString(R.styleable.TextInputLayout_cv_helperText)

        textInputLayoutType = TextInputLayoutType.values()[a.getInteger(
            R.styleable.TextInputLayout_cv_inputLayoutType,
            0
        )]
        hintBaseline = if (textInputLayoutType == TextInputLayoutType.FILLED) {
            TextBaseline.INLINE
        } else {
            TextBaseline.INSIDE
        }

        hintBaseline = TextBaseline.values()[a.getInteger(
            R.styleable.TextInputLayout_cv_hintBaseline,
            TextBaseline.valueOf(hintBaseline.name).ordinal
        )]

        innerPaddingTop = a.getDimensionPixelSize(R.styleable.TextInputLayout_cv_innerPaddingTop, 0)
        innerPaddingStart =
            a.getDimensionPixelSize(R.styleable.TextInputLayout_cv_innerPaddingStart, 0)
        innerPaddingBottom =
            a.getDimensionPixelSize(R.styleable.TextInputLayout_cv_innerPaddingBottom, 0)
        innerPaddingEnd = a.getDimensionPixelSize(R.styleable.TextInputLayout_cv_innerPaddingEnd, 0)

        viewsHorizontalSpace =
            a.getDimensionPixelSize(R.styleable.TextInputLayout_cv_viewsHorizontalSpace, 2)
        viewsVerticalSpace =
            a.getDimensionPixelSize(R.styleable.TextInputLayout_cv_viewsVerticalSpace, 4)

        val roundedViewAttrs = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.RoundedView,
            defStyleAttr,
            R.style.TextInputLayoutStyle
        )

        outlineCornerRadius =
            roundedViewAttrs.getDimensionPixelSize(R.styleable.RoundedView_cv_cornerRadius, 0)
        borders = roundedViewAttrs.getInteger(R.styleable.RoundedView_cv_border, 15)
        roundedCorners = roundedViewAttrs.getInteger(R.styleable.RoundedView_cv_roundedCorners, 15)
        bgColor =
            roundedViewAttrs.getColor(R.styleable.RoundedView_cv_backgroundColor, Color.TRANSPARENT)
        roundedViewStrokeColor =
            roundedViewAttrs.getColor(R.styleable.RoundedView_cv_strokeColor, Color.BLACK)
        strokeColor = roundedViewStrokeColor
        strokeWidth =
            roundedViewAttrs.getDimensionPixelSize(R.styleable.RoundedView_cv_strokeWidth, 3)
                .toFloat()
        strokeLineCap = Paint.Cap.values()[roundedViewAttrs.getInteger(
            R.styleable.RoundedView_cv_strokeLineCap,
            0
        )]
        strokeLineJoin = Paint.Join.values()[roundedViewAttrs.getInteger(
            R.styleable.RoundedView_cv_strokeLineJoin,
            0
        )]
        strokeLineMiter = roundedViewAttrs.getFloat(R.styleable.RoundedView_cv_strokeLineMiter, 4f)

        roundedViewAttrs.recycle()
        a.recycle()

    }

    @Suppress("unused")
    fun setLeftIconResId(resId: Int){
        leftIconResId = resId
        leftIcon?.setImageResource(resId)
    }
    @Suppress("unused")
    fun setRightIconResId(resId: Int){
        rightIconResId = resId
        rightIcon?.setImageResource(resId)
    }
    @Suppress("unused")
    fun setLeftIconDrawable(drawable: Drawable){
        leftIconDrawable = drawable
        leftIcon?.setImageDrawable(drawable)
    }
    @Suppress("unused")
    fun setRightIconDrawable(drawable: Drawable){
        rightIconDrawable = drawable
        rightIcon?.setImageDrawable(drawable)
    }

    private fun moveHint(animation: Boolean) {
        if (hint.isEmpty())
            return
        if (hintBaseline == TextBaseline.INSIDE) {
            hintTextView?.layout(hintMinimizedBound)
            return
        }
        val minTextSize = hintTextSize
        val maxTextSize = inputTextSize
        if (!animation) {
            if (input?.text.isNullOrBlank()) {
                hintTextView?.textSize = maxTextSize
                hintTextView?.layout(hintBound)
                if (hintBaseline == TextBaseline.INLINE) {
                    resetBorderTop()
                }
            } else {
                hintTextView?.textSize = hintTextSize
                hintTextView?.layout(hintMinimizedBound)
                if (hintBaseline == TextBaseline.INLINE) {
                    trimBorderTop()
                }
            }
        } else {
            if (input?.isFocused == true && !isHintMinimized) {
                getAnimator(maxTextSize, minTextSize, 1, 0) {
                    if (hintBaseline == TextBaseline.INLINE) {
                        trimBorderTop()
                    }
                }.start()

            } else if (input?.isFocused == false && input?.text.isNullOrBlank()) {
                getAnimator(minTextSize, maxTextSize, 0, 1) {
                    if (hintBaseline == TextBaseline.INLINE) {
                        resetBorderTop()
                    }

                }.start()
            }
        }
    }

    private fun trimBorderTop() {
        if (roundedView.borders == RoundedView.BORDER_ALL || roundedView.borders == RoundedView.BORDER_TOP) {
            val trimStart = hintMinimizedBound.left - outlineCornerRadius - viewsHorizontalSpace
            val borderLength =
                roundedViewBound.right - roundedViewBound.left - outlineCornerRadius * 2
            val trimEnd = borderLength - (hintMinimizedBound.right - outlineCornerRadius)
            roundedView.isTrimPathTopReversed = true
            roundedView.trimPathStartTop = if (trimStart.toFloat() < 0f) 0f else trimStart.toFloat()
            roundedView.trimPathEndTop =
                if (trimEnd.toFloat() > borderLength.toFloat()) borderLength.toFloat() else trimEnd.toFloat()
        }
    }

    private fun resetBorderTop() {
        if (roundedView.borders == RoundedView.BORDER_ALL || roundedView.borders == RoundedView.BORDER_TOP) {
            roundedView.isTrimPathTopReversed = false
            roundedView.trimPathStartTop = 0f
            roundedView.trimPathEndTop = 0f
        }
    }

    private fun getAnimator(
        fromValueF: Float,
        toValueF: Float,
        fromValue: Int,
        toValue: Int,
        onAnimationEndFunc: () -> Unit = {}
    ): AnimatorSet {
        val textAnimator = ValueAnimator.ofFloat(fromValueF, toValueF)

        textAnimator.addUpdateListener {
            hintTextView?.textSize = it.animatedValue as Float
        }

        val layoutAnimator = ValueAnimator.ofInt(fromValue, toValue)
        layoutAnimator.addUpdateListener {
            val leftDiff = hintBound.left - hintMinimizedBound.left
            val topDiff = hintBound.top - hintMinimizedBound.top
            val rightDiff = hintBound.right - hintMinimizedBound.right
            val bottomDiff = hintBound.bottom - hintMinimizedBound.bottom
            val animatedValue = it.animatedValue as Int
            hintTextView?.layout(
                hintMinimizedBound.left + leftDiff * animatedValue,
                hintMinimizedBound.top + topDiff * animatedValue,
                hintMinimizedBound.right + rightDiff * animatedValue,
                hintMinimizedBound.bottom + bottomDiff * animatedValue,
            )
        }

        return AnimatorSet().apply {
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) {
                    isViewAnimating = true
                }

                override fun onAnimationEnd(animation: Animator?) {
                    isViewAnimating = false
                    onAnimationEndFunc.invoke()
                }

                override fun onAnimationCancel(animation: Animator?) {
                    isViewAnimating = false
                }

                override fun onAnimationRepeat(animation: Animator?) {
                    isViewAnimating = false
                }
            })
            playTogether(textAnimator, layoutAnimator)
            interpolator = LinearInterpolator()
            duration = animationDuration
        }
    }

    private fun getTextViewSize(textView: TextView?, newTextSize: Float): Size {
        var size = Size()
        textView?.apply {
            val prevTextSize = textSize
            textSize = newTextSize
            measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
            size = Size(measuredWidth, measuredHeight)
            textSize = pxToSp(context, prevTextSize)
        }
        return size
    }

    private fun findEditText() {
        if (!editTextExist) {
            children.toList().filterIsInstance<TextInputEditText>().takeIf { it.isNotEmpty() }
                ?.let {
                    input = it[0]
                    inputPaddingStart = input?.paddingLeft ?: 0
                    inputPaddingTop = input?.paddingTop ?: 0
                    inputPaddingEnd = input?.paddingRight ?: 0
                    inputPaddingBottom = input?.paddingBottom ?: 0
                    input?.setPadding(0, 0, 0, 0)
                    this@TextInputLayout.inputType = it[0].inputType - 1
                }
        }
    }

    // override methods

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        findEditText()
        removeAllViews()
        if (editTextExist) {
            addView(roundedView)
            addInputView()
            addHintTextView()
            addHelperTextView()
            addLetterCounterTextView()
            addPrefixTextView()
            addSuffixTextView()
            addLeftIconView()
            addRightIconView()
            initViews()
        } else {
            throw IllegalStateException("TextInputLayout must have EditText as child")
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        Log.v(TAG, "onMeasure")

        measureVerticalBounds()
        if (isHeightWrapContent()) {
            var newHeightMeasureSpec = heightMeasureSpec
            if (isHeightWrapContent()) {
                var height = paddingTop
                when (hintBaseline) {
                    TextBaseline.OUTSIDE, TextBaseline.INSIDE -> {
                        height += minHintTextViewSize.height
                        height += viewsVerticalSpace
                        height += outlineCornerRadius
                        height += outlineCornerRadius
                    }
                    TextBaseline.INLINE -> {
                        height += minHintTextViewSize.height / 2
                        height += max(outlineCornerRadius, minHintTextViewSize.height / 2)
                        height += max(outlineCornerRadius, minHintTextViewSize.height / 2)
                    }
                }

                height += innerPaddingTop
                height += viewsVerticalSpace
                height += inputPaddingTop
                height += inputTextViewSize.height
                height += inputPaddingBottom
                height += viewsVerticalSpace
                height += innerPaddingBottom

                if (helperTextEnabled || letterCounterEnabled) {
                    height += viewsVerticalSpace
                    height += max(helperTextViewSize.height, letterCounterTextViewSize.height)
                }
                height += paddingBottom

                newHeightMeasureSpec = MeasureSpec.EXACTLY + height
            }

            setMeasuredDimension(widthMeasureSpec, newHeightMeasureSpec)
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        Log.v(TAG, "onLayout: $changed")
        if (changed) {
            measureHorizontalBounds()
            input?.layout(inputBound)
            roundedView.measure(
                MeasureSpec.EXACTLY + (roundedViewBound.right - roundedViewBound.left),
                MeasureSpec.EXACTLY + (roundedViewBound.bottom - roundedViewBound.top)
            )
            roundedView.layout(roundedViewBound)
            if (input?.isFocused == false)
                moveHint(false)
            leftIcon?.layout(leftIconBound)
            rightIcon?.layout(rightIconBound)
            prefixTextView?.layout(prefixBound)
            suffixTextView?.layout(suffixBound)
            helperTextView?.layout(helperTextBound)
            letterCounterTextView?.layout(letterCounterBound)
        }
    }

    // listeners

    override fun onSuccess(message: String?) {
        if (helperTextEnabled) {
            if (successColor != 0) {
                hintTextView?.setTextColor(successColor)
                helperTextView?.setTextColor(successColor)
                roundedView.strokeColor = successColor
            }
            if (message != null)
                helperTextView?.text = message

        }
    }

    override fun onError(message: String?) {
        if (helperTextEnabled) {
            if (errorColor != 0) {
                hintTextView?.setTextColor(errorColor)
                helperTextView?.setTextColor(errorColor)
                roundedView.strokeColor = errorColor
            }

            if (message != null)
                helperTextView?.text = message
        }
    }

    override fun onFocusChanged(focused: Boolean) {
        moveHint(true)
        if (focused) {
            setActiveColor()
            rightIcon?.visibility = View.VISIBLE
        } else {
            setNormalColor()
            if ((isPasswordInputType() && enableDefaultRightIconBehaviorForInputPassword)
                || enableDefaultRightIconBehaviorForInputText
            ) {
                if (text.isEmpty())
                    rightIcon?.visibility = View.INVISIBLE
                else
                    rightIcon?.visibility = View.VISIBLE
            }
            if (hideRightIconOnRemoveFocus)
                rightIcon?.visibility = View.INVISIBLE
        }
    }

    override fun onSetText(text: String) {
        this.text = text
        if (enableDefaultRightIconBehaviorForInputText) {
            if (text.isEmpty())
                rightIcon?.visibility = View.INVISIBLE
            else
                rightIcon?.visibility = View.VISIBLE
        }
    }

    override fun onSetHint(hint: String) {
        this.hint = hint
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        if (letterCounterEnabled) {
            s?.let { updateLetterCounter(it.toString()) }
        }
    }

    override fun afterTextChanged(s: Editable?) {}

    override fun onDetachedFromWindow() {
        input?.removeTextChangedListener(this)
        super.onDetachedFromWindow()
    }

    fun setOnClickListener(clickListener: ClickListener) {
        this.clickListener = clickListener
    }

    @Suppress("unused")
    inline fun setOnClickListener(
        crossinline onLeftIconClick: () -> Unit,
        crossinline onRightIconClick: () -> Unit
    ) {
        setOnClickListener(object : ClickListener {
            override fun onLeftIconClick() {
                onLeftIconClick()
            }

            override fun onRightIconClick() {
                onRightIconClick()
            }
        })
    }

    private fun updateLetterCounter(text: String?) {
        val length = text?.length ?: 0
        letterCounterTextView?.text = context.getString(R.string.textLength, length, inputMaxLength)
    }

    private fun changeTransformationMethod() {
        input?.transformationMethod = if (!isPasswordVisible) {
            PasswordTransformationMethod()
        } else {
            null
        }
    }

    private fun isPasswordInputType() = inputType == InputType.TYPE_NUMBER_VARIATION_PASSWORD
            || inputType == InputType.TYPE_TEXT_VARIATION_PASSWORD
            || inputType == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD

    // set colors
    private fun setActiveColor() {
        if (activeColor != 0) {
            hintTextView?.setTextColor(activeColor)
            roundedView.strokeColor = activeColor
            helperTextView?.let {
                TextViewCompat.setTextAppearance(it, helperTextAppearance)
                it.text = helperText
            }
        }
    }

    private fun setInactiveColor() {
        if (inactiveColor != 0) {
            hintTextView?.setTextColor(inactiveColor)
            roundedView.strokeColor = inactiveColor
            roundedView.strokeColor = inactiveColor
            input?.setTextColor(inactiveColor)
            helperTextView?.setTextColor(inactiveColor)
            letterCounterTextView?.setTextColor(inactiveColor)
        }
    }

    private fun setNormalColor() {
        hintTextView?.let { TextViewCompat.setTextAppearance(it, hintTextAppearance) }
        helperTextView?.let {
            TextViewCompat.setTextAppearance(it, helperTextAppearance)
            it.text = helperText
        }
        roundedView.strokeColor = roundedViewStrokeColor
    }

    // addViews

    private fun addInputView() {
        input?.apply {
            if (hintBaseline != TextBaseline.INSIDE || this@TextInputLayout.hint.isEmpty())
                this@TextInputLayout.hint = input?.hint ?: ""
            inputTextSize = pxToSp(context, textSize)
        }
        addView(input)
    }

    private fun addHintTextView() {
        if (hintTextView == null) {
            hintTextView = TextView(context).apply {
                id = View.generateViewId()
                TextViewCompat.setTextAppearance(this, hintTextAppearance)
                setPadding(0, 0, 0, 0)
                input?.includeFontPadding?.let { includeFontPadding = it }
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                hintTextSize = pxToSp(context, textSize)
            }
        }
        addView(hintTextView)
    }

    private fun addHelperTextView() {
        if (helperTextEnabled) {
            if (helperTextView == null) {
                helperTextView = TextView(context).apply {
                    id = View.generateViewId()
                    TextViewCompat.setTextAppearance(this, helperTextAppearance)
                    setPadding(0, 0, 0, 0)
                }
            }
            addView(helperTextView)
        }
    }

    private fun addLetterCounterTextView() {
        if (letterCounterEnabled) {
            if (letterCounterTextView == null) {
                letterCounterTextView = TextView(context).apply {
                    id = View.generateViewId()
                    TextViewCompat.setTextAppearance(this, letterCounterTextAppearance)
                    setPadding(0, 0, 0, 0)
                    gravity = Gravity.END
                    textAlignment = TEXT_ALIGNMENT_TEXT_END
                }
            }
            addView(letterCounterTextView)
        }
    }

    private fun addPrefixTextView() {
        if (prefix != null) {
            if (prefixTextView == null) {
                prefixTextView = TextView(context).apply {
                    id = View.generateViewId()
                    TextViewCompat.setTextAppearance(this, prefixTextAppearance)
                    setPadding(0, 0, 0, 0)
                    includeFontPadding = input?.includeFontPadding ?: true
                    gravity = input?.gravity ?: Gravity.START
                    maxLines = 1
                }
            }
            addView(prefixTextView)
        }
    }

    private fun addSuffixTextView() {
        if (suffix != null) {
            if (suffixTextView == null) {
                suffixTextView = TextView(context).apply {
                    id = View.generateViewId()
                    TextViewCompat.setTextAppearance(this, prefixTextAppearance)
                    setPadding(0, 0, 0, 0)
                    includeFontPadding = input?.includeFontPadding ?: true
                    gravity = input?.gravity ?: Gravity.END
                    maxLines = 1
                }
            }
            addView(suffixTextView)
        }
    }

    private fun addLeftIconView() {
        if (leftIconExist) {
            if (leftIcon == null) {
                leftIcon = ImageView(context).apply {
                    id = generateViewId()
                }
            }
            addView(leftIcon)
        }
    }

    private fun addRightIconView() {
        if (rightIconExist
            || enableDefaultRightIconBehaviorForInputText
            || (enableDefaultRightIconBehaviorForInputPassword && isPasswordInputType())
        ) {
            if (rightIcon == null) {
                rightIcon = ImageView(context).apply {
                    id = generateViewId()
                }
                if (enableDefaultRightIconBehaviorForInputPassword && isPasswordInputType())
                    rightIconResId = visibleOnIcon
                else if (enableDefaultRightIconBehaviorForInputText)
                    rightIconResId = clearTextIcon
            }
            addView(rightIcon)
        }
    }

    // initViews
    private fun initViews() {
        initInput()
        initRoundedView()
        initInputMaxLength()
        initHintTextView()
        initHelperTextView()
        initLetterCounterTextView()
        initPrefixTextView()
        initSuffixTextView()
        initLeftIcon()
        initRightIcon()
        initIconSize()

        if (!isEditable)
            setInactiveColor()
    }

    private fun initInput() {
        input?.apply {
            background = null
            if (hintBaseline != TextBaseline.INSIDE)
                hint = ""

            isFocusable = isEditable
            isClickable = isEditable
            isFocusableInTouchMode = isEditable
        }
        input?.setTextInputEditTextListeners(this)
        input?.addTextChangedListener(this)
    }

    private fun initRoundedView() {
        roundedView.apply {
            cornerRadius = outlineCornerRadius.toFloat()
            borders = this@TextInputLayout.borders
            roundedCorners = this@TextInputLayout.roundedCorners
            if (textInputLayoutType == TextInputLayoutType.FILLED || textInputLayoutType == TextInputLayoutType.FILLED_OUTLINE)
                bgColor = this@TextInputLayout.bgColor

            if (textInputLayoutType == TextInputLayoutType.OUTLINE || textInputLayoutType == TextInputLayoutType.FILLED_OUTLINE) {
                strokeColor = this@TextInputLayout.roundedViewStrokeColor
                strokeWidth = this@TextInputLayout.strokeWidth
                strokeLineCap = this@TextInputLayout.strokeLineCap
                strokeLineJoin = this@TextInputLayout.strokeLineJoin
                strokeLineMiter = this@TextInputLayout.strokeLineMiter
            }
            roundedCorners = this@TextInputLayout.roundedCorners
        }
    }

    private fun initHintTextView() {
        hintTextView?.apply {
            text = this@TextInputLayout.hint
            minHintTextViewSize = getTextViewSize(hintTextView, hintTextSize)
            hintTextViewSize = getTextViewSize(hintTextView, inputTextSize)
            textHeight = hintTextViewSize.height
        }
    }

    private fun initHelperTextView() {
        helperTextView?.apply {
            text = helperText
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            helperTextViewSize = getTextViewSize(helperTextView, pxToSp(context, this.textSize))
        }
    }

    private fun initLetterCounterTextView() {
        letterCounterTextView?.let {
            if (inputMaxLength == 0) {
                removeView(it)
                letterCounterEnabled = false
            } else {
                it.apply {
                    updateLetterCounter(this@TextInputLayout.text)
                    layoutParams =
                        LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
                    letterCounterTextViewSize = getTextViewSize(this, pxToSp(context, textSize))
                }
            }
        }

    }

    private fun initPrefixTextView() {
        prefixTextView?.apply {
            textSize = inputTextSize
            text = prefix
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            prefixTextViewSize = getTextViewSize(this, inputTextSize)
        }
    }

    private fun initSuffixTextView() {
        suffixTextView?.apply {
            textSize = inputTextSize
            text = suffix
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            suffixTextViewSize = getTextViewSize(this, inputTextSize)
        }
    }

    private fun initLeftIcon() {
        leftIcon?.apply {
            leftIconDrawable?.let { setImageDrawable(it) } ?: setImageResource(leftIconResId)
            if (leftIconColor != 0)
                ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(leftIconColor))
            setOnClickListener {
                clickListener?.onLeftIconClick()
            }
        }
    }

    private fun initRightIcon() {
        rightIcon?.apply {
            if (enableDefaultRightIconBehaviorForInputPassword && isPasswordInputType()) {
                setImageResource(visibleOnIcon)
            } else if (enableDefaultRightIconBehaviorForInputText) {
                setImageResource(clearTextIcon)
            } else {
                rightIconDrawable?.let { setImageDrawable(it) } ?: setImageResource(rightIconResId)
            }

            if (rightIconColor != 0)
                ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(rightIconColor))

            if (enableDefaultRightIconBehaviorForInputPassword && isPasswordInputType()) {
                setOnClickListener {
                    clickListener?.onRightIconClick()
                    if (isPasswordVisible) {
                        setImageResource(visibleOnIcon)
                    } else {
                        setImageResource(visibleOffIcon)
                    }
                    isPasswordVisible = !isPasswordVisible
                    changeTransformationMethod()
                    input?.setSelection(input?.text?.length ?: 0)
                }
                visibility =
                    if (text.isBlank() || hideRightIconOnRemoveFocus) View.INVISIBLE else View.VISIBLE
            } else if (enableDefaultRightIconBehaviorForInputText) {
                setOnClickListener {
                    clickListener?.onRightIconClick()
                    text = ""
                    if (input?.isFocused == false) {
                        moveHint(true)
                        visibility = View.INVISIBLE
                    }
                    invalidate()
                }
                visibility =
                    if (text.isBlank() || hideRightIconOnRemoveFocus) View.INVISIBLE else View.VISIBLE
            } else {
                setOnClickListener {
                    clickListener?.onRightIconClick()
                }
                if (hideRightIconOnRemoveFocus)
                    visibility = View.INVISIBLE
            }
        }
    }

    private fun initIconSize() {
        val iconHeight = textHeight + (inputPaddingTop / 2) + (inputPaddingBottom / 2)
        iconSize = Size(iconHeight, iconHeight)
    }

    private fun initInputMaxLength() {
        input?.filters?.filterIsInstance<InputFilter.LengthFilter>()?.takeIf { it.isNotEmpty() }
            ?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    inputMaxLength = it[0].max
                }
            } ?: run {
            inputMaxLength = 0
        }
    }

    // initBounds

    private fun measureVerticalBounds() {
        measureVerticalDrawBound()
        measureVerticalRoundedViewBound()
        measureVerticalInnerViewsBound()
        measureVerticalHintMinimizedBound()
        measureVerticalLeftIconBound()
        measureVerticalRightIconBound()
        measureVerticalPrefixBound()
        measureVerticalSuffixBound()
        measureVerticalLetterCounterBound()
        measureVerticalHelperTextBound()
        measureVerticalInputBound()
    }

    private fun measureVerticalDrawBound() {
        drawBound.left = paddingLeft
        drawBound.right = measuredWidth - paddingRight - paddingLeft
    }

    private fun measureVerticalRoundedViewBound() {
        roundedViewBound.left = drawBound.left
        roundedViewBound.right = drawBound.right
    }

    private fun measureVerticalInnerViewsBound() {
        innerViewsBound.left = roundedViewBound.left + outlineCornerRadius + innerPaddingStart
        innerViewsBound.right = roundedViewBound.right - outlineCornerRadius - innerPaddingEnd
    }

    private fun measureVerticalHintMinimizedBound() {
        hintMinimizedBound.left = innerViewsBound.left + viewsHorizontalSpace
        val measuredMaxLeft = innerViewsBound.right - viewsHorizontalSpace
        val measuredLeft =
            hintMinimizedBound.left + minHintTextViewSize.width + viewsHorizontalSpace

        hintMinimizedBound.right = min(measuredLeft, measuredMaxLeft)
    }

    private fun measureVerticalLeftIconBound() {
        if (leftIconExist) {
            leftIconBound.left = innerViewsBound.left + viewsHorizontalSpace
            leftIconBound.right = leftIconBound.left + iconSize.width
        } else {
            leftIconBound.left = 0
            leftIconBound.right = 0
        }
    }

    private fun measureVerticalRightIconBound() {
        if (rightIconExist) {
            rightIconBound.right = innerViewsBound.right - viewsHorizontalSpace
            rightIconBound.left = rightIconBound.right - iconSize.width
        } else {
            rightIconBound.left = 0
            rightIconBound.right = 0
        }
    }

    private fun measureVerticalPrefixBound() {
        if (prefix != null) {
            if (leftIconExist) {
                prefixBound.left = leftIconBound.right + viewsHorizontalSpace + inputPaddingStart
            } else {
                prefixBound.left = innerViewsBound.left + viewsHorizontalSpace + inputPaddingStart
            }
            prefixBound.right = prefixBound.left + prefixTextViewSize.width
        } else {
            prefixBound.left = 0
            prefixBound.right = 0
        }
    }

    private fun measureVerticalSuffixBound() {
        if (suffix != null) {
            if (rightIconExist) {
                suffixBound.right = rightIconBound.left - viewsHorizontalSpace - inputPaddingEnd
            } else {
                suffixBound.right = innerViewsBound.right - viewsHorizontalSpace - inputPaddingEnd
            }
            suffixBound.left = suffixBound.right - suffixTextViewSize.width
        } else {
            suffixBound.left = 0
            suffixBound.right = 0
        }
    }

    private fun measureVerticalLetterCounterBound() {
        if (letterCounterEnabled) {
            letterCounterBound.right = innerViewsBound.right - viewsHorizontalSpace
            letterCounterBound.left =
                letterCounterBound.right - letterCounterTextViewSize.width - letterCounterTextViewSize.width

            val viewWidth = letterCounterBound.right - letterCounterBound.left
            letterCounterTextView?.apply {
                layoutParams = LayoutParams(viewWidth, LayoutParams.WRAP_CONTENT)
                measure(MeasureSpec.EXACTLY + viewWidth, MeasureSpec.UNSPECIFIED)
            }
        } else {
            letterCounterBound.left = 0
            letterCounterBound.right = 0
        }
    }

    private fun measureVerticalHelperTextBound() {
        if (helperTextEnabled) {
            helperTextBound.left = innerViewsBound.left + viewsHorizontalSpace
            if (letterCounterEnabled) {
                helperTextBound.right = letterCounterBound.left - viewsHorizontalSpace
            } else {
                helperTextBound.right = innerViewsBound.right - viewsHorizontalSpace
            }

            val viewWidth = helperTextBound.right - helperTextBound.left
            helperTextView?.apply {
                layoutParams = LayoutParams(viewWidth, LayoutParams.WRAP_CONTENT)
                measure(MeasureSpec.EXACTLY + viewWidth, MeasureSpec.UNSPECIFIED)
                helperTextViewSize = Size(this.measuredWidth, this.measuredHeight)
            }
        } else {
            helperTextBound.left = 0
            helperTextBound.right = 0
        }
    }

    private fun measureVerticalInputBound() {
        val left = if (prefix != null && leftIconExist) {
            prefixBound.right + viewsHorizontalSpace
        } else if (prefix != null && !leftIconExist) {
            prefixBound.right + viewsHorizontalSpace
        } else if (prefix == null && leftIconExist) {
            leftIconBound.right + viewsHorizontalSpace
        } else {
            innerViewsBound.left + viewsHorizontalSpace + inputPaddingStart
        }

        val right = if (suffix != null && rightIconExist) {
            suffixBound.left - viewsHorizontalSpace
        } else if (suffix != null && !rightIconExist) {
            suffixBound.left - viewsHorizontalSpace
        } else if (suffix == null && rightIconExist) {
            rightIconBound.left - viewsHorizontalSpace
        } else {
            innerViewsBound.right - viewsHorizontalSpace - inputPaddingEnd
        }

        inputBound.left = left + inputPaddingStart
        inputBound.right = right - inputPaddingEnd

        hintBound.left = inputBound.left
        hintBound.right = inputBound.right

        val viewWidth = inputBound.right - inputBound.left
        input?.apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            measure(MeasureSpec.EXACTLY + viewWidth, MeasureSpec.UNSPECIFIED)
            inputTextViewSize = Size(this.measuredWidth, this.measuredHeight)
        }
    }

    private fun measureHorizontalBounds() {
        measureHorizontalDrawBound()
        measureHorizontalRoundedViewBound()
        measureHorizontalInnerViewsBound()
        measureHorizontalHintMinimizedBound()
        measureHorizontalLeftIconBound()
        measureHorizontalRightIconBound()
        measureHorizontalPrefixBound()
        measureHorizontalSuffixBound()
        measureHorizontalInputBound()
        measureHorizontalLetterCounterBound()
        measureHorizontalHelperTextBound()
    }

    private fun measureHorizontalDrawBound() {
        drawBound.top = paddingTop
        drawBound.bottom = measuredHeight - paddingBottom
    }

    private fun measureHorizontalRoundedViewBound() {
        roundedViewBound.top = when (hintBaseline) {
            TextBaseline.OUTSIDE -> {
                drawBound.top + minHintTextViewSize.height + viewsVerticalSpace
            }
            TextBaseline.INLINE -> {
                drawBound.top + (minHintTextViewSize.height / 2)
            }
            TextBaseline.INSIDE -> {
                drawBound.top
            }
        }

        var bottomOffset = 0
        if (bottomTextsEnabled) {
            bottomOffset = max(
                helperTextViewSize.height,
                letterCounterTextViewSize.height
            ) + viewsVerticalSpace
        }
        roundedViewBound.bottom = drawBound.bottom - bottomOffset
    }

    private fun measureHorizontalInnerViewsBound() {
        when (hintBaseline) {
            TextBaseline.OUTSIDE, TextBaseline.INSIDE -> {
                innerViewsBound.top = roundedViewBound.top + outlineCornerRadius + innerPaddingTop
                innerViewsBound.bottom =
                    roundedViewBound.bottom - outlineCornerRadius - innerPaddingBottom
            }
            TextBaseline.INLINE -> {
                innerViewsBound.top = roundedViewBound.top + max(
                    outlineCornerRadius,
                    minHintTextViewSize.height / 2
                ) + innerPaddingTop
                innerViewsBound.bottom = roundedViewBound.bottom - max(
                    outlineCornerRadius,
                    minHintTextViewSize.height / 2
                ) - innerPaddingBottom
            }
        }
    }

    private fun measureHorizontalHintMinimizedBound() {
        hintMinimizedBound.top = when (hintBaseline) {
            TextBaseline.OUTSIDE, TextBaseline.INLINE -> {
                drawBound.top
            }
            TextBaseline.INSIDE -> {
                innerViewsBound.top
            }
        }
        hintMinimizedBound.bottom = hintMinimizedBound.top + minHintTextViewSize.height
    }

    private fun measureHorizontalLeftIconBound() {
        if (leftIconExist) {
            if (hintBaseline == TextBaseline.INSIDE) {
                leftIconBound.top = hintMinimizedBound.bottom + viewsVerticalSpace
            } else {
                leftIconBound.top = innerViewsBound.top + viewsVerticalSpace + (inputPaddingTop / 2)
            }
            leftIconBound.bottom = leftIconBound.top + iconSize.height
        }
    }

    private fun measureHorizontalRightIconBound() {
        if (rightIconExist) {
            if (hintBaseline == TextBaseline.INSIDE) {
                rightIconBound.top = hintMinimizedBound.bottom + viewsVerticalSpace
            } else {
                rightIconBound.top =
                    innerViewsBound.top + viewsVerticalSpace + (inputPaddingTop / 2)
            }
            rightIconBound.bottom = rightIconBound.top + iconSize.height
        }
    }

    private fun measureHorizontalPrefixBound() {
        if (prefix != null) {
            if (hintBaseline == TextBaseline.INSIDE) {
                prefixBound.top = hintMinimizedBound.bottom + viewsVerticalSpace
            } else {
                prefixBound.top = innerViewsBound.top + viewsVerticalSpace + inputPaddingTop
            }
            prefixBound.bottom = prefixBound.top + prefixTextViewSize.height
        }
    }

    private fun measureHorizontalSuffixBound() {
        if (suffix != null) {
            if (hintBaseline == TextBaseline.INSIDE) {
                suffixBound.top = hintMinimizedBound.bottom + viewsVerticalSpace
            } else {
                suffixBound.top = innerViewsBound.top + viewsVerticalSpace + inputPaddingTop
            }
            suffixBound.bottom = suffixBound.top + suffixTextViewSize.height
        }
    }

    private fun measureHorizontalInputBound() {
        input?.apply {
            if (hintBaseline == TextBaseline.INSIDE) {
                inputBound.top = hintMinimizedBound.bottom + viewsVerticalSpace
            } else {
                inputBound.top = innerViewsBound.top + viewsVerticalSpace + inputPaddingTop
            }
            inputBound.bottom = inputBound.top + inputTextViewSize.height
            hintBound.top = inputBound.top
            hintBound.bottom = inputBound.top + hintTextViewSize.height
        }
    }

    private fun measureHorizontalLetterCounterBound() {
        if (letterCounterEnabled) {
            letterCounterBound.top = roundedViewBound.bottom + viewsVerticalSpace
            letterCounterBound.bottom = letterCounterBound.top + letterCounterTextViewSize.height
        }
    }

    private fun measureHorizontalHelperTextBound() {
        if (helperTextEnabled) {
            helperTextBound.top = roundedViewBound.bottom + viewsVerticalSpace
            helperTextBound.bottom = helperTextBound.top + helperTextViewSize.height
        }
    }

    interface ClickListener {
        fun onLeftIconClick()
        fun onRightIconClick()
    }

    enum class TextInputLayoutType {
        FILLED,
        OUTLINE,
        FILLED_OUTLINE
    }

    enum class TextBaseline {
        OUTSIDE,
        INLINE,
        INSIDE
    }

    companion object {
        private val TAG: String = TextInputLayout::class.java.simpleName

        @Suppress("unused")
        class Builder(
            private val context: Context,
            private val input: TextInputEditText,
            private val textInputLayoutType: TextInputLayoutType,
            private val hintBaseline: TextBaseline
        ) {

            private var hintTextView: TextView? = null

            private var helperTextView: TextView? = null
            private var helperText: String? = null
            private var helperTextEnabled: Boolean = false

            private var letterCounterTextView: TextView? = null
            private var letterCounterEnabled: Boolean = false
            private var inputMaxLength: Int = 0

            private var prefixTextView: TextView? = null
            private var prefix: String? = null

            private var suffixTextView: TextView? = null
            private var suffix: String? = null

            private var leftIconResId: Int? = null
            private var leftIconDrawable: Drawable? = null

            private var rightIconResId: Int? = null
            private var rightIconDrawable: Drawable? = null

            private var leftIconColor: Int? = null
            private var rightIconColor: Int? = null

            private var activeColor: Int? = null
            private var inactiveColor: Int? = null
            private var errorColor: Int? = null
            private var successColor: Int? = null

            private var borders: Int? = null
            private var roundedCorners: Int? = null
            private var bgColor: Int? = null
            private var strokeColor: Int? = null
            private var strokeWidth: Float? = null
            private var strokeLineCap: Paint.Cap? = null
            private var strokeLineJoin: Paint.Join? = null
            private var strokeLineMiter: Float? = null
            private var roundedViewStrokeColor: Int? = null
            private var outlineCornerRadius: Int? = null

            private var innerPaddingTop = 0
            private var innerPaddingStart = 0
            private var innerPaddingBottom = 0
            private var innerPaddingEnd = 0

            private var hideRightIconOnRemoveFocus = false
            private var enableDefaultRightIconBehaviorForInputText = false
            private var enableDefaultRightIconBehaviorForInputPassword = true
            private var isEditable = true

            private var hint: String = ""
            private var text: String = ""

            private var viewsVerticalSpace = 2
            private var viewsHorizontalSpace = 4

            fun withHint(hint: String, hintTextView: TextView? = null): Builder {
                if (hintBaseline != TextBaseline.INSIDE)
                    this.input.hint = hint
                this.hint = hint
                this.hintTextView = hintTextView
                return this
            }

            fun withHelperText(helperText: String, helperTextView: TextView? = null): Builder {
                this.helperTextView = helperTextView
                this.helperTextEnabled = true
                this.helperText = helperText
                return this
            }

            fun withLetterCounterTextView(
                maxLength: Int,
                letterCounterTextView: TextView? = null
            ): Builder {
                this.inputMaxLength = maxLength
                this.letterCounterEnabled = true
                this.letterCounterTextView = letterCounterTextView
                this.input.filters.toMutableList().apply {
                    add(InputFilter.LengthFilter(maxLength))
                }
                return this
            }

            fun withPrefix(prefix: String, prefixTextView: TextView? = null): Builder {
                this.prefix = prefix
                this.prefixTextView = prefixTextView
                return this
            }

            fun withSuffix(suffix: String, suffixTextView: TextView? = null): Builder {
                this.suffix = suffix
                this.suffixTextView = suffixTextView
                return this
            }

            fun withLeftIconResId(resId: Int, color: Int? = null): Builder {
                this.leftIconResId = resId
                color?.let { this.leftIconColor = color }
                return this
            }

            fun withLeftIconDrawable(drawable: Drawable, color: Int? = null): Builder {
                this.leftIconDrawable = drawable
                color?.let { this.leftIconColor = color }
                return this
            }

            fun withRightIconResId(resId: Int, color: Int? = null): Builder {
                this.rightIconResId = resId
                color?.let { this.rightIconColor = color }
                return this
            }

            fun withRightIconDrawable(drawable: Drawable, color: Int? = null): Builder {
                this.rightIconDrawable = drawable
                color?.let { this.rightIconColor = color }
                return this
            }

            fun setColors(
                activeColor: Int? = null, inactiveColor: Int? = null,
                errorColor: Int? = null, successColor: Int? = null
            ): Builder {
                this.activeColor = activeColor
                this.inactiveColor = inactiveColor
                this.errorColor = errorColor
                this.successColor = successColor
                return this
            }

            fun setRoundedView(
                outlineCornerRadius: Int? = null, roundedViewStrokeColor: Int? = null,
                borders: Int? = null, roundedCorners: Int? = null,
                bgColor: Int? = null, strokeColor: Int? = null,
                strokeWidth: Float? = null, strokeLineCap: Paint.Cap? = null,
                strokeLineJoin: Paint.Join? = null, strokeLineMiter: Float? = null
            ): Builder {
                this.outlineCornerRadius = outlineCornerRadius
                this.roundedViewStrokeColor = roundedViewStrokeColor
                this.borders = borders
                this.roundedCorners = roundedCorners
                this.bgColor = bgColor
                this.strokeColor = strokeColor
                this.strokeWidth = strokeWidth
                this.strokeLineCap = strokeLineCap
                this.strokeLineJoin = strokeLineJoin
                this.strokeLineMiter = strokeLineMiter
                return this
            }

            fun addInnerPadding(left: Int, top: Int, right: Int, bottom: Int): Builder {
                this.innerPaddingStart = left
                this.innerPaddingTop = top
                this.innerPaddingEnd = right
                this.innerPaddingBottom = bottom
                return this
            }

            fun addInnerPadding(padding: Int): Builder {
                this.innerPaddingStart = padding
                this.innerPaddingTop = padding
                this.innerPaddingEnd = padding
                this.innerPaddingBottom = padding
                return this
            }

            fun hideRightIconOnRemoveFocus(): Builder {
                hideRightIconOnRemoveFocus = true
                return this
            }

            fun enableDefaultRightIconBehaviorForInputText(): Builder {
                enableDefaultRightIconBehaviorForInputText = true
                return this
            }

            fun disableDefaultRightIconBehaviorForInputText(): Builder {
                enableDefaultRightIconBehaviorForInputText = false
                return this
            }

            fun enableDefaultRightIconBehaviorForInputPassword(): Builder {
                enableDefaultRightIconBehaviorForInputPassword = true
                return this
            }

            fun disableDefaultRightIconBehaviorForInputPassword(): Builder {
                enableDefaultRightIconBehaviorForInputPassword = false
                return this
            }

            fun withText(text: String): Builder {
                this.text = text
                this.input.text = text
                return this
            }

            fun asStatic(): Builder {
                this.isEditable = false
                return this
            }

            fun setViewsVerticalSpace(space: Int): Builder {
                this.viewsVerticalSpace = space
                return this
            }

            fun setViewsHorizontalSpace(space: Int): Builder {
                this.viewsHorizontalSpace = space
                return this
            }

            fun build(): TextInputLayout {
                return TextInputLayout(context).apply {
                    this.input = this@Builder.input
                    this.text = this@Builder.text
                    this.isEditable = this@Builder.isEditable

                    this.textInputLayoutType = this@Builder.textInputLayoutType
                    this.hintBaseline = this@Builder.hintBaseline

                    this.hintTextView = this@Builder.hintTextView
                    this.hint = this@Builder.hint

                    this.helperTextView = this@Builder.helperTextView
                    this.helperText = this@Builder.helperText
                    this.helperTextEnabled = this@Builder.helperTextEnabled

                    this.letterCounterTextView = this@Builder.letterCounterTextView
                    this.letterCounterEnabled = this@Builder.letterCounterEnabled
                    this.inputMaxLength = this@Builder.inputMaxLength

                    this.prefixTextView = this@Builder.prefixTextView
                    this.prefix = this@Builder.prefix

                    this.suffixTextView = this@Builder.suffixTextView
                    this.suffix = this@Builder.suffix

                    this@Builder.leftIconResId?.let { this.leftIconResId = it }
                    this@Builder.leftIconDrawable?.let { this.leftIconDrawable = it }
                    this@Builder.rightIconResId?.let { this.rightIconResId = it }
                    this@Builder.rightIconDrawable?.let { this.rightIconDrawable = it }
                    this@Builder.leftIconColor?.let { this.leftIconColor = it }
                    this@Builder.rightIconColor?.let { this.rightIconColor = it }

                    this@Builder.activeColor?.let { this.activeColor = it }
                    this@Builder.inactiveColor?.let { this.inactiveColor = it }
                    this@Builder.errorColor?.let { this.errorColor = it }
                    this@Builder.successColor?.let { this.successColor = it }

                    this@Builder.borders?.let { this.borders = it }
                    this@Builder.roundedCorners?.let { this.roundedCorners = it }
                    this@Builder.bgColor?.let { this.bgColor = it }
                    this@Builder.strokeColor?.let { this.strokeColor = it }
                    this@Builder.strokeWidth?.let { this.strokeWidth = it }
                    this@Builder.strokeLineCap?.let { this.strokeLineCap = it }
                    this@Builder.strokeLineJoin?.let { this.strokeLineJoin = it }
                    this@Builder.strokeLineMiter?.let { this.strokeLineMiter = it }
                    this@Builder.roundedViewStrokeColor?.let { this.roundedViewStrokeColor = it }
                    this@Builder.outlineCornerRadius?.let { this.outlineCornerRadius = it }


                    this.innerPaddingTop = this@Builder.innerPaddingTop
                    this.innerPaddingStart = this@Builder.innerPaddingStart
                    this.innerPaddingBottom = this@Builder.innerPaddingBottom
                    this.innerPaddingEnd = this@Builder.innerPaddingEnd

                    this.hideRightIconOnRemoveFocus = this@Builder.hideRightIconOnRemoveFocus
                    this.enableDefaultRightIconBehaviorForInputText =
                        this@Builder.enableDefaultRightIconBehaviorForInputText
                    this.enableDefaultRightIconBehaviorForInputPassword =
                        this@Builder.enableDefaultRightIconBehaviorForInputPassword

                    this.viewsVerticalSpace = this@Builder.viewsVerticalSpace
                    this.viewsHorizontalSpace = this@Builder.viewsHorizontalSpace
                }
            }
        }
    }
}