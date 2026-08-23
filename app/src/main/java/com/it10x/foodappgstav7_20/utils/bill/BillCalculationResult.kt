package com.it10x.waiterApp1_01.utils.bill

data class BillCalculationResult(

    val itemSubtotalPaise: Long,

    val exclusiveTaxPaise: Long,

    val inclusiveTaxPaise: Long,

    val totalTaxPaise: Long,

    val discountPaise: Long,

    val deliveryFeePaise: Long,

    val deliveryTaxPaise: Long,

    val grandTotalPaise: Long
)