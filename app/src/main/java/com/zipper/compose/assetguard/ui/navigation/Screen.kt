package com.zipper.compose.assetguard.ui.navigation

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
    data object LoanForm : Screen("loan_form/{personId}?loanId={loanId}") {
        fun createRoute(personId: Long, loanId: Long? = null) =
            if (loanId != null) "loan_form/$personId?loanId=$loanId" else "loan_form/$personId"
    }
    data object RepaymentForm : Screen("repayment_form/{loanId}?repaymentId={repaymentId}") {
        fun createRoute(loanId: Long, repaymentId: Long? = null) =
            if (repaymentId != null) "repayment_form/$loanId?repaymentId=$repaymentId"
            else "repayment_form/$loanId"
    }
    data object Settings : Screen("settings")
    data object PaymentMethodManage : Screen("payment_method_manage")
    data object Search : Screen("search")
}
