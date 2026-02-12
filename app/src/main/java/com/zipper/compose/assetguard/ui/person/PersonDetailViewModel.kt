package com.zipper.compose.assetguard.ui.person

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zipper.compose.assetguard.data.local.entity.LoanEntity
import com.zipper.compose.assetguard.data.local.entity.LoanStatus
import com.zipper.compose.assetguard.data.local.entity.LoanWithRepayments
import com.zipper.compose.assetguard.data.local.entity.PersonEntity
import com.zipper.compose.assetguard.data.repository.LoanRepository
import com.zipper.compose.assetguard.data.repository.PersonRepository
import com.zipper.compose.assetguard.di.AppContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PersonDetailUiState(
    val person: PersonEntity? = null,
    val loans: List<LoanWithRepayments> = emptyList(),
    val totalLent: Long = 0L,
    val totalRepaid: Long = 0L,
    val deleteError: String? = null,
    val pendingDeleteLoanName: String? = null,
    // 借条多选
    val isLoanSelectionMode: Boolean = false,
    val selectedLoanIds: Set<Long> = emptySet()
) {
    val totalOutstanding: Long get() = totalLent - totalRepaid
}

class PersonDetailViewModel(
    private val personRepository: PersonRepository,
    private val loanRepository: LoanRepository,
    private val personId: Long
) : ViewModel() {

    private val _deleteError = MutableStateFlow<String?>(null)
    private val _pendingDeleteLoanName = MutableStateFlow<String?>(null)

    private var pendingDeleteLoanJob: Job? = null
    private var pendingDeleteLoan: LoanEntity? = null

    // 借条多选
    private val _isLoanSelectionMode = MutableStateFlow(false)
    private val _selectedLoanIds = MutableStateFlow<Set<Long>>(emptySet())

    private val _batchMessage = MutableStateFlow<String?>(null)
    val batchMessage: StateFlow<String?> = _batchMessage.asStateFlow()

    val uiState: StateFlow<PersonDetailUiState> = combine(
        personRepository.observeById(personId),
        loanRepository.observeByPerson(personId),
        _deleteError,
        _pendingDeleteLoanName,
        _isLoanSelectionMode,
        _selectedLoanIds
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val person = values[0] as PersonEntity?
        val loans = values[1] as List<LoanWithRepayments>
        PersonDetailUiState(
            person = person,
            loans = loans,
            totalLent = loans.sumOf { it.loan.amount },
            totalRepaid = loans.sumOf { it.totalRepaid },
            deleteError = values[2] as String?,
            pendingDeleteLoanName = values[3] as String?,
            isLoanSelectionMode = values[4] as Boolean,
            selectedLoanIds = values[5] as Set<Long>
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PersonDetailUiState())

    fun deletePerson() {
        viewModelScope.launch {
            val person = personRepository.getById(personId) ?: return@launch
            val result = personRepository.delete(person)
            result.onFailure { _deleteError.value = it.message }
        }
    }

    fun deleteLoan(loan: LoanEntity) {
        pendingDeleteLoanJob?.cancel()
        pendingDeleteLoan = loan
        _pendingDeleteLoanName.value = "借条"

        pendingDeleteLoanJob = viewModelScope.launch {
            delay(5000)
            loanRepository.delete(loan)
            _pendingDeleteLoanName.value = null
            pendingDeleteLoan = null
        }
    }

    fun undoDeleteLoan() {
        pendingDeleteLoanJob?.cancel()
        pendingDeleteLoanJob = null
        pendingDeleteLoan = null
        _pendingDeleteLoanName.value = null
    }

    fun clearDeleteError() {
        _deleteError.value = null
    }

    // === 借条批量操作 ===

    fun enterLoanSelectionMode(firstLoanId: Long) {
        _isLoanSelectionMode.value = true
        _selectedLoanIds.value = setOf(firstLoanId)
    }

    fun toggleLoanSelection(loanId: Long) {
        val current = _selectedLoanIds.value
        _selectedLoanIds.value = if (loanId in current) {
            val updated = current - loanId
            if (updated.isEmpty()) {
                _isLoanSelectionMode.value = false
            }
            updated
        } else {
            current + loanId
        }
    }

    fun exitLoanSelectionMode() {
        _isLoanSelectionMode.value = false
        _selectedLoanIds.value = emptySet()
    }

    fun batchArchiveLoans() {
        viewModelScope.launch {
            val selectedIds = _selectedLoanIds.value
            var archivedCount = 0
            for (loanId in selectedIds) {
                loanRepository.updateStatus(loanId, LoanStatus.ARCHIVED)
                archivedCount++
            }
            _batchMessage.value = "已归档 $archivedCount 条借条"
            exitLoanSelectionMode()
        }
    }

    fun clearBatchMessage() {
        _batchMessage.value = null
    }

    companion object {
        fun factory(container: AppContainer, personId: Long) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PersonDetailViewModel(
                    container.personRepository,
                    container.loanRepository,
                    personId
                ) as T
            }
        }
    }
}
