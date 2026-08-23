package com.it10x.waiterApp1_01.auth

data class PosUser(

    val id: String = "",

    val fullName: String = "",

    val mobile: String = "",

    val employeeId: String = "",

    val role: String = "",

    val allowPosLogin: Boolean = true,

    val active: Boolean = true
)