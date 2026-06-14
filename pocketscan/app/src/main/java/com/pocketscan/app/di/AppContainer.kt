package com.pocketscan.app.di

import android.content.Context
import com.pocketscan.app.data.AppDatabase
import com.pocketscan.app.data.DocumentFileManager
import com.pocketscan.app.data.DocumentRepository
import com.pocketscan.app.scanner.DocumentScannerManager

/**
 * Minimal manual DI to keep the build light (no Hilt/Dagger KSP processors beyond Room).
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database: AppDatabase = AppDatabase.build(appContext)
    val fileManager: DocumentFileManager = DocumentFileManager(appContext)
    val repository: DocumentRepository = DocumentRepository(database.documentDao(), fileManager)
    val scannerManager: DocumentScannerManager = DocumentScannerManager()
}
