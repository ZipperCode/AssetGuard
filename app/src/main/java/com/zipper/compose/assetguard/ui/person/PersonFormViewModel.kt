package com.zipper.compose.assetguard.ui.person

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zipper.compose.assetguard.data.local.entity.PersonEntity
import com.zipper.compose.assetguard.data.repository.PersonRepository
import com.zipper.compose.assetguard.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PersonFormUiState(
    val name: String = "",
    val phone: String = "",
    val note: String = "",
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val nameError: String? = null
)

class PersonFormViewModel(
    private val personRepository: PersonRepository,
    private val personId: Long?
) : ViewModel() {

    private val _uiState = MutableStateFlow(PersonFormUiState(isEditing = personId != null))
    val uiState: StateFlow<PersonFormUiState> = _uiState.asStateFlow()

    init {
        if (personId != null) {
            viewModelScope.launch {
                personRepository.getById(personId)?.let { person ->
                    _uiState.value = _uiState.value.copy(
                        name = person.name,
                        phone = person.phone ?: "",
                        note = person.note ?: ""
                    )
                }
            }
        }
    }

    fun onNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(name = name, nameError = null)
    }

    fun onPhoneChanged(phone: String) {
        _uiState.value = _uiState.value.copy(phone = phone)
    }

    fun onNoteChanged(note: String) {
        _uiState.value = _uiState.value.copy(note = note)
    }

    fun save() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.value = state.copy(nameError = "姓名不能为空")
            return
        }

        _uiState.value = state.copy(isSaving = true)
        viewModelScope.launch {
            if (personId != null) {
                val existing = personRepository.getById(personId) ?: return@launch
                personRepository.update(
                    existing.copy(
                        name = state.name.trim(),
                        phone = state.phone.trim().ifBlank { null },
                        note = state.note.trim().ifBlank { null }
                    )
                )
            } else {
                personRepository.insert(
                    PersonEntity(
                        name = state.name.trim(),
                        phone = state.phone.trim().ifBlank { null },
                        note = state.note.trim().ifBlank { null }
                    )
                )
            }
            _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true)
        }
    }

    companion object {
        fun factory(container: AppContainer, personId: Long?) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PersonFormViewModel(container.personRepository, personId) as T
            }
        }
    }
}
