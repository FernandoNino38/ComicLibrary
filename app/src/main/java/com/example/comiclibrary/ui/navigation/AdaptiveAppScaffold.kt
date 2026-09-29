package com.example.comiclibrary.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.comiclibrary.R
import com.example.comiclibrary.data.model.ComicBook
import com.example.comiclibrary.ui.library.ComicDetailPane
import com.example.comiclibrary.ui.library.ComicLibraryScreen
import com.example.comiclibrary.ui.library.LibraryViewModel
import com.example.comiclibrary.ui.library.mvi.LibraryIntent
import com.example.comiclibrary.ui.reader.ComicReaderScreen
import com.example.comiclibrary.ui.settings.SettingsSupportingPane
import kotlinx.coroutines.launch

enum class AppDestination(val label: String) {
    LIBRARY("Biblioteca"),
    SETTINGS("Ajustes")
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AdaptiveAppScaffold(
    modifier: Modifier = Modifier,
    libraryViewModel: LibraryViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val state by libraryViewModel.state.collectAsState()

    var currentDestination by remember { mutableStateOf(AppDestination.LIBRARY) }
    var activeReadingComic by remember { mutableStateOf<ComicBook?>(null) }

    // ListDetailPaneScaffoldNavigator for Adaptive Canonical Layouts
    val navigator = rememberListDetailPaneScaffoldNavigator<String>()
    val currentComicId = navigator.currentDestination?.contentKey ?: state.selectedComicId
    val selectedComic = state.comics.firstOrNull { it.id == currentComicId } ?: state.comics.firstOrNull()

    // 1. BackHandler when in reader
    BackHandler(enabled = activeReadingComic != null) {
        activeReadingComic = null
    }

    // 2. BackHandler when navigator can go back from detail to list on phones
    BackHandler(enabled = activeReadingComic == null && navigator.canNavigateBack()) {
        scope.launch { navigator.navigateBack() }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Adaptive Navigation Suite: Bottom Bar on Phones, Navigation Rail on Tablets/Foldables
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                item(
                    selected = currentDestination == AppDestination.LIBRARY,
                    onClick = { currentDestination = AppDestination.LIBRARY },
                    icon = { Icon(Icons.Default.AutoStories, contentDescription = stringResource(R.string.nav_library)) },
                    label = { Text(stringResource(R.string.nav_library)) }
                )
                item(
                    selected = currentDestination == AppDestination.SETTINGS,
                    onClick = { currentDestination = AppDestination.SETTINGS },
                    icon = { Icon(Icons.Default.Tune, contentDescription = stringResource(R.string.nav_settings)) },
                    label = { Text(stringResource(R.string.nav_settings)) }
                )
            },
            modifier = Modifier.fillMaxSize()
        ) {
            when (currentDestination) {
                AppDestination.LIBRARY -> {
                    NavigableListDetailPaneScaffold(
                        navigator = navigator,
                        listPane = {
                            ComicLibraryScreen(
                                comics = state.comics,
                                isLoading = state.isLoading,
                                importProgress = state.importProgress,
                                selectedComicId = currentComicId,
                                onComicClick = { comic ->
                                    libraryViewModel.processIntent(LibraryIntent.SelectComic(comic.id))
                                    // Direct open reading without intermediate screen
                                    activeReadingComic = comic
                                },
                                onToggleFavorite = { comicId ->
                                    libraryViewModel.processIntent(LibraryIntent.ToggleFavorite(comicId))
                                },
                                onNavigateToSettings = {
                                    currentDestination = AppDestination.SETTINGS
                                }
                            )
                        },
                        detailPane = {
                            ComicDetailPane(
                                comic = selectedComic,
                                onReadClick = { comicToRead ->
                                    activeReadingComic = comicToRead
                                },
                                onToggleFavorite = { comicId ->
                                    libraryViewModel.processIntent(LibraryIntent.ToggleFavorite(comicId))
                                },
                                onDeleteComic = { comicId ->
                                    libraryViewModel.processIntent(LibraryIntent.DeleteComic(comicId))
                                    if (currentComicId == comicId) {
                                        if (navigator.canNavigateBack()) {
                                            scope.launch { navigator.navigateBack() }
                                        }
                                    }
                                }
                            )
                        }
                    )
                }
                AppDestination.SETTINGS -> {
                    SettingsSupportingPane(libraryViewModel = libraryViewModel)
                }
            }
        }

        // Full-screen immersive Comic Reader with expand-from-center scale animation
        AnimatedVisibility(
            visible = activeReadingComic != null,
            enter = fadeIn(animationSpec = tween(150)) + scaleIn(
                initialScale = 0.18f,
                transformOrigin = TransformOrigin.Center,
                animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
            ),
            exit = fadeOut(animationSpec = tween(180)) + scaleOut(
                targetScale = 0.18f,
                transformOrigin = TransformOrigin.Center,
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            activeReadingComic?.let { comic ->
                ComicReaderScreen(
                    comic = comic,
                    onBack = { activeReadingComic = null },
                    onProgressUpdate = { pageIndex ->
                        libraryViewModel.processIntent(
                            LibraryIntent.UpdateProgress(comic.id, pageIndex)
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
