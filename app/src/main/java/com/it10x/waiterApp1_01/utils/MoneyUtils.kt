package com.it10x.waiterApp1_01.utils


object MoneyUtils {

    fun toPaise(amount: Double): Long {
        return kotlin.math.round(amount * 100).toLong()
    }
    fun fromPaise(paise: Long): Double {
        return paise / 100.0
    }

}

