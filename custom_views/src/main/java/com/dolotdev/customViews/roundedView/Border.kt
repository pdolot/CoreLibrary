package com.dolotdev.customViews.roundedView

import android.graphics.PointF

internal class Border(visibleValues: Int, roundValues: Int, roundValue: Float) {
	var left: Edge = Edge(orientation = Orientation.BOTTOM_TO_TOP, roundValue = roundValue)
	var top: Edge = Edge(orientation = Orientation.LEFT_TO_RIGHT, roundValue = roundValue)
	var right: Edge = Edge(orientation = Orientation.TOP_TO_BOTTOM, roundValue = roundValue)
	var bottom: Edge = Edge(orientation = Orientation.RIGHT_TO_LEFT, roundValue = roundValue)

	init {
		visibleValues.toString(2).padStart(4, '0').let {
			bottom.visible = it[0] == '1'
			right.visible = it[1] == '1'
			top.visible = it[2] == '1'
			left.visible = it[3] == '1'
		}

		roundValues.toString(2).padStart(4, '0').let {

			left.roundedStart = it[0] == '1'
			bottom.roundedEnd = it[0] == '1'

			bottom.roundedStart = it[1] == '1'
			right.roundedEnd = it[1] == '1'

			right.roundedStart = it[2] == '1'
			top.roundedEnd = it[2] == '1'

			top.roundedStart = it[3] == '1'
			left.roundedEnd = it[3] == '1'
		}
	}

	fun isFullyBordered() = left.visible && top.visible && right.visible && bottom.visible

	class Edge(var visible: Boolean = true, var roundedStart: Boolean = false,
			   var roundedEnd: Boolean = false, val orientation: Orientation, private val roundValue: Float) {

		var startPoint: PointF = PointF()
			set(value) {
				field = if(roundedStart) {
					when (orientation) {
						Orientation.BOTTOM_TO_TOP -> PointF(value.x, value.y - roundValue)
						Orientation.LEFT_TO_RIGHT -> PointF(value.x + roundValue, value.y)
						Orientation.TOP_TO_BOTTOM -> PointF(value.x, value.y + roundValue)
						Orientation.RIGHT_TO_LEFT -> PointF(value.x - roundValue, value.y)
					}
				} else {
					value
				}
			}
		var endPoint: PointF = PointF()
			set(value) {
				field = if(roundedEnd) {
					when (orientation) {
						Orientation.BOTTOM_TO_TOP -> PointF(value.x, value.y + roundValue)
						Orientation.LEFT_TO_RIGHT -> PointF(value.x - roundValue, value.y)
						Orientation.TOP_TO_BOTTOM -> PointF(value.x, value.y - roundValue)
						Orientation.RIGHT_TO_LEFT -> PointF(value.x + roundValue, value.y)
					}
				} else {
					value
				}
			}

		val edgeLength: Float
			get() {
				return when (orientation) {
					Orientation.BOTTOM_TO_TOP -> startPoint.y - endPoint.y
					Orientation.LEFT_TO_RIGHT -> endPoint.x - startPoint.x
					Orientation.TOP_TO_BOTTOM -> endPoint.y - startPoint.y
					Orientation.RIGHT_TO_LEFT -> startPoint.x - endPoint.x
				}
			}
	}

	enum class Orientation {
		BOTTOM_TO_TOP,
		LEFT_TO_RIGHT,
		TOP_TO_BOTTOM,
		RIGHT_TO_LEFT
	}
}