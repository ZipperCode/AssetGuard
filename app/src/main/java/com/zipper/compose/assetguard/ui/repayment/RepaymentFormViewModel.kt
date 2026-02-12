package com.zipper.compose.assetguard.ui.repayment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zipper.compose.assetguard.data.local.entity.PaymentMethodEntity
import com.zipper.compose.assetguard.data.local.entity.RepaymentEntity
import com.zipper.compose.assetguard.data.repository.LoanRepository
import com.zipper.compose.assetguard.data.repository.PaymentMethodRepository
import com.zipper.compose.assetguard.data.repository.RepaymentRepository
import com.zipper.compose.assetguard.di.AppContainer
import com.zipper.compose.assetguard.util.MoneyUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RepaymentFormUiState(
    val amountText: String = "",
    val repayDate: Long = System.currentTimeMillis(),
    val paymentMethodId: Long? = null,
    val note: String = "",
    val remainingAmount: Long = 0L,
    val loanAmount: Long = 0L,
    val loanDate: Long = 0L,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val amountError: String? = null,
    val dateError: String? = null,
    val paymentMethodError: String? = null,
    val overpayWarning: String? = null
)

class RepaymentFormViewModel(
    private val repaymentRepository: RepaymentRepository,
    private val loanRepository: LoanRepository,
    private val paymentMethodRepository: PaymentMethodRepository,
    private val loanId: Long,
    private val repaymentId: Long?
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepaymentFormUiState(isEditing = repaymentId != null))
    val uiState: StateFlow<RepaymentFormUiState> = _uiState.asStateFlow()

    val paymentMethods: StateFlow<List<PaymentMethodEntity>> =
        paymentMethodRepository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val loan = loanRepository.getById(loanId)
            if (loan != null) {
                val totalRepaid = repaymentRepository.getTotalRepaidForLoan(loanId)
                _uiState.value = _uiState.value.copy(
                    loanAmount = loan.amount,
                    loanDate = loan.loanDate,
                    remainingAmount = loan.amount - totalRepaid
                )
            }

            if (repaymentId != null) {
                repaymentRepository.getById(repaymentId)?.let { repayment ->
                    _uiState.value = _uiState.value.copy(
                        amountText = MoneyUtils.centsToYuanString(repayment.amount),
                        repayDate = repayment.repayDate,
                        paymentMethodId = repayment.paymentMethodId,
                        note = repayment.note ?: ""
                    )
                }
            }
        }
    }

    fun onAmountChanged(text: String) {
        if (MoneyUtils.isValidAmountInput(text)) {
            val amountCents = MoneyUtils.yuanStringToCents(text)
            val overpayWarning = if (amountCents > _uiState.value.remainingAmount && _uiState.value.remainingAmount > 0) {
                "还款金额超过剩余待还 ${MoneyUtils.formatCents(_uiState.value.remainingAmount)}"
            } else null
            _uiState.value = _uiState.value.copy(
                amountText = text,
                amountError = null,
                overpayWarning = overpayWarning
            )
        }
    }

    fun onRepayDateChanged(date: Long) {
        val dateError = if (date < _uiState.value.loanDate && _uiState.value.loanDate > 0) {
            "还款日期不能早于借款日"
        } else null
        _uiState.value = _uiState.value.copy(repayDate = date, dateError = dateError)
    }

    fun onPaymentMethodChanged(id: Long) {
        _uiState.value = _uiState.value.copy(paymentMethodId = id, paymentMethodError = null)
    }

    fun onNoteChanged(note: String) {
        _uiState.value = _uiState.value.copy(note = note)
    }

    fun save() {
        val state = _uiState.value
        val amountCents = MoneyUtils.yuanStringToCents(state.amountText)
        if (amountCents <= 0) {
            _uiState.value = state.copy(amountError = "请输入有效金额")
            return
        }
        val paymentMethodId = state.paymentMethodId
        if (paymentMethodId == null) {
            _uiState.value = state.copy(paymentMethodError = "请选择支付方式")
            return
        }
        if (state.loanDate > 0 && state.repayDate < state.loanDate) {
            _uiState.value = state.copy(dateError = "还款日期不能早于借款日")
            return
        }

        _uiState.value = state.copy(isSaving = true)
        viewModelScope.launch {
            if (repaymentId != null) {
                val existing = repaymentRepository.getById(repaymentId) ?: return@launch
                repaymentRepository.update(
                    existing.copy(
                        amount = amountCents,
                        repayDate = state.repayDate,
                        paymentMethodId = paymentMethodId,
                        note = state.note.trim().ifBlank { null }
                    )
                )
            } else {
                repaymentRepository.insert(
                    RepaymentEntity(
                        loanId = loanId,
                        amount = amountCents,
                        repayDate = state.repayDate,
                        paymentMethodId = paymentMethodId,
                        note = state.note.trim().ifBlank { null }
                    )
                )
            }
            _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true)
        }
    }

    suspend fun getTotalRepaidForLoan(loanId: Long): Long =
        repaymentRepository.getTotalRepaidForLoan(loanId)

    companion object {
        fun factory(container: AppContainer, loanId: Long, repaymentId: Long?) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return RepaymentFormViewModel(
                        container.repaymentRepository,
                        container.loanRepository,
                        container.paymentMethodRepository,
                        loanId,
                        repaymentId
                    ) as T
                }
            }
    }
}
