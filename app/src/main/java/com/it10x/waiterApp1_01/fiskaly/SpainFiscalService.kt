package com.it10x.waiterApp1_01.fiskaly

import com.it10x.waiterApp1_01.data.pos.entities.PosKotItemEntity
import com.it10x.waiterApp1_01.fiscal.FiscalContext
import com.it10x.waiterApp1_01.fiscal.FiscalService
import com.it10x.waiterApp1_01.ui.payment.PaymentInput

class SpainFiscalService : FiscalService {

    override suspend fun start(): FiscalContext {
        return FiscalContext(null, null)
    }

    override suspend fun finish(
        context: FiscalContext,
        payments: List<PaymentInput>,
        items: List<PosKotItemEntity>
    ) {
        // TODO: implement SIGN ES later
    }

    override suspend fun cancel(context: FiscalContext) {
        // TODO
    }
}