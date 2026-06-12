package com.kapa.ailedger.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

/** 账本：每个账本有独立的记账币种 */
@Entity(tableName = "ledgers")
data class Ledger(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val currency: String = "CNY",
    val createdAt: Long = System.currentTimeMillis()
)

enum class TxnType { EXPENSE, INCOME }

/** 一笔账单 */
@Entity(tableName = "txns")
data class Txn(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ledgerId: Long,
    val type: TxnType,
    val amount: Double,
    val currency: String,
    val category: String,
    val note: String = "",
    val date: Long = System.currentTimeMillis()
)

enum class DebtType { LEND, BORROW } // LEND=借出(别人欠我) BORROW=借入(我欠别人)

/** 借款 / 欠款 */
@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val person: String,
    val type: DebtType,
    val amount: Double,
    val repaid: Double = 0.0,
    val currency: String,
    val note: String = "",
    val date: Long = System.currentTimeMillis(),
    val settled: Boolean = false
)

/** AI 助手聊天记录 */
@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String, // "user" 或 "assistant"
    val content: String,
    val ts: Long = System.currentTimeMillis()
)

class Converters {
    @TypeConverter fun txnTypeToString(t: TxnType) = t.name
    @TypeConverter fun stringToTxnType(s: String) = TxnType.valueOf(s)
    @TypeConverter fun debtTypeToString(t: DebtType) = t.name
    @TypeConverter fun stringToDebtType(s: String) = DebtType.valueOf(s)
}

data class PendingAction(
    val actionJson: String,
    val summary: String
)

val DEFAULT_EXPENSE_CATEGORIES = listOf("餐饮", "交通", "购物", "居住", "娱乐", "医疗", "学习", "通讯", "人情", "其他")
val DEFAULT_INCOME_CATEGORIES = listOf("工资", "兼职", "投资收益", "红包", "退款", "其他")
const val UNCATEGORIZED = "未分类"

val CATEGORY_EMOJI = mapOf(
    "餐饮" to "🍜",
    "交通" to "🚕",
    "购物" to "🛍️",
    "居住" to "🏠",
    "娱乐" to "🎮",
    "医疗" to "💊",
    "学习" to "📚",
    "通讯" to "📱",
    "人情" to "🎁",
    "其他" to "✨",
    "工资" to "💰",
    "兼职" to "💼",
    "投资收益" to "📈",
    "红包" to "🧧",
    "退款" to "↩️",
    "未分类" to "📝"
)
