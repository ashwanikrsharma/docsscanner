package com.pocketscan.app.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pocketscan.app.di.AppContainer
import com.pocketscan.app.domain.Document
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object Empty : HomeUiState
    data class Success(val documents: List<Document>) : HomeUiState
}

sealed interface HomeEvent {
    data class ShowSnackbar(val message: String) : HomeEvent
}

/** Pending scan awaiting the user's Save/Discard decision. */
data class PendingScan(val uri: Uri, val suggestedName: String)

class HomeViewModel(private val container: AppContainer) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = container.repository.observeAll()
        .map { docs -> if (docs.isEmpty()) HomeUiState.Empty else HomeUiState.Success(docs) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    private val _events = MutableSharedFlow<HomeEvent>(extraBufferCapacity = 4)
    val events = _events.asSharedFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _pendingScan = MutableStateFlow<PendingScan?>(null)
    val pendingScan: StateFlow<PendingScan?> = _pendingScan.asStateFlow()

    /**
     * Stage a successful scan; the user must confirm via SaveScanDialog before the file
     * is persisted. Until then, nothing is written to app storage.
     */
    fun onScanResult(uri: Uri) {
        _pendingScan.value = PendingScan(uri = uri, suggestedName = container.repository.buildDefaultScanName())
    }

    /** User confirmed Save in the dialog — persist the staged scan with the chosen name. */
    fun confirmSavePendingScan(name: String) {
        val pending = _pendingScan.value ?: return
        _pendingScan.value = null
        viewModelScope.launch {
            _saving.value = true
            runCatching { container.repository.importFromScanner(pending.uri, name) }
                .onFailure { _events.tryEmit(HomeEvent.ShowSnackbar("Could not save scan.")) }
            _saving.value = false
        }
    }

    /** User chose Discard — drop the staged scan without copying to local storage. */
    fun discardPendingScan() {
        if (_pendingScan.value != null) {
            _pendingScan.value = null
            _events.tryEmit(HomeEvent.ShowSnackbar("Scan discarded."))
        }
    }

    fun onScannerError(message: String) {
        _events.tryEmit(HomeEvent.ShowSnackbar(message))
    }

    fun renameDocument(id: Long, newName: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            container.repository.rename(id, newName)
                .onSuccess { onSuccess() }
                .onFailure { _events.tryEmit(HomeEvent.ShowSnackbar("Rename failed.")) }
        }
    }

    fun deleteDocument(id: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            container.repository.delete(id)
                .onSuccess { onSuccess() }
                .onFailure { _events.tryEmit(HomeEvent.ShowSnackbar("Could not delete scan.")) }
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(container) as T
        }
    }
}
