package com.dolotdev.utils.extension

fun <F: Float,S: Float>Pair<F, S>.min(): Float{
    return kotlin.math.min(first, second)
}

fun <F: Double,S: Double>Pair<F, S>.min(): Double{
    return kotlin.math.min(first, second)
}

fun <F: Int,S: Int>Pair<F, S>.min(): Int{
    return kotlin.math.min(first, second)
}

fun <F: Long,S: Long>Pair<F, S>.min(): Long{
    return kotlin.math.min(first, second)
}

fun <F: Float,S: Float>Pair<F, S>.max(): Float{
    return kotlin.math.max(first, second)
}

fun <F: Double,S: Double>Pair<F, S>.max(): Double{
    return kotlin.math.max(first, second)
}

fun <F: Int,S: Int>Pair<F, S>.max(): Int{
    return kotlin.math.max(first, second)
}

fun <F: Long,S: Long>Pair<F, S>.max(): Long{
    return kotlin.math.max(first, second)
}