package com.zipper.compose.assetguard.ui.loan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zipper.compose.assetguard.data.local.entity.LoanWithRepayments
import com.zipper.compose.assetguard.data.local.entity.PaymentMethodEntity
import com.zipper.compose.assetguard.data.local.entity.RepaymentEntity
import com.zipper.compose.assetguard.data.repository.LoanRepository
import com.zipper.compose.assetguard.data.repository.PaymentMethodRepository
import com.zipper.compose.assetguard.data.repository.RepaymentRepository
import com.zipper.compose.assetguard.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LoanDetailUiState(
    val loanWithRepayments: LoanWithRepayments? = null,
    val paymentMethods: Map<Long, PaymentMethodEntity> = emptyMap()
)

class LoanDetailViewModel(
    private val loanRepository: LoanRepository,
    private val repaymentRepository: RepaymentRepository,
    private val paymentMethodRepository: PaymentMethodRepository,
    private val loanId: Long
) : ViewModel() {

    val uiState: StateFlow<LoanDetailUiState> = combine(
        loanRepository.observeWithRepayments(loanId),
        paymentMethodRepository.observeAll()
    ) { loanWithRepayments, methods ->
        LoanDetailUiState(
            loanWithRepayments = loanWithRepayments,
            paymentMethods = methods.associateBy { it.id }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LoanDetailUiState())

    fun deleteRepayment(repayment: RepaymentEntity) {
        viewModelScope.launch {
            repaymentRepository.delete(repayment)
        }
    }

    companion object {
        fun factory(container: AppContainer, loanId: Long) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LoanDetailViewModel(
                    container.loanRepository,
                    container.repaymentRepository,
                    container.paymentMethodRepository,
                    loanId
                ) as T
            }
        }
    }
}
