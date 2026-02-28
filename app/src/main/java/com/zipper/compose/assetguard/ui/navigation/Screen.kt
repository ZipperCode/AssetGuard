package com.zipper.compose.assetguard.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object PersonDetail : Screen("person/{personId}") {
        fun createRoute(personId: Long) = "person/$personId"
    }
    data object PersonForm : Screen("person_form?personId={personId}") {
        fun createRoute(personId: Long? = null) =
            if (personId != null) "person_form?personId=$personId" else "person_form"
    }
    data object LoanDetail : Screen("loan/{loanId}") {
        fun createRoute(loanId: Long) = "loan/$loanId"
    }
    data object LoanForm : Screen("loan_form?personId={personId}&loanId={loanId}") {
        fun createRoute(personId: Long? = null, loanId: Long? = null): String {
            val params = mutableListOf<String>()
            personId?.let { params.add("personId=$it") }
            loanId?.let { params.add("loanId=$it") }
            return if (params.isEmpty()) "loan_form"
            else "loan_form?${params.joinToString("&")}"
        }
    }
    data object RepaymentForm : Screen("repayment_form/{loanId}?repaymentId={repaymentId}") {
        fun createRoute(loanId: Long, repaymentId: Long? = null) =
            if (repaymentId != null) "repayment_form/$loanId?repaymentId=$repaymentId"
            else "repayment_form/$loanId"
    }
    data object Settings : Screen("settings")
    data object PaymentMethodManage : Screen("payment_method_manage")
    data object Search : Screen("search")
    data object Profile : Screen("profile")
}

sealed class BottomTab(
    val route: String,
    val icon: ImageVector,
    val labelResId: Int
) {
    data object Home : BottomTab("home", Icons.Default.Home, com.zipper.compose.assetguard.R.string.nav_home)
    data object Search : BottomTab("search", Icons.Default.Search, com.zipper.compose.assetguard.R.string.nav_search)
    data object Settings : BottomTab("settings", Icons.Default.Settings, com.zipper.compose.assetguard.R.string.nav_settings)
    data object Profile : BottomTab("profile", Icons.Default.Person, com.zipper.compose.assetguard.R.string.nav_profile)

    companion object {
        val items = listOf(Home, Search, Settings, Profile)
    }
}
