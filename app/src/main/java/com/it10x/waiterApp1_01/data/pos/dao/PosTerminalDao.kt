package com.it10x.waiterApp1_01.data.pos.dao


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.it10x.waiterApp1_01.data.pos.entities.PosTerminalEntity


@Dao
interface PosTerminalDao {

    // =====================================================
    // INSERT / UPDATE
    // =====================================================

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun insert(
        terminal: PosTerminalEntity
    )


    // =====================================================
    // GET TERMINAL BY ID
    // =====================================================

    @Query("""
        SELECT *
        FROM pos_terminal
        WHERE terminalId = :terminalId
        LIMIT 1
    """)
    suspend fun getById(
        terminalId: String
    ): PosTerminalEntity?


    // =====================================================
    // GET ACTIVE TERMINAL
    // =====================================================

    @Query("""
        SELECT *
        FROM pos_terminal
        WHERE isActive = 1
        LIMIT 1
    """)
    suspend fun getActiveTerminal(): PosTerminalEntity?


    // =====================================================
    // GET TERMINAL FOR OUTLET
    // =====================================================

    @Query("""
        SELECT *
        FROM pos_terminal
        WHERE outletId = :outletId
          AND isActive = 1
        LIMIT 1
    """)
    suspend fun getActiveTerminalForOutlet(
        outletId: String
    ): PosTerminalEntity?


    // =====================================================
    // DELETE ALL
    // =====================================================

    @Query("""
        DELETE FROM pos_terminal
    """)
    suspend fun deleteAll()

}