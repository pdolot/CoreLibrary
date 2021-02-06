package com.dolotdev.utils.math

import android.graphics.PointF
import com.dolotdev.utils.extension.min
import kotlin.math.*

object AnalyticalGeometryUtil {

    private const val RADIAN = 57.2957795f

    /**
     * @param normalize - if true method find point in mathematical coordinate system, if false find point in screen coordinate system
     */
    fun getPointOnCircle(circleCenterPoint: PointF, circleRadius: Float, angle: Float, normalize: Boolean = true): PointF {
        val trueAngle = when{
            angle < 0 -> 360 + angle
            angle > 360 -> angle - 360
            else -> angle
        }
        return if(normalize) {
            PointF(circleCenterPoint.x + (circleRadius * cos(Math.toRadians(trueAngle.toDouble()))).toFloat(), circleCenterPoint.y - (circleRadius * sin(Math.toRadians(trueAngle.toDouble()))).toFloat())
        }
        else {
            PointF(circleCenterPoint.x + (circleRadius * cos(Math.toRadians(trueAngle.toDouble()))).toFloat(), circleCenterPoint.y + (circleRadius * sin(Math.toRadians(trueAngle.toDouble()))).toFloat())
        }
    }

    fun getArcType(lineStart: PointF, lineEnd: PointF, circleCenterPoint: PointF): ArcType? {
        val angle = getAnglesBetweenVertex(lineEnd, lineStart, circleCenterPoint, false).min()
        return when {
            angle > 90f -> {
                ArcType.OUTER
            }
            angle < 90f -> {
                ArcType.INNER
            }
            else -> {
                null
            }
        }
    }

    fun getProportionPoint(point: PointF, segment: Double, length: Double, dx: Double, dy: Double): PointF {
        val factor = segment / length
        return PointF((point.x - (dx * factor)).toFloat(), (point.y - (dy * factor)).toFloat())
    }

    fun getLength(dx: Double, dy: Double) = sqrt((dx * dx) + (dy * dy))

    fun getIntersectionPoints(linearFunction: LinearFunction, circleCenterPoint: PointF, circleRadius: Float): List<PointF> {
        val aParam = linearFunction.a
        val bParam = linearFunction.b

        val circleX = circleCenterPoint.x
        val circleY = circleCenterPoint.y

        val a = 1f + (aParam * aParam)
        val b = (-2f * circleX) + (2f * aParam * bParam) - (2f * aParam * circleY)
        val c = (circleX * circleX) - (2f * bParam * circleY) + (bParam * bParam) + (circleY * circleY) - (circleRadius * circleRadius)

        val delta = (b * b) - (4f * a * c)

        return when {
            delta > 0f -> {
                val x1 = (-b + sqrt(delta)) / (2f * a)
                val x2 = (-b - sqrt(delta)) / (2f * a)

                return listOf(PointF(x1, aParam * x1 + bParam), PointF(x2, aParam * x2 + bParam))
            }
            delta == 0f -> {
                val x = -b / (2f * a)
                return listOf(PointF(x, aParam * x + bParam))
            }
            else -> emptyList()
        }
    }

    fun getIntersectionPoints(x: Float, circleCenterPoint: PointF, circleRadius: Float): List<PointF> {

        val circleX = circleCenterPoint.x
        val circleY = circleCenterPoint.y

        val a = 1f
        val b = -2f * circleY
        val c = (x * x) - (2f * circleX * x) + (circleX * circleX) + (circleY * circleY) - (circleRadius * circleRadius)
        val delta = (b * b) - (4f * a * c)

        return when {
            delta > 0f -> {
                val y1 = (-b + sqrt(delta)) / 2f
                val y2 = (-b - sqrt(delta)) / 2f

                return listOf(PointF(x, y1), PointF(x, a * y2))
            }
            delta == 0f -> {
                return listOf(PointF(x, -b / (2f * a)))
            }
            else -> emptyList()
        }


    }

    fun getLinearFunctionFromTwoGivenPoints(x1: Float, y1: Float, x2: Float, y2: Float): LinearFunction? {
        if(x1 == x2) return null
        val a = (y2 - y1) / (x2 - x1)
        val b = y1 - (a * x1)
        return LinearFunction(a, b)
    }

    fun getIntersectionPointOfTwoLines(a1: Float, b1: Float, a2: Float, b2: Float): PointF {
        val x = (b2 - b1) / (a1 - a2)
        val y = a1 * x + b1
        return PointF(x, y)
    }

    fun getIntersectionPointOfTwoLines(firstLinearFunction: LinearFunction, secondLinearFunction: LinearFunction): PointF {
        val x = (secondLinearFunction.b - firstLinearFunction.b) / (firstLinearFunction.a - secondLinearFunction.a)
        val y = firstLinearFunction.a * x + firstLinearFunction.b
        return PointF(x, y)
    }


    fun getAnglesBetweenVertex(referencePoint: PointF, second: PointF, third: PointF, normalize: Boolean = false): Pair<Float, Float> {
        val firstVertex = if (normalize) {
            PointF(second.x - referencePoint.x, second.y - referencePoint.y)
        }else{
            PointF(second.x - referencePoint.x, -second.y + referencePoint.y)
        }
        val secondVertex = if (normalize){
            PointF(third.x - referencePoint.x, third.y - referencePoint.y)
        }else{
            PointF(third.x - referencePoint.x, -third.y + referencePoint.y)
        }

        val firstAngle = ((atan2(firstVertex.y, firstVertex.x) * RADIAN) + 360f) % 360
        val secondAngle = ((atan2(secondVertex.y, secondVertex.x) * RADIAN) + 360f) % 360

        val angle = abs(firstAngle - secondAngle)

        return Pair(angle, abs(360f - angle))
    }

    fun getVertexAngle(referencePoint: PointF, point: PointF, fullAngle: Boolean = true, reverse: Boolean = true): Float {
        val vertex = PointF(point.x - referencePoint.x, point.y - referencePoint.y)
        if(!fullAngle) return (atan2(vertex.y, vertex.x) * RADIAN)

        val angle = ((atan2(vertex.y, vertex.x) * RADIAN) + 360f) % 360
        return if(reverse) {
            360f - angle
        } else {
            angle
        }
    }

    class LinearFunction(var a: Float, var b: Float) {

        fun shiftBy(shift: Float, shiftDirection: Int): LinearFunction {
            val b2 = if(shiftDirection == SHIFT_UP) b + (shift * (sqrt(1 + (a * a)))) else b - (shift * (sqrt(1 + (a * a))))
            return LinearFunction(a, b2)
        }

        fun getPerpendicularFunction(x: Float, y: Float): LinearFunction {
            val a2 = -(1.0f / a)
            val b2 = y - (a2 * x)
            return LinearFunction(a2, b2)
        }

        fun getValue(x: Float): Float{
            return (x * a) + b
        }

        companion object {
            const val SHIFT_UP = 1
            const val SHIFT_DOWN = 0
        }
    }

    enum class ArcType {
        OUTER, INNER
    }
}