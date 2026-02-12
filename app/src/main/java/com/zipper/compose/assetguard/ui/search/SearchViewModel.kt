package com.zipper.compose.assetguard.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zipper.compose.assetguard.data.local.entity.LoanEntity
import com.zipper.compose.assetguard.data.local.entity.PersonWithSummary
import com.zipper.compose.assetguard.data.repository.LoanRepository
import com.zipper.compose.assetguard.data.repository.PersonRepository
import com.zipper.compose.assetguard.di.AppContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class SearchFilter(
    val minAmount: Long? = null,
    val maxAmount: Long? = null,
    val statusFilter: Int? = null,
    val dueBefore: Long? = null,
    val dueAfter: Long? = null
)

data class SearchUiState(
    val query: String = "",
    val filter: SearchFilter = SearchFilter(),
    val persons: List<PersonWithSummary> = emptyList(),
    val loans: List<LoanEntity> = emptyList(),
    val isFilterExpanded: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val personRepository: PersonRepository,
    private val loanRepository: LoanRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _filter = MutableStateFlow(SearchFilter())
    private val _isFilterExpanded = MutableStateFlow(false)

    val uiState: StateFlow<SearchUiState> = combine(
        _query.flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList())
            else personRepository.searchWithSummary(query)
        },
        combine(_query, _filter) { query, filter ->
            Pair(query, filter)
        }.flatMapLatest { (query, filter) ->
            if (query.isBlank() && filter == SearchFilter()) flowOf(emptyList())
            else loanRepository.searchLoans(
                minAmount = filter.minAmount,
                maxAmount = filter.maxAmount,
                statusFilter = filter.statusFilter,
                dueBefore = filter.dueBefore,
                dueAfter = filter.dueAfter
            )
        },
        _query,
        _filter,
        _isFilterExpanded
    ) { persons, loans, query, filter, isFilterExpanded ->
        SearchUiState(
            query = query,
            filter = filter,
            persons = persons,
            loans = loans,
            isFilterExpanded = isFilterExpanded
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState())

    fun onQueryChanged(query: String) {
        _query.value = query
    }

    fun onFilterChanged(filter: SearchFilter) {
        _filter.value = filter
    }

    fun toggleFilter() {
        _isFilterExpanded.value = !_isFilterExpanded.value
    }

    fun clearFilters() {
        _filter.value = SearchFilter()
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SearchViewModel(
                    container.personRepository,
                    container.loanRepository
                ) as T
            }
        }
    }
}
