package com.dolotdev.utils.extension

import android.graphics.PointF
import android.graphics.RectF
import android.util.Log
import com.dolotdev.utils.extension.PathExtension.TAG
import com.dolotdev.utils.math.AnalyticalGeometryUtil
import com.dolotdev.utils.math.AnalyticalGeometryUtil.getAnglesBetweenVertex
import com.dolotdev.utils.math.AnalyticalGeometryUtil.getArcType
import com.dolotdev.utils.math.AnalyticalGeometryUtil.getIntersectionPointOfTwoLines
import com.dolotdev.utils.math.AnalyticalGeometryUtil.getIntersectionPoints
import com.dolotdev.utils.math.AnalyticalGeometryUtil.getLinearFunctionFromTwoGivenPoints
import com.dolotdev.utils.math.AnalyticalGeometryUtil.getPointOnCircle
import com.dolotdev.utils.math.AnalyticalGeometryUtil.getVertexAngle
import com.dolotdev.utils.path.Path
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object PathExtension {
	val TAG: String = this::class.java.simpleName
}

/**
 * Extension to round two lines starting at last point of the path, through an angular point and ending at given endPoint
 * Remember to start path use function: moveTo
 * If last point of the path is equal to angular point, start point is set to one before last point of path
 */
fun Path.roundLines(angularPoint: PointF, endPoint: PointF, roundRadius: Float) {
	//  var radius = roundRadius

	if(points.isEmpty()) {
		Log.e(TAG, "Use moveTo method to start drawing path")
		return
	}

	val points = points.linearSet()

	val startPoint = when {
		points.last().x == angularPoint.x && points.last().y == angularPoint.y && points.size > 1 -> points[points.size - 2]
		points.last().x == angularPoint.x && points.last().y == angularPoint.y && points.size == 1 -> {
			Log.e(TAG, "Last point is the same as angular point. Drawing line")
			this.lineTo(endPoint)
			return
		}
		else -> points.last()
	}

	val intersectionPoints = arrayListOf<PointF>()

	if(startPoint.x == angularPoint.x && endPoint.x == angularPoint.x) {
		this.lineTo(angularPoint)
		this.lineTo(endPoint)
		return
	} else if(startPoint.x == angularPoint.x || endPoint.x == angularPoint.x) {
		val point = if(startPoint.x == angularPoint.x) endPoint else startPoint
		getLinearFunctionFromTwoGivenPoints(angularPoint.x, angularPoint.y, point.x, point.y)?.let { originalFunction ->
			intersectionPoints.clear()
			val firstLinearFunction = originalFunction.shiftBy(roundRadius, AnalyticalGeometryUtil.LinearFunction.SHIFT_DOWN)
			val secondLinearFunction = originalFunction.shiftBy(roundRadius, AnalyticalGeometryUtil.LinearFunction.SHIFT_UP)
			val x1 = angularPoint.x + roundRadius
			val x2 = angularPoint.x - roundRadius
			intersectionPoints.add(PointF(x1, firstLinearFunction.getValue(x1)))
			intersectionPoints.add(PointF(x2, firstLinearFunction.getValue(x2)))
			intersectionPoints.add(PointF(x1, secondLinearFunction.getValue(x1)))
			intersectionPoints.add(PointF(x2, secondLinearFunction.getValue(x2)))
		}
	} else {
		val firstOriginalFunction = getLinearFunctionFromTwoGivenPoints(startPoint.x, startPoint.y, angularPoint.x, angularPoint.y)
		val secondOriginalFunction = getLinearFunctionFromTwoGivenPoints(endPoint.x, endPoint.y, angularPoint.x, angularPoint.y)

		if(firstOriginalFunction != null && secondOriginalFunction != null) {
			intersectionPoints.clear()
			val firstLinearFunction = firstOriginalFunction.shiftBy(roundRadius, AnalyticalGeometryUtil.LinearFunction.SHIFT_DOWN)
			val secondLinearFunction = firstOriginalFunction.shiftBy(roundRadius, AnalyticalGeometryUtil.LinearFunction.SHIFT_UP)
			val thirdLinearFunction = secondOriginalFunction.shiftBy(roundRadius, AnalyticalGeometryUtil.LinearFunction.SHIFT_DOWN)
			val fourthLinearFunction = secondOriginalFunction.shiftBy(roundRadius, AnalyticalGeometryUtil.LinearFunction.SHIFT_UP)
			intersectionPoints.add(getIntersectionPointOfTwoLines(firstLinearFunction, thirdLinearFunction))
			intersectionPoints.add(getIntersectionPointOfTwoLines(firstLinearFunction, fourthLinearFunction))
			intersectionPoints.add(getIntersectionPointOfTwoLines(secondLinearFunction, thirdLinearFunction))
			intersectionPoints.add(getIntersectionPointOfTwoLines(secondLinearFunction, fourthLinearFunction))

		} else {
			this.lineTo(angularPoint)
			this.lineTo(endPoint)
			return
		}
	}

	intersectionPoints.find { isVertexBetween(angularPoint, startPoint, endPoint, it) }?.let { circlePoint ->
		val firstTangentPoint = getLinearFunctionFromTwoGivenPoints(startPoint.x, startPoint.y, angularPoint.x, angularPoint.y)?.let { f ->
			if(f.a != 0f) {
				val perpendicularFunction = f.getPerpendicularFunction(circlePoint.x, circlePoint.y)
				getIntersectionPointOfTwoLines(f.a, f.b, perpendicularFunction.a, perpendicularFunction.b)
			} else {
				PointF(circlePoint.x, angularPoint.y)
			}

		} ?: PointF(startPoint.x, circlePoint.y)

		val secondTangentPoint = getLinearFunctionFromTwoGivenPoints(endPoint.x, endPoint.y, angularPoint.x, angularPoint.y)?.let { f ->
			if(f.a != 0f) {
				val perpendicularFunction = f.getPerpendicularFunction(circlePoint.x, circlePoint.y)
				getIntersectionPointOfTwoLines(f.a, f.b, perpendicularFunction.a, perpendicularFunction.b)
			} else {
				PointF(circlePoint.x, angularPoint.y)
			}
		} ?: PointF(endPoint.x, circlePoint.y)

		val startAngle = getVertexAngle(circlePoint, firstTangentPoint, reverse = false)
		val endAngle = getVertexAngle(circlePoint, secondTangentPoint, reverse = false)
		var sweepAngle = getAnglesBetweenVertex(circlePoint, firstTangentPoint, secondTangentPoint).min()

		if(isSweepAngleNegative(startAngle, endAngle, sweepAngle)) {
			sweepAngle *= (-1f)
		}

		this.lineTo(firstTangentPoint)
		this.arcTo(getRectForCircle(circlePoint, roundRadius), startAngle, sweepAngle)
		this.lineTo(endPoint)
	} ?: kotlin.run {
		this.lineTo(angularPoint)
		this.lineTo(endPoint)
	}
}

@Throws(IllegalStateException::class)
fun Path.roundArcLine(circleCenterPoint: PointF, circleRadius: Float, startAngle: Float, sweepAngle: Float, lineEndPoint: PointF, roundRadius: Float, forceMoveTo: Boolean) {
	val lineEnd = PointF(lineEndPoint.x, -lineEndPoint.y)
	val circle = PointF(circleCenterPoint.x, -circleCenterPoint.y)

	val lineStart = getPointOnCircle(circle, circleRadius, startAngle + sweepAngle)

	val intersectionPoints = ArrayList<PointF>()
	val arcType: AnalyticalGeometryUtil.ArcType? = getArcType(lineEndPoint.apply { y *= (-1) }, lineStart, circle)
	val originalFunction = getLinearFunctionFromTwoGivenPoints(lineEnd.x, lineEnd.y, lineStart.x, lineStart.y)

	if(lineEnd.x != lineStart.x) {
		if(originalFunction != null) {
			val shiftedFirstFunction = originalFunction.shiftBy(roundRadius, AnalyticalGeometryUtil.LinearFunction.SHIFT_DOWN)
			val shiftedSecondFunction = originalFunction.shiftBy(roundRadius, AnalyticalGeometryUtil.LinearFunction.SHIFT_UP)

			when (arcType) {
				AnalyticalGeometryUtil.ArcType.OUTER -> {
					intersectionPoints.addAll(getIntersectionPoints(shiftedFirstFunction, circle, circleRadius + roundRadius))
					intersectionPoints.addAll(getIntersectionPoints(shiftedSecondFunction, circle, circleRadius + roundRadius))
				}
				AnalyticalGeometryUtil.ArcType.INNER -> {
					intersectionPoints.addAll(getIntersectionPoints(shiftedFirstFunction, circle, circleRadius - roundRadius))
					intersectionPoints.addAll(getIntersectionPoints(shiftedSecondFunction, circle, circleRadius - roundRadius))
				}
			}
		} else {
			throw IllegalStateException("Cannot find linear function for given points (${lineEnd.x},${lineEnd.y}), (${lineStart.x},${lineStart.y})")
		}
	} else {
		when (arcType) {
			AnalyticalGeometryUtil.ArcType.OUTER -> {
				intersectionPoints.addAll(getIntersectionPoints(lineStart.x - roundRadius, circle, circleRadius + roundRadius))
				intersectionPoints.addAll(getIntersectionPoints(lineStart.x + roundRadius, circle, circleRadius + roundRadius))
			}
			AnalyticalGeometryUtil.ArcType.INNER -> {
				intersectionPoints.addAll(getIntersectionPoints(lineStart.x - roundRadius, circle, circleRadius - roundRadius))
				intersectionPoints.addAll(getIntersectionPoints(lineStart.x + roundRadius, circle, circleRadius - roundRadius))
			}
		}
	}

	when (arcType) {
		AnalyticalGeometryUtil.ArcType.OUTER -> {
			val roundCirclePoint = getPotentialIntersectionPoint(intersectionPoints, circle, startAngle, sweepAngle, true)
			this.drawRoundArcLine(lineStart, lineEnd, roundRadius, roundCirclePoint, originalFunction, circle, circleRadius, startAngle, sweepAngle, forceMoveTo)
		}
		AnalyticalGeometryUtil.ArcType.INNER -> {
			val roundCirclePoint = getPotentialIntersectionPoint(intersectionPoints, circle, startAngle, sweepAngle, true)
			this.drawRoundArcLine(lineStart, lineEnd, roundRadius, roundCirclePoint, originalFunction, circle, circleRadius, startAngle, sweepAngle, forceMoveTo)
		}
		null -> {
			this.arcTo(getRectForCircle(circleCenterPoint, circleRadius), startAngle, sweepAngle)
			this.lineTo(lineStart.x, lineStart.y)
		}
	}
}

@Throws(IllegalStateException::class)
fun Path.roundLineArc(lineStartPoint: PointF, circleCenterPoint: PointF, circleRadius: Float, startAngle: Float, sweepAngle: Float, roundRadius: Float) {
	val lineStart = PointF(lineStartPoint.x, -lineStartPoint.y)
	val circle = PointF(circleCenterPoint.x, -circleCenterPoint.y)

	val lineEnd = getPointOnCircle(circle, circleRadius, startAngle)

	val intersectionPoints = ArrayList<PointF>()
	val arcType: AnalyticalGeometryUtil.ArcType? = getArcType(lineStart, lineEnd, circle)
	val originalFunction = getLinearFunctionFromTwoGivenPoints(lineStart.x, lineStart.y, lineEnd.x, lineEnd.y)

	if(lineStart.x != lineEnd.x) {
		if(originalFunction != null) {
			val shiftedFirstFunction = originalFunction.shiftBy(roundRadius, AnalyticalGeometryUtil.LinearFunction.SHIFT_DOWN)
			val shiftedSecondFunction = originalFunction.shiftBy(roundRadius, AnalyticalGeometryUtil.LinearFunction.SHIFT_UP)

			when (arcType) {
				AnalyticalGeometryUtil.ArcType.OUTER -> {
					intersectionPoints.addAll(getIntersectionPoints(shiftedFirstFunction, circle, circleRadius + roundRadius))
					intersectionPoints.addAll(getIntersectionPoints(shiftedSecondFunction, circle, circleRadius + roundRadius))
				}
				AnalyticalGeometryUtil.ArcType.INNER -> {
					intersectionPoints.addAll(getIntersectionPoints(shiftedFirstFunction, circle, circleRadius - roundRadius))
					intersectionPoints.addAll(getIntersectionPoints(shiftedSecondFunction, circle, circleRadius - roundRadius))
				}
			}
		} else {
			throw IllegalStateException("Cannot find linear function for given points (${lineStart.x},${lineStart.y}), (${lineEnd.x},${lineEnd.y})")
		}
	} else {
		when (arcType) {
			AnalyticalGeometryUtil.ArcType.OUTER -> {
				intersectionPoints.addAll(getIntersectionPoints(lineEnd.x - roundRadius, circle, circleRadius + roundRadius))
				intersectionPoints.addAll(getIntersectionPoints(lineEnd.x + roundRadius, circle, circleRadius + roundRadius))
			}
			AnalyticalGeometryUtil.ArcType.INNER -> {
				intersectionPoints.addAll(getIntersectionPoints(lineEnd.x - roundRadius, circle, circleRadius - roundRadius))
				intersectionPoints.addAll(getIntersectionPoints(lineEnd.x + roundRadius, circle, circleRadius - roundRadius))
			}
		}
	}

	when (arcType) {
		AnalyticalGeometryUtil.ArcType.OUTER -> {
			val roundCirclePoint = getPotentialIntersectionPoint(intersectionPoints, circle, startAngle, sweepAngle)
			this.drawRoundLineArc(lineEnd, roundRadius, roundCirclePoint, originalFunction, circle, circleRadius, startAngle, sweepAngle)
		}
		AnalyticalGeometryUtil.ArcType.INNER -> {
			val roundCirclePoint = getPotentialIntersectionPoint(intersectionPoints, circle, startAngle, sweepAngle)
			this.drawRoundLineArc(lineEnd, roundRadius, roundCirclePoint, originalFunction, circle, circleRadius, startAngle, sweepAngle)
		}
		null -> {
			this.lineTo(lineEnd.x, lineEnd.y)
			this.arcTo(getRectForCircle(circleCenterPoint, circleRadius), startAngle, sweepAngle)
		}
	}
}

private fun Path.drawRoundLineArc(lineEnd: PointF, roundValue: Float, roundCirclePoint: PointF?, originalFunction: AnalyticalGeometryUtil.LinearFunction?, circle: PointF, circleRadius: Float, startAngle: Float, sweepAngle: Float) {
	if(roundCirclePoint != null) {
		val tangentPointRoundCircleToLine = if(originalFunction != null) {
			if(originalFunction.a != 0f) {
				val perpendicularFunction = originalFunction.getPerpendicularFunction(roundCirclePoint.x, roundCirclePoint.y)
				getIntersectionPointOfTwoLines(originalFunction.a, originalFunction.b, perpendicularFunction.a, perpendicularFunction.b)
			} else {
				PointF(roundCirclePoint.x, originalFunction.b)
			}
		} else {
			PointF(lineEnd.x, roundCirclePoint.y)
		}

		val functionBetweenCirclesCenterPoints = getLinearFunctionFromTwoGivenPoints(roundCirclePoint.x, roundCirclePoint.y, circle.x, circle.y)
		if(functionBetweenCirclesCenterPoints != null) {
			val tangentPointOfCircles = getPotentialIntersectionPoint(getIntersectionPoints(functionBetweenCirclesCenterPoints, circle, circleRadius), circle, startAngle, sweepAngle)
			if(tangentPointOfCircles != null) {

				var roundSweepAngle = getAnglesBetweenVertex(roundCirclePoint, tangentPointRoundCircleToLine, tangentPointOfCircles).min()
				val roundStartAngle = getVertexAngle(roundCirclePoint, tangentPointRoundCircleToLine)

				val circleStartAngle = getVertexAngle(circle, tangentPointOfCircles)
				var circleSweepAngle = (startAngle + sweepAngle) - circleStartAngle

				if(sweepAngle > 0) {
					roundSweepAngle *= (-1)
				}

				this.lineTo(tangentPointRoundCircleToLine.x, -tangentPointRoundCircleToLine.y)
				this.arcTo(getRectForCircle(roundCirclePoint.apply { y *= (-1) }, roundValue), roundStartAngle, roundSweepAngle)
				this.arcTo(getRectForCircle(circle.apply { y *= (-1) }, circleRadius), circleStartAngle, circleSweepAngle)
			} else {
				throw IllegalStateException("Cannot find function between circles center points")
			}
		} else {
			throw IllegalStateException("Cannot find function between circles center points")
		}
	} else {
		throw IllegalStateException("Cannot find round circle point")
	}
}

private fun Path.drawRoundArcLine(lineEnd: PointF, drawEndPoint: PointF, roundValue: Float, roundCirclePoint: PointF?, originalFunction: AnalyticalGeometryUtil.LinearFunction?, circle: PointF, circleRadius: Float, startAngle: Float, sweepAngle: Float, forceMoveTo: Boolean) {
	if(roundCirclePoint != null) {
		val tangentPointRoundCircleToLine = if(originalFunction != null) {
			if(originalFunction.a != 0f) {
				val perpendicularFunction = originalFunction.getPerpendicularFunction(roundCirclePoint.x, roundCirclePoint.y)
				getIntersectionPointOfTwoLines(originalFunction.a, originalFunction.b, perpendicularFunction.a, perpendicularFunction.b)
			} else {
				PointF(roundCirclePoint.x, originalFunction.b)
			}
		} else {
			PointF(lineEnd.x, roundCirclePoint.y)
		}

		val functionBetweenCirclesCenterPoints = getLinearFunctionFromTwoGivenPoints(roundCirclePoint.x, roundCirclePoint.y, circle.x, circle.y)
		if(functionBetweenCirclesCenterPoints != null) {
			val tangentPointOfCircles = getPotentialIntersectionPoint(getIntersectionPoints(functionBetweenCirclesCenterPoints, circle, circleRadius), circle, startAngle, sweepAngle)
			if(tangentPointOfCircles != null) {
				var roundSweepAngle = getAnglesBetweenVertex(roundCirclePoint, tangentPointOfCircles, tangentPointRoundCircleToLine).min()
				val roundStartAngle = getVertexAngle(roundCirclePoint, tangentPointOfCircles)
				var circleSweepAngle = getAnglesBetweenVertex(circle, getPointOnCircle(circle, circleRadius, startAngle), tangentPointOfCircles).min()

				if(sweepAngle < 0) {
					circleSweepAngle *= (-1)
				} else {
					roundSweepAngle *= (-1)
				}

				this.arcTo(getRectForCircle(circle.apply { y *= (-1) }, circleRadius), startAngle, circleSweepAngle)
				this.arcTo(getRectForCircle(roundCirclePoint.apply { y *= (-1) }, roundValue), roundStartAngle, roundSweepAngle)
				if(forceMoveTo) this.lineTo(drawEndPoint.x, -drawEndPoint.y)

			} else {
				throw IllegalStateException("Cannot find function between circles center points")
			}
		} else {
			throw IllegalStateException("Cannot find function between circles center points")
		}
	} else {
		throw IllegalStateException("Cannot find round circle point")
	}
}

private fun getPotentialIntersectionPoint(points: List<PointF>, circleCenterPoint: PointF, startAngle: Float, sweepAngle: Float, reverse: Boolean = false): PointF? {
	val potentialPoints = ArrayList<Pair<PointF, Float>>()

	points.forEach {
		val angle = getVertexAngle(circleCenterPoint, it)
		if(angle >= min(startAngle, startAngle + sweepAngle) && angle <= max(startAngle, startAngle + sweepAngle)) {
			potentialPoints.add(Pair(it, angle))
		}
	}

	var closestPoint: PointF? = null
	var angle = 360f
	potentialPoints.forEach {
		if(reverse) {
			if(abs(it.second - (startAngle + sweepAngle)) < angle) {
				closestPoint = it.first
				angle = abs(it.second - (startAngle + sweepAngle))
			}
		} else {
			if(abs(it.second - startAngle) < angle) {
				closestPoint = it.first
				angle = abs(it.second - startAngle)
			}
		}

	}
	return closestPoint
}

fun isVertexBetween(referencePointF: PointF, firstVertex: PointF, secondVertex: PointF, checkVertex: PointF): Boolean {
	val angle = getAnglesBetweenVertex(referencePointF, firstVertex, secondVertex).min()
	val firstAngle = getAnglesBetweenVertex(referencePointF, firstVertex, checkVertex).min()
	val secondAngle = getAnglesBetweenVertex(referencePointF, secondVertex, checkVertex).min()

	return firstAngle + secondAngle == angle
}

fun getRectForCircle(circleCenterPoint: PointF, radius: Float) = RectF(circleCenterPoint.x - radius, circleCenterPoint.y - radius, circleCenterPoint.x + radius, circleCenterPoint.y + radius)

private fun isSweepAngleNegative(startAngle: Float, endAngle: Float, sweepAngle: Float): Boolean {
	var endAngleNew = endAngle
	if(startAngle + sweepAngle == endAngleNew) {
		return false
	}
	if(startAngle - sweepAngle < 0) {
		endAngleNew = startAngle - sweepAngle
	}
	if(startAngle - sweepAngle == endAngleNew) {
		if(startAngle > endAngleNew)
			return true
	}
	return false
}

fun Path.moveTo(point: PointF) {
	this.moveTo(point.x, point.y)
}

fun Path.lineTo(point: PointF) {
	this.lineTo(point.x, point.y)
}