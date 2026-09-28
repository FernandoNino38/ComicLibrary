package com.example.comiclibrary.data.worker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.comiclibrary.data.archive.CbzArchiveManager
import com.example.comiclibrary.data.repository.ComicRepository
import com.example.comiclibrary.di.AppDispatchers
import kotlinx.coroutines.withContext

/**
 * Resilient WorkManager background worker for bulk CBZ folder indexing (Report Section 8).
 * Runs on [AppDispatchers.libraryIndexing] with thread limits to prevent memory and CPU spikes.
 * Emits progress data and gracefully reacts to system preemption or cancellation.
 */
class CbzIndexWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_TREE_URI = "key_tree_uri"
        const val KEY_PROGRESS_CURRENT = "key_progress_current"
        const val KEY_PROGRESS_TOTAL = "key_progress_total"
        const val TAG = "CbzIndexWorker"
    }

    override suspend fun doWork(): Result = withContext(AppDispatchers.libraryIndexing) {
        val treeUriString = inputData.getString(KEY_TREE_URI) ?: return@withContext Result.failure()
        val treeUri = Uri.parse(treeUriString)

        try {
            // Persist read permissions
            try {
                applicationContext.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}

            val rootDoc = DocumentFile.fromTreeUri(applicationContext, treeUri)
                ?: return@withContext Result.failure()

            val cbzUris = mutableListOf<Uri>()
            collectCbzFiles(rootDoc, cbzUris)

            if (cbzUris.isEmpty()) {
                return@withContext Result.success()
            }

            val repository = ComicRepository(applicationContext)
            val total = cbzUris.size

            setProgress(workDataOf(KEY_PROGRESS_CURRENT to 0, KEY_PROGRESS_TOTAL to total))

            // Batch import via repository
            repository.importComics(cbzUris)

            setProgress(workDataOf(KEY_PROGRESS_CURRENT to total, KEY_PROGRESS_TOTAL to total))
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Background indexing failed: ${e.message}", e)
            Result.failure()
        }
    }

    private fun collectCbzFiles(dir: DocumentFile, result: MutableList<Uri>) {
        if (isStopped) return
        val files = dir.listFiles()
        for (file in files) {
            if (isStopped) return
            if (file.isDirectory) {
                collectCbzFiles(file, result)
            } else {
                val name = file.name?.lowercase() ?: ""
                if (name.endsWith(".cbz") || name.endsWith(".zip")) {
                    result.add(file.uri)
                }
            }
        }
    }
}
