package com.zipper.compose.assetguard

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zipper.compose.assetguard.data.model.ThemeMode
import com.zipper.compose.assetguard.ui.components.AssetGuardBottomBar
import com.zipper.compose.assetguard.ui.navigation.AppNavGraph
import com.zipper.compose.assetguard.ui.navigation.BottomTab
import com.zipper.compose.assetguard.ui.navigation.Screen
import com.zipper.compose.assetguard.ui.theme.AssetGuardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as AssetGuardApplication).container
        setContent {
            val themeMode by container.userPreferencesRepository
                .observeThemeMode()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            AssetGuardTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val tabRoutes = BottomTab.items.map { it.route }
                val showBottomBar = currentRoute in tabRoutes

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            AssetGuardBottomBar(
                                currentRoute = currentRoute,
                                onTabSelected = { tab ->
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    },
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
                ) { innerPadding ->
                    AppNavGraph(
                        navController = navController,
                        container = container,
                        modifier = Modifier.padding(innerPadding)
                    )
                }

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
