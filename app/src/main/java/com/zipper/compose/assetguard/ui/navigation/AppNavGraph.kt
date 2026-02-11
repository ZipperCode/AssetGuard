package com.zipper.compose.assetguard.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.zipper.compose.assetguard.di.AppContainer
import com.zipper.compose.assetguard.ui.home.HomeScreen
import com.zipper.compose.assetguard.ui.loan.LoanDetailScreen
import com.zipper.compose.assetguard.ui.loan.LoanFormScreen
import com.zipper.compose.assetguard.ui.person.PersonDetailScreen
import com.zipper.compose.assetguard.ui.person.PersonFormScreen
import com.zipper.compose.assetguard.ui.repayment.RepaymentFormScreen
import com.zipper.compose.assetguard.ui.settings.PaymentMethodManageScreen
import com.zipper.compose.assetguard.ui.settings.SettingsScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    container: AppContainer,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                container = container,
                onPersonClick = { personId ->
                    navController.navigate(Screen.PersonDetail.createRoute(personId))
                },
                onAddPerson = {
                    navController.navigate(Screen.PersonForm.createRoute())
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(
            route = Screen.PersonDetail.route,
            arguments = listOf(navArgument("personId") { type = NavType.LongType })
        ) { backStackEntry ->
            val personId = backStackEntry.arguments?.getLong("personId") ?: return@composable
            PersonDetailScreen(
                personId = personId,
                container = container,
                onBack = { navController.popBackStack() },
                onEditPerson = { navController.navigate(Screen.PersonForm.createRoute(personId)) },
                onAddLoan = { navController.navigate(Screen.LoanForm.createRoute(personId)) },
                onLoanClick = { loanId -> navController.navigate(Screen.LoanDetail.createRoute(loanId)) }
            )
        }

        composable(
            route = "person_form?personId={personId}",
            arguments = listOf(navArgument("personId") {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) { backStackEntry ->
            val personId = backStackEntry.arguments?.getLong("personId")?.takeIf { it != -1L }
            PersonFormScreen(
                personId = personId,
                container = container,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.LoanDetail.route,
            arguments = listOf(navArgument("loanId") { type = NavType.LongType })
        ) { backStackEntry ->
            val loanId = backStackEntry.arguments?.getLong("loanId") ?: return@composable
            LoanDetailScreen(
                loanId = loanId,
                container = container,
                onBack = { navController.popBackStack() },
                onEditLoan = { personId ->
                    navController.navigate(Screen.LoanForm.createRoute(personId, loanId))
                },
                onAddRepayment = {
                    navController.navigate(Screen.RepaymentForm.createRoute(loanId))
                },
                onEditRepayment = { repaymentId ->
                    navController.navigate(Screen.RepaymentForm.createRoute(loanId, repaymentId))
                }
            )
        }

        composable(
            route = "loan_form/{personId}?loanId={loanId}",
            arguments = listOf(
                navArgument("personId") { type = NavType.LongType },
                navArgument("loanId") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val personId = backStackEntry.arguments?.getLong("personId") ?: return@composable
            val loanId = backStackEntry.arguments?.getLong("loanId")?.takeIf { it != -1L }
            LoanFormScreen(
                personId = personId,
                loanId = loanId,
                container = container,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "repayment_form/{loanId}?repaymentId={repaymentId}",
            arguments = listOf(
                navArgument("loanId") { type = NavType.LongType },
                navArgument("repaymentId") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val loanId = backStackEntry.arguments?.getLong("loanId") ?: return@composable
            val repaymentId = backStackEntry.arguments?.getLong("repaymentId")?.takeIf { it != -1L }
            RepaymentFormScreen(
                loanId = loanId,
                repaymentId = repaymentId,
                container = container,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                container = container,
                onBack = { navController.popBackStack() },
                onPaymentMethodManage = {
                    navController.navigate(Screen.PaymentMethodManage.route)
                }
            )
        }

        composable(Screen.PaymentMethodManage.route) {
            PaymentMethodManageScreen(
                container = container,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
