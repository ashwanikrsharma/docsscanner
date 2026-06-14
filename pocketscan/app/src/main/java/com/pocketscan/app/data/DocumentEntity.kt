package com.pocketscan.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pocketscan.app.domain.Document

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val pdfPath: String,
    val createdAt: Long,
    val updatedAt: Long,
    val pageCount: Int,
    val fileSizeBytes: Long,
    val thumbnailPath: String?,
) {
    fun toDomain(): Document = Document(
        id = id,
        name = name,
        pdfPath = pdfPath,
        createdAt = createdAt,
        updatedAt = updatedAt,
        pageCount = pageCount,
        fileSizeBytes = fileSizeBytes,
        thumbnailPath = thumbnailPath,
    )
}
