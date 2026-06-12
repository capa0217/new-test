package com.kapa.ailedger.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kapa.ailedger.ai.AiAssistant
import com.kapa.ailedger.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    val settings = SettingsStore(app)

    val ledgers: StateFlow<List<Ledger>> = db.ledgerDao().all()
        .onEach { list ->
            if (list.isEmpty()) db.ledgerDao().insert(Ledger(name = "日常账本", currency = "CNY"))
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _currentLedgerId = MutableStateFlow<Long?>(null)
    val currentLedger: StateFlow<Ledger?> = combine(ledgers, _currentLedgerId) { list, id ->
        list.firstOrNull { it.id == id } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val txns: StateFlow<List<Txn>> = currentLedger
        .filterNotNull()
        .flatMapLatest { db.txnDao().byLedger(it.id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val debts: StateFlow<List<Debt>> = db.debtDao().all()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val rates: StateFlow<Map<String, Double>> = settings.rates
        .stateIn(viewModelScope, SharingStarted.Eagerly, Currencies.defaultRatesToCny)

    val apiKey: StateFlow<String> = settings.apiKey.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val model: StateFlow<String> = settings.model.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsStore.DEFAULT_MODEL)
    val baseUrl: StateFlow<String> = settings.baseUrl.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsStore.DEFAULT_BASE_URL)

    // ---- 账本 ----
    fun selectLedger(id: Long) { _currentLedgerId.value = id }
    fun addLedger(name: String, currency: String) = viewModelScope.launch {
        val id = db.ledgerDao().insert(Ledger(name = name, currency = currency))
        _currentLedgerId.value = id
    }
    fun deleteLedger(l: Ledger) = viewModelScope.launch {
        db.txnDao().deleteByLedger(l.id)
        db.ledgerDao().delete(l)
        if (_currentLedgerId.value == l.id) _currentLedgerId.value = null
    }

    // ---- 账单 ----
    fun addTxn(type: TxnType, amount: Double, currency: String, category: String, note: String, date: Long) =
        viewModelScope.launch {
            val ledger = currentLedger.value ?: return@launch
            db.txnDao().insert(Txn(ledgerId = ledger.id, type = type, amount = amount, currency = currency, category = category, note = note, date = date))
        }
    fun deleteTxn(t: Txn) = viewModelScope.launch { db.txnDao().delete(t) }

    // ---- 借还款 ----
    fun addDebt(person: String, type: DebtType, amount: Double, currency: String, note: String) =
        viewModelScope.launch { db.debtDao().insert(Debt(person = person, type = type, amount = amount, currency = currency, note = note)) }
    fun repayDebt(d: Debt, amount: Double) = viewModelScope.launch {
        val newRepaid = (d.repaid + amount).coerceAtMost(d.amount)
        db.debtDao().update(d.copy(repaid = newRepaid, settled = newRepaid >= d.amount))
    }
    fun deleteDebt(d: Debt) = viewModelScope.launch { db.debtDao().delete(d) }

    // ---- 设置 ----
    fun saveApiKey(v: String) = viewModelScope.launch { settings.setApiKey(v.trim()) }
    fun saveModel(v: String) = viewModelScope.launch { settings.setModel(v.trim()) }
    fun saveBaseUrl(v: String) = viewModelScope.launch { settings.setBaseUrl(v.trim()) }
    fun saveRate(code: String, rate: Double) = viewModelScope.launch { settings.setRate(code, rate) }

    // ---- AI 归类 ----
    private val _categorizing = MutableStateFlow(false)
    val categorizing: StateFlow<Boolean> = _categorizing
    private val _toast = MutableSharedFlow<String>()
    val toast: SharedFlow<String> = _toast

    fun aiCategorize() = viewModelScope.launch {
        val key = apiKey.value
        if (key.isBlank()) { _toast.emit("请先在设置中填写 API Key"); return@launch }
        val targets = txns.value.filter { it.category == UNCATEGORIZED || it.category.isBlank() }.take(40)
        if (targets.isEmpty()) { _toast.emit("没有未分类的账单"); return@launch }
        _categorizing.value = true
        val result = AiAssistant.categorize(baseUrl.value, key, model.value, targets, DEFAULT_EXPENSE_CATEGORIES + DEFAULT_INCOME_CATEGORIES)
        result.onSuccess { mapping ->
            mapping.forEach { (id, cat) -> db.txnDao().setCategory(id, cat) }
            _toast.emit("已归类 ${mapping.size} 条账单")
        }.onFailure { _toast.emit("归类失败：${it.message}") }
        _categorizing.value = false
    }
}
