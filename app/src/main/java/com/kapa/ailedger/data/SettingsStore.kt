package com.kapa.ailedger.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {
    companion object {
        val KEY_API = stringPreferencesKey("ai_api_key")
        val KEY_MODEL = stringPreferencesKey("ai_model")
        val KEY_BASE_URL = stringPreferencesKey("ai_base_url")
        val KEY_RATES = stringPreferencesKey("rates_json")
        const val DEFAULT_MODEL = "claude-sonnet-4-5"
        const val DEFAULT_BASE_URL = "https://api.anthropic.com/v1/messages"

        /** 常用服务商预设：名称 / API地址 / 推荐模型 */
        data class Provider(val name: String, val baseUrl: String, val model: String)
        val PROVIDERS = listOf(
            Provider("Anthropic", "https://api.anthropic.com/v1/messages", "claude-sonnet-4-5"),
            Provider("DeepSeek", "https://api.deepseek.com/chat/completions", "deepseek-chat"),
            Provider("Kimi", "https://api.moonshot.cn/v1/chat/completions", "moonshot-v1-8k"),
            Provider("智谱GLM", "https://open.bigmodel.cn/api/paas/v4/chat/completions", "glm-4-flash"),
            Provider("通义千问", "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions", "qwen-plus"),
            Provider("OpenAI", "https://api.openai.com/v1/chat/completions", "gpt-4o-mini")
        )
    }

    val apiKey: Flow<String> = context.dataStore.data.map { it[KEY_API] ?: "" }
    val model: Flow<String> = context.dataStore.data.map { it[KEY_MODEL] ?: DEFAULT_MODEL }
    val baseUrl: Flow<String> = context.dataStore.data.map { it[KEY_BASE_URL] ?: DEFAULT_BASE_URL }
    val rates: Flow<Map<String, Double>> = context.dataStore.data.map { prefs ->
        val json = prefs[KEY_RATES]
        if (json.isNullOrBlank()) Currencies.defaultRatesToCny
        else runCatching {
            val obj = JSONObject(json)
            buildMap { obj.keys().forEach { k -> put(k, obj.getDouble(k)) } }
        }.getOrDefault(Currencies.defaultRatesToCny)
    }

    suspend fun setApiKey(v: String) = context.dataStore.edit { it[KEY_API] = v }
    suspend fun setModel(v: String) = context.dataStore.edit { it[KEY_MODEL] = v }
    suspend fun setBaseUrl(v: String) = context.dataStore.edit { it[KEY_BASE_URL] = v }
    suspend fun setRate(code: String, rate: Double) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_RATES]?.let { runCatching { JSONObject(it) }.getOrNull() } ?: JSONObject().apply {
                Currencies.defaultRatesToCny.forEach { (k, v) -> put(k, v) }
            }
            current.put(code, rate)
            prefs[KEY_RATES] = current.toString()
        }
    }
}
