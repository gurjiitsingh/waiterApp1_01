package com.it10x.waiterApp1_01.fiscal

import com.it10x.waiterApp1_01.fiskaly.GermanyFiscalService
import com.it10x.waiterApp1_01.fiskaly.IndiaFiscalService
import com.it10x.waiterApp1_01.fiskaly.SpainFiscalService
import com.it10x.waiterApp1_01.fiskaly.FiskalyRepository

fun getFiscalService(
    countryCode: String,
    fiskalyRepository: FiskalyRepository? = null
): FiscalService {

    return when (countryCode) {
        "DE" -> GermanyFiscalService(fiskalyRepository!!)
        "IN" -> IndiaFiscalService()
        "ES" -> SpainFiscalService()
        else -> IndiaFiscalService()
    }
}