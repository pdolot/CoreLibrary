package com.dolotdev.customViews.textInputLayout

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText

open class TextInputEditText @JvmOverloads constructor(
	context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = android.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

    private var textInputEditTextListeners: TextInputEditTextListener? = null

    var text: String = ""
        set(value) {
            field = value
            textInputEditTextListeners?.onSetText(value) ?: super.setText(text)
        }
        get() = this.getText()?.toString() ?: ""

    var hint: String = ""
        set(value) {
            textInputEditTextListeners?.onSetHint(value) ?: kotlin.run {
                field = value
                super.setHint(value)
            }
        }
        get() = this.getHint()?.toString() ?: ""

    internal fun setTextInputEditTextListeners(textInputEditTextListener: TextInputEditTextListener) {
        this.textInputEditTextListeners = textInputEditTextListener
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        textInputEditTextListeners?.onFocusChanged(focused)
    }

    internal interface TextInputEditTextListener {
        fun onFocusChanged(focused: Boolean)
        fun onSetText(text: String)
        fun onSetHint(hint: String)
    }

}