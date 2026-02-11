package com.zipper.compose.assetguard.ui.loan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zipper.compose.assetguard.data.local.entity.LoanEntity
import com.zipper.compose.assetguard.data.local.entity.PaymentMethodEntity
import com.zipper.compose.assetguard.data.repository.LoanRepository
import com.zipper.compose.assetguard.data.repository.PaymentMethodRepository
import com.zipper.compose.assetguard.di.AppContainer
import com.zipper.compose.assetguard.util.MoneyUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LoanFormUiState(
    val amountText: String = "",
    val loanDate: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val paymentMethodId: Long? = null,
    val note: String = "",
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val amountError: String? = null
)

class LoanFormViewModel(
    private val loanRepository: LoanRepository,
    private val paymentMethodRepository: PaymentMethodRepository,
    private val personId: Long,
    private val loanId: Long?
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoanFormUiState(isEditing = loanId != null))
    val uiState: StateFlow<LoanFormUiState> = _uiState.asStateFlow()

    val paymentMethods: StateFlow<List<PaymentMethodEntity>> =
        paymentMethodRepository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        if (loanId != null) {
            viewModelScope.launch {
                loanRepository.getById(loanId)?.let { loan ->
                    _uiState.value = _uiState.value.copy(
                        amountText = MoneyUtils.centsToYuanString(loan.amount),
                        loanDate = loan.loanDate,
                        dueDate = loan.dueDate,
                        paymentMethodId = loan.paymentMethodId,
                        note = loan.note ?: ""
                    )
                }
            }
        }
    }

    fun onAmountChanged(text: String) {
        if (MoneyUtils.isValidAmountInput(text)) {
            _uiState.value = _uiState.value.copy(amountText = text, amountError = null)
        }
    }

    fun onLoanDateChanged(date: Long) {
        _uiState.value = _uiState.value.copy(loanDate = date)
    }

    fun onDueDateChanged(date: Long?) {
        _uiState.value = _uiState.value.copy(dueDate = date)
    }

    fun onPaymentMethodChanged(id: Long) {
        _uiState.value = _uiState.value.copy(paymentMethodId = id)
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
            _uiState.value = state.copy(amountError = "请选择支付方式")
            return
        }

        _uiState.value = state.copy(isSaving = true)
        viewModelScope.launch {
            if (loanId != null) {
                val existing = loanRepository.getById(loanId) ?: return@launch
                loanRepository.update(
                    existing.copy(
                        amount = amountCents,
                        loanDate = state.loanDate,
                        dueDate = state.dueDate,
                        paymentMethodId = paymentMethodId,
                        note = state.note.trim().ifBlank { null }
                    )
                )
            } else {
                loanRepository.insert(
                    LoanEntity(
                        personId = personId,
                        amount = amountCents,
                        loanDate = state.loanDate,
                        dueDate = state.dueDate,
                        paymentMethodId = paymentMethodId,
                        note = state.note.trim().ifBlank { null }
                    )
                )
            }
            _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true)
        }
    }

    companion object {
        fun factory(container: AppContainer, personId: Long, loanId: Long?) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LoanFormViewModel(
                        container.loanRepository,
                        container.paymentMethodRepository,
                        personId,
                        loanId
                    ) as T
                }
            }
    }
}
