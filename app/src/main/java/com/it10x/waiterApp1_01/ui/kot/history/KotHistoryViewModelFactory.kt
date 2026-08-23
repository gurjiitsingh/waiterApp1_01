package com.it10x.waiterApp1_01.ui.kot.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.it10x.waiterApp1_01.data.pos.repository.KotRepository

class KotHistoryViewModelFactory(
    private val repository: KotRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (modelClass.isAssignableFrom(KotHistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return KotHistoryViewModel(repository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel")
    }
}