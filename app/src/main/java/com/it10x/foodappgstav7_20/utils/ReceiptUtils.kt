package com.it10x.waiterApp1_01.utils

fun formatAmount(value: Double?): String {
    return "%.2f".format(value ?: 0.0)
}
