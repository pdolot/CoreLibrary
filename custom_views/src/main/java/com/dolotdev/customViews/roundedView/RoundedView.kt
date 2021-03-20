package com.dolotdev.customViews.roundedView

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.dolotdev.customViews.R
import com.dolotdev.utils.extension.getRectForCircle
import com.dolotdev.utils.math.AnalyticalGeometryUtil

class RoundedView @JvmOverloads constructor(
		context: Context,
		attrs: AttributeSet? = null,
		defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

	// stroke
	private val viewStrokePaint: Paint = Paint().apply {
		isAntiAlias = true
		style = Paint.Style.STROKE
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

	var strokeLineJoin: Paint.Join = Paint.Join.ROUND
		set(value) {
			field = value
			viewStrokePaint.strokeJoin = value
			invalidate()
		}

	var strokeLineCap: Paint.Cap = Paint.Cap.ROUND
		set(value) {
			field = value
			viewStrokePaint.strokeCap = value
			invalidate()
		}

	var strokeLineMiter: Float = 4f
		set(value) {
			field = value
			viewStrokePaint.strokeMiter = value
			invalidate()
		}

	// inner paint
	private val viewPaint: Paint = Paint().apply {
		isAntiAlias = true
		style = Paint.Style.FILL
		setLayerType(LAYER_TYPE_HARDWARE, this)
	}

	var bgColor: Int = Color.TRANSPARENT
		set(value) {
			field = value
			viewPaint.color = value
			invalidate()
		}

	private var border: Border? = null
		set(value) {
			field = value
			invalidate()
		}

	var cornerRadius: Float = 0f
		set(value) {
			field = value
			border = Border(borders, roundedCorners, value)
		}

	private var outlineDrawRect: RectF = RectF()
	private var backgroundDrawRect: RectF = RectF()

	// path settings

	private var trimPathModeTop: TrimPathMode = TrimPathMode.DIMEN
	var isTrimPathTopReversed = false
	var trimPathStartTop = 0f
		set(value) {
			field = value
			invalidate()
		}
	var trimPathStartPercentTop = 0f
		set(value) {
			field = if(value < 0f) 0f else if(value > 1f) 1f else value
			invalidate()
		}
	var trimPathEndTop = 0f
		set(value) {
			field = value
			invalidate()
		}
	var trimPathEndPercentTop = 0f
		set(value) {
			field = if(value < 0f) 0f else if(value > 1f) 1f else value
			invalidate()
		}

	private var trimPathModeLeft: TrimPathMode = TrimPathMode.DIMEN
	var isTrimPathLeftReversed = false
	var trimPathStartLeft = 0f
		set(value) {
			field = value
			invalidate()
		}
	var trimPathStartPercentLeft = 0f
		set(value) {
			field = if(value < 0f) 0f else if(value > 1f) 1f else value
			invalidate()
		}
	var trimPathEndLeft = 0f
		set(value) {
			field = value
			invalidate()
		}
	var trimPathEndPercentLeft = 0f
		set(value) {
			field = if(value < 0f) 0f else if(value > 1f) 1f else value
			invalidate()
		}

	private var trimPathModeRight: TrimPathMode = TrimPathMode.DIMEN
	var isTrimPathRightReversed = false
	var trimPathStartRight = 0f
		set(value) {
			field = value
			invalidate()
		}
	var trimPathStartPercentRight = 0f
		set(value) {
			field = if(value < 0f) 0f else if(value > 1f) 1f else value
			invalidate()
		}
	var trimPathEndRight = 0f
		set(value) {
			field = value
			invalidate()
		}
	var trimPathEndPercentRight = 0f
		set(value) {
			field = if(value < 0f) 0f else if(value > 1f) 1f else value
			invalidate()
		}

	private var trimPathModeBottom: TrimPathMode = TrimPathMode.DIMEN
	var isTrimPathBottomReversed = false
	var trimPathStartBottom = 0f
		set(value) {
			field = value
			invalidate()
		}
	var trimPathStartPercentBottom = 0f
		set(value) {
			field = if(value < 0f) 0f else if(value > 1f) 1f else value
			invalidate()
		}
	var trimPathEndBottom = 0f
		set(value) {
			field = value
			invalidate()
		}
	var trimPathEndPercentBottom = 0f
		set(value) {
			field = if(value < 0f) 0f else if(value > 1f) 1f else value
			invalidate()
		}

	var borders = BORDER_ALL
		set(value) {
			field = value
			border = Border(value, roundedCorners, cornerRadius)
		}
	var roundedCorners = ROUNDED_ALL
		set(value) {
			field = value
			border = Border(borders, value, cornerRadius)
		}

	init {
		initAttrs(context, attrs, defStyleAttr)
	}

	fun initAttrs(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
		val a = context.theme.obtainStyledAttributes(attrs, R.styleable.RoundedView, defStyleAttr, 0)

		strokeColor = a.getColor(R.styleable.RoundedView_cv_strokeColor, Color.BLACK)
		strokeWidth = a.getDimensionPixelSize(R.styleable.RoundedView_cv_strokeWidth, strokeWidth.toInt()).toFloat()
		strokeLineCap = Paint.Cap.values()[a.getInteger(R.styleable.RoundedView_cv_strokeLineCap, 0)]
		strokeLineJoin = Paint.Join.values()[a.getInteger(R.styleable.RoundedView_cv_strokeLineJoin, 0)]
		strokeLineMiter = a.getFloat(R.styleable.RoundedView_cv_strokeLineMiter, 4f)

		bgColor = a.getColor(R.styleable.RoundedView_cv_backgroundColor, Color.TRANSPARENT)

		cornerRadius = a.getDimensionPixelSize(R.styleable.RoundedView_cv_cornerRadius, cornerRadius.toInt()).toFloat()
		borders = a.getInteger(R.styleable.RoundedView_cv_border, borders)
		roundedCorners = a.getInteger(R.styleable.RoundedView_cv_roundedCorners, roundedCorners)

		isTrimPathLeftReversed = a.getBoolean(R.styleable.RoundedView_cv_trimPathReverse_left, false)
		isTrimPathTopReversed = a.getBoolean(R.styleable.RoundedView_cv_trimPathReverse_top, false)
		isTrimPathRightReversed = a.getBoolean(R.styleable.RoundedView_cv_trimPathReverse_right, false)
		isTrimPathBottomReversed = a.getBoolean(R.styleable.RoundedView_cv_trimPathReverse_bottom, false)

		trimPathModeLeft = TrimPathMode.values()[a.getInteger(R.styleable.RoundedView_cv_trimPathMode_left, 0)]
		trimPathModeTop = TrimPathMode.values()[a.getInteger(R.styleable.RoundedView_cv_trimPathMode_top, 0)]
		trimPathModeRight = TrimPathMode.values()[a.getInteger(R.styleable.RoundedView_cv_trimPathMode_right, 0)]
		trimPathModeBottom = TrimPathMode.values()[a.getInteger(R.styleable.RoundedView_cv_trimPathMode_bottom, 0)]

		if(trimPathModeLeft == TrimPathMode.DIMEN) {
			trimPathStartLeft = a.getDimensionPixelSize(R.styleable.RoundedView_cv_trimPathStart_left, 0).toFloat()
			trimPathEndLeft = a.getDimensionPixelSize(R.styleable.RoundedView_cv_trimPathEnd_left, 0).toFloat()
		} else {
			trimPathStartPercentLeft = a.getFloat(R.styleable.RoundedView_cv_trimPathStartPercent_left, 0f)
			trimPathEndPercentLeft = a.getFloat(R.styleable.RoundedView_cv_trimPathEndPercent_left, 0f)
		}

		if(trimPathModeTop == TrimPathMode.DIMEN) {
			trimPathStartTop = a.getDimensionPixelSize(R.styleable.RoundedView_cv_trimPathStart_top, 0).toFloat()
			trimPathEndTop = a.getDimensionPixelSize(R.styleable.RoundedView_cv_trimPathEnd_top, 0).toFloat()
		} else {
			trimPathStartPercentTop = a.getFloat(R.styleable.RoundedView_cv_trimPathStartPercent_top, 0f)
			trimPathEndPercentTop = a.getFloat(R.styleable.RoundedView_cv_trimPathEndPercent_top, 0f)
		}

		if(trimPathModeRight == TrimPathMode.DIMEN) {
			trimPathStartRight = a.getDimensionPixelSize(R.styleable.RoundedView_cv_trimPathStart_right, 0).toFloat()
			trimPathEndRight = a.getDimensionPixelSize(R.styleable.RoundedView_cv_trimPathEnd_right, 0).toFloat()
		} else {
			trimPathStartPercentRight = a.getFloat(R.styleable.RoundedView_cv_trimPathStartPercent_right, 0f)
			trimPathEndPercentRight = a.getFloat(R.styleable.RoundedView_cv_trimPathEndPercent_right, 0f)
		}

		if(trimPathModeBottom == TrimPathMode.DIMEN) {
			trimPathStartBottom = a.getDimensionPixelSize(R.styleable.RoundedView_cv_trimPathStart_bottom, 0).toFloat()
			trimPathEndBottom = a.getDimensionPixelSize(R.styleable.RoundedView_cv_trimPathEnd_bottom, 0).toFloat()
		} else {
			trimPathStartPercentBottom = a.getFloat(R.styleable.RoundedView_cv_trimPathStartPercent_bottom, 0f)
			trimPathEndPercentBottom = a.getFloat(R.styleable.RoundedView_cv_trimPathEndPercent_bottom, 0f)
		}

		a.recycle()
	}

	fun getBorderLeftLength() = border?.left?.edgeLength ?: 0f
	fun getBorderTopLength() = border?.top?.edgeLength ?: 0f
	fun getBorderRightLength() = border?.right?.edgeLength ?: 0f
	fun getBorderBottomLength() = border?.bottom?.edgeLength ?: 0f

	private fun getDrawRect() {
		val left = paddingLeft + strokeOffset
		val top = paddingTop + strokeOffset
		val right = measuredWidth - (paddingRight + strokeOffset)
		val bottom = measuredHeight - (paddingBottom + strokeOffset)
		outlineDrawRect = RectF(left, top, right, bottom)
		backgroundDrawRect = RectF(left - strokeOffset, top - strokeOffset, right + strokeOffset, bottom + strokeOffset)

		border?.apply {
			this.left.startPoint = PointF(left, bottom)
			this.left.endPoint = PointF(left, top)

			this.top.startPoint = PointF(left, top)
			this.top.endPoint = PointF(right, top)

			this.right.startPoint = PointF(right, top)
			this.right.endPoint = PointF(right, bottom)

			this.bottom.startPoint = PointF(right, bottom)
			this.bottom.endPoint = PointF(left, bottom)
		}
	}

	private fun generatePath(): Path {
		val path = Path()
		var isPathInterrupted = false
		border?.apply {
			if(left.visible) {
				if(left.roundedStart) {
					AnalyticalGeometryUtil.getPointOnCircle(PointF(outlineDrawRect.left + cornerRadius, outlineDrawRect.bottom - cornerRadius
					), cornerRadius, 135f, false).let {
						path.moveTo(it.x, it.y)
					}
					getRectForCircle(PointF(outlineDrawRect.left + cornerRadius, outlineDrawRect.bottom - cornerRadius), cornerRadius).let {
						path.arcTo(it, 135f, 45f)
					}
				} else {
					path.moveTo(outlineDrawRect.left, outlineDrawRect.bottom)
				}

				if(isLeftBorderTrim()) {
					getTrimBorderPoints(left, trimPathModeLeft,
							trimPathStartLeft, trimPathEndLeft,
							trimPathStartPercentLeft, trimPathEndPercentLeft)?.let {

						if(isTrimPathLeftReversed) {
							path.lineTo(it.first.x, it.first.y)
							path.moveTo(it.second.x, it.second.y)
						} else {
							path.moveTo(it.first.x, it.first.y)
							path.lineTo(it.second.x, it.second.y)

							if(top.roundedEnd)
								path.moveTo(outlineDrawRect.left, outlineDrawRect.top - cornerRadius)
							else
								path.moveTo(outlineDrawRect.left, outlineDrawRect.top)
						}
					}
				}


				if(left.roundedEnd) {
					path.lineTo(outlineDrawRect.left, outlineDrawRect.top + cornerRadius)
					getRectForCircle(PointF(outlineDrawRect.left + cornerRadius, outlineDrawRect.top + cornerRadius), cornerRadius).let {
						path.arcTo(it, 180f, 45f)
					}
				} else {
					path.lineTo(outlineDrawRect.left, outlineDrawRect.top)
				}
			} else {
				isPathInterrupted = true
			}

			if(top.visible) {
				if(top.roundedStart) {
					if(isPathInterrupted) {
						AnalyticalGeometryUtil.getPointOnCircle(
								PointF(outlineDrawRect.left + cornerRadius, outlineDrawRect.top + cornerRadius
								), cornerRadius, 225f, false
						).let {
							path.moveTo(it.x, it.y)
						}
					}
					getRectForCircle(PointF(outlineDrawRect.left + cornerRadius, outlineDrawRect.top + cornerRadius), cornerRadius).let {
						path.arcTo(it, 225f, 45f)
					}
				} else {
					if(isPathInterrupted) {
						path.moveTo(outlineDrawRect.left, outlineDrawRect.top)
					}
				}

				if(isTopBorderTrim()) {
					getTrimBorderPoints(top, trimPathModeTop,
							trimPathStartTop, trimPathEndTop,
							trimPathStartPercentTop, trimPathEndPercentTop)?.let {
						if(isTrimPathTopReversed) {
							path.lineTo(it.first.x, it.first.y)
							path.moveTo(it.second.x, it.second.y)
						} else {
							path.moveTo(it.first.x, it.first.y)
							path.lineTo(it.second.x, it.second.y)

							if(top.roundedEnd)
								path.moveTo(outlineDrawRect.right - cornerRadius, outlineDrawRect.top)
							else
								path.moveTo(outlineDrawRect.right, outlineDrawRect.top)
						}
					}
				}

				if(top.roundedEnd) {
					path.lineTo(outlineDrawRect.right - cornerRadius, outlineDrawRect.top)
					getRectForCircle(PointF(outlineDrawRect.right - cornerRadius, outlineDrawRect.top + cornerRadius), cornerRadius).let {
						path.arcTo(it, 270f, 45f)
					}
				} else {
					path.lineTo(outlineDrawRect.right, outlineDrawRect.top)
				}
			} else {
				isPathInterrupted = true
			}

			if(right.visible) {
				if(right.roundedStart) {
					if(isPathInterrupted) {
						AnalyticalGeometryUtil.getPointOnCircle(
								PointF(outlineDrawRect.right - cornerRadius, outlineDrawRect.top + cornerRadius),
								cornerRadius, 315f, false
						).let {
							path.moveTo(it.x, it.y)
						}
					}
					getRectForCircle(PointF(outlineDrawRect.right - cornerRadius, outlineDrawRect.top + cornerRadius), cornerRadius).let {
						path.arcTo(it, 315f, 45f)
					}
				} else {
					if(isPathInterrupted) {
						path.moveTo(outlineDrawRect.right, outlineDrawRect.top)
					}
				}

				if(isRightBorderTrim()) {
					getTrimBorderPoints(right, trimPathModeRight,
							trimPathStartRight, trimPathEndRight,
							trimPathStartPercentRight, trimPathEndPercentRight)?.let {
						if(isTrimPathRightReversed) {
							path.lineTo(it.first.x, it.first.y)
							path.moveTo(it.second.x, it.second.y)
						} else {
							path.moveTo(it.first.x, it.first.y)
							path.lineTo(it.second.x, it.second.y)

							if(right.roundedEnd)
								path.moveTo(outlineDrawRect.right, outlineDrawRect.bottom - cornerRadius)
							else
								path.moveTo(outlineDrawRect.right, outlineDrawRect.bottom)
						}
					}
				}

				if(right.roundedEnd) {
					path.lineTo(outlineDrawRect.right, outlineDrawRect.bottom - cornerRadius)
					getRectForCircle(
							PointF(outlineDrawRect.right - cornerRadius, outlineDrawRect.bottom - cornerRadius), cornerRadius
					).let {
						path.arcTo(it, 0f, 45f)
					}
				} else {
					path.lineTo(outlineDrawRect.right, outlineDrawRect.bottom)
				}
			} else {
				isPathInterrupted = true
			}

			if(bottom.visible) {
				if(bottom.roundedStart) {
					if(isPathInterrupted) {
						AnalyticalGeometryUtil.getPointOnCircle(
								PointF(outlineDrawRect.right - cornerRadius, outlineDrawRect.bottom - cornerRadius),
								cornerRadius, 45f, false
						).let {
							path.moveTo(it.x, it.y)
						}
					}
					getRectForCircle(PointF(outlineDrawRect.right - cornerRadius, outlineDrawRect.bottom - cornerRadius), cornerRadius).let {
						path.arcTo(it, 45f, 45f)
					}
				} else {
					if(isPathInterrupted) {
						path.moveTo(outlineDrawRect.right, outlineDrawRect.bottom)
					}
				}

				if(isBottomBorderTrim()) {
					getTrimBorderPoints(bottom, trimPathModeBottom,
							trimPathStartBottom, trimPathEndBottom,
							trimPathStartPercentBottom, trimPathEndPercentBottom)?.let {
						if(isTrimPathBottomReversed) {
							path.lineTo(it.first.x, it.first.y)
							path.moveTo(it.second.x, it.second.y)
						} else {
							path.moveTo(it.first.x, it.first.y)
							path.lineTo(it.second.x, it.second.y)

							if(bottom.roundedEnd)
								path.moveTo(outlineDrawRect.left + cornerRadius, outlineDrawRect.bottom)
							else
								path.moveTo(outlineDrawRect.left, outlineDrawRect.bottom)
						}
					}
				}

				if(bottom.roundedEnd) {
					path.lineTo(outlineDrawRect.left + cornerRadius, outlineDrawRect.bottom)
					getRectForCircle(PointF(outlineDrawRect.left + cornerRadius, outlineDrawRect.bottom - cornerRadius), cornerRadius).let {
						path.arcTo(it, 90f, 45f)
					}
				} else {
					path.lineTo(outlineDrawRect.left, outlineDrawRect.bottom)
				}
			} else {
				isPathInterrupted = true
			}
		}

		if(isPathFullBordered())
			path.close()
		return path
	}

	private fun isLeftBorderTrim() = ((trimPathModeLeft == TrimPathMode.DIMEN && (trimPathStartLeft != 0f || trimPathEndLeft != 0f))
			|| (trimPathModeLeft == TrimPathMode.PERCENT && (trimPathStartPercentLeft != 0f || trimPathEndPercentLeft != 0f)))

	private fun isTopBorderTrim() = ((trimPathModeTop == TrimPathMode.DIMEN && (trimPathStartTop != 0f || trimPathEndTop != 0f))
			|| (trimPathModeTop == TrimPathMode.PERCENT && (trimPathStartPercentTop != 0f || trimPathEndPercentTop != 0f)))

	private fun isRightBorderTrim() = ((trimPathModeRight == TrimPathMode.DIMEN && (trimPathStartRight != 0f || trimPathEndRight != 0f))
			|| (trimPathModeRight == TrimPathMode.PERCENT && (trimPathStartPercentRight != 0f || trimPathEndPercentRight != 0f)))

	private fun isBottomBorderTrim() = ((trimPathModeBottom == TrimPathMode.DIMEN && (trimPathStartBottom != 0f || trimPathEndBottom != 0f))
			|| (trimPathModeBottom == TrimPathMode.PERCENT && (trimPathStartPercentBottom != 0f || trimPathEndPercentBottom != 0f)))

	private fun getTrimBorderPoints(borderEdge: Border.Edge?, mode: TrimPathMode,
									trimPathStart: Float, trimPathEnd: Float,
									trimPathStartPercent: Float, trimPathEndPercent: Float): Pair<PointF, PointF>? {
		borderEdge?.let { edge ->

			var firstPointX = edge.startPoint.x
			var firstPointY = edge.startPoint.y
			var secondPointX = edge.endPoint.x
			var secondPointY = edge.endPoint.y

			val edgeLength = edge.edgeLength

			if(mode == TrimPathMode.DIMEN) {
				if(trimPathStart + trimPathEnd < edgeLength) {
					when (edge.orientation) {
						Border.Orientation.BOTTOM_TO_TOP -> {
							firstPointY -= trimPathStart
							secondPointY += trimPathEnd
						}
						Border.Orientation.LEFT_TO_RIGHT -> {
							firstPointX += trimPathStart
							secondPointX -= trimPathEnd
						}
						Border.Orientation.TOP_TO_BOTTOM -> {
							firstPointY += trimPathStart
							secondPointY -= trimPathEnd
						}
						Border.Orientation.RIGHT_TO_LEFT -> {
							firstPointX -= trimPathStart
							secondPointX += trimPathEnd
						}
					}
				}

			} else {
				if(trimPathStartPercent + trimPathEndPercent < 1f) {
					when (edge.orientation) {
						Border.Orientation.BOTTOM_TO_TOP -> {
							firstPointY -= (trimPathStartPercent * edgeLength)
							secondPointY += (trimPathEndPercent * edgeLength)
						}
						Border.Orientation.LEFT_TO_RIGHT -> {
							firstPointX += (trimPathStartPercent * edgeLength)
							secondPointX -= (trimPathEndPercent * edgeLength)
						}
						Border.Orientation.TOP_TO_BOTTOM -> {
							firstPointY += (trimPathStartPercent * edgeLength)
							secondPointY -= (trimPathEndPercent * edgeLength)
						}
						Border.Orientation.RIGHT_TO_LEFT -> {
							firstPointX -= (trimPathStartPercent * edgeLength)
							secondPointX += (trimPathEndPercent * edgeLength)
						}
					}
				}
			}

			return Pair(PointF(firstPointX, firstPointY), PointF(secondPointX, secondPointY))
		}
		return null
	}

	private fun isPathFullBordered(): Boolean = border?.isFullyBordered() == true
			&& !isLeftBorderTrim() && !isRightBorderTrim() && !isTopBorderTrim() && !isBottomBorderTrim()

	override fun onDraw(canvas: Canvas?) {
		super.onDraw(canvas)
		getDrawRect()
		val viewPath = generatePath()

		if(isPathFullBordered() || viewPaint.color != Color.TRANSPARENT)
			canvas?.drawRoundRect(backgroundDrawRect, cornerRadius, cornerRadius, viewPaint)

		if(strokeWidth > 0f)
			canvas?.drawPath(viewPath, viewStrokePaint)
	}

	private enum class TrimPathMode {
		DIMEN,
		PERCENT
	}

	companion object {
		private val TAG: String = RoundedView::class.java.simpleName

		const val ROUNDED_TOP_LEFT: Int = 1
		const val ROUNDED_TOP_RIGHT: Int = 2
		const val ROUNDED_BOTTOM_RIGHT: Int = 4
		const val ROUNDED_BOTTOM_LEFT: Int = 8
		const val ROUNDED_ALL: Int = 15

		const val BORDER_LEFT: Int = 1
		const val BORDER_TOP: Int = 2
		const val BORDER_RIGHT: Int = 4
		const val BORDER_BOTTOM: Int = 8
		const val BORDER_ALL: Int = 15
	}
}