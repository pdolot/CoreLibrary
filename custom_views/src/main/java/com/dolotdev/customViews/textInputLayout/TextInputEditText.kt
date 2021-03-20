package com.dolotdev.customViews.textInputLayout

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText

class TextInputEditText @JvmOverloads constructor(
	context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = android.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

    private var textInputEditTextListeners: TextInputEditTextListener? = null

    var hint: String = ""
        set(value) {
            textInputEditTextListeners?.onSetHint(value) ?: kotlin.run {
                field = value
                super.setHint(value)
            }
        }
        get() = this.getHint()?.toString() ?: ""


    init {
        setPadding(paddingLeft / 3, paddingTop / 3, paddingRight / 3, paddingBottom / 3)
    }


    internal fun setTextInputEditTextListeners(textInputEditTextListener: TextInputEditTextListener) {
        this.textInputEditTextListeners = textInputEditTextListener
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        textInputEditTextListeners?.onFocusChanged(focused)
    }

    internal interface TextInputEditTextListener {
        fun onFocusChanged(focused: Boolean)
        fun onSetHint(hint: String)
    }

}