package com.example.comiclibrary.data.archive

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.comiclibrary.data.model.ComicBook
import com.example.comiclibrary.data.model.ComicMetadata
import com.example.comiclibrary.data.model.ComicPage
import com.example.comiclibrary.data.parser.ComicInfoXmlParser
import com.example.comiclibrary.data.parser.FilenameMetadataParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

/**
 * Robust, Scoped-Storage compliant CBZ Archive Manager.
 * Handles single-file and folder imports via Storage Access Framework (SAF).
 * Extracts metadata and cover thumbnails safely, avoiding SELinux /proc/self/fd restrictions.
 */
object CbzArchiveManager {

    private const val TAG = "CbzArchiveManager"
    private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "avif", "gif")

    /**
     * Inspects a CBZ archive via ContentResolver or File URI,
     * extracts ComicInfo.xml metadata (with filename fallback),
     * and extracts the cover thumbnail into internal app storage.
     */
    suspend fun inspectCbz(
        context: Context,
        uri: Uri,
        fileName: String,
        fileSize: Long
    ): ComicBook? = withContext(Dispatchers.IO) {
        val comicId = UUID.nameUUIDFromBytes((uri.toString() + fileName).toByteArray()).toString()
        val pages = mutableListOf<String>()
        var metadata = ComicMetadata()
        var comicInfoBytes: ByteArray? = null
        var coverBytes: ByteArray? = null

        val coversDir = File(context.filesDir, "covers").apply { if (!exists()) mkdirs() }
        val coverFile = File(coversDir, "$comicId.jpg")

        try {
            // If already local file scheme
            if (uri.scheme == "file") {
                val file = File(uri.path ?: "")
                if (file.exists() && file.canRead()) {
                    ZipFile(file).use { zip ->
                        val entries = zip.entries().asSequence().toList()
                        val sortedImageEntries = entries.filter { isImageEntry(it.name) }
                            .sortedWith(AlphanumericComparator)

                        sortedImageEntries.forEach { pages.add(it.name) }

                        // Extract ComicInfo.xml if present
                        val xmlEntry = entries.firstOrNull { it.name.endsWith("ComicInfo.xml", ignoreCase = true) }
                        if (xmlEntry != null) {
                            zip.getInputStream(xmlEntry).use { stream ->
                                metadata = ComicInfoXmlParser.parse(stream)
                            }
                        }

                        // Extract cover thumbnail
                        if (sortedImageEntries.isNotEmpty() && !coverFile.exists()) {
                            zip.getInputStream(sortedImageEntries.first()).use { input ->
                                FileOutputStream(coverFile).use { output -> input.copyTo(output) }
                            }
                        }
                    }
                }
            } else {
                // Content URI (SAF Scoped Storage): Stream zip entries safely
                context.contentResolver.openInputStream(uri)?.use { rawStream ->
                    ZipInputStream(BufferedInputStream(rawStream)).use { zipStream ->
                        var entry = zipStream.nextEntry
                        while (entry != null) {
                            val name = entry.name
                            if (!entry.isDirectory && !name.startsWith("__MACOSX") && !name.contains("/.")) {
                                if (name.endsWith("ComicInfo.xml", ignoreCase = true)) {
                                    comicInfoBytes = zipStream.readBytes()
                                } else if (isImageEntry(name)) {
                                    pages.add(name)
                                    // If we haven't captured cover bytes yet, capture from the first image
                                    if (coverBytes == null && !coverFile.exists()) {
                                        coverBytes = zipStream.readBytes()
                                    }
                                }
                            }
                            entry = zipStream.nextEntry
                        }
                    }
                }

                // Parse XML if found
                if (comicInfoBytes != null) {
                    try {
                        ByteArrayInputStream(comicInfoBytes).use { stream ->
                            metadata = ComicInfoXmlParser.parse(stream)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed parsing ComicInfo.xml for $fileName: ${e.message}")
                    }
                }

                // Save cover file if extracted
                if (coverBytes != null && !coverFile.exists()) {
                    try {
                        FileOutputStream(coverFile).use { it.write(coverBytes) }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed writing cover thumbnail: ${e.message}")
                    }
                }
            }

            // Natural sort of pages
            pages.sortWith(StringAlphanumericComparator)

            // Fallback metadata if ComicInfo.xml wasn't present
            if (metadata.title.isBlank() && metadata.series.isBlank()) {
                metadata = FilenameMetadataParser.parse(fileName, pageCount = pages.size)
            } else if (metadata.pageCount <= 0) {
                metadata = metadata.copy(pageCount = pages.size)
            }

            val comicPages = pages.mapIndexed { index, name ->
                ComicPage(index = index, entryName = name)
            }

            if (comicPages.isEmpty()) {
                Log.w(TAG, "No pages found for $fileName, aborting import.")
                return@withContext null
            }

            ComicBook(
                id = comicId,
                uriString = uri.toString(),
                fileName = fileName,
                fileSizeBytes = fileSize,
                coverPath = if (coverFile.exists()) coverFile.absolutePath else null,
                pages = comicPages,
                metadata = metadata,
                lastOpenedTimestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error inspecting CBZ archive $fileName: ${e.message}", e)
            null
        }
    }

    /**
     * Prepares an active reading session by creating a temporary copy in cacheDir.
     * This provides true O(1) random-access for ZipFile and BitmapRegionDecoder during reading.
     */
    suspend fun prepareReadingSession(context: Context, uri: Uri): File? = withContext(Dispatchers.IO) {
        try {
            if (uri.scheme == "file") {
                val f = File(uri.path ?: "")
                if (f.exists()) return@withContext f
            }

            val sessionFile = File(context.cacheDir, "current_reading.cbz")
            if (sessionFile.exists()) sessionFile.delete()

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(sessionFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (sessionFile.exists() && sessionFile.length() > 0) sessionFile else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to prepare reading session for $uri: ${e.message}", e)
            null
        }
    }

    /**
     * Retrieves the uncompressed byte array of a single page entry.
     * Uses session ZipFile for O(1) random access when provided, or streams as fallback.
     */
    suspend fun getPageBytes(
        context: Context,
        uri: Uri,
        entryName: String,
        sessionZipFile: ZipFile? = null
    ): ByteArray? = withContext(Dispatchers.IO) {
        try {
            // Fast-path: O(1) random-access from session ZipFile
            if (sessionZipFile != null) {
                val entry = sessionZipFile.getEntry(entryName)
                if (entry != null) {
                    return@withContext sessionZipFile.getInputStream(entry).use { it.readBytes() }
                }
            }

            // Fallback: direct streaming
            context.contentResolver.openInputStream(uri)?.use { raw ->
                ZipInputStream(BufferedInputStream(raw)).use { zipStream ->
                    var entry = zipStream.nextEntry
                    while (entry != null) {
                        if (entry.name == entryName) {
                            return@withContext zipStream.readBytes()
                        }
                        entry = zipStream.nextEntry
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading page $entryName: ${e.message}", e)
        }
        null
    }

    private fun isImageEntry(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in IMAGE_EXTENSIONS
    }

    private object AlphanumericComparator : Comparator<ZipEntry> {
        override fun compare(e1: ZipEntry, e2: ZipEntry): Int {
            return StringAlphanumericComparator.compare(e1.name, e2.name)
        }
    }

    private object StringAlphanumericComparator : Comparator<String> {
        private val SPLIT_REGEX = Regex("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)")

        override fun compare(s1: String, s2: String): Int {
            val parts1 = s1.split(SPLIT_REGEX)
            val parts2 = s2.split(SPLIT_REGEX)
            val minLength = minOf(parts1.size, parts2.size)

            for (i in 0 until minLength) {
                val p1 = parts1[i]
                val p2 = parts2[i]
                val num1 = p1.toLongOrNull()
                val num2 = p2.toLongOrNull()

                val comp = if (num1 != null && num2 != null) {
                    num1.compareTo(num2)
                } else {
                    p1.compareTo(p2, ignoreCase = true)
                }
                if (comp != 0) return comp
            }
            return parts1.size.compareTo(parts2.size)
        }
    }
}
