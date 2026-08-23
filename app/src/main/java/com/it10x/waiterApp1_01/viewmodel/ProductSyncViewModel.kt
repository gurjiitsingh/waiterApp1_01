package com.it10x.waiterApp1_01.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.it10x.waiterApp1_01.data.pos.AppDatabaseProvider
import com.it10x.waiterApp1_01.data.online.repository.ProductSyncRepository
import com.it10x.waiterApp1_01.data.online.repository.ModifierSyncRepository
import com.it10x.waiterApp1_01.data.online.repository.UserSyncRepository
import com.it10x.waiterApp1_01.data.online.repository.ProductRecipeSyncRepository
import com.it10x.waiterApp1_01.data.online.repository.OrderCounterSyncRepository
import com.it10x.waiterApp1_01.data.online.repository.PosTerminalSyncRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


class ProductSyncViewModel(
    app: Application
) : AndroidViewModel(app) {

    private val db =
        AppDatabaseProvider.get(app)


    // =====================================================
    // PRODUCT
    // =====================================================

    private val repo =
        ProductSyncRepository(db)


    // =====================================================
    // POS TERMINAL
    // =====================================================

    private val posTerminalRepo =
        PosTerminalSyncRepository(
            db.posTerminalDao(),
            db.outletDao()
        )


    // =====================================================
    // MODIFIERS
    // =====================================================

    private val modifierRepo =
        ModifierSyncRepository(db)


    // =====================================================
    // USERS
    // =====================================================

    private val userRepo =
        UserSyncRepository(db)


    // =====================================================
    // PRODUCT RECIPES
    // =====================================================

    private val recipeRepo =
        ProductRecipeSyncRepository(db)


    // =====================================================
    // ORDER COUNTER
    // =====================================================

    private val orderCounterRepo =
        OrderCounterSyncRepository(
            db,
            FirebaseFirestore.getInstance()
        )


    // =====================================================
    // STATE
    // =====================================================

    private val _syncing =
        MutableStateFlow(false)

    val syncing: StateFlow<Boolean> =
        _syncing


    private val _status =
        MutableStateFlow<String>("")

    val status: StateFlow<String> =
        _status


    // =====================================================
    // SYNC ALL
    // =====================================================

    fun syncAll() {

        viewModelScope.launch {

            try {

                _syncing.value = true


                // -------------------------------------------------
                // ORDER COUNTER
                // -------------------------------------------------

                _status.value =
                    "Syncing order counter..."

                orderCounterRepo
                    .syncLastOrderSerialNo()


                // -------------------------------------------------
                // CATEGORIES
                // -------------------------------------------------

                _status.value =
                    "Syncing categories..."

                repo.syncCategories()


                // -------------------------------------------------
                // PRODUCTS
                // -------------------------------------------------

                _status.value =
                    "Syncing products..."

                repo.syncProducts()


                // -------------------------------------------------
                // MODIFIER GROUPS
                // -------------------------------------------------

                _status.value =
                    "Syncing modifier groups..."

                modifierRepo
                    .syncModifierGroups()


                // -------------------------------------------------
                // MODIFIER ITEMS
                // -------------------------------------------------

                _status.value =
                    "Syncing modifier items..."

                modifierRepo
                    .syncModifierItems()


                // -------------------------------------------------
                // PRODUCT MODIFIERS
                // -------------------------------------------------

                _status.value =
                    "Syncing product modifiers..."

                modifierRepo
                    .syncProductModifiers()


                // -------------------------------------------------
                // PRODUCT RECIPES
                // -------------------------------------------------

                _status.value =
                    "Syncing product recipes..."

                recipeRepo
                    .syncProductRecipes()


                // -------------------------------------------------
                // USERS
                // -------------------------------------------------

                _status.value =
                    "Syncing users..."

                userRepo
                    .syncUsers()


                // -------------------------------------------------
                // POS TERMINALS
                // -------------------------------------------------

                _status.value =
                    "Syncing POS terminals..."

                posTerminalRepo
                    .syncPosTerminal()


                // -------------------------------------------------
                // COMPLETE
                // -------------------------------------------------

                _status.value =
                    "Sync complete 🎉"


            } catch (e: Exception) {

                _status.value =
                    "Sync failed: ${e.message}"

            } finally {

                _syncing.value = false

            }
        }
    }


    // =====================================================
    // SYNC USERS ONLY
    // =====================================================

    fun syncUsersOnly() {

        viewModelScope.launch {

            try {

                _syncing.value = true


                _status.value =
                    "Syncing users..."

                userRepo.syncUsers()


                _status.value =
                    "Users updated successfully 🎉"


            } catch (e: Exception) {

                _status.value =
                    "User sync failed: ${e.message}"

            } finally {

                _syncing.value = false

            }
        }
    }
}