package com.it10x.waiterApp1_01.ui.cart

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.it10x.waiterApp1_01.data.pos.manager.TableSyncManager
import com.it10x.waiterApp1_01.data.pos.repository.CartRepository
import com.it10x.waiterApp1_01.data.pos.repository.CategoryRepository
import com.it10x.waiterApp1_01.domain.usecase.TableReleaseUseCase

class CartViewModelFactory(
    private val app: Application,
    private val repository: CartRepository,
    private val categoryRepository: CategoryRepository,
    private val tableReleaseUseCase: TableReleaseUseCase,
    private val tableSyncManager: TableSyncManager
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>,
        extras: CreationExtras
    ): T {
        if (modelClass.isAssignableFrom(CartViewModel::class.java)) {
            val handle: SavedStateHandle = extras.createSavedStateHandle()
            return CartViewModel(
                app = app,
                repository = repository,
                categoryRepository = categoryRepository,
                tableReleaseUseCase = tableReleaseUseCase,
                tableSyncManager = tableSyncManager,
                savedStateHandle = handle
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
