package com.kapa.ailedger.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.kapa.ailedger.data.Currencies
import com.kapa.ailedger.data.Ledger
import com.kapa.ailedger.data.SettingsStore
import com.kapa.ailedger.ui.theme.*
import com.kapa.ailedger.vm.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: AppViewModel) {
    val apiKey by vm.apiKey.collectAsState()
    val model by vm.model.collectAsState()
    val rates by vm.rates.collectAsState()
    val ledgers by vm.ledgers.collectAsState()

    val baseUrl by vm.baseUrl.collectAsState()
    var keyInput by remember(apiKey) { mutableStateOf(apiKey) }
    var modelInput by remember(model) { mutableStateOf(model) }
    var urlInput by remember(baseUrl) { mutableStateOf(baseUrl) }
    var ledgerToDelete by remember { mutableStateOf<Ledger?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = TextPrimary
                )
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("AI 助手", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("选择服务商（也可手动填地址，兼容 OpenAI 格式）", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SettingsStore.PROVIDERS) { p ->
                            FilterChip(
                                selected = urlInput.trimEnd('/') == p.baseUrl.trimEnd('/'),
                                onClick = { urlInput = p.baseUrl; modelInput = p.model },
                                label = { Text(p.name) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = urlInput, onValueChange = { urlInput = it },
                        label = { Text("API 地址") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = keyInput, onValueChange = { keyInput = it },
                        label = { Text("API Key") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = modelInput, onValueChange = { modelInput = it },
                        label = { Text("模型") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = { vm.saveApiKey(keyInput); vm.saveModel(modelInput); vm.saveBaseUrl(urlInput) }) { Text("保存") }
                    Text(
                        "密钥只保存在本机，直连所选服务商，不经过任何中转。",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            SettingsCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("汇率（1 单位外币 = ? 人民币）", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Currencies.list.filter { it.code != "CNY" }.forEach { c ->
                        var rateText by remember(rates) { mutableStateOf((rates[c.code] ?: 1.0).toString()) }
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text("${c.code} ${c.cname}", Modifier.weight(1f), color = TextPrimary)
                            OutlinedTextField(
                                value = rateText,
                                onValueChange = { s ->
                                    if (s.all { it.isDigit() || it == '.' }) {
                                        rateText = s
                                        s.toDoubleOrNull()?.let { vm.saveRate(c.code, it) }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.width(120.dp)
                            )
                        }
                    }
                }
            }

            SettingsCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("账本管理", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    ledgers.forEach { l ->
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text("${l.name}（${l.currency}）", Modifier.weight(1f), color = TextPrimary)
                            if (ledgers.size > 1) {
                                IconButton(onClick = { ledgerToDelete = l }) {
                                    Icon(Icons.Filled.Delete, "删除", tint = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    ledgerToDelete?.let { l ->
        AlertDialog(
            onDismissRequest = { ledgerToDelete = null },
            title = { Text("删除账本「${l.name}」？") },
            text = { Text("该账本下的所有账单也会一并删除，无法恢复。") },
            confirmButton = {
                TextButton(onClick = { vm.deleteLedger(l); ledgerToDelete = null }) { Text("删除", color = IosRed) }
            },
            dismissButton = { TextButton(onClick = { ledgerToDelete = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        content = content
    )
}
