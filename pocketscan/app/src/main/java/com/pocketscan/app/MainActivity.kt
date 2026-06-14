package com.pocketscan.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.pocketscan.app.ui.navigation.PocketScanNavHost
import com.pocketscan.app.BuildConfig
import com.pocketscan.app.ui.theme.PocketScanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        if (!BuildConfig.DEBUG) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE,
            )
        }
        enableEdgeToEdge()
        val container = (application as PocketScanApplication).container
        setContent {
            PocketScanTheme {
                PocketScanNavHost(container = container)
            }
        }
    }
}
