package com.pocketscan.app.ui.viewer

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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ViewerUiState {
    data object Loading : ViewerUiState
    data class Loaded(val document: Document) : ViewerUiState
    data object Deleted : ViewerUiState
    data class Error(val message: String) : ViewerUiState
}

sealed interface ViewerEvent {
    data class Snackbar(val message: String) : ViewerEvent
    data object Close : ViewerEvent
}

class ViewerViewModel(
    private val container: AppContainer,
    private val documentId: Long,
) : ViewModel() {

    val uiState: StateFlow<ViewerUiState> = container.repository.observeAll()
        .map { docs -> docs.firstOrNull { it.id == documentId } }
        .map { doc ->
            when {
                doc != null -> ViewerUiState.Loaded(doc)
                else -> ViewerUiState.Deleted
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ViewerUiState.Loading)

    private val _events = MutableSharedFlow<ViewerEvent>(extraBufferCapacity = 4)
    val events = _events.asSharedFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    fun rename(newName: String) {
        viewModelScope.launch {
            _busy.value = true
            container.repository.rename(documentId, newName)
                .onFailure { _events.tryEmit(ViewerEvent.Snackbar("Rename failed.")) }
            _busy.value = false
        }
    }

    fun delete() {
        viewModelScope.launch {
            _busy.value = true
            container.repository.delete(documentId)
                .onSuccess { _events.tryEmit(ViewerEvent.Close) }
                .onFailure { _events.tryEmit(ViewerEvent.Snackbar("Could not delete scan.")) }
            _busy.value = false
        }
    }

    fun shareUri(): android.net.Uri? {
        val s = uiState.value
        return if (s is ViewerUiState.Loaded) container.fileManager.shareUri(s.document.pdfPath) else null
    }

    companion object {
        fun factory(container: AppContainer, id: Long): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ViewerViewModel(container, id) as T
            }
    }
}
