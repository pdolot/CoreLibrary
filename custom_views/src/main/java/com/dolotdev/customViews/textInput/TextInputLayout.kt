package com.dolotdev.customViews.textInput

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.text.Editable
import android.text.InputFilter
import android.text.TextUtils
import android.text.TextWatcher
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
import com.dolotdev.customViews.textInput.action.TextEventListener
import com.dolotdev.customViews.util.Bound
import com.dolotdev.customViews.util.Size
import kotlin.math.max
import kotlin.math.min

class TextInputLayout @JvmOverloads constructor(
		context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr), TextWatcher, TextInputEditText.TextInputEditTextListener, TextEventListener {

	private var textInputLayoutType: TextInputLayoutType = TextInputLayoutType.FILLED
	private var hintBaseline: TextBaseline = TextBaseline.INSIDE

	// views
	private var input: TextInputEditText? = null
	private var hintTextView: TextView? = null
	private var roundedView: RoundedView = RoundedView(context).apply {
		id = View.generateViewId()
		setPadding(0, 0, 0, 0)
	}

	private var helperTextView: TextView? = null
	private var letterCounterTextView: TextView? = null

	private var suffixTextView: TextView? = null
	private var prefixTextView: TextView? = null

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
	private var leftIconResId: Int = 0
	private var rightIconResId: Int = 0

	// colors
	private var leftIconColor: Int = 0
	private var rightIconColor: Int = 0
	private var activeColor: Int = 0
	private var inactiveColor: Int = 0
	private var errorColor: Int = 0
	private var successColor: Int = 0
	private var roundedViewStrokeColor: Int = 0

	// input padding
	private var inputPaddingTop = 0
	private var inputPaddingStart = 0
	private var inputPaddingBottom = 0
	private var inputPaddingEnd = 0

	// view inner padding
	private var innerPaddingTop = 0
	private var innerPaddingStart = 0
	private var innerPaddingBottom = 0
	private var innerPaddingEnd = 0

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

	private var outlineCornerRadius = 0

	private val editTextExist: Boolean
		get() = input != null

	private val bottomTextsEnabled: Boolean
		get() = helperTextView != null || letterCounterTextView != null

	private var helperTextEnabled: Boolean = false
	private var letterCounterEnabled: Boolean = false

	private var inputHint: String = ""
	private val text: String
		get() = input?.text?.toString() ?: ""

	private var prefix: String? = null
	private var suffix: String? = null
	private var helperText: String? = null

	// text appearances
	private var hintTextAppearance: Int = 0
	private var letterCounterTextAppearance: Int = 0
	private var helperTextAppearance: Int = 0
	private var suffixTextAppearance: Int = 0
	private var prefixTextAppearance: Int = 0

	private var inputMaxLength: Int = 0
	private var animationDuration: Int = 200

	private var isViewAnimating = false
	private val isHintMinimized
		get() = !input?.text?.toString().isNullOrBlank()

	private var viewsVerticalSpace = 2
	private var viewsHorizontalSpace = 4

	init {
		val a = context.theme.obtainStyledAttributes(attrs, R.styleable.TextInputLayout, defStyleAttr, R.style.TextInputLayoutStyle)

		inputHint = a.getString(R.styleable.TextInputLayout_cv_hint) ?: ""

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

		hintTextAppearance = a.getResourceId(R.styleable.TextInputLayout_cv_hintTextAppearance, R.style.HintTextAppearance)
		letterCounterTextAppearance = a.getResourceId(R.styleable.TextInputLayout_cv_letterCountTextAppearance, R.style.LetterCountTextAppearance)
		helperTextAppearance = a.getResourceId(R.styleable.TextInputLayout_cv_helperTextAppearance, R.style.HelperTextAppearance)
		suffixTextAppearance = a.getResourceId(R.styleable.TextInputLayout_cv_suffixTextAppearance, R.style.SuffixTextAppearance)
		prefixTextAppearance = a.getResourceId(R.styleable.TextInputLayout_cv_prefixTextAppearance, R.style.PrefixTextAppearance)

		letterCounterEnabled = a.getBoolean(R.styleable.TextInputLayout_cv_letterCounterEnabled, false)
		helperTextEnabled = a.getBoolean(R.styleable.TextInputLayout_cv_helperTextEnabled, false)

		helperText = a.getString(R.styleable.TextInputLayout_cv_helperText)

		textInputLayoutType = TextInputLayoutType.values()[a.getInteger(R.styleable.TextInputLayout_cv_inputLayoutType, 0)]
		hintBaseline = if(textInputLayoutType == TextInputLayoutType.FILLED) {
			TextBaseline.INLINE
		} else {
			TextBaseline.INSIDE
		}

		hintBaseline = TextBaseline.values()[a.getInteger(R.styleable.TextInputLayout_cv_hintBaseline, TextBaseline.valueOf(hintBaseline.name).ordinal)]

		innerPaddingTop = a.getDimensionPixelSize(R.styleable.TextInputLayout_cv_innerPaddingTop, 0)
		innerPaddingStart = a.getDimensionPixelSize(R.styleable.TextInputLayout_cv_innerPaddingStart, 0)
		innerPaddingBottom = a.getDimensionPixelSize(R.styleable.TextInputLayout_cv_innerPaddingBottom, 0)
		innerPaddingEnd = a.getDimensionPixelSize(R.styleable.TextInputLayout_cv_innerPaddingEnd, 0)

		viewsHorizontalSpace = a.getDimensionPixelSize(R.styleable.TextInputLayout_cv_viewsHorizontalSpace, 2)
		viewsVerticalSpace = a.getDimensionPixelSize(R.styleable.TextInputLayout_cv_viewsVerticalSpace, 4)

		val roundedViewAttrs = context.theme.obtainStyledAttributes(attrs, R.styleable.RoundedView, defStyleAttr, R.style.TextInputLayoutStyle)

		outlineCornerRadius = roundedViewAttrs.getDimensionPixelSize(R.styleable.RoundedView_cv_cornerRadius, 0)
		roundedView.cornerRadius = outlineCornerRadius.toFloat()
		roundedView.borders = roundedViewAttrs.getInteger(R.styleable.RoundedView_cv_border, 15)
		roundedView.roundedCorners = roundedViewAttrs.getInteger(R.styleable.RoundedView_cv_roundedCorners, 15)

		if(textInputLayoutType == TextInputLayoutType.FILLED_OUTLINE || textInputLayoutType == TextInputLayoutType.FILLED) {
			roundedView.apply {
				bgColor = roundedViewAttrs.getColor(R.styleable.RoundedView_cv_backgroundColor, Color.TRANSPARENT)
			}
		}

		if(textInputLayoutType == TextInputLayoutType.FILLED_OUTLINE || textInputLayoutType == TextInputLayoutType.OUTLINE) {
			roundedView.apply {
				roundedViewStrokeColor = roundedViewAttrs.getColor(R.styleable.RoundedView_cv_strokeColor, Color.BLACK)
				strokeColor = roundedViewStrokeColor
				strokeWidth = roundedViewAttrs.getDimensionPixelSize(R.styleable.RoundedView_cv_strokeWidth, 3).toFloat()
				strokeLineCap = Paint.Cap.values()[roundedViewAttrs.getInteger(R.styleable.RoundedView_cv_strokeLineCap, 0)]
				strokeLineJoin = Paint.Join.values()[roundedViewAttrs.getInteger(R.styleable.RoundedView_cv_strokeLineJoin, 0)]
				strokeLineMiter = roundedViewAttrs.getFloat(R.styleable.RoundedView_cv_strokeLineMiter, 4f)
			}
		}
		roundedViewAttrs.recycle()
		a.recycle()

	}

	private fun moveHint(animation: Boolean) {
		if(hintBaseline == TextBaseline.INSIDE) {
			hintTextView?.layout(hintMinimizedBound)
			return
		}
		val minTextSize = hintTextSize
		val maxTextSize = inputTextSize
		if(!animation) {
			if(input?.text?.toString().isNullOrBlank()) {
				hintTextView?.textSize = maxTextSize
				hintTextView?.layout(hintBound)
				if(hintBaseline == TextBaseline.INLINE) {
					resetBorderTop()
				}
			} else {
				hintTextView?.textSize = hintTextSize
				hintTextView?.layout(hintMinimizedBound)
				if(hintBaseline == TextBaseline.INLINE) {
					trimBorderTop()
				}
			}
		} else {
			if(input?.isFocused == true && !isHintMinimized) {
				getAnimator(maxTextSize, minTextSize, 1, 0) {
					if(hintBaseline == TextBaseline.INLINE) {
						trimBorderTop()
					}
				}.start()

			} else if(input?.isFocused == false && input?.text?.toString().isNullOrBlank()) {
				getAnimator(minTextSize, maxTextSize, 0, 1) {
					if(hintBaseline == TextBaseline.INLINE) {
						resetBorderTop()
					}

				}.start()
			}
		}
	}

	private fun trimBorderTop() {
		if(roundedView.borders == RoundedView.BORDER_ALL || roundedView.borders == RoundedView.BORDER_TOP) {
			val trimStart = inputPaddingStart - viewsHorizontalSpace
			val borderLength = roundedViewBound.right - roundedViewBound.left - outlineCornerRadius * 2
			val trimEnd = borderLength - (trimStart + viewsHorizontalSpace + viewsHorizontalSpace
					+ (hintMinimizedBound.right - hintMinimizedBound.left) + viewsHorizontalSpace)
			roundedView.isTrimPathTopReversed = true
			roundedView.trimPathStartTop = if(trimStart.toFloat() < 0f) 0f else trimStart.toFloat()
			roundedView.trimPathEndTop = if(trimEnd.toFloat() > borderLength.toFloat()) borderLength.toFloat() else trimEnd.toFloat()
		}
	}

	private fun resetBorderTop() {
		if(roundedView.borders == RoundedView.BORDER_ALL || roundedView.borders == RoundedView.BORDER_TOP) {
			roundedView.isTrimPathTopReversed = false
			roundedView.trimPathStartTop = 0f
			roundedView.trimPathEndTop = 0f
		}
	}

	private fun getAnimator(fromValueF: Float, toValueF: Float, fromValue: Int, toValue: Int, onAnimationEndFunc: () -> Unit = {}): AnimatorSet {
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
			duration = 200
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
		if(!editTextExist) {
			children.toList().filterIsInstance<TextInputEditText>().takeIf { it.isNotEmpty() }?.let {
				input = it[0]
				inputPaddingStart = input?.paddingLeft ?: 0
				inputPaddingTop = input?.paddingTop ?: 0
				inputPaddingEnd = input?.paddingRight ?: 0
				inputPaddingBottom = input?.paddingBottom ?: 0
				input?.setPadding(0, 0, 0, 0)
			}
		}
	}

	// override methods

	override fun onAttachedToWindow() {
		super.onAttachedToWindow()
		findEditText()
		removeAllViews()
		if(editTextExist) {
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
		if(isHeightWrapContent()) {
			var newHeightMeasureSpec = heightMeasureSpec
			if(isHeightWrapContent()) {
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

				if(helperTextEnabled || letterCounterEnabled) {
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
		if(!isViewAnimating && changed) {
			measureHorizontalBounds()
			input?.layout(inputBound)
			roundedView.measure(
					MeasureSpec.EXACTLY + (roundedViewBound.right - roundedViewBound.left),
					MeasureSpec.EXACTLY + (roundedViewBound.bottom - roundedViewBound.top)
			)
			roundedView.layout(roundedViewBound)
			if(input?.isFocused == false)
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
		if(helperTextEnabled){
			hintTextView?.setTextColor(successColor)
			helperTextView?.setTextColor(successColor)
			if(message != null)
				helperTextView?.text = message
			roundedView.strokeColor = successColor
		}
	}

	override fun onError(message: String?) {
		if(helperTextEnabled){
			hintTextView?.setTextColor(errorColor)
			helperTextView?.setTextColor(errorColor)
			if(message != null)
				helperTextView?.text = message
			roundedView.strokeColor = errorColor
		}
	}

	override fun onFocusChanged(focused: Boolean) {
		moveHint(true)
		if(focused)
			setActiveColor()
		else
			setNormalColor()
	}

	override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

	override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
		if(letterCounterEnabled) {
			letterCounterTextView?.text = "${s?.length ?: 0}/$inputMaxLength"
		}
	}

	override fun afterTextChanged(s: Editable?) {}

	override fun onDetachedFromWindow() {
		input?.removeTextChangedListener(this)
		super.onDetachedFromWindow()
	}

	// set colors
	private fun setActiveColor() {
		hintTextView?.setTextColor(activeColor)
		roundedView.strokeColor = activeColor
		helperTextView?.let {
			TextViewCompat.setTextAppearance(it, helperTextAppearance)
			it.text = helperText
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
			if(hintBaseline != TextBaseline.INSIDE || inputHint.isEmpty())
				inputHint = input?.hint?.toString() ?: ""
			inputTextSize = pxToSp(context, textSize)
		}
		addView(input)
	}

	private fun addHintTextView() {
		if(hintTextView == null) {
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
		if(helperTextEnabled) {
			if(helperTextView == null) {
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
		if(letterCounterEnabled) {
			if(letterCounterTextView == null) {
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
		if(prefix != null) {
			if(prefixTextView == null) {
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
		if(suffix != null) {
			if(suffixTextView == null) {
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
		if(leftIconResId != 0) {
			if(leftIcon == null) {
				leftIcon = ImageView(context).apply {
					id = generateViewId()
				}
			}
			addView(leftIcon)
		}
	}

	private fun addRightIconView() {
		if(rightIconResId != 0) {
			if(rightIcon == null) {
				rightIcon = ImageView(context).apply {
					id = generateViewId()
				}
			}
			addView(rightIcon)
		}
	}

	// initViews
	private fun initViews() {
		initInput()
		initInputMaxLength()
		initHintTextView()
		initHelperTextView()
		initLetterCounterTextView()
		initPrefixTextView()
		initSuffixTextView()
		initLeftIcon()
		initRightIcon()
		initIconSize()
	}

	private fun initInput() {
		input?.apply {
			background = null
			if(hintBaseline != TextBaseline.INSIDE)
				hint = ""
		}

		input?.setTextInputEditTextListeners(this)
		input?.addTextChangedListener(this)
	}

	private fun initHintTextView() {
		hintTextView?.apply {
			text = inputHint
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
			if(inputMaxLength == 0) {
				removeView(it)
				letterCounterEnabled = false
			} else {
				it.apply {
					text = "${this@TextInputLayout.text.length}/$inputMaxLength"
					layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
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
			setImageResource(leftIconResId)
			if(leftIconColor != 0)
				ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(leftIconColor))
		}
	}

	private fun initRightIcon() {
		rightIcon?.apply {
			setImageResource(rightIconResId)
			if(rightIconColor != 0)
				ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(rightIconColor))
		}
	}

	private fun initIconSize() {
		val iconHeight = textHeight + (inputPaddingTop / 2) + (inputPaddingBottom / 2)
		iconSize = Size(iconHeight, iconHeight)
	}

	private fun initInputMaxLength() {
		input?.filters?.filterIsInstance<InputFilter.LengthFilter>()?.takeIf { it.isNotEmpty() }?.let {
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
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
		val measuredLeft = hintMinimizedBound.left + minHintTextViewSize.width + viewsHorizontalSpace

		hintMinimizedBound.right = min(measuredLeft, measuredMaxLeft)
	}

	private fun measureVerticalLeftIconBound() {
		if(leftIconResId != 0) {
			leftIconBound.left = innerViewsBound.left + viewsHorizontalSpace
			leftIconBound.right = leftIconBound.left + iconSize.width
		} else {
			leftIconBound.left = 0
			leftIconBound.right = 0
		}
	}

	private fun measureVerticalRightIconBound() {
		if(rightIconResId != 0) {
			rightIconBound.right = innerViewsBound.right - viewsHorizontalSpace
			rightIconBound.left = rightIconBound.right - iconSize.width
		} else {
			rightIconBound.left = 0
			rightIconBound.right = 0
		}
	}

	private fun measureVerticalPrefixBound() {
		if(prefix != null) {
			if(leftIconResId != 0) {
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
		if(suffix != null) {
			if(rightIconResId != 0) {
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
		if(letterCounterEnabled) {
			letterCounterBound.right = innerViewsBound.right - viewsHorizontalSpace
			letterCounterBound.left = letterCounterBound.right - letterCounterTextViewSize.width - letterCounterTextViewSize.width

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
		if(helperTextEnabled) {
			helperTextBound.left = innerViewsBound.left + viewsHorizontalSpace
			if(letterCounterEnabled) {
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
		val left = if(prefix != null && leftIconResId != 0) {
			prefixBound.right + viewsHorizontalSpace
		} else if(prefix != null && leftIconResId == 0) {
			prefixBound.right + viewsHorizontalSpace
		} else if(prefix == null && leftIconResId != 0) {
			leftIconBound.right + viewsHorizontalSpace
		} else {
			innerViewsBound.left + viewsHorizontalSpace + inputPaddingStart
		}

		val right = if(suffix != null && rightIconResId != 0) {
			suffixBound.left - viewsHorizontalSpace
		} else if(suffix != null && rightIconResId == 0) {
			suffixBound.left - viewsHorizontalSpace
		} else if(suffix == null && rightIconResId != 0) {
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
		if(bottomTextsEnabled) {
			bottomOffset = max(helperTextViewSize.height, letterCounterTextViewSize.height) + viewsVerticalSpace
		}
		roundedViewBound.bottom = drawBound.bottom - bottomOffset
	}

	private fun measureHorizontalInnerViewsBound() {
		when (hintBaseline) {
			TextBaseline.OUTSIDE, TextBaseline.INSIDE -> {
				innerViewsBound.top = roundedViewBound.top + outlineCornerRadius + innerPaddingTop
				innerViewsBound.bottom = roundedViewBound.bottom - outlineCornerRadius - innerPaddingBottom
			}
			TextBaseline.INLINE -> {
				innerViewsBound.top = roundedViewBound.top + max(outlineCornerRadius, minHintTextViewSize.height / 2) + innerPaddingTop
				innerViewsBound.bottom = roundedViewBound.bottom - max(outlineCornerRadius, minHintTextViewSize.height / 2) - innerPaddingBottom
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
		if(leftIconResId != 0) {
			if(hintBaseline == TextBaseline.INSIDE) {
				leftIconBound.top = hintMinimizedBound.bottom + viewsVerticalSpace
			} else {
				leftIconBound.top = innerViewsBound.top + viewsVerticalSpace + (inputPaddingTop / 2)
			}
			leftIconBound.bottom = leftIconBound.top + iconSize.height
		}
	}

	private fun measureHorizontalRightIconBound() {
		if(rightIconResId != 0) {
			if(hintBaseline == TextBaseline.INSIDE) {
				rightIconBound.top = hintMinimizedBound.bottom + viewsVerticalSpace
			} else {
				rightIconBound.top = innerViewsBound.top + viewsVerticalSpace + (inputPaddingTop / 2)
			}
			rightIconBound.bottom = rightIconBound.top + iconSize.height
		}
	}

	private fun measureHorizontalPrefixBound() {
		if(prefix != null) {
			if(hintBaseline == TextBaseline.INSIDE) {
				prefixBound.top = hintMinimizedBound.bottom + viewsVerticalSpace
			} else {
				prefixBound.top = innerViewsBound.top + viewsVerticalSpace + inputPaddingTop
			}
			prefixBound.bottom = prefixBound.top + prefixTextViewSize.height
		}
	}

	private fun measureHorizontalSuffixBound() {
		if(suffix != null) {
			if(hintBaseline == TextBaseline.INSIDE) {
				suffixBound.top = hintMinimizedBound.bottom + viewsVerticalSpace
			} else {
				suffixBound.top = innerViewsBound.top + viewsVerticalSpace + inputPaddingTop
			}
			suffixBound.bottom = suffixBound.top + suffixTextViewSize.height
		}
	}

	private fun measureHorizontalInputBound() {
		input?.apply {
			if(hintBaseline == TextBaseline.INSIDE) {
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
		if(letterCounterEnabled) {
			letterCounterBound.top = roundedViewBound.bottom + viewsVerticalSpace
			letterCounterBound.bottom = letterCounterBound.top + letterCounterTextViewSize.height
		}
	}

	private fun measureHorizontalHelperTextBound() {
		if(helperTextEnabled) {
			helperTextBound.top = roundedViewBound.bottom + viewsVerticalSpace
			helperTextBound.bottom = helperTextBound.top + helperTextViewSize.height
		}
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
	}
}

fun pxToSp(context: Context, newSize: Float): Float {
	return newSize / context.resources.displayMetrics.scaledDensity
}

fun View.layout(rect: Bound) {
	this.layout(rect.left, rect.top, rect.right, rect.bottom)
}

fun View.isWidthWrapContent() =
		ViewGroup.LayoutParams.WRAP_CONTENT == this.layoutParams.width

fun View.isHeightWrapContent() =
		ViewGroup.LayoutParams.WRAP_CONTENT == this.layoutParams.height

fun View.isMeasureUnspecified(measureSpec: Int) =
		View.MeasureSpec.UNSPECIFIED == View.MeasureSpec.getMode(measureSpec)

fun View.hasExactMeasure(measureSpec: Int) =
		View.MeasureSpec.EXACTLY == View.MeasureSpec.getMode(measureSpec)
