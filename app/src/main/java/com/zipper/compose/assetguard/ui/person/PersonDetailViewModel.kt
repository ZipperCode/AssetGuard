package com.zipper.compose.assetguard.ui.person

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zipper.compose.assetguard.data.local.entity.LoanEntity
import com.zipper.compose.assetguard.data.local.entity.LoanWithRepayments
import com.zipper.compose.assetguard.data.local.entity.PersonEntity
import com.zipper.compose.assetguard.data.repository.LoanRepository
import com.zipper.compose.assetguard.data.repository.PersonRepository
import com.zipper.compose.assetguard.di.AppContainer
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
    val deleteError: String? = null
) {
    val totalOutstanding: Long get() = totalLent - totalRepaid
}

class PersonDetailViewModel(
    private val personRepository: PersonRepository,
    private val loanRepository: LoanRepository,
    private val personId: Long
) : ViewModel() {

    private val _deleteError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<PersonDetailUiState> = combine(
        personRepository.observeById(personId),
        loanRepository.observeByPerson(personId),
        _deleteError
    ) { person, loans, deleteError ->
        PersonDetailUiState(
            person = person,
            loans = loans,
            totalLent = loans.sumOf { it.loan.amount },
            totalRepaid = loans.sumOf { it.totalRepaid },
            deleteError = deleteError
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
        viewModelScope.launch {
            loanRepository.delete(loan)
        }
    }

    fun clearDeleteError() {
        _deleteError.value = null
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
