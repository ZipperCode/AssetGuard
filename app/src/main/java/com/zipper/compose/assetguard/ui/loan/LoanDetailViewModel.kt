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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LoanDetailUiState(
    val loanWithRepayments: LoanWithRepayments? = null,
    val paymentMethods: Map<Long, PaymentMethodEntity> = emptyMap(),
    val pendingDeleteRepaymentId: Long? = null
)

class LoanDetailViewModel(
    private val loanRepository: LoanRepository,
    private val repaymentRepository: RepaymentRepository,
    private val paymentMethodRepository: PaymentMethodRepository,
    private val loanId: Long
) : ViewModel() {

    private val _pendingDeleteRepaymentId = MutableStateFlow<Long?>(null)

    private var pendingDeleteJob: Job? = null
    private var pendingDeleteRepayment: RepaymentEntity? = null

    val uiState: StateFlow<LoanDetailUiState> = combine(
        loanRepository.observeWithRepayments(loanId),
        paymentMethodRepository.observeAll(),
        _pendingDeleteRepaymentId
    ) { loanWithRepayments, methods, pendingId ->
        LoanDetailUiState(
            loanWithRepayments = loanWithRepayments,
            paymentMethods = methods.associateBy { it.id },
            pendingDeleteRepaymentId = pendingId
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LoanDetailUiState())

    fun deleteRepayment(repayment: RepaymentEntity) {
        pendingDeleteJob?.cancel()
        pendingDeleteRepayment = repayment
        _pendingDeleteRepaymentId.value = repayment.id

        pendingDeleteJob = viewModelScope.launch {
            delay(5000)
            repaymentRepository.delete(repayment).onSuccess {
                _pendingDeleteRepaymentId.value = null
                pendingDeleteRepayment = null
            }
        }
    }

    fun deleteLoan() {
        viewModelScope.launch {
            val loan = loanRepository.getById(loanId) ?: return@launch
            loanRepository.delete(loan)
        }
    }

    fun undoDeleteRepayment() {
        pendingDeleteJob?.cancel()
        pendingDeleteJob = null
        pendingDeleteRepayment = null
        _pendingDeleteRepaymentId.value = null
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
