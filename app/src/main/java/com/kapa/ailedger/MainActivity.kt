package com.kapa.ailedger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kapa.ailedger.ui.AppRoot
import com.kapa.ailedger.ui.theme.AiLedgerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AiLedgerTheme {
                AppRoot()
            }
        }
    }
}
