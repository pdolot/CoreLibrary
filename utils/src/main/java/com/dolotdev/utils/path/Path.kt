package com.dolotdev.utils.path

import android.graphics.Path
import android.graphics.PointF
import com.dolotdev.utils.extension.roundLines
import com.dolotdev.utils.math.AnalyticalGeometryUtil.getPointOnCircle

class Path : Path() {

	val points = arrayListOf<PointF>()

	override fun addArc(left: Float, top: Float, right: Float, bottom: Float, startAngle: Float, sweepAngle: Float) {
		super.addArc(left, top, right, bottom, startAngle, sweepAngle)
		val centerPoint = PointF(((left + right) / 2f), (top + bottom) / 2f)
		val radius = right - centerPoint.x
		points.add(getPointOnCircle(centerPoint, radius, startAngle, false))
		points.add(getPointOnCircle(centerPoint, radius, startAngle + sweepAngle, false))
	}

	override fun arcTo(left: Float, top: Float, right: Float, bottom: Float, startAngle: Float, sweepAngle: Float, forceMoveTo: Boolean) {
		super.arcTo(left, top, right, bottom, startAngle, sweepAngle, forceMoveTo)
		val centerPoint = PointF(((left + right) / 2f), (top + bottom) / 2f)
		val radius = right - centerPoint.x
		points.add(getPointOnCircle(centerPoint, radius, startAngle, false))
		points.add(getPointOnCircle(centerPoint, radius, startAngle + sweepAngle, false))
	}

	override fun cubicTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
		super.cubicTo(x1, y1, x2, y2, x3, y3)
		points.add(PointF(x3, y3))
	}

	override fun quadTo(x1: Float, y1: Float, x2: Float, y2: Float) {
		super.quadTo(x1, y1, x2, y2)
		points.add(PointF(x2, y2))
	}

	override fun moveTo(x: Float, y: Float) {
		super.moveTo(x, y)
		points.add(PointF(x, y))
	}

	override fun lineTo(x: Float, y: Float) {
		super.lineTo(x, y)
		points.add(PointF(x, y))
	}

	override fun reset() {
		super.reset()
		points.clear()
	}

	fun roundClose(roundRadius: Float) {
		roundLines(points[0], points[1], roundRadius)
		super.close()
	}

	fun lineTo(point: PointF) {
		lineTo(point.x, point.y)
	}

}