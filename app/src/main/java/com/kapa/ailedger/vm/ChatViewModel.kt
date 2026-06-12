package com.kapa.ailedger.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kapa.ailedger.ai.AiAssistant
import com.kapa.ailedger.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

class ChatViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val settings = SettingsStore(app)

    val messages: StateFlow<List<ChatMessage>> = db.chatDao().all()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending

    private val _pendingAction = MutableStateFlow<PendingAction?>(null)
    val pendingAction: StateFlow<PendingAction?> = _pendingAction

    fun clear() = viewModelScope.launch {
        db.chatDao().clear()
        _pendingAction.value = null
    }

    fun cancelPending() {
        _pendingAction.value = null
        viewModelScope.launch {
            db.chatDao().insert(ChatMessage(role = "assistant", content = "已取消待确认的记账～"))
        }
    }

    fun confirmPending(ledger: Ledger?) {
        val pending = _pendingAction.value ?: return
        viewModelScope.launch {
            db.chatDao().insert(ChatMessage(role = "user", content = "确认记账"))
            _sending.value = true
            val finalText = executeActions("<action>${pending.actionJson}</action>", ledger)
            db.chatDao().insert(ChatMessage(role = "assistant", content = finalText))
            _pendingAction.value = null
            _sending.value = false
        }
    }

    fun send(text: String, ledger: Ledger?, txns: List<Txn>, debts: List<Debt>) {
        if (text.isBlank() || _sending.value) return
        viewModelScope.launch {
            val apiKey = settings.apiKey.first()
            db.chatDao().insert(ChatMessage(role = "user", content = text))
            if (apiKey.isBlank()) {
                db.chatDao().insert(ChatMessage(role = "assistant", content = "还没有配置 API Key。请到「设置」页选择服务商并填写 API Key 后再来找我聊天～"))
                return@launch
            }
            _sending.value = true
            val model = settings.model.first()
            val baseUrl = settings.baseUrl.first()
            val debtSummary = if (debts.isEmpty()) "无" else debts.joinToString("；") { d ->
                val dir = if (d.type == DebtType.LEND) "${d.person}欠我" else "我欠${d.person}"
                val status = if (d.settled) "已结清" else "已还${d.repaid}"
                "$dir ${d.amount}${d.currency}（$status）"
            }
            var system = AiAssistant.buildContext(
                ledgerName = ledger?.name ?: "默认账本",
                ledgerCurrency = ledger?.currency ?: "CNY",
                txns = txns,
                debtSummary = debtSummary
            )
            _pendingAction.value?.let { pending ->
                system += "\n\n⚠️ 用户上一轮有未确认的待记账请求：${pending.summary}。用户本轮消息很可能是对此的确认或补充信息。如果用户明确确认或补充了关键信息，请用 <action> 正式完成记账。如果用户取消或转移了新话题，则忽略待确认请求。"
            }
            val history = db.chatDao().allOnce().takeLast(20).map { it.role to it.content }
            val result = AiAssistant.complete(baseUrl, apiKey, model, system, history)
            result.onSuccess { reply ->
                val finalText = executeActions(reply, ledger)
                db.chatDao().insert(ChatMessage(role = "assistant", content = finalText))
            }.onFailure { e ->
                db.chatDao().insert(ChatMessage(role = "assistant", content = "出错了：${e.message}"))
            }
            _sending.value = false
        }
    }

    private val actionRegex = Regex("<action>(.*?)</action>", RegexOption.DOT_MATCHES_ALL)
    private val pendingRegex = Regex("<action_pending>(.*?)</action_pending>", RegexOption.DOT_MATCHES_ALL)
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)

    private suspend fun executeActions(reply: String, ledger: Ledger?): String {
        val results = mutableListOf<String>()

        pendingRegex.findAll(reply).forEach { m ->
            runCatching {
                val obj = JSONObject(m.groupValues[1].trim())
                val summary = buildPendingSummary(obj)
                _pendingAction.value = PendingAction(m.groupValues[1].trim(), summary)
            }
        }

        var hasAction = false
        actionRegex.findAll(reply).forEach { m ->
            hasAction = true
            runCatching {
                val obj = JSONObject(m.groupValues[1].trim())
                when (obj.getString("type")) {
                    "add_txn" -> {
                        val ledgerId = ledger?.id ?: return@runCatching
                        val items = obj.getJSONArray("items")
                        for (i in 0 until items.length()) {
                            val it = items.getJSONObject(i)
                            val type = if (it.optString("txnType") == "INCOME") TxnType.INCOME else TxnType.EXPENSE
                            val amount = it.getDouble("amount")
                            if (amount <= 0) continue
                            val date = it.optString("date").takeIf { d -> d.isNotBlank() }
                                ?.let { d -> runCatching { dateFmt.parse(d)?.time }.getOrNull() }
                                ?: System.currentTimeMillis()
                            db.txnDao().insert(
                                Txn(
                                    ledgerId = ledgerId,
                                    type = type,
                                    amount = amount,
                                    currency = it.optString("currency").ifBlank { ledger.currency },
                                    category = it.optString("category").ifBlank { UNCATEGORIZED },
                                    note = it.optString("note"),
                                    date = date
                                )
                            )
                            val sign = if (type == TxnType.EXPENSE) "支出" else "收入"
                            results.add("✅ 已记$sign：${it.optString("category")} ${amount}${it.optString("currency").ifBlank { ledger.currency }} ${it.optString("note")}")
                        }
                    }
                    "add_debt" -> {
                        val amount = obj.getDouble("amount")
                        if (amount <= 0) return@runCatching
                        val type = if (obj.optString("debtType") == "BORROW") DebtType.BORROW else DebtType.LEND
                        db.debtDao().insert(
                            Debt(
                                person = obj.getString("person"),
                                type = type,
                                amount = amount,
                                currency = obj.optString("currency").ifBlank { ledger?.currency ?: "CNY" },
                                note = obj.optString("note")
                            )
                        )
                        val dir = if (type == DebtType.LEND) "${obj.getString("person")} 欠我" else "我欠 ${obj.getString("person")}"
                        results.add("✅ 已登记借还款：$dir $amount${obj.optString("currency").ifBlank { ledger?.currency ?: "CNY" }}")
                    }
                }
            }.onFailure { results.add("⚠️ 有一条记录解析失败，请手动记一下") }
        }

        if (hasAction) {
            _pendingAction.value = null
        }

        val cleaned = reply.replace(actionRegex, "").replace(pendingRegex, "").trim()
        return if (results.isEmpty()) cleaned
        else (cleaned + "\n\n" + results.joinToString("\n")).trim()
    }

    private fun buildPendingSummary(obj: JSONObject): String {
        return when (obj.optString("type")) {
            "add_txn" -> {
                val items = obj.optJSONArray("items") ?: return "待确认账单"
                val parts = mutableListOf<String>()
                for (i in 0 until items.length()) {
                    val it = items.getJSONObject(i)
                    val sign = if (it.optString("txnType") == "INCOME") "+" else "-"
                    val cat = it.optString("category").ifBlank { "未知分类" }
                    val amount = it.optDouble("amount", 0.0)
                    val currency = it.optString("currency").ifBlank { "CNY" }
                    val note = it.optString("note")
                    parts.add("$sign $cat ${amount}$currency${if (note.isNotBlank()) "（$note）" else ""}")
                }
                parts.joinToString("；")
            }
            "add_debt" -> {
                val person = obj.optString("person", "某人")
                val dir = if (obj.optString("debtType") == "BORROW") "我欠" else "借出给"
                val amount = obj.optDouble("amount", 0.0)
                val currency = obj.optString("currency").ifBlank { "CNY" }
                "$dir$person ${amount}$currency"
            }
            else -> "待确认记录"
        }
    }
}