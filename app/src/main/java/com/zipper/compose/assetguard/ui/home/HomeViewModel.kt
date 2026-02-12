package com.zipper.compose.assetguard.ui.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zipper.compose.assetguard.data.backup.BackupManager
import com.zipper.compose.assetguard.data.local.entity.LoanEntity
import com.zipper.compose.assetguard.data.local.entity.LoanStatus
import com.zipper.compose.assetguard.data.local.entity.PersonWithSummary
import com.zipper.compose.assetguard.data.repository.LoanRepository
import com.zipper.compose.assetguard.data.repository.PersonRepository
import com.zipper.compose.assetguard.data.repository.RepaymentRepository
import com.zipper.compose.assetguard.di.AppContainer
import com.zipper.compose.assetguard.notification.NotificationHelper
import com.zipper.compose.assetguard.util.DateUtils
import com.zipper.compose.assetguard.util.MoneyUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val deleteError: String? = null,
    // KPI
    val overdueCount: Int = 0,
    val dueTodayCount: Int = 0,
    val totalPending: Long = 0L,
    // 可恢复删除
    val pendingDeletePersonName: String? = null,
    // 批量操作
    val isSelectionMode: Boolean = false,
    val selectedPersonIds: Set<Long> = emptySet()
) {
    val totalOutstanding: Long get() = totalLent - totalRepaid
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val personRepository: PersonRepository,
    private val loanRepository: LoanRepository,
    private val repaymentRepository: RepaymentRepository,
    private val backupManager: BackupManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError: StateFlow<String?> = _deleteError.asStateFlow()

    private val _pendingDeletePersonName = MutableStateFlow<String?>(null)

    private var pendingDeleteJob: Job? = null
    private var pendingDeletePerson: PersonWithSummary? = null

    // 批量选择
    private val _isSelectionMode = MutableStateFlow(false)
    private val _selectedPersonIds = MutableStateFlow<Set<Long>>(emptySet())

    private val _batchMessage = MutableStateFlow<String?>(null)
    val batchMessage: StateFlow<String?> = _batchMessage.asStateFlow()

    private val todayStart = DateUtils.todayStart()
    private val tomorrowStart = todayStart + 86_400_000L

    val uiState: StateFlow<HomeUiState> = combine(
        _searchQuery.flatMapLatest { query ->
            if (query.isBlank()) personRepository.observeAllWithSummary()
            else personRepository.searchWithSummary(query)
        },
        loanRepository.observeTotalLent(),
        repaymentRepository.observeTotalRepaid(),
        loanRepository.observeOverdueOrDueSoon(DateUtils.daysFromNow(7)),
        _deleteError,
        loanRepository.observeOverdueCount(todayStart),
        loanRepository.observeDueTodayCount(todayStart, tomorrowStart),
        loanRepository.observeTotalOutstanding(),
        _pendingDeletePersonName,
        _isSelectionMode,
        _selectedPersonIds
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        HomeUiState(
            persons = values[0] as List<PersonWithSummary>,
            totalLent = values[1] as Long,
            totalRepaid = values[2] as Long,
            dueSoonLoans = values[3] as List<LoanEntity>,
            searchQuery = _searchQuery.value,
            deleteError = values[4] as String?,
            overdueCount = values[5] as Int,
            dueTodayCount = values[6] as Int,
            totalPending = values[7] as Long,
            pendingDeletePersonName = values[8] as String?,
            isSelectionMode = values[9] as Boolean,
            selectedPersonIds = values[10] as Set<Long>
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun deletePerson(personWithSummary: PersonWithSummary) {
        pendingDeleteJob?.cancel()
        pendingDeletePerson = personWithSummary
        _pendingDeletePersonName.value = personWithSummary.person.name

        pendingDeleteJob = viewModelScope.launch {
            delay(5000)
            val result = personRepository.delete(personWithSummary.person)
            result.onFailure { _deleteError.value = it.message }
            _pendingDeletePersonName.value = null
            pendingDeletePerson = null
        }
    }

    fun undoDelete() {
        pendingDeleteJob?.cancel()
        pendingDeleteJob = null
        pendingDeletePerson = null
        _pendingDeletePersonName.value = null
    }

    fun clearDeleteError() {
        _deleteError.value = null
    }

    // === 批量操作 ===

    fun enterSelectionMode(firstPersonId: Long) {
        _isSelectionMode.value = true
        _selectedPersonIds.value = setOf(firstPersonId)
    }

    fun togglePersonSelection(personId: Long) {
        val current = _selectedPersonIds.value
        _selectedPersonIds.value = if (personId in current) {
            val updated = current - personId
            if (updated.isEmpty()) {
                _isSelectionMode.value = false
            }
            updated
        } else {
            current + personId
        }
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedPersonIds.value = emptySet()
    }

    fun batchRemind(context: Context) {
        viewModelScope.launch {
            val selectedIds = _selectedPersonIds.value
            val persons = uiState.value.persons.filter { it.person.id in selectedIds }
            var count = 0
            persons.forEach { person ->
                if (person.outstanding > 0) {
                    NotificationHelper.showReminderNotification(
                        context = context,
                        title = "还款提醒 - ${person.person.name}",
                        message = "待还金额: ${MoneyUtils.formatCents(person.outstanding)}"
                    )
                    count++
                }
            }
            _batchMessage.value = "已发送 $count 条提醒通知"
            exitSelectionMode()
        }
    }

    fun batchExport(context: Context, uri: Uri) {
        viewModelScope.launch {
            val selectedIds = _selectedPersonIds.value
            val result = backupManager.exportPartial(context, uri, selectedIds)
            _batchMessage.value = if (result.isSuccess) {
                "已导出 ${selectedIds.size} 位联系人的数据"
            } else {
                "导出失败: ${result.exceptionOrNull()?.message}"
            }
            exitSelectionMode()
        }
    }

    fun batchArchive() {
        viewModelScope.launch {
            val selectedIds = _selectedPersonIds.value
            var archivedCount = 0
            for (personId in selectedIds) {
                val loans = loanRepository.getByPersonId(personId)
                for (loan in loans) {
                    if (loan.status !in LoanStatus.EXCLUDED_FROM_ACTIVE) {
                        loanRepository.updateStatus(loan.id, LoanStatus.ARCHIVED)
                        archivedCount++
                    }
                }
            }
            _batchMessage.value = "已归档 $archivedCount 条借条"
            exitSelectionMode()
        }
    }

    fun clearBatchMessage() {
        _batchMessage.value = null
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(
                    container.personRepository,
                    container.loanRepository,
                    container.repaymentRepository,
                    BackupManager(container)
                ) as T
            }
        }
    }
}
