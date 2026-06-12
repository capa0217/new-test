package com.kapa.ailedger.data

/** 支持的货币与默认汇率（以 1 CNY 为基准，可在设置中修改） */
data class CurrencyInfo(val code: String, val symbol: String, val cname: String)

object Currencies {
    val list = listOf(
        CurrencyInfo("CNY", "¥", "人民币"),
        CurrencyInfo("USD", "$", "美元"),
        CurrencyInfo("AUD", "A$", "澳元"),
        CurrencyInfo("HKD", "HK$", "港币"),
        CurrencyInfo("JPY", "JP¥", "日元"),
        CurrencyInfo("EUR", "€", "欧元"),
        CurrencyInfo("GBP", "£", "英镑"),
        CurrencyInfo("KRW", "₩", "韩元"),
        CurrencyInfo("SGD", "S$", "新元"),
        CurrencyInfo("TWD", "NT$", "新台币")
    )

    /** 默认汇率：1 单位外币 = rate 人民币 */
    val defaultRatesToCny = mapOf(
        "CNY" to 1.0, "USD" to 7.2, "AUD" to 4.7, "HKD" to 0.92,
        "JPY" to 0.047, "EUR" to 7.8, "GBP" to 9.1, "KRW" to 0.0053,
        "SGD" to 5.3, "TWD" to 0.22
    )

    fun symbol(code: String): String = list.firstOrNull { it.code == code }?.symbol ?: code

    /** 将 amount(from币种) 折算为 to 币种 */
    fun convert(amount: Double, from: String, to: String, ratesToCny: Map<String, Double>): Double {
        if (from == to) return amount
        val fromRate = ratesToCny[from] ?: 1.0
        val toRate = ratesToCny[to] ?: 1.0
        return amount * fromRate / toRate
    }
}
