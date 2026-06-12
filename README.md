# 随手账（AiLedger）

安卓记账 App，Kotlin + Jetpack Compose + Room。

## 功能
- **记账**：收入/支出、分类、备注，按日分组展示，本月收支结余卡片
- **多账本**：顶部标题可切换/新建账本，每个账本有自己的基准币种（设置页可删账本）
- **多货币**：每笔账单可选币种（CNY/USD/AUD/HKD/JPY/EUR/GBP/KRW/SGD/TWD），汇总时按汇率自动折算成账本币种；汇率在设置页手动调整
- **借还款**：借出/借入登记，支持分次还款（进度条）、自动结清、按人民币折算汇总
- **AI 助手**：
  - 对话框聊天：AI 能看到当前账本最近 60 条账单和借还款情况，可以问"这个月钱花哪了"
  - 对话记账：直接说"昨天打车花了23块"/"今天发工资5000"/"小明借了我200"，AI 自动写入账本或借还款（通过解析 AI 输出的 <action> JSON 动作块实现，支持一句话记多笔、相对日期换算）
  - 一键归类：明细页右上角「AI归类」，把未分类账单自动归入分类

## 如何运行
1. 用 Android Studio（Koala 或更新）打开本目录，等待 Gradle Sync
   （如提示缺少 gradle wrapper jar，让 Android Studio 自动修复，或本地执行 `gradle wrapper`）
2. 连接手机或启动模拟器，点 Run
3. 最低支持 Android 8.0（API 26）

## 启用 AI 助手（支持多家服务商）
「设置 → AI 助手」内置预设：Anthropic / DeepSeek / Kimi / 智谱GLM / 通义千问 / OpenAI，
点一下预设会自动填好 API 地址和推荐模型，再填上对应平台的 API Key 即可。
也可以手动填任何 OpenAI 兼容格式的 API 地址（如本地 Ollama、One-API 中转等）。
Key 只保存在本机 DataStore，直连服务商。

## 结构
```
app/src/main/java/com/kapa/ailedger/
├── MainActivity.kt          入口
├── data/                    Room 实体、DAO、数据库、货币表、设置存储
├── ai/AiAssistant.kt        Anthropic API 客户端 + 账单归类
├── vm/                      AppViewModel（账本/账单/借还款）、ChatViewModel
└── ui/                      Compose 界面（明细/记账弹窗/借还/聊天/设置/主题）
```

## 后续可以加的
- 自定义分类、预算提醒、图表统计（饼图/折线）
- 汇率自动拉取（接 exchangerate API）
- 数据导出 CSV / 备份
