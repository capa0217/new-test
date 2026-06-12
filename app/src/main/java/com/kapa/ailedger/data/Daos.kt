package com.kapa.ailedger.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {
    @Query("SELECT * FROM ledgers ORDER BY createdAt") fun all(): Flow<List<Ledger>>
    @Insert suspend fun insert(l: Ledger): Long
    @Update suspend fun update(l: Ledger)
    @Delete suspend fun delete(l: Ledger)
}

@Dao
interface TxnDao {
    @Query("SELECT * FROM txns WHERE ledgerId = :ledgerId ORDER BY date DESC") fun byLedger(ledgerId: Long): Flow<List<Txn>>
    @Insert suspend fun insert(t: Txn): Long
    @Update suspend fun update(t: Txn)
    @Delete suspend fun delete(t: Txn)
    @Query("UPDATE txns SET category = :cat WHERE id = :id") suspend fun setCategory(id: Long, cat: String)
    @Query("DELETE FROM txns WHERE ledgerId = :ledgerId") suspend fun deleteByLedger(ledgerId: Long)
}

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts ORDER BY settled ASC, date DESC") fun all(): Flow<List<Debt>>
    @Insert suspend fun insert(d: Debt): Long
    @Update suspend fun update(d: Debt)
    @Delete suspend fun delete(d: Debt)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY ts") fun all(): Flow<List<ChatMessage>>
    @Query("SELECT * FROM chat_messages ORDER BY ts") suspend fun allOnce(): List<ChatMessage>
    @Insert suspend fun insert(m: ChatMessage)
    @Query("DELETE FROM chat_messages") suspend fun clear()
}
