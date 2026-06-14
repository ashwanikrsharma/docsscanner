package com.pocketscan.app.scanner

import android.app.Activity
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

/**
 * Thin wrapper around GmsDocumentScanning so the UI layer doesn't depend on ML Kit types directly.
 */
class DocumentScannerManager {

    private val options: GmsDocumentScannerOptions = GmsDocumentScannerOptions.Builder()
        .setGalleryImportAllowed(true)
        .setPageLimit(20)
        .setResultFormats(
            GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
            GmsDocumentScannerOptions.RESULT_FORMAT_PDF,
        )
        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
        .build()

    fun startScanIntent(activity: Activity): Task<android.content.IntentSender> =
        GmsDocumentScanning.getClient(options).getStartScanIntent(activity)

    fun parseResult(activityResult: android.content.Intent?): GmsDocumentScanningResult? =
        GmsDocumentScanningResult.fromActivityResultIntent(activityResult)

    fun toRequest(sender: android.content.IntentSender): IntentSenderRequest =
        IntentSenderRequest.Builder(sender).build()
}
