package com.dolotdev.utils.extension

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.dolotdev.utils.view.Bound

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