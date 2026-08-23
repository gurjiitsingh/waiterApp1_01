package com.it10x.waiterApp1_01.ui.settings

import com.it10x.waiterApp1_01.data.PrinterConfig
import com.it10x.waiterApp1_01.data.PrinterRole

data class PrinterSettingsState(
    val printers: Map<PrinterRole, PrinterConfig> = emptyMap()
)
