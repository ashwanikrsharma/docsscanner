package com.pocketscan.app.data

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Persistence layer.
 *
 *  - **PDFs** are written to the device's public Downloads folder under "PocketScan/".
 *      - Android 10+ : via MediaStore.Downloads (scoped storage, no permission)
 *      - Android 9 and below : via legacy public Downloads dir (WRITE_EXTERNAL_STORAGE)
 *  - **Thumbnails** stay in app-private storage. They are not user content.
 *  - **Sharing** uses the MediaStore Uri directly when available (already content://), or
 *    falls back to FileProvider for legacy paths.
 *
 * The "pdfPath" stored in Room is opaque to the rest of the app — either a content:// URI
 * string (modern) or an absolute file path (legacy / pre-Q).
 */
class DocumentFileManager(private val context: Context) {

    companion object {
        const val DOWNLOAD_SUBDIR = "PocketScan"
    }

    private val thumbnailsDir: File by lazy {
        File(context.filesDir, "thumbnails").apply { mkdirs() }
    }

    /**
     * Copy a Uri returned by the ML Kit scanner into the device's Downloads folder.
     * Returns a string identifier we persist in Room:
     *   - on Q+ : the content:// MediaStore URI
     *   - on legacy: an absolute file path
     */
    suspend fun importPdf(uri: Uri, displayName: String): String = withContext(Dispatchers.IO) {
        validateUri(uri)
        val safeName = ensurePdfExtension(displayName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            importPdfMediaStore(uri, safeName)
        } else {
            importPdfLegacy(uri, safeName)
        }
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
    private fun importPdfMediaStore(source: Uri, displayName: String): String {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val finalName = uniqueDownloadName(displayName)
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, finalName)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$DOWNLOAD_SUBDIR")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val target = resolver.insert(collection, values) ?: error("Could not create Downloads entry")
        try {
            resolver.openInputStream(source)?.use { input ->
                resolver.openOutputStream(target, "w")?.use { output ->
                    input.copyTo(output)
                } ?: error("Could not open Downloads output stream")
            } ?: error("Could not open scanner source")
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(target, values, null, null)
        } catch (t: Throwable) {
            resolver.delete(target, null, null)
            throw t
        }
        return target.toString()
    }

    private fun importPdfLegacy(source: Uri, displayName: String): String {
        @Suppress("DEPRECATION")
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val dir = File(downloads, DOWNLOAD_SUBDIR).apply { mkdirs() }
        val target = uniqueFile(dir, displayName)
        context.contentResolver.openInputStream(source)?.use { input ->
            FileOutputStream(target).use { output ->
                input.copyTo(output)
            }
        } ?: error("Could not open scanner source")
        return target.absolutePath
    }

    /** Generate a thumbnail JPEG (page 0). Returns relative path under filesDir, or null. */
    suspend fun generateThumbnail(pdfRef: String): String? = withContext(Dispatchers.IO) {
        val thumbFile = File(thumbnailsDir, "${UUID.randomUUID()}.jpg")
        runCatching {
            openPdf(pdfRef).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (renderer.pageCount == 0) return@withContext null
                    renderer.openPage(0).use { page ->
                        val width = 480
                        val height = (width.toFloat() / page.width * page.height).toInt().coerceAtLeast(1)
                        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        FileOutputStream(thumbFile).use { out ->
                            bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
                        }
                        bmp.recycle()
                    }
                }
            }
            relativeFilesPath(thumbFile)
        }.getOrNull()
    }

    suspend fun pageCount(pdfRef: String): Int = withContext(Dispatchers.IO) {
        runCatching {
            openPdf(pdfRef).use { pfd -> PdfRenderer(pfd).use { it.pageCount } }
        }.getOrDefault(0)
    }

    suspend fun fileSize(pdfRef: String): Long = withContext(Dispatchers.IO) {
        runCatching {
            if (pdfRef.startsWith("content://")) {
                context.contentResolver.openFileDescriptor(Uri.parse(pdfRef), "r")?.use { it.statSize } ?: 0L
            } else {
                File(pdfRef).takeIf { it.exists() }?.length() ?: 0L
            }
        }.getOrDefault(0L)
    }

    /** Delete the PDF (Downloads). Returns true if the PDF is gone. */
    suspend fun deletePdf(pdfRef: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            if (pdfRef.startsWith("content://")) {
                context.contentResolver.delete(Uri.parse(pdfRef), null, null) > 0
            } else {
                val f = File(pdfRef)
                !f.exists() || f.delete()
            }
        }.getOrDefault(false)
    }

    suspend fun deleteThumbnail(relativePath: String): Boolean = withContext(Dispatchers.IO) {
        val f = File(context.filesDir, relativePath)
        !f.exists() || f.delete()
    }

    fun resolveThumbnail(relativePath: String): File = File(context.filesDir, relativePath)

    /** Rename the underlying file/MediaStore entry. Returns the new pdfRef. Throws on failure. */
    suspend fun renamePdf(pdfRef: String, newDisplayName: String): String = withContext(Dispatchers.IO) {
        val safeName = ensurePdfExtension(newDisplayName)
        if (pdfRef.startsWith("content://")) {
            val uri = Uri.parse(pdfRef)
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, safeName)
            }
            val rows = context.contentResolver.update(uri, values, null, null)
            if (rows <= 0) error("Rename failed")
            pdfRef
        } else {
            val src = File(pdfRef)
            val dst = uniqueFile(src.parentFile ?: src, safeName)
            if (!src.renameTo(dst)) error("Rename failed")
            dst.absolutePath
        }
    }

    /** A URI other apps can read via share/open intent (with FLAG_GRANT_READ_URI_PERMISSION). */
    fun shareUri(pdfRef: String): Uri {
        return if (pdfRef.startsWith("content://")) {
            Uri.parse(pdfRef)
        } else {
            val file = File(pdfRef)
            val authority = "${context.packageName}.fileprovider"
            FileProvider.getUriForFile(context, authority, file)
        }
    }

    /** Strip path separators and reserved chars; cap length. Does NOT append .pdf. */
    fun sanitizeName(raw: String): String {
        val trimmed = raw.trim()
        val cleaned = trimmed
            .replace(Regex("""[\\/:*?"<>|]"""), "")
            .replace("..", "")
        return cleaned.take(80)
    }

    private fun ensurePdfExtension(raw: String): String {
        val base = sanitizeName(raw).ifBlank { "Scan" }
        return if (base.lowercase().endsWith(".pdf")) base else "$base.pdf"
    }

    private fun openPdf(pdfRef: String): ParcelFileDescriptor {
        return if (pdfRef.startsWith("content://")) {
            context.contentResolver.openFileDescriptor(Uri.parse(pdfRef), "r")
                ?: error("Could not open content URI")
        } else {
            ParcelFileDescriptor.open(File(pdfRef), ParcelFileDescriptor.MODE_READ_ONLY)
        }
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
    private fun uniqueDownloadName(name: String): String {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val relPath = "${Environment.DIRECTORY_DOWNLOADS}/$DOWNLOAD_SUBDIR/"
        val (base, ext) = splitExtension(name)
        var candidate = name
        var i = 1
        while (existsInDownloads(resolver, collection, relPath, candidate)) {
            candidate = "$base ($i)$ext"
            i++
            if (i > 999) break
        }
        return candidate
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
    private fun existsInDownloads(
        resolver: ContentResolver,
        collection: Uri,
        relPath: String,
        displayName: String,
    ): Boolean {
        val projection = arrayOf(MediaStore.Downloads._ID)
        val selection = "${MediaStore.Downloads.RELATIVE_PATH}=? AND ${MediaStore.Downloads.DISPLAY_NAME}=?"
        val args = arrayOf(relPath, displayName)
        resolver.query(collection, projection, selection, args, null)?.use { c ->
            return c.moveToFirst()
        }
        return false
    }

    private fun uniqueFile(dir: File, displayName: String): File {
        val (base, ext) = splitExtension(displayName)
        var candidate = File(dir, displayName)
        var i = 1
        while (candidate.exists()) {
            candidate = File(dir, "$base ($i)$ext")
            i++
            if (i > 999) break
        }
        return candidate
    }

    private fun splitExtension(name: String): Pair<String, String> {
        val dot = name.lastIndexOf('.')
        return if (dot > 0) name.substring(0, dot) to name.substring(dot) else name to ""
    }

    private fun relativeFilesPath(file: File): String {
        val base = context.filesDir.absolutePath
        require(file.absolutePath.startsWith(base)) { "File escapes filesDir" }
        return file.absolutePath.removePrefix("$base/")
    }

    private fun validateUri(uri: Uri) {
        val scheme = uri.scheme
        require(scheme == ContentResolver.SCHEME_CONTENT || scheme == ContentResolver.SCHEME_FILE) {
            "Unsupported URI scheme: $scheme"
        }
    }
}
