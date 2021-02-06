package com.dolotdev.customViews.textInput

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText

open class TextInputEditText @JvmOverloads constructor(
		context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = android.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

	private var textInputEditTextListeners: TextInputEditTextListener? = null

	internal fun setTextInputEditTextListeners(textInputEditTextListener: TextInputEditTextListener) {
		this.textInputEditTextListeners = textInputEditTextListener
	}

	override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
		super.onFocusChanged(focused, direction, previouslyFocusedRect)
		textInputEditTextListeners?.onFocusChanged(focused)
	}

	internal interface TextInputEditTextListener {
		fun onFocusChanged(focused: Boolean)
	}

}