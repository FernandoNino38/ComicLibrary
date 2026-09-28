package com.example.comiclibrary.ui.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.comiclibrary.data.repository.ComicRepository
import com.example.comiclibrary.di.AppDispatchers
import com.example.comiclibrary.ui.library.mvi.LibraryIntent
import com.example.comiclibrary.ui.library.mvi.LibraryState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVI ViewModel managing Comic Library State.
 * Utilizes [AppDispatchers.libraryIndexing] (limitedParallelism(4)) for concurrency limits.
 */
class LibraryViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: ComicRepository = ComicRepository(application)

    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    init {
        // Collect repository updates and project into LibraryState
        repository.comics.onEach { comicsList ->
            _state.update { it.copy(comics = comicsList) }
        }.launchIn(viewModelScope)

        repository.isLoading.onEach { loading ->
            _state.update { it.copy(isLoading = loading) }
        }.launchIn(viewModelScope)

        repository.importProgress.onEach { progress ->
            _state.update { it.copy(importProgress = progress) }
        }.launchIn(viewModelScope)
    }

    fun processIntent(intent: LibraryIntent) {
        when (intent) {
            is LibraryIntent.ImportFiles -> {
                viewModelScope.launch(AppDispatchers.libraryIndexing) {
                    try {
                        repository.importComics(intent.uris)
                    } catch (e: Exception) {
                        _state.update { it.copy(errorMessage = "Erro ao importar arquivos: ${e.localizedMessage}") }
                    }
                }
            }
            is LibraryIntent.ImportFolder -> {
                viewModelScope.launch(AppDispatchers.libraryIndexing) {
                    try {
                        repository.importFolder(intent.treeUri)
                    } catch (e: Exception) {
                        _state.update { it.copy(errorMessage = "Erro ao importar pasta: ${e.localizedMessage}") }
                    }
                }
            }
            is LibraryIntent.SelectComic -> {
                _state.update { it.copy(selectedComicId = intent.comicId) }
            }
            is LibraryIntent.ToggleFavorite -> {
                repository.toggleFavorite(intent.comicId)
            }
            is LibraryIntent.DeleteComic -> {
                repository.deleteComic(intent.comicId)
                if (_state.value.selectedComicId == intent.comicId) {
                    _state.update { it.copy(selectedComicId = null) }
                }
            }
            is LibraryIntent.UpdateProgress -> {
                repository.updateReadingProgress(intent.comicId, intent.pageIndex)
            }
            is LibraryIntent.UpdateSearch -> {
                _state.update { it.copy(searchQuery = intent.query) }
            }
            is LibraryIntent.UpdateFilter -> {
                _state.update { it.copy(selectedFilter = intent.filter) }
            }
            is LibraryIntent.DismissError -> {
                _state.update { it.copy(errorMessage = null) }
            }
        }
    }
}
