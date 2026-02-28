package com.zipper.compose.assetguard.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zipper.compose.assetguard.data.repository.LoanRepository
import com.zipper.compose.assetguard.data.repository.PersonRepository
import com.zipper.compose.assetguard.data.repository.RepaymentRepository
import com.zipper.compose.assetguard.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ProfileUiState(
    val totalPersons: Int = 0,
    val totalLoans: Int = 0,
    val totalLent: Long = 0L,
    val totalRepaid: Long = 0L,
) {
    val totalOutstanding: Long get() = totalLent - totalRepaid
}

class ProfileViewModel(
    personRepository: PersonRepository,
    loanRepository: LoanRepository,
    repaymentRepository: RepaymentRepository,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        personRepository.observeAllWithSummary().map { it.size },
        loanRepository.observeUnpaidCount(),
        loanRepository.observeTotalLent(),
        repaymentRepository.observeTotalRepaid(),
    ) { personCount, loanCount, totalLent, totalRepaid ->
        ProfileUiState(
            totalPersons = personCount,
            totalLoans = loanCount,
            totalLent = totalLent,
            totalRepaid = totalRepaid,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileUiState())

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProfileViewModel(
                    container.personRepository,
                    container.loanRepository,
                    container.repaymentRepository,
                ) as T
            }
        }
    }
}
