package com.pocketscan.app.data

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.pocketscan.app.domain.Document
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DocumentRepository(
    private val dao: DocumentDao,
    private val files: DocumentFileManager,
) {
    fun observeAll(): Flow<List<Document>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun get(id: Long): Document? = dao.findById(id)?.toDomain()

    /** Default scan name based on current time. Public so the UI can pre-fill the Save dialog. */
    fun buildDefaultScanName(): String = buildDefaultName()

    /**
     * Import a scanner Uri, persist to public Downloads, and create the metadata row.
     * If metadata insert fails after file copy, the copied PDF is cleaned up.
     */
    suspend fun importFromScanner(uri: Uri, defaultName: String? = null): Document {
        val rawName = defaultName ?: buildDefaultName()
        val safeName = files.sanitizeName(rawName).ifBlank { buildDefaultName() }
        val pdfRef = files.importPdf(uri, safeName)
        try {
            val pageCount = files.pageCount(pdfRef)
            val size = files.fileSize(pdfRef)
            val thumbnail = files.generateThumbnail(pdfRef)
            val now = System.currentTimeMillis()
            val entity = DocumentEntity(
                name = safeName,
                pdfPath = pdfRef,
                createdAt = now,
                updatedAt = now,
                pageCount = pageCount,
                fileSizeBytes = size,
                thumbnailPath = thumbnail,
            )
            val id = dao.insert(entity)
            return entity.copy(id = id).toDomain()
        } catch (t: Throwable) {
            files.deletePdf(pdfRef)
            throw t
        }
    }

    suspend fun rename(id: Long, newName: String): Result<Unit> {
        val safe = files.sanitizeName(newName)
        if (safe.isBlank()) return Result.failure(IllegalArgumentException("empty"))
        val current = dao.findById(id) ?: return Result.failure(IllegalStateException("not found"))
        // Try to rename the underlying file too. If filesystem rename fails (e.g., MediaStore
        // permission edge case), fall back to renaming metadata only.
        val newRef = runCatching { files.renamePdf(current.pdfPath, safe) }.getOrNull() ?: current.pdfPath
        val updated = dao.update(
            current.copy(
                name = safe,
                pdfPath = newRef,
                updatedAt = System.currentTimeMillis(),
            )
        )
        return if (updated > 0) Result.success(Unit) else Result.failure(IllegalStateException("not found"))
    }

    suspend fun delete(id: Long): Result<Unit> {
        val doc = dao.findById(id) ?: return Result.failure(IllegalStateException("not found"))
        val fileGone = files.deletePdf(doc.pdfPath)
        doc.thumbnailPath?.let { files.deleteThumbnail(it) }
        if (!fileGone) return Result.failure(IllegalStateException("file"))
        dao.delete(id)
        return Result.success(Unit)
    }

    /**
     * Human-readable on-device location for the Details screen, e.g.
     *   "Downloads/PocketScan/Scan 2026-06-14 19-23.pdf"
     * Falls back to the raw absolute path for legacy file refs.
     */
    fun describeLocation(context: Context, pdfRef: String): String {
        return if (pdfRef.startsWith("content://")) {
            val uri = Uri.parse(pdfRef)
            val name = queryDisplayName(context, uri) ?: "scan.pdf"
            "Downloads/${DocumentFileManager.DOWNLOAD_SUBDIR}/$name"
        } else {
            val f = File(pdfRef)
            val downloads = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
            } else null
            if (downloads != null && f.absolutePath.startsWith(downloads)) {
                "Downloads${f.absolutePath.removePrefix(downloads)}"
            } else {
                f.absolutePath
            }
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(
                uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
            )?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()
    }

    private fun buildDefaultName(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH-mm", Locale.US)
        return "Scan ${fmt.format(Date())}"
    }
}
