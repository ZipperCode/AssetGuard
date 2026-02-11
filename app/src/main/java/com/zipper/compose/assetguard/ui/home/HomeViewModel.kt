package com.zipper.compose.assetguard.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zipper.compose.assetguard.data.local.entity.LoanEntity
import com.zipper.compose.assetguard.data.local.entity.PersonWithSummary
import com.zipper.compose.assetguard.data.repository.LoanRepository
import com.zipper.compose.assetguard.data.repository.PersonRepository
import com.zipper.compose.assetguard.data.repository.RepaymentRepository
import com.zipper.compose.assetguard.di.AppContainer
import com.zipper.compose.assetguard.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val persons: List<PersonWithSummary> = emptyList(),
    val totalLent: Long = 0L,
    val totalRepaid: Long = 0L,
    val dueSoonLoans: List<LoanEntity> = emptyList(),
    val searchQuery: String = "",
    val deleteError: String? = null
) {
    val totalOutstanding: Long get() = totalLent - totalRepaid
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val personRepository: PersonRepository,
    private val loanRepository: LoanRepository,
    private val repaymentRepository: RepaymentRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError: StateFlow<String?> = _deleteError.asStateFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        _searchQuery.flatMapLatest { query ->
            if (query.isBlank()) personRepository.observeAllWithSummary()
            else personRepository.searchWithSummary(query)
        },
        loanRepository.observeTotalLent(),
        repaymentRepository.observeTotalRepaid(),
        loanRepository.observeOverdueOrDueSoon(DateUtils.daysFromNow(7)),
        _deleteError
    ) { persons, totalLent, totalRepaid, dueSoonLoans, deleteError ->
        HomeUiState(
            persons = persons,
            totalLent = totalLent,
            totalRepaid = totalRepaid,
            dueSoonLoans = dueSoonLoans,
            searchQuery = _searchQuery.value,
            deleteError = deleteError
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun deletePerson(personWithSummary: PersonWithSummary) {
        viewModelScope.launch {
            val result = personRepository.delete(personWithSummary.person)
            result.onFailure { _deleteError.value = it.message }
        }
    }

    fun clearDeleteError() {
        _deleteError.value = null
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(
                    container.personRepository,
                    container.loanRepository,
                    container.repaymentRepository
                ) as T
            }
        }
    }
}
