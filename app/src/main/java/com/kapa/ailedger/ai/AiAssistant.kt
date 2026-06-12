package com.kapa.ailedger.ai

import com.kapa.ailedger.data.Txn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 通用 AI 客户端：
 * - API 地址包含 "anthropic.com" 时走 Anthropic Messages 协议
 * - 其他地址一律走 OpenAI 兼容协议（DeepSeek / Kimi / 智谱 / 通义 / OpenAI 等都适用）
 */
object AiAssistant {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val json = "application/json; charset=utf-8".toMediaType()

    suspend fun complete(
        baseUrl: String,
        apiKey: String,
        model: String,
        system: String,
        history: List<Pair<String, String>>
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (baseUrl.contains("anthropic.com")) anthropicCall(baseUrl, apiKey, model, system, history)
            else openAiCall(baseUrl, apiKey, model, system, history)
        }
    }

    // ---------- Anthropic 协议 ----------
    private fun anthropicCall(baseUrl: String, apiKey: String, model: String, system: String, history: List<Pair<String, String>>): String {
        val messages = JSONArray()
        history.forEach { (role, content) -> messages.put(JSONObject().put("role", role).put("content", content)) }
        val body = JSONObject()
            .put("model", model)
            .put("max_tokens", 1500)
            .put("system", system)
            .put("messages", messages)
            .toString().toRequestBody(json)
        val request = Request.Builder()
            .url(normalize(baseUrl, "/v1/messages"))
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .post(body).build()
        client.newCall(request).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error(extractError(resp.code, text))
            val content = JSONObject(text).getJSONArray("content")
            return buildString {
                for (i in 0 until content.length()) {
                    val block = content.getJSONObject(i)
                    if (block.getString("type") == "text") append(block.getString("text"))
                }
            }
        }
    }

    // ---------- OpenAI 兼容协议（DeepSeek/Kimi/智谱/通义等） ----------
    private fun openAiCall(baseUrl: String, apiKey: String, model: String, system: String, history: List<Pair<String, String>>): String {
        val messages = JSONArray().put(JSONObject().put("role", "system").put("content", system))
        history.forEach { (role, content) -> messages.put(JSONObject().put("role", role).put("content", content)) }
        val body = JSONObject()
            .put("model", model)
            .put("max_tokens", 1500)
            .put("messages", messages)
            .toString().toRequestBody(json)
        val request = Request.Builder()
            .url(normalize(baseUrl, "/chat/completions"))
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body).build()
        client.newCall(request).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error(extractError(resp.code, text))
            return JSONObject(text).getJSONArray("choices")
                .getJSONObject(0).getJSONObject("message").getString("content")
        }
    }

    /** 用户填基础域名或完整路径都能用 */
    private fun normalize(baseUrl: String, expectedSuffix: String): String {
        val trimmed = baseUrl.trimEnd('/')
        return if (trimmed.endsWith(expectedSuffix) || trimmed.contains("/chat/completions") || trimmed.contains("/v1/messages")) trimmed
        else trimmed + expectedSuffix
    }

    private fun extractError(code: Int, text: String): String {
        val msg = runCatching {
            val obj = JSONObject(text)
            obj.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() } ?: text.take(300)
        }.getOrDefault(text.take(300))
        return "API 错误 $code: $msg"
    }

    private val df = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)

    /** 生成账单上下文摘要，让 AI 了解用户最近的账目 */
    fun buildContext(
        ledgerName: String,
        ledgerCurrency: String,
        txns: List<Txn>,
        debtSummary: String
    ): String {
        val recent = txns.take(60).joinToString("\n") { t ->
            val sign = if (t.type == com.kapa.ailedger.data.TxnType.EXPENSE) "支出" else "收入"
            "[id=${t.id}] ${df.format(t.date)} $sign ${t.amount} ${t.currency} 分类:${t.category} 备注:${t.note.ifBlank { "无" }}"
        }
        val today = df.format(System.currentTimeMillis())
        val cats = (com.kapa.ailedger.data.DEFAULT_EXPENSE_CATEGORIES + com.kapa.ailedger.data.DEFAULT_INCOME_CATEGORIES).joinToString("、")
        return """你是一个记账 App 内置的 AI 助手，帮助用户分析账单、归类消费、回答理财问题，并且可以直接帮用户记账。回答使用中文，简洁实用。今天是 $today。
当前账本：$ledgerName（币种 $ledgerCurrency）
最近账单（最多60条）：
${recent.ifBlank { "（暂无账单）" }}
借还款情况：$debtSummary

【记账能力——多轮对话确认】当你识别到用户想记账/记借还款时：
1. 如果信息完整（金额、分类、备注都明确），在回复最后输出 <action> 动作块，App 自动执行。
2. 如果信息不够完整（如分类不确定、忘记备注、金额模糊），先输出 <action_pending> 暂存已识别的信息，同时在回复中追问用户一两个关键问题。例如：
   用户："昨天打车23块"
   你可以回："好的，打车23元记下了。请问这是工作通勤还是个人出行呢？"
    <action_pending>{"type":"add_txn","items":[{"txnType":"EXPENSE","amount":23,"currency":"$ledgerCurrency","category":"交通","note":"打车","date":"$today"}]}</action_pending>
3. 当用户后续补充信息（如"个人出行"），你再用 <action> 正式记账。借钱同理——优先追问再确认。

<action> 格式示例：
<action>{"type":"add_txn","items":[{"txnType":"EXPENSE","amount":23,"currency":"$ledgerCurrency","category":"交通","note":"打车","date":"$today"}]}</action>
<action>{"type":"add_debt","person":"小明","debtType":"LEND","amount":200,"currency":"$ledgerCurrency","note":"午饭"}</action>

规则：txnType 只能是 EXPENSE(支出) 或 INCOME(收入)；category 必须从这些分类中选一个：$cats；currency 默认 $ledgerCurrency，用户明确说了其他货币才更换；date 用 yyyy-MM-dd（"昨天"等相对日期请换算）；一句话里有多笔就在 items 里放多条。debtType：LEND=我借出给别人（别人欠我），BORROW=我向别人借入（我欠别人）。
金额或对象完全不明确时（比如"帮我记一笔"没说金额），不要输出任何动作块，先追问清楚。用户没有要求记账时，绝不输出动作块。
注意：分析账目只能基于以上数据，不要编造不存在的账单。"""
    }

    /** 让 AI 给未分类账单归类，返回 id -> 分类 */
    suspend fun categorize(
        baseUrl: String,
        apiKey: String,
        model: String,
        txns: List<Txn>,
        categories: List<String>
    ): Result<Map<Long, String>> {
        val items = txns.joinToString("\n") { "[id=${it.id}] 金额:${it.amount}${it.currency} 备注:${it.note.ifBlank { "无" }}" }
        val system = """你是账单分类引擎。根据备注和金额，把每条账单归入以下分类之一：${categories.joinToString("、")}。
只输出 JSON 对象，键为 id 字符串，值为分类名，不要输出任何其他文字或 markdown。例如 {"3":"餐饮","7":"交通"}"""
        return complete(baseUrl, apiKey, model, system, listOf("user" to items)).mapCatching { raw ->
            val cleaned = raw.replace("```json", "").replace("```", "").trim()
            val obj = JSONObject(cleaned)
            buildMap {
                obj.keys().forEach { k ->
                    val cat = obj.getString(k)
                    if (cat in categories) put(k.toLong(), cat)
                }
            }
        }
    }
}
