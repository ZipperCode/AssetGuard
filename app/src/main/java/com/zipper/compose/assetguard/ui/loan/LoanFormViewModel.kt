package com.zipper.compose.assetguard.ui.loan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zipper.compose.assetguard.R
import com.zipper.compose.assetguard.data.local.entity.LoanEntity
import com.zipper.compose.assetguard.data.local.entity.PaymentMethodEntity
import com.zipper.compose.assetguard.data.local.entity.PersonEntity
import com.zipper.compose.assetguard.data.repository.LoanRepository
import com.zipper.compose.assetguard.data.repository.PaymentMethodRepository
import com.zipper.compose.assetguard.data.repository.PersonRepository
import com.zipper.compose.assetguard.di.AppContainer
import com.zipper.compose.assetguard.util.MoneyUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val amountError: Int? = null,
    val dateError: Int? = null,
    val paymentMethodError: Int? = null,
    // Person selector fields
    val isPersonLocked: Boolean = false,
    val selectedPerson: PersonEntity? = null,
    val newPersonName: String = "",
    val personQuery: String = "",
    val personSuggestions: List<PersonEntity> = emptyList(),
    val personError: Int? = null
)

class LoanFormViewModel(
    private val loanRepository: LoanRepository,
    private val paymentMethodRepository: PaymentMethodRepository,
    private val personRepository: PersonRepository,
    private val personId: Long?,
    private val loanId: Long?
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoanFormUiState(isEditing = loanId != null))
    val uiState: StateFlow<LoanFormUiState> = _uiState.asStateFlow()

    val paymentMethods: StateFlow<List<PaymentMethodEntity>> =
        paymentMethodRepository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var searchJob: Job? = null

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
        if (personId != null) {
            viewModelScope.launch {
                personRepository.getById(personId)?.let { person ->
                    _uiState.value = _uiState.value.copy(
                        selectedPerson = person,
                        isPersonLocked = true
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
        _uiState.value = _uiState.value.copy(loanDate = date, dateError = null)
        validateDueDate()
    }

    fun onDueDateChanged(date: Long?) {
        _uiState.value = _uiState.value.copy(dueDate = date, dateError = null)
        validateDueDate()
    }

    fun onPaymentMethodChanged(id: Long) {
        _uiState.value = _uiState.value.copy(paymentMethodId = id, paymentMethodError = null)
    }

    fun onNoteChanged(note: String) {
        _uiState.value = _uiState.value.copy(note = note)
    }

    fun onPersonQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(personQuery = query, personError = null)
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(personSuggestions = emptyList())
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            val results = personRepository.searchByName(query)
            _uiState.value = _uiState.value.copy(personSuggestions = results)
        }
    }

    fun onPersonSelected(person: PersonEntity) {
        _uiState.value = _uiState.value.copy(
            selectedPerson = person,
            newPersonName = "",
            personQuery = "",
            personSuggestions = emptyList(),
            personError = null
        )
    }

    fun onNewPersonSelected(name: String) {
        _uiState.value = _uiState.value.copy(
            newPersonName = name,
            selectedPerson = null,
            personQuery = "",
            personSuggestions = emptyList(),
            personError = null
        )
    }

    fun clearPersonSelection() {
        _uiState.value = _uiState.value.copy(
            selectedPerson = null,
            newPersonName = "",
            personQuery = "",
            personSuggestions = emptyList()
        )
    }

    private fun validateDueDate() {
        val state = _uiState.value
        val dueDate = state.dueDate ?: return
        if (dueDate < state.loanDate) {
            _uiState.value = state.copy(dateError = R.string.error_due_date_before_loan)
        }
    }

    fun save() {
        val state = _uiState.value

        // Resolve person ID
        val resolvedPersonId: Long?
        val needsCreatePerson: Boolean
        when {
            state.isPersonLocked && state.selectedPerson != null -> {
                resolvedPersonId = state.selectedPerson.id
                needsCreatePerson = false
            }
            state.selectedPerson != null -> {
                resolvedPersonId = state.selectedPerson.id
                needsCreatePerson = false
            }
            state.newPersonName.isNotBlank() -> {
                resolvedPersonId = null
                needsCreatePerson = true
            }
            else -> {
                _uiState.value = state.copy(personError = R.string.error_select_person)
                return
            }
        }

        val amountCents = MoneyUtils.yuanStringToCents(state.amountText)
        if (amountCents <= 0) {
            _uiState.value = state.copy(amountError = R.string.error_invalid_amount)
            return
        }
        val paymentMethodId = state.paymentMethodId
        if (paymentMethodId == null) {
            _uiState.value = state.copy(paymentMethodError = R.string.error_select_payment_method)
            return
        }
        if (state.dueDate != null && state.dueDate < state.loanDate) {
            _uiState.value = state.copy(dateError = R.string.error_due_date_before_loan)
            return
        }

        _uiState.value = state.copy(isSaving = true)
        viewModelScope.launch {
            val finalPersonId = if (needsCreatePerson) {
                personRepository.insert(PersonEntity(name = state.newPersonName))
            } else {
                resolvedPersonId!!
            }

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
                        personId = finalPersonId,
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
        fun factory(container: AppContainer, personId: Long?, loanId: Long?) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LoanFormViewModel(
                        container.loanRepository,
                        container.paymentMethodRepository,
                        container.personRepository,
                        personId,
                        loanId
                    ) as T
                }
            }
    }
}
