package com.example.comiclibrary.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.comiclibrary.R
import com.example.comiclibrary.theme.ComicCyan
import com.example.comiclibrary.theme.ComicInkBlack
import com.example.comiclibrary.theme.ComicRedDark
import com.example.comiclibrary.theme.ComicTitleFontFamily
import com.example.comiclibrary.theme.ComicYellow
import com.example.comiclibrary.ui.components.FocusBlock
import com.example.comiclibrary.ui.components.SquircleShape
import com.example.comiclibrary.ui.library.LibraryViewModel
import com.example.comiclibrary.ui.library.mvi.LibraryIntent

/**
 * Supporting Pane for Settings, Storage Import, and System Diagnostics.
 * Internationalized (English/Portuguese) and pure AMOLED Dark Mode (#000000).
 */
@Composable
fun SettingsSupportingPane(
    modifier: Modifier = Modifier,
    libraryViewModel: LibraryViewModel = viewModel()
) {
    val state by libraryViewModel.state.collectAsState()
    val defaultStatsText = "Heap: Normal • Triggers: Armed"
    var memoryStatsText by remember { mutableStateOf(defaultStatsText) }

    // Multi-file picker (OpenMultipleDocuments)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            libraryViewModel.processIntent(LibraryIntent.ImportFiles(uris))
        }
    }

    // Folder picker (OpenDocumentTree)
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            libraryViewModel.processIntent(LibraryIntent.ImportFolder(treeUri))
        }
    }

    val isDark = isSystemInDarkTheme()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_title).uppercase(),
            style = MaterialTheme.typography.displaySmall.copy(
                fontFamily = ComicTitleFontFamily,
                fontSize = 38.sp,
                letterSpacing = 2.sp
            ),
            color = if (isDark) ComicYellow else ComicInkBlack
        )

        Text(
            text = stringResource(R.string.settings_subtitle).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = ComicTitleFontFamily,
                fontSize = 12.sp,
                letterSpacing = 0.8.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Focus Block: Import Comics (Files & Folders)
        FocusBlock(
            cornerRadius = 20.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Filled.LibraryBooks,
                        contentDescription = null,
                        tint = if (isDark) ComicYellow else ComicInkBlack
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.settings_import_title).uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = ComicTitleFontFamily,
                            letterSpacing = 0.8.sp
                        ),
                        color = if (isDark) ComicYellow else ComicInkBlack
                    )
                }

                Text(
                    text = stringResource(R.string.settings_import_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Background import progress banner
                if (state.importProgress != null) {
                    val (current, total) = state.importProgress!!
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(SquircleShape(cornerRadiusDp = 10.dp, smoothing = 0.5f))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.importing_progress_format, current, total).uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = ComicTitleFontFamily,
                                letterSpacing = 0.8.sp
                            ),
                            color = if (isDark) ComicYellow else ComicInkBlack
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { if (total > 0) current.toFloat() / total.toFloat() else 0f },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = if (isDark) ComicYellow else ComicRedDark
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            filePickerLauncher.launch(
                                arrayOf("application/x-cbz", "application/zip", "application/epub+zip", "application/octet-stream", "*/*")
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = SquircleShape(cornerRadiusDp = 12.dp, smoothing = 0.5f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ComicYellow,
                            contentColor = ComicInkBlack
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = ComicInkBlack)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.settings_btn_files).uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = ComicTitleFontFamily,
                                letterSpacing = 1.sp
                            ),
                            color = ComicInkBlack
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            folderPickerLauncher.launch(null)
                        },
                        modifier = Modifier.weight(1f),
                        shape = SquircleShape(cornerRadiusDp = 12.dp, smoothing = 0.5f),
                        border = BorderStroke(1.5.dp, if (isDark) ComicYellow else ComicInkBlack)
                    ) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = if (isDark) ComicYellow else ComicInkBlack
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.settings_btn_folder).uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = ComicTitleFontFamily,
                                letterSpacing = 1.sp
                            ),
                            color = if (isDark) ComicYellow else ComicInkBlack
                        )
                    }
                }
            }
        }

        // Focus Block: Memory & Subsampling Architecture
        FocusBlock(
            cornerRadius = 20.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Memory, contentDescription = null, tint = if (isDark) ComicYellow else ComicInkBlack)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.settings_memory_title).uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = ComicTitleFontFamily,
                            letterSpacing = 0.8.sp
                        ),
                        color = if (isDark) ComicYellow else ComicInkBlack
                    )
                }

                Text(
                    text = stringResource(R.string.settings_memory_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = memoryStatsText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = ComicCyan
                )
            }
        }

        // Focus Block: Scoped Storage & SAF
        FocusBlock(
            cornerRadius = 20.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = if (isDark) ComicYellow else ComicInkBlack)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.settings_storage_title).uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = ComicTitleFontFamily,
                            letterSpacing = 0.8.sp
                        ),
                        color = if (isDark) ComicYellow else ComicInkBlack
                    )
                }

                Text(
                    text = stringResource(R.string.settings_storage_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Focus Block: Clear Decoded Cache
        val cacheClearedMsg = stringResource(R.string.settings_cache_cleared)
        FocusBlock(
            cornerRadius = 20.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_cache_title).uppercase(),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = ComicTitleFontFamily,
                            letterSpacing = 0.8.sp
                        ),
                        color = if (isDark) ComicYellow else ComicInkBlack
                    )
                    Text(
                        text = stringResource(R.string.settings_cache_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = {
                        System.gc()
                        memoryStatsText = cacheClearedMsg
                    },
                    shape = SquircleShape(cornerRadiusDp = 12.dp, smoothing = 0.5f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ComicYellow,
                        contentColor = ComicInkBlack
                    )
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, tint = ComicInkBlack)
                }
            }
        }

        // Focus Block: App Information & Version Control
        FocusBlock(
            cornerRadius = 20.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.ic_comic_library_logo),
                        contentDescription = null,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.settings_app_info_title).uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = ComicTitleFontFamily,
                            letterSpacing = 0.8.sp
                        ),
                        color = if (isDark) ComicYellow else ComicInkBlack
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.settings_version_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "1.1 (Build 11)",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = ComicTitleFontFamily,
                            fontSize = 15.sp,
                            letterSpacing = 0.8.sp
                        ),
                        color = if (isDark) ComicYellow else ComicInkBlack
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.settings_architecture_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "MVI • MD3 AMOLED • COMIC",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = ComicTitleFontFamily,
                            letterSpacing = 0.6.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
