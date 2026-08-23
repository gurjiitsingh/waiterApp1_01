package com.it10x.waiterApp1_01.data.pos.repository

import com.it10x.waiterApp1_01.data.pos.AppDatabase
import com.it10x.waiterApp1_01.data.pos.model.ModifierGroupWithItems

class ModifierRepository(private val db: AppDatabase) {

    suspend fun getModifiersForProduct(productId: String): List<ModifierGroupWithItems> {

        val mappings = db.productModifierDao().getByProduct(productId)

        val result = mutableListOf<ModifierGroupWithItems>()

        for (map in mappings) {

            val group = db.modifierGroupDao()
                .getAll()
                .find { it.id == map.groupId } ?: continue

            val items = db.modifierItemDao()
                .getByGroup(group.id)

            result.add(
                ModifierGroupWithItems(
                    group = group,
                    items = items
                )
            )
        }

        return result
    }
}