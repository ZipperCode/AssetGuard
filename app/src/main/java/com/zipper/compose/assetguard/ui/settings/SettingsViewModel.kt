package com.zipper.compose.assetguard.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zipper.compose.assetguard.data.backup.BackupData
import com.zipper.compose.assetguard.data.backup.BackupManager
import com.zipper.compose.assetguard.data.backup.ConflictStrategy
import com.zipper.compose.assetguard.data.backup.DataIntegrityChecker
import com.zipper.compose.assetguard.data.backup.ImportPreview
import com.zipper.compose.assetguard.data.backup.ImportResult
import com.zipper.compose.assetguard.data.model.ThemeMode
import com.zipper.compose.assetguard.data.preferences.UserPreferences
import com.zipper.compose.assetguard.data.preferences.UserPreferencesRepository
import com.zipper.compose.assetguard.data.repository.PaymentMethodRepository
import com.zipper.compose.assetguard.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class ImportState {
    data object Idle : ImportState()
    data class Previewing(val data: BackupData, val preview: ImportPreview) : ImportState()
    data object Importing : ImportState()
    data class Done(val result: ImportResult) : ImportState()
    data class Error(val message: String) : ImportState()
}

class SettingsViewModel(
    private val paymentMethodRepository: PaymentMethodRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val backupManager: BackupManager,
    private val integrityChecker: DataIntegrityChecker
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    private val _integrityReport = MutableStateFlow<DataIntegrityChecker.IntegrityReport?>(null)
    val integrityReport: StateFlow<DataIntegrityChecker.IntegrityReport?> = _integrityReport.asStateFlow()

    val userPreferences: StateFlow<UserPreferences> =
        userPreferencesRepository.userPreferences
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    fun updateReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            userPreferencesRepository.updateReminderTime(hour, minute)
        }
    }

    fun updateReminderAdvanceDays(days: Int) {
        viewModelScope.launch {
            userPreferencesRepository.updateReminderAdvanceDays(days)
        }
    }

    fun updateOnlyOverdue(onlyOverdue: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateOnlyOverdue(onlyOverdue)
        }
    }

    fun updateSilentHours(startHour: Int, endHour: Int) {
        viewModelScope.launch {
            userPreferencesRepository.updateSilentHours(startHour, endHour)
        }
    }

    fun setNotificationPermissionAsked() {
        viewModelScope.launch {
            userPreferencesRepository.setNotificationPermissionAsked(true)
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            userPreferencesRepository.saveThemeMode(mode)
        }
    }

    fun parseBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = backupManager.parseBackup(context, uri)
            if (result.isSuccess) {
                val data = result.getOrThrow()
                val preview = backupManager.previewImport(data)
                _importState.value = ImportState.Previewing(data, preview)
            } else {
                _importState.value = ImportState.Error(
                    result.exceptionOrNull()?.message ?: "解析备份文件失败"
                )
            }
        }
    }

    fun confirmImport(data: BackupData, strategy: ConflictStrategy = ConflictStrategy.SKIP) {
        viewModelScope.launch {
            _importState.value = ImportState.Importing
            val result = backupManager.executeImport(data, strategy)
            _importState.value = ImportState.Done(result)
        }
    }

    fun resetImportState() {
        _importState.value = ImportState.Idle
    }

    fun runIntegrityCheck() {
        viewModelScope.launch {
            val report = integrityChecker.check()
            _integrityReport.value = report
        }
    }

    fun clearIntegrityReport() {
        _integrityReport.value = null
    }

    fun clearMessage() {
        _message.value = null
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(
                    container.paymentMethodRepository,
                    container.userPreferencesRepository,
                    BackupManager(container),
                    DataIntegrityChecker(container)
                ) as T
            }
        }
    }
}
