package com.zipper.compose.assetguard

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.zipper.compose.assetguard.ui.navigation.AppNavGraph
import com.zipper.compose.assetguard.ui.navigation.Screen
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

                // 处理通知 DeepLink
                LaunchedEffect(Unit) {
                    handleDeepLink(intent, navController)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun handleDeepLink(intent: Intent?, navController: NavHostController) {
        intent ?: return
        val navigateTo = intent.getStringExtra("navigate_to") ?: return
        when (navigateTo) {
            "loan_detail" -> {
                val loanId = intent.getLongExtra("loan_id", -1L)
                if (loanId != -1L) {
                    navController.navigate(Screen.LoanDetail.createRoute(loanId))
                }
            }
        }
        intent.removeExtra("navigate_to")
        intent.removeExtra("loan_id")
    }
}
