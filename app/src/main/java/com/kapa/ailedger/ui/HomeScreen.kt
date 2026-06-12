package com.kapa.ailedger.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kapa.ailedger.data.*
import com.kapa.ailedger.ui.theme.*
import com.kapa.ailedger.vm.AppViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val dayFmt = SimpleDateFormat("M月d日 EEEE", Locale.CHINA)
private val dayKeyFmt = SimpleDateFormat("yyyyMMdd", Locale.CHINA)

fun fmtMoney(v: Double): String =
    if (v % 1.0 == 0.0) String.format(Locale.CHINA, "%,.0f", v) else String.format(Locale.CHINA, "%,.2f", v)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: AppViewModel) {
    val ledgers by vm.ledgers.collectAsState()
    val ledger by vm.currentLedger.collectAsState()
    val txns by vm.txns.collectAsState()
    val rates by vm.rates.collectAsState()
    val categorizing by vm.categorizing.collectAsState()

    var showAdd by remember { mutableStateOf(false) }
    var showLedgerMenu by remember { mutableStateOf(false) }
    var showNewLedger by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Txn?>(null) }

    val base = ledger?.currency ?: "CNY"
    val now = Calendar.getInstance()
    val monthTxns = txns.filter {
        val c = Calendar.getInstance().apply { timeInMillis = it.date }
        c.get(Calendar.YEAR) == now.get(Calendar.YEAR) && c.get(Calendar.MONTH) == now.get(Calendar.MONTH)
    }
    val monthExpense = monthTxns.filter { it.type == TxnType.EXPENSE }
        .sumOf { Currencies.convert(it.amount, it.currency, base, rates) }
    val monthIncome = monthTxns.filter { it.type == TxnType.INCOME }
        .sumOf { Currencies.convert(it.amount, it.currency, base, rates) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { showLedgerMenu = true }) {
                            Text(ledger?.name ?: "账本", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Icon(Icons.Filled.ExpandMore, null)
                        }
                        DropdownMenu(expanded = showLedgerMenu, onDismissRequest = { showLedgerMenu = false }) {
                            ledgers.forEach { l ->
                                DropdownMenuItem(
                                    text = { Text("${l.name}（${l.currency}）") },
                                    onClick = { vm.selectLedger(l.id); showLedgerMenu = false }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(text = { Text("＋ 新建账本") }, onClick = { showLedgerMenu = false; showNewLedger = true })
                        }
                    }
                },
                actions = {
                    TextButton(onClick = { vm.aiCategorize() }, enabled = !categorizing) {
                        if (categorizing) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Filled.AutoAwesome, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("AI归类")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = TextPrimary,
                    actionIconContentColor = IosBlue
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                containerColor = IosBlue,
                contentColor = Color.White
            ) { Icon(Icons.Filled.Add, "记一笔") }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().navigationBarsPadding()) {
            Card(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, CardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "${now.get(Calendar.MONTH) + 1}月结余（$base）",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        "${Currencies.symbol(base)} ${fmtMoney(monthIncome - monthExpense)}",
                        color = TextPrimary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Text("收入 ${fmtMoney(monthIncome)}", color = IosGreen, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.width(16.dp))
                        Text("支出 ${fmtMoney(monthExpense)}", color = IosRed, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            if (txns.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyLedgerIllustration()
                }
            } else {
                val grouped = txns.groupBy { dayKeyFmt.format(it.date) }
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
                    grouped.forEach { (_, dayTxns) ->
                        item(key = "h" + dayTxns.first().id) {
                            Text(
                                dayFmt.format(dayTxns.first().date),
                                Modifier.padding(start = 20.dp, top = 14.dp, bottom = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                        items(dayTxns, key = { it.id }) { t ->
                            TxnRow(t, onLongDelete = { toDelete = t })
                        }
                    }
                }
            }
        }
    }

    if (showAdd) AddTxnSheet(ledger, onDismiss = { showAdd = false }) { type, amount, cur, cat, note, date ->
        vm.addTxn(type, amount, cur, cat, note, date); showAdd = false
    }
    if (showNewLedger) NewLedgerDialog(
        onDismiss = { showNewLedger = false },
        onConfirm = { name, cur -> vm.addLedger(name, cur); showNewLedger = false }
    )
    toDelete?.let { t ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("删除这笔账单？") },
            text = { Text("${t.category} ${Currencies.symbol(t.currency)}${fmtMoney(t.amount)} ${t.note}") },
            confirmButton = { TextButton(onClick = { vm.deleteTxn(t); toDelete = null }) { Text("删除", color = IosRed) } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("取消") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TxnRow(t: Txn, onLongDelete: () -> Unit) {
    val isExpense = t.type == TxnType.EXPENSE
    val tint = if (isExpense) IosRed else IosGreen
    ListItem(
        headlineContent = { Text(t.category, color = TextPrimary) },
        supportingContent = { if (t.note.isNotBlank()) Text(t.note, maxLines = 1, color = TextSecondary) },
        leadingContent = {
            Box(
                Modifier
                    .size(38.dp)
                    .background(tint.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) { Text(CATEGORY_EMOJI[t.category] ?: t.category.take(1), fontSize = 18.sp) }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    (if (isExpense) "-" else "+") + Currencies.symbol(t.currency) + fmtMoney(t.amount),
                    color = tint,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onLongDelete) {
                    Icon(Icons.Filled.Delete, "删除", Modifier.size(18.dp), tint = TextSecondary)
                }
            }
        }
    )
}

@Composable
fun NewLedgerDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("CNY") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建账本") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("账本名称") }, singleLine = true)
                Spacer(Modifier.height(12.dp))
                CurrencyPicker(currency) { currency = it }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onConfirm(name.trim(), currency) }) { Text("创建") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyPicker(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = "$selected ${Currencies.list.firstOrNull { it.code == selected }?.cname ?: ""}",
            onValueChange = {},
            readOnly = true,
            label = { Text("币种") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Currencies.list.forEach { c ->
                DropdownMenuItem(
                    text = { Text("${c.code} ${c.symbol} ${c.cname}") },
                    onClick = { onSelect(c.code); expanded = false }
                )
            }
        }
    }
}
