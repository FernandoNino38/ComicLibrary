package com.example.comiclibrary.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.example.comiclibrary.data.archive.CbzArchiveManager
import com.example.comiclibrary.data.model.ComicBook
import com.example.comiclibrary.data.model.ComicMetadata
import com.example.comiclibrary.data.model.ComicPage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ComicRepository(private val context: Context) {

    private val _comics = MutableStateFlow<List<ComicBook>>(emptyList())
    val comics: StateFlow<List<ComicBook>> = _comics.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _importProgress = MutableStateFlow<Pair<Int, Int>?>(null)
    val importProgress: StateFlow<Pair<Int, Int>?> = _importProgress.asStateFlow()

    private val repositoryScope = CoroutineScope(Dispatchers.IO)
    private val manifestFile = File(context.filesDir, "library_manifest.json")

    init {
        loadPersistedLibrary()
    }

    private fun loadPersistedLibrary() {
        repositoryScope.launch {
            _isLoading.value = true
            try {
                if (manifestFile.exists()) {
                    val jsonStr = manifestFile.readText()
                    val array = JSONArray(jsonStr)
                    val loaded = mutableListOf<ComicBook>()

                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        loaded.add(deserializeComic(obj))
                    }
                    _comics.value = loaded
                }
            } catch (e: Exception) {
                Log.e("ComicRepository", "Failed to load library manifest: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun saveLibrary() = withContext(Dispatchers.IO) {
        try {
            val array = JSONArray()
            _comics.value.forEach { comic ->
                array.put(serializeComic(comic))
            }
            manifestFile.writeText(array.toString())
        } catch (e: Exception) {
            Log.e("ComicRepository", "Failed to save library manifest: ${e.message}", e)
        }
    }

    /**
     * Imports multiple user-selected CBZ URIs into the library.
     */
    suspend fun importComics(uris: List<Uri>) = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext

        _isLoading.value = true
        _importProgress.value = 0 to uris.size

        val newComics = mutableListOf<ComicBook>()

        uris.forEachIndexed { index, uri ->
            try {
                // Persist read permissions so the app can access the file across restarts
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}

                var fileName = "comic_${System.currentTimeMillis()}.cbz"
                var fileSize = 0L

                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                        if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                    }
                }

                val comic = CbzArchiveManager.inspectCbz(context, uri, fileName, fileSize)
                if (comic != null) {
                    newComics.add(comic)
                }
            } catch (e: Exception) {
                Log.e("ComicRepository", "Error importing $uri: ${e.message}", e)
            }
            _importProgress.value = (index + 1) to uris.size
        }

        if (newComics.isNotEmpty()) {
            _comics.update { current ->
                val newIds = newComics.map { it.id }.toSet()
                val retained = current.filterNot { it.id in newIds }
                newComics + retained
            }
            saveLibrary()
        }

        _importProgress.value = null
        _isLoading.value = false
    }

    /**
     * Scans an entire folder via Storage Access Framework (SAF OpenDocumentTree)
     * and imports all .cbz and .zip files found recursively.
     */
    suspend fun importFolder(treeUri: Uri) = withContext(Dispatchers.IO) {
        try {
            try {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}

            _isLoading.value = true
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
            val foundUris = mutableListOf<Uri>()

            if (rootDoc != null) {
                scanDirectoryRecursively(rootDoc, foundUris)
            }

            if (foundUris.isNotEmpty()) {
                importComics(foundUris)
            }
        } catch (e: Exception) {
            Log.e("ComicRepository", "Error scanning folder: ${e.message}", e)
        } finally {
            _isLoading.value = false
        }
    }

    private fun scanDirectoryRecursively(dir: DocumentFile, result: MutableList<Uri>) {
        val files = dir.listFiles()
        for (file in files) {
            if (file.isDirectory) {
                scanDirectoryRecursively(file, result)
            } else {
                val name = file.name?.lowercase() ?: ""
                if (name.endsWith(".cbz") || name.endsWith(".zip")) {
                    result.add(file.uri)
                }
            }
        }
    }

    fun deleteComic(comicId: String) {
        repositoryScope.launch {
            val comicToDelete = _comics.value.firstOrNull { it.id == comicId }
            if (comicToDelete?.coverPath != null) {
                try {
                    File(comicToDelete.coverPath).delete()
                } catch (_: Exception) {}
            }

            _comics.update { list -> list.filterNot { it.id == comicId } }
            saveLibrary()
        }
    }

    fun updateReadingProgress(comicId: String, pageIndex: Int) {
        repositoryScope.launch {
            _comics.update { list ->
                list.map { comic ->
                    if (comic.id == comicId) {
                        comic.copy(
                            lastReadPage = pageIndex,
                            isFinished = pageIndex >= (comic.totalPages - 1),
                            lastOpenedTimestamp = System.currentTimeMillis()
                        )
                    } else {
                        comic
                    }
                }
            }
            saveLibrary()
        }
    }

    fun toggleFavorite(comicId: String) {
        repositoryScope.launch {
            _comics.update { list ->
                list.map { comic ->
                    if (comic.id == comicId) {
                        comic.copy(isFavorite = !comic.isFavorite)
                    } else {
                        comic
                    }
                }
            }
            saveLibrary()
        }
    }

    // ---------------- Serialization Helpers ----------------

    private fun serializeComic(comic: ComicBook): JSONObject {
        val obj = JSONObject()
        obj.put("id", comic.id)
        obj.put("uriString", comic.uriString)
        obj.put("fileName", comic.fileName)
        obj.put("fileSizeBytes", comic.fileSizeBytes)
        obj.put("coverPath", comic.coverPath ?: "")
        obj.put("lastReadPage", comic.lastReadPage)
        obj.put("isFinished", comic.isFinished)
        obj.put("lastOpenedTimestamp", comic.lastOpenedTimestamp)
        obj.put("isFavorite", comic.isFavorite)

        val metaObj = JSONObject()
        metaObj.put("title", comic.metadata.title)
        metaObj.put("series", comic.metadata.series)
        metaObj.put("number", comic.metadata.number)
        metaObj.put("volume", comic.metadata.volume)
        metaObj.put("summary", comic.metadata.summary)
        metaObj.put("year", comic.metadata.year ?: -1)
        metaObj.put("pageCount", comic.metadata.pageCount)
        metaObj.put("writer", comic.metadata.writer)
        metaObj.put("penciller", comic.metadata.penciller)
        metaObj.put("inker", comic.metadata.inker)
        metaObj.put("publisher", comic.metadata.publisher)
        metaObj.put("genre", comic.metadata.genre)
        metaObj.put("isManga", comic.metadata.isManga)
        obj.put("metadata", metaObj)

        val pagesArr = JSONArray()
        comic.pages.forEach { p ->
            val pObj = JSONObject()
            pObj.put("index", p.index)
            pObj.put("entryName", p.entryName)
            pagesArr.put(pObj)
        }
        obj.put("pages", pagesArr)

        return obj
    }

    private fun deserializeComic(obj: JSONObject): ComicBook {
        val metaObj = obj.optJSONObject("metadata") ?: JSONObject()
        val yearVal = metaObj.optInt("year", -1)

        val metadata = ComicMetadata(
            title = metaObj.optString("title", ""),
            series = metaObj.optString("series", ""),
            number = metaObj.optString("number", ""),
            volume = metaObj.optString("volume", ""),
            summary = metaObj.optString("summary", ""),
            year = if (yearVal != -1) yearVal else null,
            pageCount = metaObj.optInt("pageCount", 0),
            writer = metaObj.optString("writer", ""),
            penciller = metaObj.optString("penciller", ""),
            inker = metaObj.optString("inker", ""),
            publisher = metaObj.optString("publisher", ""),
            genre = metaObj.optString("genre", ""),
            isManga = metaObj.optBoolean("isManga", false)
        )

        val pages = mutableListOf<ComicPage>()
        val pagesArr = obj.optJSONArray("pages")
        if (pagesArr != null) {
            for (i in 0 until pagesArr.length()) {
                val pObj = pagesArr.getJSONObject(i)
                pages.add(
                    ComicPage(
                        index = pObj.optInt("index", i),
                        entryName = pObj.optString("entryName", "")
                    )
                )
            }
        }

        val coverPathStr = obj.optString("coverPath", "")

        return ComicBook(
            id = obj.getString("id"),
            uriString = obj.getString("uriString"),
            fileName = obj.getString("fileName"),
            fileSizeBytes = obj.optLong("fileSizeBytes", 0L),
            coverPath = if (coverPathStr.isNotBlank()) coverPathStr else null,
            pages = pages,
            metadata = metadata,
            lastReadPage = obj.optInt("lastReadPage", 0),
            isFinished = obj.optBoolean("isFinished", false),
            lastOpenedTimestamp = obj.optLong("lastOpenedTimestamp", 0L),
            isFavorite = obj.optBoolean("isFavorite", false)
        )
    }
}
