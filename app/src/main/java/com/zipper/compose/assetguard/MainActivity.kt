package com.zipper.compose.assetguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.zipper.compose.assetguard.ui.navigation.AppNavGraph
import com.zipper.compose.assetguard.ui.theme.AssetGuardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as AssetGuardApplication).container
        setContent {
            AssetGuardTheme {
                val navController = rememberNavController()
                AppNavGraph(
                    navController = navController,
                    container = container
                )
            }
        }
    }
}
