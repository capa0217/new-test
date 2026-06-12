package com.kapa.ailedger.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.kapa.ailedger.data.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTxnSheet(
    ledger: Ledger?,
    onDismiss: () -> Unit,
    onSave: (TxnType, Double, String, String, String, Long) -> Unit
) {
    var type by remember { mutableStateOf(TxnType.EXPENSE) }
    var amountText by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(ledger?.currency ?: "CNY") }
    var category by remember { mutableStateOf(UNCATEGORIZED) }
    var note by remember { mutableStateOf("") }
    val cats = if (type == TxnType.EXPENSE) DEFAULT_EXPENSE_CATEGORIES else DEFAULT_INCOME_CATEGORIES

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = type == TxnType.EXPENSE,
                    onClick = { type = TxnType.EXPENSE; category = UNCATEGORIZED },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text("支出") }
                SegmentedButton(
                    selected = type == TxnType.INCOME,
                    onClick = { type = TxnType.INCOME; category = UNCATEGORIZED },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text("收入") }
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = amountText,
                onValueChange = { s -> if (s.count { it == '.' } <= 1 && s.all { it.isDigit() || it == '.' }) amountText = s },
                label = { Text("金额") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            CurrencyPicker(currency) { currency = it }
            Spacer(Modifier.height(14.dp))
            Text("分类", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 5
            ) {
                FilterChip(
                    selected = category == UNCATEGORIZED,
                    onClick = { category = UNCATEGORIZED },
                    label = {
                        Text(
                            "📝 不选",
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
                )
                cats.forEach { c ->
                    FilterChip(
                        selected = category == c,
                        onClick = { category = if (category == c) UNCATEGORIZED else c },
                        label = {
                            Text(
                                "${CATEGORY_EMOJI[c] ?: ""}$c",
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                        }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("不选分类的话，之后可以让 AI 帮你归类", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = note, onValueChange = { note = it },
                label = { Text("备注（如：和室友吃火锅）") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: return@Button
                    onSave(type, amount, currency, category, note.trim(), System.currentTimeMillis())
                },
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0.0,
                modifier = Modifier.fillMaxWidth()
            ) { Text("保存") }
        }
    }
}
