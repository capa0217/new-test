package com.kapa.ailedger.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.kapa.ailedger.ui.theme.*
import com.kapa.ailedger.vm.AppViewModel
import com.kapa.ailedger.vm.ChatViewModel

@Composable
fun ChatScreen(vm: AppViewModel, chatVm: ChatViewModel) {
    val messages by chatVm.messages.collectAsState()
    val sending by chatVm.sending.collectAsState()
    val pendingAction by chatVm.pendingAction.collectAsState()
    val ledger by vm.currentLedger.collectAsState()
    val txns by vm.txns.collectAsState()
    val debts by vm.debts.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, sending) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "AI 助手",
                modifier = Modifier.padding(start = 16.dp).weight(1f),
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                style = MaterialTheme.typography.titleLarge
            )
            IconButton(onClick = { chatVm.clear() }) {
                Icon(Icons.Filled.DeleteSweep, "清空对话", tint = TextSecondary)
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        border = BorderStroke(1.dp, CardBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                DogAvatar(Modifier.size(48.dp))
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("汪！我是你的记账助手 🐾", fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("可以直接跟我说账单，我帮你记～", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            listOf("昨天打车花了23块，帮我记一下", "今天发工资5000，记一笔收入", "小明借了我200块", "这个月钱都花哪了？", "帮我看看哪类消费可以省一省").forEach { q ->
                                SuggestionChip(onClick = { input = q }, label = { Text(q) })
                            }
                        }
                    }
                }
            }
            items(messages, key = { it.id }) { m ->
                val isUser = m.role == "user"
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
                    if (!isUser) {
                        DogAvatar(Modifier.size(36.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Box(
                        Modifier
                            .widthIn(max = 300.dp)
                            .background(
                                if (isUser) BubbleUser else BubbleAi,
                                RoundedCornerShape(
                                    topStart = 16.dp, topEnd = 16.dp,
                                    bottomStart = if (isUser) 16.dp else 4.dp,
                                    bottomEnd = if (isUser) 4.dp else 16.dp
                                )
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        if (isUser) {
                            Text(m.content, color = TextPrimary)
                        } else {
                            Text(renderMarkdown(m.content), color = TextPrimary)
                        }
                    }
                }
            }
            if (sending) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("思考中…", color = TextSecondary)
                    }
                }
            }
        }
        pendingAction?.let { pending ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, CardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("待确认记账", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Spacer(Modifier.height(4.dp))
                        Text(pending.summary, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    }
                    Row {
                        TextButton(onClick = { chatVm.cancelPending() }) { Text("取消") }
                        Button(onClick = { chatVm.confirmPending(ledger) }) { Text("确认记账") }
                    }
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("聊聊你的账单…") },
                modifier = Modifier.weight(1f),
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IosBlue.copy(alpha = 0.5f),
                    unfocusedBorderColor = TrackGray,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = IosBlue,
                    focusedLabelColor = TextSecondary,
                    unfocusedLabelColor = TextSecondary,
                    unfocusedPlaceholderColor = TextSecondary
                )
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = {
                    chatVm.send(input.trim(), ledger, txns, debts)
                    input = ""
                },
                enabled = input.isNotBlank() && !sending,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = IosBlue,
                    contentColor = Color.White,
                    disabledContainerColor = TrackGray,
                    disabledContentColor = TextSecondary
                )
            ) { Icon(Icons.AutoMirrored.Filled.Send, "发送") }
        }
    }
}

private fun renderMarkdown(text: String) = buildAnnotatedString {
    var last = 0
    val bold = Regex("\\*\\*(.+?)\\*\\*")
    bold.findAll(text).forEach { m ->
        append(text.substring(last, m.range.first))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(m.groupValues[1])
        }
        last = m.range.last + 1
    }
    append(text.substring(last))
}
