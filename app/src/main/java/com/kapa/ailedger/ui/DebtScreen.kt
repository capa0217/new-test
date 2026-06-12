package com.kapa.ailedger.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.kapa.ailedger.data.*
import com.kapa.ailedger.ui.theme.*
import com.kapa.ailedger.vm.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtScreen(vm: AppViewModel) {
    val debts by vm.debts.collectAsState()
    val rates by vm.rates.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var repaying by remember { mutableStateOf<Debt?>(null) }

    val owedToMe = debts.filter { it.type == DebtType.LEND && !it.settled }
        .sumOf { Currencies.convert(it.amount - it.repaid, it.currency, "CNY", rates) }
    val iOwe = debts.filter { it.type == DebtType.BORROW && !it.settled }
        .sumOf { Currencies.convert(it.amount - it.repaid, it.currency, "CNY", rates) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("借还款", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = TextPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                containerColor = IosBlue,
                contentColor = Color.White
            ) { Icon(Icons.Filled.Add, "新增") }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard("别人欠我", owedToMe, IosGreen, Modifier.weight(1f))
                SummaryCard("我欠别人", iOwe, IosRed, Modifier.weight(1f))
            }
            if (debts.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyDebtIllustration()
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(debts, key = { it.id }) { d ->
                        DebtRow(d, onRepay = { repaying = d }, onDelete = { vm.deleteDebt(d) })
                    }
                }
            }
        }
    }

    if (showAdd) AddDebtDialog(onDismiss = { showAdd = false }) { person, type, amount, cur, note ->
        vm.addDebt(person, type, amount, cur, note); showAdd = false
    }
    repaying?.let { d ->
        RepayDialog(d, onDismiss = { repaying = null }) { amount ->
            vm.repayDebt(d, amount); repaying = null
        }
    }
}

@Composable
private fun SummaryCard(title: String, amountCny: Double, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Text("¥ ${fmtMoney(amountCny)}", color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Text("按汇率折合人民币", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@Composable
private fun DebtRow(d: Debt, onRepay: () -> Unit, onDelete: () -> Unit) {
    val isLend = d.type == DebtType.LEND
    val remaining = d.amount - d.repaid
    val tint = if (isLend) IosGreen else IosRed
    ListItem(
        headlineContent = {
            Text(
                if (isLend) "${d.person} 欠我" else "我欠 ${d.person}",
                color = TextPrimary,
                textDecoration = if (d.settled) TextDecoration.LineThrough else TextDecoration.None
            )
        },
        supportingContent = {
            Column {
                if (d.note.isNotBlank()) Text(d.note, maxLines = 1, color = TextSecondary)
                if (!d.settled && d.repaid > 0) {
                    LinearProgressIndicator(
                        progress = { (d.repaid / d.amount).toFloat() },
                        modifier = Modifier.fillMaxWidth(0.6f).padding(top = 4.dp),
                        color = tint,
                        trackColor = TrackGray
                    )
                }
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${Currencies.symbol(d.currency)}${fmtMoney(if (d.settled) d.amount else remaining)}",
                        color = if (d.settled) TextSecondary else tint,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (d.settled) Text("已结清", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    else TextButton(onClick = onRepay, contentPadding = PaddingValues(0.dp)) { Text(if (isLend) "记还款" else "记归还") }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, "删除", Modifier.size(18.dp), tint = TextSecondary)
                }
            }
        }
    )
}

@Composable
private fun AddDebtDialog(onDismiss: () -> Unit, onConfirm: (String, DebtType, Double, String, String) -> Unit) {
    var person by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(DebtType.LEND) }
    var amountText by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("CNY") }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增借还款") },
        text = {
            Column {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(selected = type == DebtType.LEND, onClick = { type = DebtType.LEND }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("借出") }
                    SegmentedButton(selected = type == DebtType.BORROW, onClick = { type = DebtType.BORROW }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text("借入") }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = person, onValueChange = { person = it }, label = { Text("对方姓名") }, singleLine = true)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { s -> if (s.all { it.isDigit() || it == '.' }) amountText = s },
                    label = { Text("金额") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                CurrencyPicker(currency) { currency = it }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("备注") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(
                enabled = person.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0.0,
                onClick = { onConfirm(person.trim(), type, amountText.toDouble(), currency, note.trim()) }
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun RepayDialog(d: Debt, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var amountText by remember { mutableStateOf(fmtMoney(d.amount - d.repaid).replace(",", "")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("记一笔还款") },
        text = {
            Column {
                Text("剩余 ${Currencies.symbol(d.currency)}${fmtMoney(d.amount - d.repaid)}")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { s -> if (s.all { it.isDigit() || it == '.' }) amountText = s },
                    label = { Text("本次金额") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0.0,
                onClick = { onConfirm(amountText.toDouble()) }
            ) { Text("确认") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
